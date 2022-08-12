package com.zyj.gulimall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.zyj.common.to.mq.SeckillOrderTo;
import com.zyj.common.utils.R;
import com.zyj.common.vo.MemberRespVo;
import com.zyj.gulimall.seckill.feign.CouponFeignService;
import com.zyj.gulimall.seckill.feign.ProductFeignService;
import com.zyj.gulimall.seckill.interceptor.LoginUserInterceptor;
import com.zyj.gulimall.seckill.service.SeckillService;
import com.zyj.gulimall.seckill.to.SeckillSkuRedisTo;
import com.zyj.gulimall.seckill.vo.SeckillSessionsWithSkus;
import com.zyj.gulimall.seckill.vo.SkuInfoVo;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Zyj
 * @date 2022/7/16
 */
@Service
public class SeckillServiceImpl implements SeckillService {

    public static final String SKU_STOCK_SEMAPHORE = "seckill:stock:";//+商品随机码

    public static final String SKUKILL_CACHE_PREFIX = "seckill:skus:";

    public static final String SESSIONS_CACHE_PREFIX = "seckill:sessions:";

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private CouponFeignService couponFeignService;

    @Autowired
    private StringRedisTemplate redisTemplate;


    /**
     * 上架最近3内的商品
     */
    @Override
    public void uploadSeckillSkuLatest3Days() {
        //1.扫描需要参与秒杀的活动
        R r = couponFeignService.getLatest3DaySession();
        if (r.getCode() == 0) {
            //上架商品
            List<SeckillSessionsWithSkus> data = r.getData(new TypeReference<List<SeckillSessionsWithSkus>>() {
            });
            //缓存到redis
            //1.活动信息
            saveSessionInfos(data);

            //2.活动关联的商品信息
            saveSessionSkuInfos(data);

        }

    }


    private void saveSessionInfos(List<SeckillSessionsWithSkus> sessions) {
        sessions.stream().forEach(session -> {
            Long startTime = session.getStartTime().getTime();
            Long endTime = session.getEndTime().getTime();
            String key = SESSIONS_CACHE_PREFIX + startTime + "_" + endTime;
            List<String> skuIds = session.getRelationSkus().stream().map(sku -> {
                return sku.getPromotionSessionId().toString() + "_" + sku.getSkuId().toString();
            }).collect(Collectors.toList());

            Boolean hasKey = redisTemplate.hasKey(key);
            if (!hasKey) {
                //缓存活动信息
                redisTemplate.opsForList().leftPushAll(key, skuIds);
            }
        });
    }


    private void saveSessionSkuInfos(List<SeckillSessionsWithSkus> sessions) {
        sessions.stream().forEach(session -> {
            BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
            session.getRelationSkus().stream().forEach(sku -> {
                //4.商品随机码
                String token = UUID.randomUUID().toString().replace("-", "");

                Boolean hasSku = ops.hasKey(sku.getPromotionSessionId().toString() + "_" + sku.getSkuId().toString());
                if (!hasSku) {

                    //缓存商品
                    SeckillSkuRedisTo to = new SeckillSkuRedisTo();
                    //1.sku的基本信息
                    R r = productFeignService.skuInfo(sku.getSkuId());
                    if (r.getCode() == 0) {
                        SkuInfoVo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                        });
                        to.setSkuInfoVo(skuInfo);
                    }

                    //2.sku的秒杀信息
                    BeanUtils.copyProperties(sku, to);

                    //3.设置商品的秒杀时间信息
                    to.setStartTime(session.getStartTime().getTime());
                    to.setEndTime(session.getEndTime().getTime());

                    to.setRandomCode(token);

                    String skuJson = JSON.toJSONString(to);
                    ops.put(sku.getPromotionSessionId().toString() + "_" + sku.getSkuId().toString(), skuJson);


                    //5.引入库存作为分布式;信号量
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                    //商品的秒杀数量
                    semaphore.trySetPermits(sku.getSeckillCount().intValue());

                }
            });
        });
    }


    /**
     * 获取当前可以参与秒杀的商品
     *
     * @return
     */
    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
        //1.确定当前时间属于哪个场次
        long time = new Date().getTime();
        Set<String> keys = redisTemplate.keys(SESSIONS_CACHE_PREFIX + "*");
        for (String key : keys) {
            String[] s = key.replace(SESSIONS_CACHE_PREFIX, "").split("_");
            long startTime = Long.parseLong(s[0]);
            long endTime = Long.parseLong(s[1]);
            if (time >= startTime && time <= endTime) {
                //2.获取该秒杀场次需要的所有商品信息
                List<String> skus = redisTemplate.opsForList().range(key, 0, -1);
                BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                List<String> list = hashOps.multiGet(skus);
                if (list != null) {
                    List<SeckillSkuRedisTo> res = list.stream().map(item -> {
                        SeckillSkuRedisTo redisTo = JSON.parseObject((String) item, SeckillSkuRedisTo.class);
                        //redisTo.setRandomCode(null); 当前秒杀开始需要随机码
                        return redisTo;
                    }).collect(Collectors.toList());
                    return res;
                }
                break;
            }
        }

        return null;
    }

    /**
     * 获取对应sku秒杀信息
     *
     * @param skuId
     * @return
     */
    @Override
    public SeckillSkuRedisTo getSkuSeckillInfo(Long skuId) {
        //1.找到所有参与秒杀的key信息
        BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        Set<String> keys = ops.keys();
        if (keys != null && keys.size() > 0) {
            String regx = "\\d_" + skuId;
            for (String key : keys) {
                //2-1
                if (Pattern.matches(regx, key)) {
                    String s = ops.get(key);
                    SeckillSkuRedisTo to = JSON.parseObject(s, SeckillSkuRedisTo.class);
                    Long startTime = to.getStartTime();
                    Long endTime = to.getEndTime();
                    long curr = new Date().getTime();
                    if (curr >= startTime && curr <= endTime) {

                    } else {
                        to.setRandomCode(null);
                    }
                    return to;
                }
            }
        }

        return null;
    }


    /**
     * 秒杀业务
     *
     * @param killId
     * @param key
     * @param num
     * @return
     */
    @Override
    public String kill(String killId, String key, Integer num) {
        MemberRespVo loginUser = LoginUserInterceptor.loginUser.get();

        //1.获取当前秒杀商品的详细信息
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        String s = hashOps.get(killId);
        if (StringUtils.isEmpty(s)) {
            return null;
        } else {
            SeckillSkuRedisTo to = JSON.parseObject(s, new TypeReference<SeckillSkuRedisTo>() {
            });
            //校验合法性
            Long startTime = to.getStartTime();
            Long endTime = to.getEndTime();
            long time = new Date().getTime();
            //1.校验时间合法性
            if (time < startTime || time > endTime) {
                return null;
            } else {
                //2.校验随机码和商品id是否正确
                String code = to.getRandomCode();
                String skuId = to.getPromotionSessionId() + "_" + to.getSkuId();
                if (code.equals(key) && killId.equals(skuId)) {
                    //3.验证购物数量是否合理
                    BigDecimal limit = to.getSeckillLimit();
                    if (num <= limit.intValue()) {
                        //4.验证用户是否已经参与秒杀
                        String userKey = loginUser.getId() + "_" + killId;
                        //设置自动过期
                        long ttl = endTime - startTime;
                        Boolean success = redisTemplate.opsForValue().setIfAbsent(userKey, num.toString(), ttl, TimeUnit.MILLISECONDS);
                        if (success) {
                            //占位成功说明第一次抢购
                            RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + code);

                            boolean b = semaphore.tryAcquire(num);
                            if (b) {
                                //秒杀成功快速下单
                                String timeId = String.valueOf(IdWorker.getId());
                                SeckillOrderTo orderTo = new SeckillOrderTo();
                                orderTo.setOrderSn(timeId);
                                orderTo.setMemberId(loginUser.getId());
                                orderTo.setNum(new BigDecimal(num));
                                orderTo.setSkuId(to.getSkuId());
                                orderTo.setPromotionSessionId(to.getPromotionSessionId());
                                orderTo.setSeckillPrice(to.getSeckillPrice());
                                rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", orderTo);
                                return timeId;
                            }

                        } else {
                            return null;
                        }
                    }
                } else {
                    return null;
                }
            }
        }


        return null;
    }
}

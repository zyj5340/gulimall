package com.zyj.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.zyj.common.utils.R;
import com.zyj.gulimall.ware.feign.MemberFeignService;
import com.zyj.gulimall.ware.vo.FareVo;
import com.zyj.gulimall.ware.vo.MemberAddressVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyj.common.utils.PageUtils;
import com.zyj.common.utils.Query;

import com.zyj.gulimall.ware.dao.WareInfoDao;
import com.zyj.gulimall.ware.entity.WareInfoEntity;
import com.zyj.gulimall.ware.service.WareInfoService;
import org.springframework.util.StringUtils;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {
    
    @Autowired
    private MemberFeignService memberFeignService;
    
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareInfoEntity> wrapper = new QueryWrapper<>();

        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            wrapper.eq("id",key).or().like("name",key)
                    .or().like("address",key)
                    .or().like("areacode",key);
        }

        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    /**
     * 根据收货地址计算运费
     *
     * @param addrId
     * @return
     */
    @Override
    public FareVo getFare(Long addrId) {
        R info = memberFeignService.addrInfo(addrId);
        MemberAddressVo address = info.getData("memberReceiveAddress",new TypeReference<MemberAddressVo>() {
        });
        if (address != null){
            String phone = address.getPhone();
            String s = phone.substring(phone.length() - 2);
            BigDecimal fare = new BigDecimal(s);
            FareVo fareVo = new FareVo();
            fareVo.setAddress(address);
            fareVo.setFare(fare);
            return fareVo;
        }
        return null;
    }

}
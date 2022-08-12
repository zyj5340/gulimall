package com.zyj.gulimall.member.service.impl;

import com.zyj.gulimall.member.dao.MemberLevelDao;
import com.zyj.gulimall.member.entity.MemberLevelEntity;
import com.zyj.gulimall.member.exception.PhoneExistException;
import com.zyj.gulimall.member.exception.UserNameExistException;
import com.zyj.gulimall.member.vo.MemberLoginVo;
import com.zyj.gulimall.member.vo.MemberRegistVo;
import com.zyj.gulimall.member.vo.SocialUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyj.common.utils.PageUtils;
import com.zyj.common.utils.Query;

import com.zyj.gulimall.member.dao.MemberDao;
import com.zyj.gulimall.member.entity.MemberEntity;
import com.zyj.gulimall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {
    @Autowired
    private MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }


    /**
     * 用户注册
     * @param vo
     */
    @Override
    public void regist(MemberRegistVo vo) {
        MemberEntity entity = new MemberEntity();
        //设置会员默认等级
        MemberLevelEntity levelEntity = memberLevelDao.getDefaultLevel();
        entity.setLevelId(levelEntity.getId());

        //检查用户名和手机号的唯一性,让controller感知异常 使用异常机制
        checkPhoneUnique(vo.getPhone());
        checkUserNameUnique(vo.getUserName());

        entity.setMobile(vo.getPhone());
        entity.setUsername(vo.getUserName());

        //密码加密存储
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(vo.getPassword());
        entity.setPassword(encode);

        //TODO 其他默认信息
        entity.setNickname(vo.getUserName());

        //保存
        baseMapper.insert(entity);
    }


    /**
     * 用户登陆
     * @param vo
     * @return
     */
    @Override
    public MemberEntity login(MemberLoginVo vo) {
        String loginacct = vo.getLoginacct();
        String password = vo.getPassword();
        //1.数据库查询
        MemberEntity entity = baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("username", loginacct).or().
                eq("mobile", loginacct));
        if (entity == null){
            //失败
            return null;
        }else {
            String password1 = entity.getPassword();
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            //2.密码匹配
            boolean matches = passwordEncoder.matches(password, password1);
            if (matches){
                return entity;
            }else {
                return null;
            }
        }
    }

    /**
     * 社交登陆
     * @param socialUser
     * @return
     */
    @Override
    public MemberEntity login(SocialUser socialUser) {
        String uid = socialUser.getId();
        //1.判断当前社交用户是否已经登陆过系统
        MemberEntity entity = baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uid));
        if (entity != null){
            //该用户已注册
            /**************************此处不同weibo**************************/
            MemberEntity update = new MemberEntity();
            update.setId(entity.getId());
            update.setAccessToken(socialUser.getAccessToken());
            update.setExpiresIn(socialUser.getExpiresIn());
            baseMapper.updateById(update);

            entity.setAccessToken(socialUser.getAccessToken());
            entity.setExpiresIn(socialUser.getExpiresIn());
            return entity;
        }else {
            //2.没有查询到当前社交用户uid
            MemberEntity regist = new MemberEntity();
            regist.setNickname(socialUser.getName());
            regist.setSocialUid(uid);
            regist.setAccessToken(socialUser.getAccessToken());
            regist.setExpiresIn(socialUser.getExpiresIn());
            regist.setCreateTime(new Date());
            baseMapper.insert(regist);
            return regist;
        }
    }


    @Override
    public void checkPhoneUnique(String phone) throws PhoneExistException {
        Integer count = baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if (count > 0){
            throw new PhoneExistException();
        }
    }


    @Override
    public void checkUserNameUnique(String userName) throws UserNameExistException{
        Integer count = baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", userName));
        if (count > 0){
            throw new UserNameExistException();
        }
    }




}
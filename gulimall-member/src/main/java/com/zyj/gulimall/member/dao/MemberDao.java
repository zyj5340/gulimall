package com.zyj.gulimall.member.dao;

import com.zyj.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author YongjieZhang
 * @email 534054720@qq.com
 * @date 2022-04-30 17:15:17
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}

package com.limou.movieticket.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limou.movieticket.auth.domain.AppUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppUserMapper extends BaseMapper<AppUser> {
}

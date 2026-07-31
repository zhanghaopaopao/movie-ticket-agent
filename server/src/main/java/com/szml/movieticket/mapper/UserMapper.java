package com.szml.movieticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.szml.movieticket.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 Mapper。
 *
 * @author zhanghao
 * @since 2026-07-30
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}

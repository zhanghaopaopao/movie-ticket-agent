package com.szml.movieticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.szml.movieticket.entity.UserPreference;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户观影偏好 Mapper。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
@Mapper
public interface UserPreferenceMapper extends BaseMapper<UserPreference> {
}

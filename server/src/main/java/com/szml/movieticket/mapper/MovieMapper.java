package com.szml.movieticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.szml.movieticket.entity.Movie;
import org.apache.ibatis.annotations.Mapper;

/**
 * 影片 Mapper。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Mapper
public interface MovieMapper extends BaseMapper<Movie> {
}

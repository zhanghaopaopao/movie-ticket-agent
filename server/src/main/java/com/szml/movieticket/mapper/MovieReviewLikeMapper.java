package com.szml.movieticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.szml.movieticket.entity.MovieReviewLike;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MovieReviewLikeMapper extends BaseMapper<MovieReviewLike> {

    @Insert("""
            INSERT INTO movie_review_like (review_id, user_id, create_time)
            VALUES (#{reviewId}, #{userId}, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE id = id
            """)
    int insertIgnore(@Param("reviewId") Long reviewId, @Param("userId") Long userId);
}

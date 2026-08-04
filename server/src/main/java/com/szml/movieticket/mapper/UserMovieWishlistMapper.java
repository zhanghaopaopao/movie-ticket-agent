package com.szml.movieticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.szml.movieticket.entity.UserMovieWishlist;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMovieWishlistMapper extends BaseMapper<UserMovieWishlist> {

    @Insert("""
            INSERT INTO user_movie_wishlist (user_id, movie_id, create_time)
            VALUES (#{userId}, #{movieId}, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE id = id
            """)
    int insertIgnore(@Param("userId") Long userId, @Param("movieId") Long movieId);
}

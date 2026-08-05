package com.szml.movieticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_movie_wishlist")
public class UserMovieWishlist {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long movieId;

    private LocalDateTime createTime;
}

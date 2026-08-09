package com.szml.movieticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户已看过影片记录。 */
@Data
@TableName("user_movie_watched")
public class UserMovieWatched {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long movieId;
    private LocalDateTime createTime;
}

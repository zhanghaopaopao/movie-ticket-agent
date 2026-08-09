package com.szml.movieticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 影片评论。 */
@Data
@TableName("movie_review")
public class MovieReview {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long movieId;

    private Long userId;

    /** 父影评 ID，空表示顶级影评。 */
    private Long parentId;

    private String content;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

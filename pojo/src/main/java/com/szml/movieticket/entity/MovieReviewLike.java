package com.szml.movieticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户对影片评论的点赞记录。 */
@Data
@TableName("movie_review_like")
public class MovieReviewLike {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reviewId;

    private Long userId;

    private LocalDateTime createTime;
}

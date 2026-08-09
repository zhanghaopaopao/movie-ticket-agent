package com.szml.movieticket.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** C端影片评论。 */
@Data
public class MovieReviewVO {

    private Long id;
    private Long movieId;
    private Long parentId;
    private String content;
    private String authorName;
    private String authorAvatarUrl;
    private int likeCount;
    private boolean liked;
    private boolean mine;
    private LocalDateTime createTime;
}

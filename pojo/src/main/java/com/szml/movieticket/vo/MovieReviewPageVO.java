package com.szml.movieticket.vo;

import lombok.Data;

import java.util.List;

/** 影片评论分页响应。 */
@Data
public class MovieReviewPageVO {

    private long total;
    private int page;
    private int size;
    private List<MovieReviewVO> records;
}

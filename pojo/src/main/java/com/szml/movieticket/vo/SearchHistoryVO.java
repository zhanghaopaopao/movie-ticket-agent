package com.szml.movieticket.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户搜索历史返回对象。
 */
@Data
public class SearchHistoryVO {

    private Long id;

    private String keyword;

    private Integer searchCount;

    private LocalDateTime lastSearchTime;
}

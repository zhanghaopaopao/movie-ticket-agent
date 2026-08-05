package com.szml.movieticket.vo;

import lombok.Data;

import java.util.List;

/** 分页零食商品响应。 */
@Data
public class SnackProductPageVO {

    private long total;
    private int page;
    private int size;
    private List<SnackProductVO> records;
}

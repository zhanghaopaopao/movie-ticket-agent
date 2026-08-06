package com.szml.movieticket.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 零食商品响应。 */
@Data
public class SnackProductVO {

    private Long id;
    private Long cinemaId;
    private String cinemaName;
    private String name;
    private String description;
    private String image;
    private Integer priceFen;
    private Integer stock;
    private Integer soldCount;
    private Integer status;
    private String statusDesc;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

package com.szml.movieticket.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 影厅 VO。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
public class HallVO {

    private Long id;

    private Long cinemaId;

    private String name;

    private String hallType;

    private String hallTypeDesc;

    /** 总座位数 */
    private Integer totalSeats;

    private LocalDateTime createTime;
}

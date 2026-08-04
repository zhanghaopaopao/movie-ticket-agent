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

    /** 影厅ID */
    private Long id;

    /** 所属影院ID */
    private Long cinemaId;

    /** 影厅名称 */
    private String name;

    /** 厅型代码，如 IMAX、4DX */
    private String hallType;

    /** 厅型描述，如 IMAX厅 */
    private String hallTypeDesc;

    /** 状态：0=停用 1=启用 */
    private Integer status;

    /** 状态描述 */
    private String statusDesc;

    /** 总座位数 */
    private Integer totalSeats;

    /** 创建时间 */
    private LocalDateTime createTime;
}

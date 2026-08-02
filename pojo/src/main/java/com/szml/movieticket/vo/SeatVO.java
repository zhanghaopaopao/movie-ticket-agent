package com.szml.movieticket.vo;

import lombok.Data;

/**
 * 物理座位 VO。
 */
@Data
public class SeatVO {

    private Long id;
    private Long hallId;
    private Integer rowNo;
    private Integer seatNo;
    private String zone;
    private Integer seatType;
    private String seatTypeDesc;
    private Integer status;
    private String statusDesc;
}

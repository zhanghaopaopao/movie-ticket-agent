package com.szml.movieticket.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** C 端退款结果。 */
@Data
public class RefundResultVO {

    private Long orderId;

    /** SUCCESS、PENDING、FAIL。 */
    private String status;

    /** 退款金额，单位为元。 */
    private Double amount;

    /** 手续费，单位为元。 */
    private Double serviceFee;

    private String outRequestNo;

    /** 面向用户的结果说明。 */
    private String message;

    private LocalDateTime updatedAt;
}

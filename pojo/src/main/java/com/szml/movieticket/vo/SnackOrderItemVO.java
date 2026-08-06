package com.szml.movieticket.vo;

import lombok.Data;

/** 订单中展示的零食快照，金额字段单位为元。 */
@Data
public class SnackOrderItemVO {

    private Long snackId;
    private String name;
    private String image;
    private Double unitPrice;
    private Integer quantity;
    private Double amount;
    private String inventoryStatus;
}

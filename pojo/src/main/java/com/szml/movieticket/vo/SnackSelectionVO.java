package com.szml.movieticket.vo;

import lombok.Data;

import java.util.List;

/** 待支付订单对应影院的可售零食。 */
@Data
public class SnackSelectionVO {

    private Long orderId;
    private Long cinemaId;
    private String cinemaName;
    private List<Option> options;
    private List<SnackOrderItemVO> selected;
    private Double ticketAmount;
    private Double snackAmount;
    private Double totalAmount;

    @Data
    public static class Option {
        private Long id;
        private String name;
        private String description;
        private String image;
        private Integer priceFen;
        private Integer availableStock;
        private Integer selectedQuantity;
        private Integer status;
    }
}

package com.limou.movieticket.booking.domain;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter @TableName("order_item")
public class OrderItem {
    @TableId private String id;
    private String orderId;
    private String seatId;
    private Integer unitPrice;
}

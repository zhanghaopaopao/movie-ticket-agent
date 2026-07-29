package com.limou.movieticket.booking.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
@TableName("ticket_order")
public class TicketOrder {
    @TableId private String id;
    private String orderNo;
    private String userId;
    private String showtimeId;
    private OrderStatus status;
    private Integer amount;
    private LocalDateTime expiresAt;
    @Version private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.limou.movieticket.booking.domain;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Getter @Setter @TableName("ticket")
public class Ticket {
    @TableId private String id;
    private String orderId;
    private String orderItemId;
    private String ticketCode;
    private String qrContent;
    private TicketStatus status;
    private LocalDateTime createdAt;
}

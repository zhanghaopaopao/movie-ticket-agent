package com.limou.movieticket.booking.domain;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Getter @Setter @TableName("payment")
public class Payment {
    @TableId private String id;
    private String orderId;
    private String idempotencyKey;
    private PaymentStatus status;
    private Integer amount;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
}

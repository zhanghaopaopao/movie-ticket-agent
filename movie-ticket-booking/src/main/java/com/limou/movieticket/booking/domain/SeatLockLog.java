package com.limou.movieticket.booking.domain;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Getter @Setter @TableName("seat_lock_log")
public class SeatLockLog {
    @TableId private String id;
    private String orderId;
    private String showtimeId;
    private String seatId;
    private SeatLockAction action;
    private LocalDateTime createdAt;
}

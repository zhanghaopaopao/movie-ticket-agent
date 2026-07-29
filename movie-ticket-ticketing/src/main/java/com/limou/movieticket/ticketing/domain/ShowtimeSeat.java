package com.limou.movieticket.ticketing.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("showtime_seat")
public class ShowtimeSeat {
    @TableId private String id;
    private String showtimeId;
    private String seatId;
    private Integer price;
    private SeatInventoryStatus status;
    private String lockOwner;
    private LocalDateTime lockExpiresAt;
    private Integer version;
    private LocalDateTime updatedAt;

    public String getId() { return id; } public void setId(String id) { this.id = id; }
    public String getShowtimeId() { return showtimeId; } public void setShowtimeId(String showtimeId) { this.showtimeId = showtimeId; }
    public String getSeatId() { return seatId; } public void setSeatId(String seatId) { this.seatId = seatId; }
    public Integer getPrice() { return price; } public void setPrice(Integer price) { this.price = price; }
    public SeatInventoryStatus getStatus() { return status; } public void setStatus(SeatInventoryStatus status) { this.status = status; }
    public String getLockOwner() { return lockOwner; } public void setLockOwner(String lockOwner) { this.lockOwner = lockOwner; }
    public LocalDateTime getLockExpiresAt() { return lockExpiresAt; } public void setLockExpiresAt(LocalDateTime lockExpiresAt) { this.lockExpiresAt = lockExpiresAt; }
    public Integer getVersion() { return version; } public void setVersion(Integer version) { this.version = version; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

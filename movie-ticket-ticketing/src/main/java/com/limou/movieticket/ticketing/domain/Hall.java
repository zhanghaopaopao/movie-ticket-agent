package com.limou.movieticket.ticketing.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("hall")
public class Hall {
    @TableId private String id;
    private String cinemaId;
    private String name;
    private String hallType;
    private String seatTemplateId;
    private ResourceStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() { return id; } public void setId(String id) { this.id = id; }
    public String getCinemaId() { return cinemaId; } public void setCinemaId(String cinemaId) { this.cinemaId = cinemaId; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getHallType() { return hallType; } public void setHallType(String hallType) { this.hallType = hallType; }
    public String getSeatTemplateId() { return seatTemplateId; } public void setSeatTemplateId(String seatTemplateId) { this.seatTemplateId = seatTemplateId; }
    public ResourceStatus getStatus() { return status; } public void setStatus(ResourceStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

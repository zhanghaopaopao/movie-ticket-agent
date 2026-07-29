package com.limou.movieticket.ticketing.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("showtime")
public class Showtime {
    @TableId private String id;
    private String movieId;
    private String hallId;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer basePrice;
    private String language;
    private String format;
    private ShowtimeStatus status;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() { return id; } public void setId(String id) { this.id = id; }
    public String getMovieId() { return movieId; } public void setMovieId(String movieId) { this.movieId = movieId; }
    public String getHallId() { return hallId; } public void setHallId(String hallId) { this.hallId = hallId; }
    public LocalDateTime getStartAt() { return startAt; } public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public LocalDateTime getEndAt() { return endAt; } public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
    public Integer getBasePrice() { return basePrice; } public void setBasePrice(Integer basePrice) { this.basePrice = basePrice; }
    public String getLanguage() { return language; } public void setLanguage(String language) { this.language = language; }
    public String getFormat() { return format; } public void setFormat(String format) { this.format = format; }
    public ShowtimeStatus getStatus() { return status; } public void setStatus(ShowtimeStatus status) { this.status = status; }
    public Integer getVersion() { return version; } public void setVersion(Integer version) { this.version = version; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

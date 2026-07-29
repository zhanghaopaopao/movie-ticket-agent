package com.limou.movieticket.ticketing.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("movie")
public class Movie {
    @TableId private String id;
    private String name;
    private String englishName;
    private String genres;
    private Integer durationMinutes;
    private BigDecimal rating;
    private String posterUrl;
    private LocalDate releaseDate;
    private String synopsis;
    private String castNames;
    private Long wantCount;
    private MovieStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() { return id; } public void setId(String id) { this.id = id; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getEnglishName() { return englishName; } public void setEnglishName(String englishName) { this.englishName = englishName; }
    public String getGenres() { return genres; } public void setGenres(String genres) { this.genres = genres; }
    public Integer getDurationMinutes() { return durationMinutes; } public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public BigDecimal getRating() { return rating; } public void setRating(BigDecimal rating) { this.rating = rating; }
    public String getPosterUrl() { return posterUrl; } public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
    public LocalDate getReleaseDate() { return releaseDate; } public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }
    public String getSynopsis() { return synopsis; } public void setSynopsis(String synopsis) { this.synopsis = synopsis; }
    public String getCastNames() { return castNames; } public void setCastNames(String castNames) { this.castNames = castNames; }
    public Long getWantCount() { return wantCount; } public void setWantCount(Long wantCount) { this.wantCount = wantCount; }
    public MovieStatus getStatus() { return status; } public void setStatus(MovieStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

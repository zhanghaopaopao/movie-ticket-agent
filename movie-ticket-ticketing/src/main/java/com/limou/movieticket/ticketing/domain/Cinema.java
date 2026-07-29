package com.limou.movieticket.ticketing.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("cinema")
public class Cinema {
    @TableId private String id;
    private String name;
    private String brand;
    private String city;
    private String district;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String serviceTags;
    private ResourceStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() { return id; } public void setId(String id) { this.id = id; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getBrand() { return brand; } public void setBrand(String brand) { this.brand = brand; }
    public String getCity() { return city; } public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; } public void setDistrict(String district) { this.district = district; }
    public String getAddress() { return address; } public void setAddress(String address) { this.address = address; }
    public BigDecimal getLatitude() { return latitude; } public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; } public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public String getServiceTags() { return serviceTags; } public void setServiceTags(String serviceTags) { this.serviceTags = serviceTags; }
    public ResourceStatus getStatus() { return status; } public void setStatus(ResourceStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

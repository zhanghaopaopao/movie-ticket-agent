package com.limou.movieticket.ticketing.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("seat")
public class Seat {
    @TableId private String id;
    private String hallId;
    private Integer rowNo;
    private Integer seatNo;
    private String zone;
    private SeatType seatType;
    private String coupleGroup;

    public String getId() { return id; } public void setId(String id) { this.id = id; }
    public String getHallId() { return hallId; } public void setHallId(String hallId) { this.hallId = hallId; }
    public Integer getRowNo() { return rowNo; } public void setRowNo(Integer rowNo) { this.rowNo = rowNo; }
    public Integer getSeatNo() { return seatNo; } public void setSeatNo(Integer seatNo) { this.seatNo = seatNo; }
    public String getZone() { return zone; } public void setZone(String zone) { this.zone = zone; }
    public SeatType getSeatType() { return seatType; } public void setSeatType(SeatType seatType) { this.seatType = seatType; }
    public String getCoupleGroup() { return coupleGroup; } public void setCoupleGroup(String coupleGroup) { this.coupleGroup = coupleGroup; }
}

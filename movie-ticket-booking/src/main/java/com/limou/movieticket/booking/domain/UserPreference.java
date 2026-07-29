package com.limou.movieticket.booking.domain;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Getter @Setter @TableName("user_preference")
public class UserPreference {
    @TableId private String userId;
    private String district;
    private String hallType;
    private Integer budget;
    private String seatZone;
    private LocalDateTime updatedAt;
}

package com.limou.movieticket.booking.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
@TableName("purchase_draft")
public class PurchaseDraft {
    @TableId private String id;
    private String userId;
    private String movieId;
    private String cinemaId;
    private String dateTimeJson;
    private String showtimeId;
    private Integer ticketCount;
    private String budgetJson;
    private String seatsJson;
    private SourceMode sourceMode;
    private PurchaseDraftStatus status;
    @Version private Integer version;
    private String orderId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

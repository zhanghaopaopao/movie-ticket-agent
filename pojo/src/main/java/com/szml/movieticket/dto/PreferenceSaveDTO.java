package com.szml.movieticket.dto;

import lombok.Data;

/**
 * 保存观影偏好 DTO。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
@Data
public class PreferenceSaveDTO {

    private String district;

    private String hallType;

    /** 单票预算上限（分） */
    private Integer budget;

    /** 偏好座位区域：FRONT/MIDDLE/BACK/COUPLE */
    private String seatZone;
}

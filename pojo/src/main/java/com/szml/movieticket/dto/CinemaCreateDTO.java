package com.szml.movieticket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 影院新增 DTO。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
public class CinemaCreateDTO {

    @NotBlank(message = "影院名称不能为空")
    private String name;

    @NotBlank(message = "详细地址不能为空")
    private String address;

    @NotBlank(message = "所属商圈不能为空")
    private String district;

    private String brand;

    private BigDecimal latitude;

    private BigDecimal longitude;

    /** 服务标签，如 ["退改签","小吃","停车"] */
    private List<String> services;
}

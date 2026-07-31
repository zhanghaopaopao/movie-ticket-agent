package com.szml.movieticket.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 影院编辑 DTO。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
public class CinemaUpdateDTO {

    private String name;

    private String address;

    private String district;

    private String brand;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private List<String> services;
}

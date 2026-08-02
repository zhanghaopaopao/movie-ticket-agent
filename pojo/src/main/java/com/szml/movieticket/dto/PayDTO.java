package com.szml.movieticket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 模拟支付请求 DTO。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
@Data
public class PayDTO {

    @NotBlank(message = "幂等键不能为空")
    private String idempotencyKey;
}

package com.szml.movieticket.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 锁座请求 DTO。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
@Data
public class LockSeatsDTO {

    @NotNull(message = "场次ID不能为空")
    private Long showtimeId;

    @NotEmpty(message = "座位ID列表不能为空")
    private List<Long> seatIds;

    @NotNull(message = "草稿版本号不能为空")
    private Integer draftVersion;
}

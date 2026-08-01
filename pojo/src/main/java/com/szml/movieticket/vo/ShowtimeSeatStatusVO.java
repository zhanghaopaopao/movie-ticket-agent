package com.szml.movieticket.vo;

import lombok.Data;

import java.util.List;

/**
 * 场次座位状态批量设置结果 VO。
 *
 * @author zhanghao
 * @since 2026-08-01
 */
@Data
public class ShowtimeSeatStatusVO {

    /** 成功更新的座位ID列表 */
    private List<Long> updatedSeatIds;

    /** 被跳过的座位ID列表 */
    private List<Long> skippedSeatIds;

    /** 跳过原因 */
    private String skippedReason;
}

package com.szml.movieticket.vo;

import lombok.Data;

import java.util.List;

/**
 * 影院选项 VO（供下拉联动使用：影院 → 影厅）。
 *
 * @author zhanghao
 * @since 2026-08-04
 */
@Data
public class CinemaOptionVO {

    /** 影院ID */
    private Long id;

    /** 影院名称 */
    private String name;

    /** 该影院下未停用的影厅 */
    private List<HallBrief> halls;

    @Data
    public static class HallBrief {
        /** 影厅ID */
        private Long id;
        /** 影厅名称 */
        private String name;
    }
}

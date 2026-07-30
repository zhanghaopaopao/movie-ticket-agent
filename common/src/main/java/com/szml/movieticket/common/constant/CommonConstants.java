package com.szml.movieticket.common.constant;

/**
 * 项目通用常量。
 *
 * @author zhanghao
 * @since 2026-07-30
 */
public interface CommonConstants {

    /** 链路追踪 ID 请求头 */
    String TRACE_ID_HEADER = "X-Trace-Id";

    /** 默认日期格式 */
    String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

    /** 默认日期时间格式 */
    String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /** 默认分页大小 */
    Integer DEFAULT_PAGE_SIZE = 10;

    /** 最大分页大小 */
    Integer MAX_PAGE_SIZE = 100;
}

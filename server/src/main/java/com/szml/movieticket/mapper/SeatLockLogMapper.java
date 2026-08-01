package com.szml.movieticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.szml.movieticket.entity.SeatLockLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 座位锁审计日志 Mapper。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Mapper
public interface SeatLockLogMapper extends BaseMapper<SeatLockLog> {
}

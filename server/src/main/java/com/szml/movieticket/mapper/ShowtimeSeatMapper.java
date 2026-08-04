package com.szml.movieticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.szml.movieticket.entity.ShowtimeSeat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 场次座位库存 Mapper。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Mapper
public interface ShowtimeSeatMapper extends BaseMapper<ShowtimeSeat> {

    /**
     * 行级锁查询，按 seatId ASC 排序以降低死锁概率。
     *
     * @param showtimeId 场次ID
     * @param seatIds    座位ID列表
     * @return 被锁定的座位列表
     */
    List<ShowtimeSeat> selectForUpdate(@Param("showtimeId") Long showtimeId,
                                       @Param("seatIds") List<Long> seatIds);

    /**
     * 批量插入场次座位库存。
     */
    int insertBatch(@Param("list") List<ShowtimeSeat> list);
}

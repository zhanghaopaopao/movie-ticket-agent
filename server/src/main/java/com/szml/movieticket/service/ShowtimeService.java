package com.szml.movieticket.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szml.movieticket.dto.ShowtimeCreateDTO;
import com.szml.movieticket.dto.ShowtimeStatusDTO;
import com.szml.movieticket.dto.ShowtimeUpdateDTO;
import com.szml.movieticket.entity.Showtime;
import com.szml.movieticket.dto.ShowtimeSeatStatusDTO;
import com.szml.movieticket.vo.ShowtimePageVO;
import com.szml.movieticket.vo.ShowtimeSeatStatusVO;
import com.szml.movieticket.vo.ShowtimeSeatLayoutVO;
import com.szml.movieticket.vo.ShowtimeVO;

/**
 * 场次服务接口。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
public interface ShowtimeService extends IService<Showtime> {

    /**
     * 分页查询场次列表。
     */
    ShowtimePageVO pageShowtimes(int page, int size, Long movieId, Long cinemaId, String date, String status);

    /**
     * 新增场次。
     */
    ShowtimeVO createShowtime(ShowtimeCreateDTO dto);

    /**
     * 编辑场次。
     */
    ShowtimeVO updateShowtime(Long id, ShowtimeUpdateDTO dto);

    /**
     * 停售/取消场次。
     */
    ShowtimeVO updateShowtimeStatus(Long id, ShowtimeStatusDTO dto);

    /**
     * 批量设置场次座位状态。
     *
     * @param showtimeId 场次ID
     * @param dto        座位ID列表 + 目标状态
     * @return 更新结果（成功/跳过的座位列表）
     */
    ShowtimeSeatStatusVO updateSeatStatus(Long showtimeId, ShowtimeSeatStatusDTO dto);

    /** 查询场次座位库存布局。 */
    ShowtimeSeatLayoutVO getSeatLayout(Long showtimeId);
}

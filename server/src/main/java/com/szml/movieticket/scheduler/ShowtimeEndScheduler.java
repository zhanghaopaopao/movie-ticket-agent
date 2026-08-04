package com.szml.movieticket.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.szml.movieticket.entity.Showtime;
import com.szml.movieticket.enums.ShowtimeStatus;
import com.szml.movieticket.mapper.ShowtimeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 场次自动标记已结束定时任务，每天凌晨 1 点执行。
 *
 * @author zhanghao
 * @since 2026-08-04
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShowtimeEndScheduler {

    private final ShowtimeMapper showtimeMapper;

    @Scheduled(cron = "0 0 1 * * *")
    public void markEnded() {
        LocalDateTime now = LocalDateTime.now();

        List<Showtime> ended = showtimeMapper.selectList(
                new LambdaQueryWrapper<Showtime>()
                        .lt(Showtime::getEndAt, now)
                        .ne(Showtime::getStatus, ShowtimeStatus.ENDED));

        if (ended.isEmpty()) {
            return;
        }

        for (Showtime s : ended) {
            s.setStatus(ShowtimeStatus.ENDED);
            showtimeMapper.updateById(s);
        }

        log.info("场次自动标记已结束完成，共处理 {} 个场次", ended.size());
    }
}

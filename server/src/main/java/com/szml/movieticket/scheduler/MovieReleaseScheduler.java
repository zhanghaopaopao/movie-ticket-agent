package com.szml.movieticket.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.szml.movieticket.entity.Movie;
import com.szml.movieticket.enums.MovieStatus;
import com.szml.movieticket.mapper.MovieMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 影片自动上架定时任务，每天凌晨 1 点将到达上映日期的待映影片改为热映中。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MovieReleaseScheduler {

    private final MovieMapper movieMapper;

    /**
     * 每天凌晨 1 点执行：COMING_SOON 且 release_date <= 今天 → 变为 NOW_SHOWING。
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void autoRelease() {
        LocalDate today = LocalDate.now();

        List<Movie> movies = movieMapper.selectList(
                new LambdaQueryWrapper<Movie>()
                        .eq(Movie::getStatus, MovieStatus.COMING_SOON)
                        .le(Movie::getReleaseDate, today));

        if (movies.isEmpty()) {
            return;
        }

        for (Movie movie : movies) {
            movie.setStatus(MovieStatus.NOW_SHOWING);
            movieMapper.updateById(movie);
            log.info("影片自动上架, id: {}, name: {}, releaseDate: {}", movie.getId(), movie.getName(), movie.getReleaseDate());
        }

        log.info("影片自动上架完成，共处理 {} 部", movies.size());
    }
}

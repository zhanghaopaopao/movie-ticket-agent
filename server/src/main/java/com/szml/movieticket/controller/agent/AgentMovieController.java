package com.szml.movieticket.controller.agent;

import com.szml.movieticket.context.UserContext;
import com.szml.movieticket.result.Result;
import com.szml.movieticket.service.MovieService;
import com.szml.movieticket.vo.MoviePageVO;
import com.szml.movieticket.vo.MovieVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Agent 影片查询接口。
 * <p>
 * 供 Python AI Agent 调用，按影片类型、关键词等条件
 * 搜索在映影片，让 Agent 先展示可选影片再引导用户选择。
 *
 * @author zhanghao
 * @since 2026-08-07
 */
@Slf4j
@RestController
@RequestMapping("/api/agent/movies")
@RequiredArgsConstructor
public class AgentMovieController {

    private final MovieService movieService;

    /**
     * 按类型搜索影片。
     * <p>
     * 当用户表达"想看动作片""最近有什么喜剧"等模糊购片意图时，
     * Agent 应先调用此接口列出影片，让用户选择后再查场次。
     */
    @GetMapping
    public Result<MoviePageVO> search(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "createTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {

        Long userId = UserContext.getUserId();
        log.info("Agent查询影片, userId: {}, genre: {}, keyword: {}, page: {}, size: {}",
                userId, genre, keyword, page, size);

        MoviePageVO result = movieService.listMoviesWithShowtimes(
                userId, page, size, genre, keyword, sortBy, sortOrder);
        return Result.success(result);
    }

    /**
     * 影片详情。
     */
    @GetMapping("/{id}")
    public Result<MovieVO> detail(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        log.info("Agent查询影片详情, userId: {}, movieId: {}", userId, id);
        MovieVO movieVO = movieService.getMovieDetailForUser(userId, id);
        return Result.success(movieVO);
    }
}

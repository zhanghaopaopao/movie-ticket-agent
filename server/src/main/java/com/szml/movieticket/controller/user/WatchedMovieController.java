package com.szml.movieticket.controller.user;

import com.szml.movieticket.context.UserContext;
import com.szml.movieticket.result.Result;
import com.szml.movieticket.service.UserMovieWatchedService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** C端已看过影片接口。 */
@RestController
@RequestMapping("/api/user/watched-movies")
@RequiredArgsConstructor
public class WatchedMovieController {

    private final UserMovieWatchedService watchedService;

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size) {
        return Result.success(watchedService.list(UserContext.getUserId(), page, size));
    }

    @GetMapping("/{movieId}")
    public Result<Boolean> isWatched(@PathVariable Long movieId) {
        return Result.success(watchedService.isWatched(UserContext.getUserId(), movieId));
    }

    @PutMapping("/{movieId}")
    public Result<Void> add(@PathVariable Long movieId) {
        watchedService.add(UserContext.getUserId(), movieId);
        return Result.success();
    }

    @DeleteMapping("/{movieId}")
    public Result<Void> remove(@PathVariable Long movieId) {
        watchedService.remove(UserContext.getUserId(), movieId);
        return Result.success();
    }
}

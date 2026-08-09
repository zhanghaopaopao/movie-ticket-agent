package com.szml.movieticket.controller.user;

import com.szml.movieticket.context.UserContext;
import com.szml.movieticket.dto.MovieReviewCreateDTO;
import com.szml.movieticket.result.Result;
import com.szml.movieticket.service.MovieReviewService;
import com.szml.movieticket.vo.MovieReviewPageVO;
import com.szml.movieticket.vo.MovieReviewVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** C端影片评论接口。 */
@RestController
@RequestMapping("/api/user/movies/{movieId}/reviews")
@RequiredArgsConstructor
public class MovieReviewController {

    private final MovieReviewService reviewService;

    @GetMapping
    public Result<MovieReviewPageVO> list(@PathVariable Long movieId,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return Result.success(reviewService.list(UserContext.getUserId(), movieId, page, size));
    }

    @PostMapping
    public Result<MovieReviewVO> create(@PathVariable Long movieId, @Valid @RequestBody MovieReviewCreateDTO dto) {
        return Result.success(reviewService.create(UserContext.getUserId(), movieId, dto.getParentId(), dto.getContent()));
    }

    @DeleteMapping("/{reviewId}")
    public Result<Void> delete(@PathVariable Long movieId, @PathVariable Long reviewId) {
        reviewService.delete(UserContext.getUserId(), reviewId);
        return Result.success();
    }

    @PutMapping("/{reviewId}/like")
    public Result<Void> like(@PathVariable Long movieId, @PathVariable Long reviewId) {
        reviewService.like(UserContext.getUserId(), reviewId);
        return Result.success();
    }

    @DeleteMapping("/{reviewId}/like")
    public Result<Void> unlike(@PathVariable Long movieId, @PathVariable Long reviewId) {
        reviewService.unlike(UserContext.getUserId(), reviewId);
        return Result.success();
    }
}

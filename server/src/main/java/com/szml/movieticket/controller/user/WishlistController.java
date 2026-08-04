package com.szml.movieticket.controller.user;

import com.szml.movieticket.context.UserContext;
import com.szml.movieticket.result.Result;
import com.szml.movieticket.service.UserMovieWishlistService;
import com.szml.movieticket.vo.MoviePageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final UserMovieWishlistService wishlistService;

    @GetMapping
    public Result<MoviePageVO> list(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return Result.success(wishlistService.list(UserContext.getUserId(), page, size));
    }

    @PutMapping("/{movieId}")
    public Result<Void> add(@PathVariable Long movieId) {
        wishlistService.add(UserContext.getUserId(), movieId);
        return Result.success();
    }

    @DeleteMapping("/{movieId}")
    public Result<Void> remove(@PathVariable Long movieId) {
        wishlistService.remove(UserContext.getUserId(), movieId);
        return Result.success();
    }
}

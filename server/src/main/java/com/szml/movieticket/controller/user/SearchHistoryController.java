package com.szml.movieticket.controller.user;

import com.szml.movieticket.context.UserContext;
import com.szml.movieticket.dto.SearchHistorySaveDTO;
import com.szml.movieticket.result.Result;
import com.szml.movieticket.service.UserSearchHistoryService;
import com.szml.movieticket.vo.SearchHistoryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C端用户搜索历史接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/user/search-history")
@RequiredArgsConstructor
public class SearchHistoryController {

    private final UserSearchHistoryService searchHistoryService;

    @GetMapping
    public Result<List<SearchHistoryVO>> list(
            @RequestParam(defaultValue = "10") int limit) {
        Long userId = UserContext.getUserId();
        return Result.success(searchHistoryService.list(userId, limit));
    }

    @PostMapping
    public Result<SearchHistoryVO> record(@Valid @RequestBody SearchHistorySaveDTO dto) {
        Long userId = UserContext.getUserId();
        return Result.success(searchHistoryService.record(userId, dto.getKeyword()));
    }

    @DeleteMapping
    public Result<Void> clear() {
        Long userId = UserContext.getUserId();
        searchHistoryService.clear(userId);
        return Result.success();
    }
}

package com.szml.movieticket.controller.user;

import com.szml.movieticket.context.UserContext;
import com.szml.movieticket.dto.DraftSaveDTO;
import com.szml.movieticket.result.Result;
import com.szml.movieticket.service.DraftService;
import com.szml.movieticket.vo.DraftVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 购票草稿接口（C 端）。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
@Slf4j
@RestController
@RequestMapping("/api/user/draft")
@RequiredArgsConstructor
public class DraftController {

    private final DraftService draftService;

    /**
     * 获取当前用户活动草稿。
     */
    @GetMapping("/current")
    public Result<DraftVO> current() {
        Long userId = UserContext.getUserId();
        log.info("查询当前草稿, 用户ID: {}", userId);
        DraftVO draftVO = draftService.getCurrentDraft(userId);
        return Result.success(draftVO);
    }

    /**
     * 保存/更新草稿（乐观锁 + 级联清除）。
     */
    @PostMapping
    public Result<DraftVO> save(@Valid @RequestBody DraftSaveDTO dto) {
        Long userId = UserContext.getUserId();
        log.info("保存草稿, 用户ID: {}, 版本号: {}", userId, dto.getVersion());
        DraftVO draftVO = draftService.saveDraft(userId, dto);
        return Result.success(draftVO);
    }
}

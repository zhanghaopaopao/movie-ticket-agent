package com.szml.movieticket.controller.user;

import com.szml.movieticket.context.UserContext;
import com.szml.movieticket.dto.PasswordChangeDTO;
import com.szml.movieticket.dto.PreferenceSaveDTO;
import com.szml.movieticket.result.Result;
import com.szml.movieticket.service.UserProfileService;
import com.szml.movieticket.vo.UserProfileVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 个人中心接口（C 端）。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
@Slf4j
@RestController
@RequestMapping("/api/user/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserProfileService userProfileService;

    /**
     * 个人中心首页。
     */
    @GetMapping
    public Result<UserProfileVO> profile() {
        Long userId = UserContext.getUserId();
        log.info("查询个人中心, 用户ID: {}", userId);
        UserProfileVO userProfileVO = userProfileService.getProfile(userId);
        return Result.success(userProfileVO);
    }

    /**
     * 保存观影偏好。
     */
    @PutMapping("/preference")
    public Result<UserProfileVO.PreferenceVO> savePreference(@RequestBody PreferenceSaveDTO dto) {
        Long userId = UserContext.getUserId();
        log.info("保存观影偏好, 用户ID: {}", userId);
        UserProfileVO.PreferenceVO preferenceVO = userProfileService.savePreference(userId, dto);
        return Result.success(preferenceVO);
    }

    /**
     * 修改密码。
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody PasswordChangeDTO dto) {
        Long userId = UserContext.getUserId();
        log.info("修改密码, 用户ID: {}", userId);
        userProfileService.changePassword(userId, dto.getOldPassword(), dto.getNewPassword());
        return Result.success();
    }
}

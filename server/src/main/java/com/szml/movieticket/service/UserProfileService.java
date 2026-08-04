package com.szml.movieticket.service;

import com.szml.movieticket.dto.PreferenceSaveDTO;
import com.szml.movieticket.dto.UploadResultDTO;
import com.szml.movieticket.vo.UserProfileVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户个人中心服务接口。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
public interface UserProfileService {

    /**
     * 个人中心首页。
     */
    UserProfileVO getProfile(Long userId);

    /**
     * 保存观影偏好。
     */
    UserProfileVO.PreferenceVO savePreference(Long userId, PreferenceSaveDTO dto);

    /**
     * 上传并更新当前用户头像。
     */
    UploadResultDTO updateAvatar(Long userId, MultipartFile file);

    /**
     * 修改密码。
     */
    void sendCurrentEmailCode(Long userId);

    void sendNewEmailCode(Long userId, String newEmail);

    void changePassword(Long userId, String oldPassword, String emailCode, String newPassword);

    void changeEmail(Long userId, String currentEmailCode, String newEmail, String newEmailCode);
}

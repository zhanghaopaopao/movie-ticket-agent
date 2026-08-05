package com.szml.movieticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.szml.movieticket.dto.PreferenceSaveDTO;
import com.szml.movieticket.dto.UploadResultDTO;
import com.szml.movieticket.entity.TicketOrder;
import com.szml.movieticket.entity.User;
import com.szml.movieticket.entity.UserPreference;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.exception.AuthException;
import com.szml.movieticket.exception.BusinessException;
import com.szml.movieticket.mapper.OrderMapper;
import com.szml.movieticket.mapper.UserMapper;
import com.szml.movieticket.mapper.UserPreferenceMapper;
import com.szml.movieticket.service.UserProfileService;
import com.szml.movieticket.service.EmailCodeService;
import com.szml.movieticket.service.FileService;
import com.szml.movieticket.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import com.szml.movieticket.util.AmountUtil;import com.szml.movieticket.util.OrderStatusUtil;

/**
 * 用户个人中心服务实现类。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserMapper userMapper;
    private final UserPreferenceMapper preferenceMapper;
    private final OrderMapper orderMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailCodeService emailCodeService;
    private final FileService fileService;

    @Override
    public UserProfileVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AuthException(ErrorCode.AUTH_ACCOUNT_NOT_FOUND);
        }

        UserProfileVO vo = new UserProfileVO();
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAvatarUrl(user.getAvatarUrl());

        // 统计
        List<TicketOrder> paidOrders = orderMapper.selectList(new LambdaQueryWrapper<TicketOrder>()
                .eq(TicketOrder::getUserId, userId)
                .in(TicketOrder::getStatus, List.of("PAID", "TICKETED")));

        UserProfileVO.UserStats stats = new UserProfileVO.UserStats();
        stats.setTotalOrders(orderMapper.selectCount(
                new LambdaQueryWrapper<TicketOrder>().eq(TicketOrder::getUserId, userId)));
        long totalCents = paidOrders.stream().mapToLong(o -> o.getAmount() != null ? o.getAmount() : 0).sum();
        stats.setTotalSpent(AmountUtil.yuan(totalCents));
        vo.setStats(stats);

        // 偏好
        UserPreference preference = preferenceMapper.selectOne(
                new LambdaQueryWrapper<UserPreference>().eq(UserPreference::getUserId, userId));
        if (preference != null) {
            UserProfileVO.PreferenceVO preferenceVO = new UserProfileVO.PreferenceVO();
            preferenceVO.setDistrict(preference.getDistrict());
            preferenceVO.setHallType(preference.getHallType());
            preferenceVO.setBudgetRaw(preference.getBudget());
            preferenceVO.setBudget(preference.getBudget() != null ? AmountUtil.yuan(preference.getBudget()) : null);
            preferenceVO.setSeatZone(preference.getSeatZone());
            vo.setPreference(preferenceVO);
        }

        return vo;
    }

    @Override
    public UserProfileVO.PreferenceVO savePreference(Long userId, PreferenceSaveDTO dto) {
        UserPreference preference = preferenceMapper.selectOne(
                new LambdaQueryWrapper<UserPreference>().eq(UserPreference::getUserId, userId));

        if (preference == null) {
            preference = new UserPreference();
            preference.setUserId(userId);
        }

        if (dto.getDistrict() != null) preference.setDistrict(dto.getDistrict());
        if (dto.getHallType() != null) preference.setHallType(dto.getHallType());
        if (dto.getBudget() != null) preference.setBudget(dto.getBudget());
        if (dto.getSeatZone() != null) preference.setSeatZone(dto.getSeatZone());

        preferenceMapper.insertOrUpdate(preference);

        log.info("保存观影偏好成功, userId: {}", userId);

        UserProfileVO.PreferenceVO preferenceVO = new UserProfileVO.PreferenceVO();
        preferenceVO.setDistrict(preference.getDistrict());
        preferenceVO.setHallType(preference.getHallType());
        preferenceVO.setBudgetRaw(preference.getBudget());
        preferenceVO.setBudget(preference.getBudget() != null ? AmountUtil.yuan(preference.getBudget()) : null);
        preferenceVO.setSeatZone(preference.getSeatZone());
        return preferenceVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadResultDTO updateAvatar(Long userId, MultipartFile file) {
        User user = requireUser(userId);
        UploadResultDTO uploadResult = fileService.uploadAvatar(file);
        user.setAvatarUrl(uploadResult.getUrl());
        userMapper.updateById(user);
        log.info("更新用户头像成功, userId: {}, fileName: {}", userId, uploadResult.getFileName());
        return uploadResult;
    }

    @Override
    public void sendCurrentEmailCode(Long userId) {
        User user = requireUser(userId);
        requireBoundEmail(user);
        emailCodeService.sendCode(user.getEmail(), EmailCodeService.PURPOSE_ACCOUNT_SECURITY);
    }

    @Override
    public void sendNewEmailCode(Long userId, String newEmail) {
        User user = requireUser(userId);
        requireBoundEmail(user);
        String normalizedEmail = normalizeEmail(newEmail);
        validateNewEmail(user, normalizedEmail);
        emailCodeService.sendCode(normalizedEmail, EmailCodeService.PURPOSE_NEW_EMAIL);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String emailCode, String newPassword) {
        User user = requireUser(userId);
        requireBoundEmail(user);

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new AuthException(ErrorCode.USER_OLD_PASSWORD_WRONG);
        }

        consumeEmailCode(user.getEmail(), EmailCodeService.PURPOSE_ACCOUNT_SECURITY, emailCode,
                ErrorCode.USER_CURRENT_EMAIL_CODE_INVALID);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);

        log.info("修改密码成功, userId: {}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeEmail(Long userId, String currentEmailCode, String newEmail, String newEmailCode) {
        User user = requireUser(userId);
        requireBoundEmail(user);
        String normalizedEmail = normalizeEmail(newEmail);
        validateNewEmail(user, normalizedEmail);

        // Verify both codes before consuming either one. A wrong new-email code must not
        // invalidate a correct current-email code and force the user to request both again.
        verifyEmailCode(user.getEmail(), EmailCodeService.PURPOSE_ACCOUNT_SECURITY, currentEmailCode,
                ErrorCode.USER_CURRENT_EMAIL_CODE_INVALID);
        verifyEmailCode(normalizedEmail, EmailCodeService.PURPOSE_NEW_EMAIL, newEmailCode,
                ErrorCode.USER_NEW_EMAIL_CODE_INVALID);
        consumeEmailCode(user.getEmail(), EmailCodeService.PURPOSE_ACCOUNT_SECURITY, currentEmailCode,
                ErrorCode.USER_CURRENT_EMAIL_CODE_INVALID);
        consumeEmailCode(normalizedEmail, EmailCodeService.PURPOSE_NEW_EMAIL, newEmailCode,
                ErrorCode.USER_NEW_EMAIL_CODE_INVALID);

        // Recheck inside the transaction before the update. The database unique index
        // remains the final guard against concurrent requests.
        validateNewEmail(user, normalizedEmail);
        user.setEmail(normalizedEmail);
        try {
            userMapper.updateById(user);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.USER_EMAIL_EXISTS);
        }
        log.info("换绑邮箱成功, userId: {}", userId);
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new AuthException(ErrorCode.AUTH_ACCOUNT_NOT_FOUND);
        return user;
    }

    private void verifyEmailCode(String email, int purpose, String code, ErrorCode invalidError) {
        try {
            emailCodeService.verifyCode(email, purpose, code);
        } catch (AuthException exception) {
            throw new AuthException(invalidError);
        }
    }

    private void consumeEmailCode(String email, int purpose, String code, ErrorCode invalidError) {
        try {
            emailCodeService.consumeCode(email, purpose, code);
        } catch (AuthException exception) {
            throw new AuthException(invalidError);
        }
    }

    private static void requireBoundEmail(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new AuthException(ErrorCode.USER_EMAIL_REQUIRED);
        }
    }

    private void validateNewEmail(User user, String newEmail) {
        if (user.getEmail().equalsIgnoreCase(newEmail)) {
            throw new BusinessException(ErrorCode.USER_EMAIL_SAME);
        }
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, newEmail)
                .ne(User::getId, user.getId()));
        if (count != null && count > 0) throw new BusinessException(ErrorCode.USER_EMAIL_EXISTS);
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }

}

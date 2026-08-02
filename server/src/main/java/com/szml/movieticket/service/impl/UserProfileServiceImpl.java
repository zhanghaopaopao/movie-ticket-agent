package com.szml.movieticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.szml.movieticket.dto.PreferenceSaveDTO;
import com.szml.movieticket.entity.TicketOrder;
import com.szml.movieticket.entity.User;
import com.szml.movieticket.entity.UserPreference;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.exception.AuthException;
import com.szml.movieticket.mapper.OrderMapper;
import com.szml.movieticket.mapper.UserMapper;
import com.szml.movieticket.mapper.UserPreferenceMapper;
import com.szml.movieticket.service.UserProfileService;
import com.szml.movieticket.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

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

    @Override
    public UserProfileVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AuthException(ErrorCode.AUTH_ACCOUNT_NOT_FOUND);
        }

        UserProfileVO vo = new UserProfileVO();
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());

        // 统计
        List<TicketOrder> paidOrders = orderMapper.selectList(new LambdaQueryWrapper<TicketOrder>()
                .eq(TicketOrder::getUserId, userId)
                .in(TicketOrder::getStatus, List.of("PAID", "TICKETED")));

        UserProfileVO.UserStats stats = new UserProfileVO.UserStats();
        stats.setTotalOrders(orderMapper.selectCount(
                new LambdaQueryWrapper<TicketOrder>().eq(TicketOrder::getUserId, userId)));
        long totalCents = paidOrders.stream().mapToLong(o -> o.getAmount() != null ? o.getAmount() : 0).sum();
        stats.setTotalSpent(yuan(totalCents));
        vo.setStats(stats);

        // 偏好
        UserPreference preference = preferenceMapper.selectOne(
                new LambdaQueryWrapper<UserPreference>().eq(UserPreference::getUserId, userId));
        if (preference != null) {
            UserProfileVO.PreferenceVO preferenceVO = new UserProfileVO.PreferenceVO();
            preferenceVO.setDistrict(preference.getDistrict());
            preferenceVO.setHallType(preference.getHallType());
            preferenceVO.setBudgetRaw(preference.getBudget());
            preferenceVO.setBudget(preference.getBudget() != null ? yuan(preference.getBudget()) : null);
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
        preferenceVO.setBudget(preference.getBudget() != null ? yuan(preference.getBudget()) : null);
        preferenceVO.setSeatZone(preference.getSeatZone());
        return preferenceVO;
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AuthException(ErrorCode.AUTH_ACCOUNT_NOT_FOUND);
        }

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new AuthException(ErrorCode.USER_OLD_PASSWORD_WRONG);
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);

        log.info("修改密码成功, userId: {}", userId);
    }

    private static double yuan(long cents) {
        return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).doubleValue();
    }
}

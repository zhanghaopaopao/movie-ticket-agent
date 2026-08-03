package com.szml.movieticket.service.impl;

import com.szml.movieticket.entity.UserSearchHistory;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.exception.BusinessException;
import com.szml.movieticket.mapper.UserSearchHistoryMapper;
import com.szml.movieticket.service.UserSearchHistoryService;
import com.szml.movieticket.vo.SearchHistoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户搜索历史服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSearchHistoryServiceImpl implements UserSearchHistoryService {

    private static final int MAX_HISTORY_SIZE = 20;

    private final UserSearchHistoryMapper searchHistoryMapper;

    @Override
    public List<SearchHistoryVO> list(Long userId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_HISTORY_SIZE);
        return searchHistoryMapper.selectRecent(userId, safeLimit).stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public SearchHistoryVO record(Long userId, String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        searchHistoryMapper.upsert(userId, normalizedKeyword);

        UserSearchHistory history = searchHistoryMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserSearchHistory>()
                        .eq(UserSearchHistory::getUserId, userId)
                        .eq(UserSearchHistory::getKeyword, normalizedKeyword));
        if (history == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        log.debug("记录用户搜索历史, userId: {}, keyword: {}", userId, normalizedKeyword);
        return toVO(history);
    }

    @Override
    public void clear(Long userId) {
        searchHistoryMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserSearchHistory>()
                        .eq(UserSearchHistory::getUserId, userId));
        log.debug("清空用户搜索历史, userId: {}", userId);
    }

    private String normalizeKeyword(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().replaceAll("\\s+", " ");
        if (!StringUtils.hasText(normalized) || normalized.length() > 128) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        return normalized;
    }

    private SearchHistoryVO toVO(UserSearchHistory history) {
        SearchHistoryVO vo = new SearchHistoryVO();
        vo.setId(history.getId());
        vo.setKeyword(history.getKeyword());
        vo.setSearchCount(history.getSearchCount());
        vo.setLastSearchTime(history.getLastSearchTime());
        return vo;
    }
}

package com.szml.movieticket.service;

import com.szml.movieticket.vo.SearchHistoryVO;

import java.util.List;

/**
 * 用户搜索历史服务。
 */
public interface UserSearchHistoryService {

    List<SearchHistoryVO> list(Long userId, int limit);

    SearchHistoryVO record(Long userId, String keyword);

    void clear(Long userId);
}

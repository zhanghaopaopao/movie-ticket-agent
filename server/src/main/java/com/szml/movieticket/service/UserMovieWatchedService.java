package com.szml.movieticket.service;

import com.szml.movieticket.vo.MoviePageVO;

/** 用户已看过影片服务。 */
public interface UserMovieWatchedService {

    MoviePageVO list(Long userId, int page, int size);

    boolean isWatched(Long userId, Long movieId);
    void add(Long userId, Long movieId);
    void remove(Long userId, Long movieId);
}

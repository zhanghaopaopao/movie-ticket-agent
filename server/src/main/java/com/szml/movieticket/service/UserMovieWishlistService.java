package com.szml.movieticket.service;

import com.szml.movieticket.vo.MoviePageVO;

public interface UserMovieWishlistService {

    MoviePageVO list(Long userId, int page, int size);

    void add(Long userId, Long movieId);

    void remove(Long userId, Long movieId);
}

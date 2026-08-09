package com.szml.movieticket.service;

import com.szml.movieticket.vo.MovieReviewPageVO;
import com.szml.movieticket.vo.MovieReviewVO;

/** 影片评论服务。 */
public interface MovieReviewService {

    MovieReviewPageVO list(Long userId, Long movieId, int page, int size);

    MovieReviewVO create(Long userId, Long movieId, Long parentId, String content);

    void delete(Long userId, Long reviewId);

    void like(Long userId, Long reviewId);

    void unlike(Long userId, Long reviewId);
}

package com.szml.movieticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.szml.movieticket.entity.Movie;
import com.szml.movieticket.entity.MovieReview;
import com.szml.movieticket.entity.MovieReviewLike;
import com.szml.movieticket.entity.User;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.exception.BusinessException;
import com.szml.movieticket.mapper.MovieMapper;
import com.szml.movieticket.mapper.MovieReviewLikeMapper;
import com.szml.movieticket.mapper.MovieReviewMapper;
import com.szml.movieticket.mapper.UserMapper;
import com.szml.movieticket.service.MovieReviewService;
import com.szml.movieticket.vo.MovieReviewPageVO;
import com.szml.movieticket.vo.MovieReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 影片评论服务实现。 */
@Service
@RequiredArgsConstructor
public class MovieReviewServiceImpl implements MovieReviewService {

    private static final int MAX_PAGE_SIZE = 50;

    private final MovieReviewMapper reviewMapper;
    private final MovieReviewLikeMapper reviewLikeMapper;
    private final MovieMapper movieMapper;
    private final UserMapper userMapper;

    @Override
    public MovieReviewPageVO list(Long userId, Long movieId, int page, int size) {
        validatePage(page, size);
        Page<MovieReview> reviewPage = reviewMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<MovieReview>()
                        .eq(MovieReview::getMovieId, movieId)
                        .orderByDesc(MovieReview::getCreateTime)
                        .orderByDesc(MovieReview::getId));
        List<MovieReview> reviews = reviewPage.getRecords();
        List<Long> reviewIds = reviews.stream().map(MovieReview::getId).toList();
        List<Long> userIds = reviews.stream().map(MovieReview::getUserId).distinct().toList();

        Map<Long, User> users = userIds.isEmpty() ? Collections.emptyMap() : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<MovieReviewLike> likes = reviewIds.isEmpty() ? List.of() : reviewLikeMapper.selectList(
                new LambdaQueryWrapper<MovieReviewLike>().in(MovieReviewLike::getReviewId, reviewIds));
        Map<Long, Integer> likeCounts = likes.stream().collect(Collectors.groupingBy(
                MovieReviewLike::getReviewId, Collectors.summingInt(item -> 1)));
        Set<Long> likedReviewIds = likes.stream().filter(item -> item.getUserId().equals(userId))
                .map(MovieReviewLike::getReviewId).collect(Collectors.toSet());

        MovieReviewPageVO result = new MovieReviewPageVO();
        result.setTotal(reviewPage.getTotal());
        result.setPage(page);
        result.setSize(size);
        result.setRecords(reviews.stream().map(review -> toVO(review, users.get(review.getUserId()),
                likeCounts.getOrDefault(review.getId(), 0), likedReviewIds.contains(review.getId()), userId)).toList());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MovieReviewVO create(Long userId, Long movieId, Long parentId, String content) {
        ensureMovieExists(movieId);
        String normalized = content == null ? "" : content.trim();
        if (!StringUtils.hasText(normalized) || normalized.length() > 500) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        MovieReview review = new MovieReview();
        review.setMovieId(movieId);
        review.setUserId(userId);
        if (parentId != null) {
            MovieReview parent = getReview(parentId);
            if (!parent.getMovieId().equals(movieId) || parent.getParentId() != null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR);
            }
            review.setParentId(parentId);
        }
        review.setContent(normalized);
        review.setCreateTime(LocalDateTime.now());
        reviewMapper.insert(review);
        return toVO(review, userMapper.selectById(userId), 0, false, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long reviewId) {
        MovieReview review = getReview(reviewId);
        if (!review.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.REVIEW_DELETE_FORBIDDEN);
        }
        reviewLikeMapper.delete(new LambdaQueryWrapper<MovieReviewLike>().eq(MovieReviewLike::getReviewId, reviewId));
        reviewMapper.deleteById(reviewId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void like(Long userId, Long reviewId) {
        getReview(reviewId);
        reviewLikeMapper.insertIgnore(reviewId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlike(Long userId, Long reviewId) {
        getReview(reviewId);
        reviewLikeMapper.delete(new LambdaQueryWrapper<MovieReviewLike>()
                .eq(MovieReviewLike::getReviewId, reviewId)
                .eq(MovieReviewLike::getUserId, userId));
    }

    private void validatePage(int page, int size) {
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) throw new BusinessException(ErrorCode.PARAM_ERROR);
    }

    private void ensureMovieExists(Long movieId) {
        if (movieMapper.selectById(movieId) == null) throw new BusinessException(ErrorCode.MOVIE_NOT_FOUND);
    }

    private MovieReview getReview(Long reviewId) {
        MovieReview review = reviewMapper.selectById(reviewId);
        if (review == null) throw new BusinessException(ErrorCode.REVIEW_NOT_FOUND);
        return review;
    }

    private MovieReviewVO toVO(MovieReview review, User user, int likeCount, boolean liked, Long currentUserId) {
        MovieReviewVO vo = new MovieReviewVO();
        vo.setId(review.getId());
        vo.setMovieId(review.getMovieId());
        vo.setParentId(review.getParentId());
        vo.setContent(review.getContent());
        vo.setAuthorName(maskEmail(user == null ? null : user.getEmail()));
        vo.setAuthorAvatarUrl(user == null ? null : user.getAvatarUrl());
        vo.setLikeCount(likeCount);
        vo.setLiked(liked);
        vo.setMine(review.getUserId().equals(currentUserId));
        vo.setCreateTime(review.getCreateTime());
        return vo;
    }

    private String maskEmail(String email) {
        if (!StringUtils.hasText(email)) return "匿名用户";
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return email.charAt(0) + "***";
        return email.substring(0, 1) + "***" + email.substring(atIndex);
    }
}

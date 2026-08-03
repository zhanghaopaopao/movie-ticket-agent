package com.szml.movieticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.szml.movieticket.entity.UserSearchHistory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户搜索历史 Mapper。
 */
@Mapper
public interface UserSearchHistoryMapper extends BaseMapper<UserSearchHistory> {

    /**
     * 按用户和关键词原子新增或累加搜索次数，避免并发请求产生重复记录。
     */
    @Insert("""
            INSERT INTO user_search_history
                (user_id, `keyword`, search_count, last_search_time, create_time, update_time)
            VALUES
                (#{userId}, #{keyword}, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE
                search_count = search_count + 1,
                last_search_time = CURRENT_TIMESTAMP,
                update_time = CURRENT_TIMESTAMP
            """)
    int upsert(@Param("userId") Long userId, @Param("keyword") String keyword);

    @Select("""
            SELECT id, user_id, `keyword`, search_count, last_search_time, create_time, update_time
            FROM user_search_history
            WHERE user_id = #{userId}
            ORDER BY last_search_time DESC, id DESC
            LIMIT #{limit}
            """)
    List<UserSearchHistory> selectRecent(@Param("userId") Long userId, @Param("limit") int limit);
}

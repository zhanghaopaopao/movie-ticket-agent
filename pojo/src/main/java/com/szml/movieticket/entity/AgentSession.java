package com.szml.movieticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agent 会话状态。
 *
 * <p>memoryId 直接复用 agent_session.id，不再额外创建 agent_memory 表。</p>
 */
@Data
@TableName("agent_session")
public class AgentSession {

    @TableId(type = IdType.INPUT)
    private String id;

    private Long userId;

    private Long draftId;

    private String state;

    private String slotsJson;

    private String candidateContext;

    /**
     * 保存可恢复的 AgentState JSON 快照。
     *
     * <p>现有表没有 state_json 字段，因此复用 summary 保存机器可恢复快照。</p>
     */
    private String summary;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

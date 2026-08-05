package com.szml.movieticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agent 对话消息明细。
 *
 * <p>字段与现有 agent_message 表保持一致，正文写入 content，卡片写入 cards_json。</p>
 */
@Data
@TableName("agent_message")
public class AgentMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;

    private String role;

    private String content;

    private String cardsJson;

    private String traceId;

    private LocalDateTime createTime;
}

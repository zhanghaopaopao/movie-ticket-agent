package com.szml.movieticket.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 长期记忆会话及最近消息。
 */
@Data
public class AgentMemoryVO {

    private String memoryId;

    private String sessionId;

    private String stateJson;

    private LocalDateTime lastMessageTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<AgentMessageVO> messages;
}

package com.szml.movieticket.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agent 历史会话列表项。
 */
@Data
public class AgentSessionSummaryVO {

    private String memoryId;

    private String sessionId;

    private String title;

    private String previewMessage;

    private Integer messageCount;

    private LocalDateTime lastMessageTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

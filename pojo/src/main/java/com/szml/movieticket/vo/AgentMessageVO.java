package com.szml.movieticket.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agent 消息展示对象。
 */
@Data
public class AgentMessageVO {

    private Long id;

    private String memoryId;

    private String role;

    private String content;

    /**
     * Agent 卡片数据，使用 JSON 数组保存，供历史会话恢复展示。
     */
    private String cardsJson;

    private String event;

    private String intent;

    private String action;

    private String state;

    private LocalDateTime createTime;
}

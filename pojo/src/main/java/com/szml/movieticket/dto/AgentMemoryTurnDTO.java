package com.szml.movieticket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Agent 一轮对话持久化请求。
 */
@Data
public class AgentMemoryTurnDTO {

    private String memoryId;

    @NotBlank
    private String sessionId;

    @NotBlank
    private String userMessage;

    @NotBlank
    private String assistantMessage;

    private String event;

    private String intent;

    private String action;

    private String state;

    private String stateJson;

    private String requestJson;

    private String responseJson;
}

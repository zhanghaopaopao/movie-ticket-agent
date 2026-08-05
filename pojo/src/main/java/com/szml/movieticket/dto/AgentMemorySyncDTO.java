package com.szml.movieticket.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Agent 前端会话快照同步请求。
 */
@Data
public class AgentMemorySyncDTO {

    private String memoryId;

    @NotBlank
    private String sessionId;

    private String stateJson;

    @Valid
    @NotEmpty
    private List<Message> messages;

    @Data
    public static class Message {

        @NotBlank
        private String role;

        @NotBlank
        private String content;

        private String cardsJson;
    }
}

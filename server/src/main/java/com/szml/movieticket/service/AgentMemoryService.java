package com.szml.movieticket.service;

import com.szml.movieticket.dto.AgentMemoryTurnDTO;
import com.szml.movieticket.dto.AgentMemorySyncDTO;
import com.szml.movieticket.vo.AgentMemoryVO;
import com.szml.movieticket.vo.AgentSessionSummaryVO;

import java.util.List;

/**
 * Agent 长期记忆服务。
 */
public interface AgentMemoryService {

    AgentMemoryVO current(Long userId, String sessionId, String memoryId, int limit);

    AgentMemoryVO saveTurn(Long userId, AgentMemoryTurnDTO dto);

    AgentMemoryVO syncSession(Long userId, AgentMemorySyncDTO dto);

    List<AgentSessionSummaryVO> listSessions(Long userId, int limit);
}

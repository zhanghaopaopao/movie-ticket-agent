package com.szml.movieticket.controller.user;

import com.szml.movieticket.context.UserContext;
import com.szml.movieticket.dto.AgentMemorySyncDTO;
import com.szml.movieticket.dto.AgentMemoryTurnDTO;
import com.szml.movieticket.result.Result;
import com.szml.movieticket.service.AgentMemoryService;
import com.szml.movieticket.vo.AgentMemoryVO;
import com.szml.movieticket.vo.AgentSessionSummaryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Agent 长期记忆接口。
 */
@RestController
@RequestMapping("/api/user/agent/memory")
@RequiredArgsConstructor
public class AgentMemoryController {

    private final AgentMemoryService memoryService;

    @GetMapping("/current")
    public Result<AgentMemoryVO> current(
            @RequestParam String sessionId,
            @RequestParam(required = false) String memoryId,
            @RequestParam(defaultValue = "50") int limit) {
        Long userId = UserContext.getUserId();
        return Result.success(
                memoryService.current(userId, sessionId, memoryId, limit));
    }

    @GetMapping("/list")
    public Result<List<AgentSessionSummaryVO>> list(
            @RequestParam(defaultValue = "20") int limit) {
        Long userId = UserContext.getUserId();
        return Result.success(memoryService.listSessions(userId, limit));
    }

    @PostMapping("/turn")
    public Result<AgentMemoryVO> saveTurn(
            @Valid @RequestBody AgentMemoryTurnDTO dto) {
        Long userId = UserContext.getUserId();
        return Result.success(memoryService.saveTurn(userId, dto));
    }

    @PostMapping("/sync")
    public Result<AgentMemoryVO> sync(
            @Valid @RequestBody AgentMemorySyncDTO dto) {
        Long userId = UserContext.getUserId();
        return Result.success(memoryService.syncSession(userId, dto));
    }
}

package com.szml.movieticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.szml.movieticket.dto.AgentMemorySyncDTO;
import com.szml.movieticket.dto.AgentMemoryTurnDTO;
import com.szml.movieticket.entity.AgentMessage;
import com.szml.movieticket.entity.AgentSession;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.exception.BusinessException;
import com.szml.movieticket.mapper.AgentMessageMapper;
import com.szml.movieticket.mapper.AgentSessionMapper;
import com.szml.movieticket.service.AgentMemoryService;
import com.szml.movieticket.vo.AgentMemoryVO;
import com.szml.movieticket.vo.AgentMessageVO;
import com.szml.movieticket.vo.AgentSessionSummaryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Agent 长期记忆服务实现。
 *
 * <p>现有数据库没有 agent_memory 表，因此 memoryId 直接复用
 * agent_session.id，消息正文写入 agent_message.content。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentMemoryServiceImpl implements AgentMemoryService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;

    private final AgentSessionMapper sessionMapper;
    private final AgentMessageMapper messageMapper;
    private final ObjectMapper objectMapper;

    @Override
    public AgentMemoryVO current(
            Long userId,
            String sessionId,
            String memoryId,
            int limit) {
        if (userId == null) {
            return null;
        }
        AgentSession session = findOwnedSession(userId, sessionId, memoryId);
        return session == null ? null : toVO(session, limit);
    }

    @Override
    public List<AgentSessionSummaryVO> listSessions(Long userId, int limit) {
        if (userId == null) {
            return List.of();
        }
        int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        List<AgentSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<AgentSession>()
                        .eq(AgentSession::getUserId, userId)
                        .orderByDesc(AgentSession::getUpdateTime)
                        .orderByDesc(AgentSession::getCreateTime)
                        .last("LIMIT " + safeLimit));
        return sessions.stream()
                .map(this::toSummaryVO)
                .toList();
    }

    @Override
    @Transactional
    public AgentMemoryVO saveTurn(Long userId, AgentMemoryTurnDTO dto) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String requestSessionId = normalizeRequired(
                dto.getSessionId(),
                "sessionId");
        String userMessage = normalizeRequired(
                dto.getUserMessage(),
                "userMessage");
        String assistantMessage = normalizeRequired(
                dto.getAssistantMessage(),
                "assistantMessage");

        String persistentSessionId = normalizeSessionId(
                StringUtils.hasText(dto.getMemoryId())
                        ? dto.getMemoryId()
                        : requestSessionId);
        AgentSession existing = sessionMapper.selectById(persistentSessionId);
        if (existing != null && !userId.equals(existing.getUserId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        AgentSession session = findOwnedSession(
                userId,
                requestSessionId,
                persistentSessionId);
        if (session == null) {
            session = new AgentSession();
            session.setId(persistentSessionId);
            session.setUserId(userId);
            session.setCreateTime(LocalDateTime.now());
        }

        ObjectNode snapshot = parseObject(dto.getStateJson());
        snapshot.put("session_id", requestSessionId);
        snapshot.put("memory_id", persistentSessionId);
        snapshot.put("user_id", userId);

        String state = firstNonBlank(
                dto.getState(),
                textValue(snapshot, "state"),
                "COLLECTING");
        session.setState(limitText(state, 32));
        session.setSlotsJson(jsonValue(snapshot.get("slots"), "{}"));
        session.setCandidateContext(
                jsonValue(snapshot.get("selected"), "{}"));

        Long draftId = extractDraftId(snapshot);
        if (draftId == null) {
            draftId = extractDraftId(parseObject(dto.getRequestJson()));
        }
        if (draftId != null) {
            session.setDraftId(draftId);
        }
        session.setSummary(writeJson(snapshot));
        session.setUpdateTime(LocalDateTime.now());

        if (session.getCreateTime() == null) {
            session.setCreateTime(LocalDateTime.now());
        }
        if (existing == null) {
            sessionMapper.insert(session);
        } else {
            sessionMapper.updateById(session);
        }

        String traceId = extractTraceId(dto.getResponseJson());
        String cardsJson = extractCardsJson(dto.getResponseJson());
        messageMapper.insert(newMessage(
                session.getId(),
                "USER",
                userMessage,
                null,
                traceId));
        messageMapper.insert(newMessage(
                session.getId(),
                "AGENT",
                assistantMessage,
                cardsJson,
                traceId));

        log.debug(
                "保存 Agent 长期记忆, userId: {}, sessionId: {}, messageCount: 2",
                userId,
                session.getId());
        return toVO(session, DEFAULT_LIMIT);
    }

    @Override
    @Transactional
    public AgentMemoryVO syncSession(Long userId, AgentMemorySyncDTO dto) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String requestSessionId = normalizeRequired(
                dto.getSessionId(),
                "sessionId");
        String persistentSessionId = normalizeSessionId(
                StringUtils.hasText(dto.getMemoryId())
                        ? dto.getMemoryId()
                        : requestSessionId);
        AgentSession existing = sessionMapper.selectById(persistentSessionId);
        if (existing != null && !userId.equals(existing.getUserId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        AgentSession session = existing == null ? new AgentSession() : existing;
        if (existing == null) {
            session.setId(persistentSessionId);
            session.setUserId(userId);
            session.setCreateTime(LocalDateTime.now());
        }

        ObjectNode snapshot = parseObject(dto.getStateJson());
        snapshot.put("session_id", requestSessionId);
        snapshot.put("memory_id", persistentSessionId);
        snapshot.put("user_id", userId);

        session.setState(limitText(firstNonBlank(
                textValue(snapshot, "state"),
                "ARCHIVED"), 32));
        session.setSlotsJson(jsonValue(snapshot.get("slots"), "{}"));
        session.setCandidateContext(jsonValue(snapshot.get("selected"), "{}"));
        Long draftId = extractDraftId(snapshot);
        if (draftId != null) {
            session.setDraftId(draftId);
        }
        session.setSummary(writeJson(snapshot));
        session.setUpdateTime(LocalDateTime.now());

        if (existing == null) {
            sessionMapper.insert(session);
        } else {
            sessionMapper.updateById(session);
        }

        messageMapper.delete(
                new LambdaQueryWrapper<AgentMessage>()
                        .eq(AgentMessage::getSessionId, session.getId()));

        String traceId = UUID.randomUUID().toString();
        for (AgentMemorySyncDTO.Message item : dto.getMessages()) {
            String content = normalizeRequired(item.getContent(), "content");
            messageMapper.insert(newMessage(
                    session.getId(),
                    toStoredRole(item.getRole()),
                    content,
                    item.getCardsJson(),
                    traceId));
        }

        log.info(
                "同步 Agent 会话快照, userId: {}, sessionId: {}, messageCount: {}",
                userId,
                session.getId(),
                dto.getMessages().size());
        return toVO(session, DEFAULT_LIMIT);
    }

    private AgentSession findOwnedSession(
            Long userId,
            String sessionId,
            String memoryId) {
        String key = normalizeSessionId(
                StringUtils.hasText(memoryId) ? memoryId : sessionId);
        if (!StringUtils.hasText(key)) {
            return null;
        }
        return sessionMapper.selectOne(
                new LambdaQueryWrapper<AgentSession>()
                        .eq(AgentSession::getId, key)
                        .eq(AgentSession::getUserId, userId)
                        .last("LIMIT 1"));
    }

    private AgentMessage newMessage(
            String sessionId,
            String role,
            String content,
            String cardsJson,
            String traceId) {
        AgentMessage message = new AgentMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setCardsJson(cardsJson);
        message.setTraceId(traceId);
        message.setCreateTime(LocalDateTime.now());
        return message;
    }

    private String toStoredRole(String role) {
        String normalized = normalizeRequired(role, "role").trim();
        if ("user".equalsIgnoreCase(normalized)) {
            return "USER";
        }
        if ("assistant".equalsIgnoreCase(normalized)
                || "agent".equalsIgnoreCase(normalized)) {
            return "AGENT";
        }
        if ("system".equalsIgnoreCase(normalized)) {
            return "SYSTEM";
        }
        return limitText(normalized.toUpperCase(), 8);
    }

    private AgentMemoryVO toVO(AgentSession session, int requestedLimit) {
        int limit = Math.min(
                Math.max(requestedLimit, 1),
                MAX_LIMIT);
        List<AgentMessage> records = messageMapper.selectList(
                        new LambdaQueryWrapper<AgentMessage>()
                                .eq(
                                        AgentMessage::getSessionId,
                                        session.getId())
                                .orderByAsc(AgentMessage::getCreateTime)
                                .orderByAsc(AgentMessage::getId)
                                .last("LIMIT " + limit));

        AgentMemoryVO vo = new AgentMemoryVO();
        vo.setMemoryId(session.getId());
        vo.setSessionId(session.getId());
        vo.setStateJson(resolveStateJson(session, records));
        vo.setLastMessageTime(session.getUpdateTime());
        vo.setCreateTime(session.getCreateTime());
        vo.setUpdateTime(session.getUpdateTime());
        vo.setMessages(
                records.stream()
                        .map(message -> toMessageVO(session, message))
                        .toList());
        return vo;
    }

    private AgentSessionSummaryVO toSummaryVO(AgentSession session) {
        AgentMessage firstUserMessage = findOneMessage(
                session.getId(),
                "USER",
                true);
        AgentMessage latestMessage = findOneMessage(
                session.getId(),
                null,
                false);
        Long messageCount = messageMapper.selectCount(
                new LambdaQueryWrapper<AgentMessage>()
                        .eq(
                                AgentMessage::getSessionId,
                                session.getId()));

        AgentSessionSummaryVO vo = new AgentSessionSummaryVO();
        vo.setMemoryId(session.getId());
        vo.setSessionId(session.getId());
        vo.setTitle(toSessionTitle(firstUserMessage));
        vo.setPreviewMessage(latestMessage == null
                ? ""
                : limitText(latestMessage.getContent(), 80));
        vo.setMessageCount(messageCount == null
                ? 0
                : Math.toIntExact(messageCount));
        vo.setLastMessageTime(session.getUpdateTime());
        vo.setCreateTime(session.getCreateTime());
        vo.setUpdateTime(session.getUpdateTime());
        return vo;
    }

    private AgentMessage findOneMessage(
            String sessionId,
            String role,
            boolean asc) {
        LambdaQueryWrapper<AgentMessage> wrapper =
                new LambdaQueryWrapper<AgentMessage>()
                        .eq(AgentMessage::getSessionId, sessionId);
        if (StringUtils.hasText(role)) {
            wrapper.eq(AgentMessage::getRole, role);
        }
        if (asc) {
            wrapper.orderByAsc(AgentMessage::getCreateTime)
                    .orderByAsc(AgentMessage::getId);
        } else {
            wrapper.orderByDesc(AgentMessage::getCreateTime)
                    .orderByDesc(AgentMessage::getId);
        }
        return messageMapper.selectOne(wrapper.last("LIMIT 1"));
    }

    private String toSessionTitle(AgentMessage firstUserMessage) {
        if (firstUserMessage == null
                || !StringUtils.hasText(firstUserMessage.getContent())) {
            return "新会话";
        }
        return limitText(firstUserMessage.getContent().trim(), 24);
    }

    private String resolveStateJson(
            AgentSession session,
            List<AgentMessage> records) {
        ObjectNode stored = parseObject(session.getSummary());
        if (!stored.isEmpty()) {
            return writeJson(stored);
        }

        ObjectNode fallback = objectMapper.createObjectNode();
        fallback.put("session_id", session.getId());
        fallback.put("memory_id", session.getId());
        fallback.put("user_id", session.getUserId());
        fallback.put("state", session.getState());
        fallback.set("slots", parseJson(session.getSlotsJson()));
        fallback.set(
                "selected",
                parseJson(session.getCandidateContext()));

        ArrayNode history = fallback.putArray("history");
        for (AgentMessage record : records) {
            if (!"USER".equalsIgnoreCase(record.getRole())) {
                continue;
            }
            ObjectNode item = history.addObject();
            item.put("user", record.getContent());
            item.put("success", true);
        }
        return writeJson(fallback);
    }

    private AgentMessageVO toMessageVO(
            AgentSession session,
            AgentMessage message) {
        AgentMessageVO vo = new AgentMessageVO();
        vo.setId(message.getId());
        vo.setMemoryId(session.getId());
        vo.setRole(toClientRole(message.getRole()));
        vo.setContent(message.getContent());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }

    private String toClientRole(String role) {
        if ("USER".equalsIgnoreCase(role)) {
            return "user";
        }
        if ("AGENT".equalsIgnoreCase(role)) {
            return "assistant";
        }
        if ("SYSTEM".equalsIgnoreCase(role)) {
            return "system";
        }
        return StringUtils.hasText(role)
                ? role.toLowerCase()
                : "system";
    }

    private ObjectNode parseObject(String raw) {
        if (!StringUtils.hasText(raw)) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode node = objectMapper.readTree(raw);
            return node != null && node.isObject()
                    ? (ObjectNode) node
                    : objectMapper.createObjectNode();
        } catch (Exception exception) {
            log.debug("忽略无法解析的 Agent 状态快照", exception);
            return objectMapper.createObjectNode();
        }
    }

    private JsonNode parseJson(String raw) {
        if (!StringUtils.hasText(raw)) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode node = objectMapper.readTree(raw);
            return node == null ? objectMapper.createObjectNode() : node;
        } catch (Exception exception) {
            return objectMapper.createObjectNode();
        }
    }

    private String jsonValue(JsonNode node, String fallback) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return fallback;
        }
        return writeJson(node);
    }

    private String writeJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private String textValue(ObjectNode object, String field) {
        JsonNode value = object.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private Long extractDraftId(ObjectNode snapshot) {
        Long direct = longValue(snapshot.get("draft_id"));
        if (direct != null) {
            return direct;
        }
        direct = longValue(snapshot.get("draftId"));
        if (direct != null) {
            return direct;
        }
        JsonNode payload = snapshot.get("payload");
        if (payload != null && payload.isObject()) {
            direct = longValue(payload.get("draftId"));
            if (direct != null) {
                return direct;
            }
        }
        JsonNode slots = snapshot.get("slots");
        if (slots != null && slots.isObject()) {
            return longValue(slots.get("draftId"));
        }
        return null;
    }

    private Long longValue(JsonNode value) {
        if (value == null || value.isNull() || !value.isNumber()
                && !value.isTextual()) {
            return null;
        }
        try {
            return Long.valueOf(value.asText());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String extractCardsJson(String responseJson) {
        ObjectNode response = parseObject(responseJson);
        return jsonValue(response.get("cards"), null);
    }

    private String extractTraceId(String responseJson) {
        ObjectNode response = parseObject(responseJson);
        String traceId = textValue(response, "traceId");
        return StringUtils.hasText(traceId)
                ? limitText(traceId, 36)
                : UUID.randomUUID().toString();
    }

    private String normalizeRequired(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        return value.trim();
    }

    /**
     * agent_session.id 是 varchar(36)。对历史 h5-xxx 会话做稳定转换，
     * 保证刷新页面后仍能映射到同一个数据库会话。
     */
    private String normalizeSessionId(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return UUID.fromString(trimmed).toString();
        } catch (IllegalArgumentException ignored) {
            return UUID.nameUUIDFromBytes(
                    trimmed.getBytes(StandardCharsets.UTF_8)).toString();
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String limitText(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}

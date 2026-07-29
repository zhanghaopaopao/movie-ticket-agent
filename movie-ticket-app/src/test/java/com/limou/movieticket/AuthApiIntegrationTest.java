package com.limou.movieticket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void setUpUser() {
        jdbcTemplate.update("DELETE FROM refresh_token");
        jdbcTemplate.update("DELETE FROM user_preference");
        jdbcTemplate.update("DELETE FROM app_user");
        jdbcTemplate.update("""
                INSERT INTO app_user(id,email,password_hash,role,status,login_failure_count,created_at,updated_at)
                VALUES ('user_test','user@test.local',?,'USER','ACTIVE',0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """, passwordEncoder.encode("Password123"));
    }

    @Test
    void loginReturnsTokenPairAndTraceId() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Trace-Id", "trace-auth-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"USER@test.local\",\"password\":\"Password123\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "trace-auth-test"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.traceId").value("trace-auth-test"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    void refreshRotatesTokenAndRejectsReusingTheOldToken() throws Exception {
        String oldToken = loginAndReadRefreshToken();
        String refreshResponse = mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TokenBody(oldToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andReturn().getResponse().getContentAsString();
        JsonNode newToken = objectMapper.readTree(refreshResponse).path("data").path("refreshToken");
        org.assertj.core.api.Assertions.assertThat(newToken.asText()).isNotBlank().isNotEqualTo(oldToken);

        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TokenBody(oldToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_REVOKED"));
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        String refreshToken = loginAndReadRefreshToken();
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TokenBody(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TokenBody(refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_REVOKED"));
    }

    @Test
    void invalidPasswordReturnsStableErrorAndEventuallyLocksAccount() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"user@test.local\",\"password\":\"Wrong123\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
        }
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@test.local\",\"password\":\"Password123\"}"))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("AUTH_ACCOUNT_LOCKED"));
    }

    @Test
    void userRoleCannotAccessAdminNamespace() throws Exception {
        mockMvc.perform(get("/api/v1/admin/not-implemented")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    private String loginAndReadRefreshToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@test.local\",\"password\":\"Password123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("refreshToken").asText();
    }

    private record TokenBody(String refreshToken) { }
}

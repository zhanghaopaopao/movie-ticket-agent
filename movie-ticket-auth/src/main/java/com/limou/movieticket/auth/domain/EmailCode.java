package com.limou.movieticket.auth.domain;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
@TableName("email_code")
public class EmailCode {
    @TableId private String id;
    private String email;
    private EmailCodePurpose purpose;
    private String codeHash;
    private LocalDateTime expiresAt;
    private Integer attempts;
    private LocalDateTime createdAt;
    public String getId() { return id; } public void setId(String id) { this.id = id; }
    public String getEmail() { return email; } public void setEmail(String email) { this.email = email; }
    public EmailCodePurpose getPurpose() { return purpose; } public void setPurpose(EmailCodePurpose purpose) { this.purpose = purpose; }
    public String getCodeHash() { return codeHash; } public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; } public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public Integer getAttempts() { return attempts; } public void setAttempts(Integer attempts) { this.attempts = attempts; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

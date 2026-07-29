package com.limou.movieticket.admin.domain;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
@TableName("system_config")
public class SystemConfig {
    @TableId private String configKey;
    private String configValue;
    private String valueType;
    private String description;
    private String updatedBy;
    private LocalDateTime updatedAt;
    public String getConfigKey() { return configKey; } public void setConfigKey(String configKey) { this.configKey = configKey; }
    public String getConfigValue() { return configValue; } public void setConfigValue(String configValue) { this.configValue = configValue; }
    public String getValueType() { return valueType; } public void setValueType(String valueType) { this.valueType = valueType; }
    public String getDescription() { return description; } public void setDescription(String description) { this.description = description; }
    public String getUpdatedBy() { return updatedBy; } public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

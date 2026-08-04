package com.szml.movieticket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 支付宝沙箱支付配置。密钥只从环境变量或本地未提交配置读取。 */
@Data
@Configuration
@ConfigurationProperties(prefix = "alipay")
public class AlipayProperties {

    private boolean enabled;
    private String appId;
    private String merchantPrivateKey;
    private String alipayPublicKey;
    private String gatewayUrl;
    private String notifyUrl;
    private String returnUrl;
    private String frontendReturnUrl;

    public boolean isConfigured() {
        return enabled && hasText(appId) && hasText(merchantPrivateKey)
                && hasText(alipayPublicKey) && hasText(gatewayUrl)
                && hasText(notifyUrl) && hasText(returnUrl);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

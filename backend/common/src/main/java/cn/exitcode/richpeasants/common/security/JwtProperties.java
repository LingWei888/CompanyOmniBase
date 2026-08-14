package cn.exitcode.richpeasants.common.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * HMAC secret,至少 32 字节
     */
    private String secret = "change-me-to-a-very-long-secret-key-32bytes";

    /**
     * Access Token 有效期（秒）
     */
    private long accessTokenExpireSeconds = 7200;

    /**
     * Refresh Token 有效期（秒）
     */
    private long refreshTokenExpireSeconds = 604800;
}

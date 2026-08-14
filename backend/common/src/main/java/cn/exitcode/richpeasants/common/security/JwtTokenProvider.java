package cn.exitcode.richpeasants.common.security;

import cn.exitcode.richpeasants.common.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_TOKEN_TYPE = "type";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId, String username, UserRole role) {
        return buildToken(userId, username, role, TYPE_ACCESS, jwtProperties.getAccessTokenExpireSeconds());
    }

    public String createRefreshToken(Long userId, String username, UserRole role) {
        return buildToken(userId, username, role, TYPE_REFRESH, jwtProperties.getRefreshTokenExpireSeconds());
    }

    private String buildToken(Long userId, String username, UserRole role, String type, long expireSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_TOKEN_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireSeconds)))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenType(Claims claims, String expectedType) {
        return expectedType.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
    }
}

package com.ax.template.authblueprint.auth;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class JwtTokenService {

    private static final long ACCESS_TOKEN_EXPIRY_HOURS = 1;

    private final JwtEncoder jwtEncoder;

    public JwtTokenService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public String generateAccessToken(String userId, String email, String role) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("auth-blueprint")
                .issuedAt(now)
                .expiresAt(now.plus(ACCESS_TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS))
                .subject(userId)
                .claim("email", email)
                .claim("role", role)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}

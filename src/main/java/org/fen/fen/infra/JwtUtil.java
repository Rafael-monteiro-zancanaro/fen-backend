package org.fen.fen.infra;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Date;

@Component
public class JwtUtil {
    private static final String JWT_SECRET = "d4bd384c-deb8-4e48-823c-daeb96924ca6";
    private static final Long AFTER_TEN_HOURS = System.currentTimeMillis() + 1000 * 60 * 60 * 10;


    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(AFTER_TEN_HOURS))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

}

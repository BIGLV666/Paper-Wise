package org.example.paperwise.Until;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.security.config.annotation.rsocket.RSocketSecurity;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
@Component
public class JwtUntil {
    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.expiration}")
    private Long expiration;


    public String generateToken(Long userId,String userName) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userName", userName);
        return Jwts.builder().setSubject(userId.toString())
                .claims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+expiration))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();

    }
    public Claims parseToken(String token) {
        return Jwts.parser().setSigningKey(secret)
                .build().parseSignedClaims(token).getBody();
    }
    public Long getUserIdFromToken(String token) {
        return Long.parseLong(parseToken(token).getSubject());

    }

}

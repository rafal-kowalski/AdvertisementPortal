package com.example.rk.security.jwt;

import com.example.rk.config.ApplicationProperties;
import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class TokenProvider {
    private final Logger log = LoggerFactory.getLogger(TokenProvider.class);

    private static final String AUTHORITIES_KEY = "auth";

    private final ApplicationProperties properties;

    public TokenProvider(ApplicationProperties properties) {
        this.properties = properties;
    }

    private String secretKey;
    private long tokenValidityInMilliseconds;
    private long tokenValidityInMillisecondsRememberMe;

    @PostConstruct
    public void init() {
        this.secretKey = properties.getSecurity().getJWT().getSecret();
        this.tokenValidityInMilliseconds = 1000 * properties.getSecurity().getJWT().getTokenValidityInSeconds();
        this.tokenValidityInMillisecondsRememberMe = 1000 * properties.getSecurity().getJWT().getTokenValidityInSecondsForRememberMe();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(secretKey)
            .parseClaimsJws(token)
            .getBody();

        Collection<? extends GrantedAuthority> authorities =
            Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        User principal = new User(claims.getSubject(), "", authorities);

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    public String createToken(Authentication authentication, boolean rememberMe) {
        String authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(","));

        long now = (new Date()).getTime();
        Date validity;
        if (rememberMe) {
            validity = new Date(now + this.tokenValidityInMillisecondsRememberMe);
        } else {
            validity = new Date(now + this.tokenValidityInMilliseconds);
        }

        return Jwts.builder()
            .setSubject(authentication.getName())
            .claim(AUTHORITIES_KEY, authorities)
            .signWith(SignatureAlgorithm.HS512, secretKey)
            .setExpiration(validity)
            .compact();
    }

    public boolean validate(String authToken) {
        try {
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(authToken);
            return true;
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT");
            log.info("Expired JWT trace: {}", e);
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT");
            log.trace("Unsupported JWT trace: {}", e);
        } catch (MalformedJwtException e) {
            log.info("Malformed JWT");
            log.trace("Malformed JWT trace: {}", e);
        } catch (SignatureException e) {
            log.info("Invalid JWT signature.");
            log.trace("Invalid JWT signature trace: {}", e);
        } catch (IllegalArgumentException e) {
            log.info("JWT token compact of handler are invalid.");
            log.trace("JWT token compact of handler are invalid trace: {}", e);
        }
        return false;
    }
}

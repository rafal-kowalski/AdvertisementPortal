package com.example.rk.security.jwt;

import com.example.rk.config.ApplicationProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class TokenProviderTest {
    private final String secretKey = "SecretForTest";
    private final long MINUTE = 60_000;
    private ApplicationProperties propertiesMock;
    private TokenProvider sut;

    @Before
    public void setUp() throws Exception {
        propertiesMock = Mockito.mock(ApplicationProperties.class);
        sut = new TokenProvider(propertiesMock);
        ReflectionTestUtils.setField(sut, "secretKey", secretKey);
        ReflectionTestUtils.setField(sut, "tokenValidityInMilliseconds", MINUTE);
    }

    @Test
    public void shouldReturnFalseWhenTokenIsUnsupported() {
        //given
        String unsupportedToken = createUnsupportedToken();

        //when
        boolean isTokenValid = sut.validate(unsupportedToken);

        //then
        assertThat(isTokenValid).isEqualTo(false);
    }

    @Test
    public void shouldReturnFalseWhenTokenIsInvalid() {
        //when
        boolean isTokenValid = sut.validate("");

        //then
        assertThat(isTokenValid).isEqualTo(false);
    }

    @Test
    public void shouldReturnFalseWhenTokenHasInvalidSignature() {
        //when
        boolean isTokenValid = sut.validate(createTokenWithDifferentSignature());

        //then
        assertThat(isTokenValid).isEqualTo(false);
    }

    @Test
    public void shouldReturnFalseWhenTokenIsMalformed() {
        //given
        Authentication authentication = createAuthentication();
        String token = sut.createToken(authentication, false);
        String invalidToken = token.substring(1);

        //when
        boolean isTokenValid = sut.validate(invalidToken);

        //then
        assertThat(isTokenValid).isEqualTo(false);
    }

    @Test
    public void shouldReturnFalseWhenTokenIsExpired() {
        //given
        ReflectionTestUtils.setField(sut, "tokenValidityInMilliseconds", -MINUTE);

        Authentication authentication = createAuthentication();
        String token = sut.createToken(authentication, false);

        //when
        boolean isTokenValid = sut.validate(token);

        //then
        assertThat(isTokenValid).isEqualTo(false);
    }

    private Authentication createAuthentication() {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ANONYMOUS"));
        return new UsernamePasswordAuthenticationToken("anonymous", "anonymous", authorities);
    }

    private String createUnsupportedToken() {
        return Jwts.builder()
            .setPayload("payload")
            .signWith(SignatureAlgorithm.HS512, secretKey)
            .compact();
    }

    private String createTokenWithDifferentSignature() {
        return Jwts.builder()
            .setSubject("anonymous")
            .signWith(SignatureAlgorithm.HS512, "DifferentSecretForTest")
            .setExpiration(new Date(new Date().getTime() + MINUTE))
            .compact();
    }
}

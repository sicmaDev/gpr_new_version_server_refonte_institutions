package com.sicmagroup.gpr.service.jwt;

import java.util.Map;

import org.springframework.security.core.userdetails.UserDetails;

public interface JwtService {
    String extracUserName(String token);

    String generateToken(Map<String, Object> extraClaims, UserDetails userDetails);

    String generateToken(UserDetails userDetails);

    boolean isTokenValid(String token, UserDetails userDetails);

    boolean isTokenExpired(String token);
}

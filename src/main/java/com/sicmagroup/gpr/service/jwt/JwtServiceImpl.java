package com.sicmagroup.gpr.service.jwt;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtServiceImpl implements JwtService {

    /** HS256 exige une clé d'au moins 256 bits. */
    static final int MIN_KEY_BYTES = 32;

    private final Key signInKey;

    /**
     * Clé de signature lue depuis gpr.jwt.secret (variable d'environnement GPR_JWT_SECRET, Base64).
     * L'application refuse de démarrer si elle est absente ou trop courte.
     */
    public JwtServiceImpl(Environment environment) {
        this.signInKey = loadKey(environment);
    }

    static Key loadKey(Environment environment) {
        String help = " Générez-la avec : openssl rand -base64 64";
        String secret;
        try {
            secret = environment.getProperty("gpr.jwt.secret");
        } catch (IllegalArgumentException e) {
            // Placeholder ${GPR_JWT_SECRET} non résolu : variable d'environnement absente
            secret = null;
        }
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Démarrage impossible : GPR_JWT_SECRET est absente." + help);
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret.trim());
        } catch (RuntimeException e) {
            throw new IllegalStateException("Démarrage impossible : GPR_JWT_SECRET n'est pas une valeur Base64 valide." + help);
        }
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("Démarrage impossible : GPR_JWT_SECRET doit faire au moins "
                    + MIN_KEY_BYTES + " octets (reçu : " + keyBytes.length + ")." + help);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public String extracUserName(String token) {

        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver){
        final Claims claims = extractAllClaims(token);

        return claimsResolver.apply(claims);
    }



    private Claims extractAllClaims(String token){
        return Jwts.parserBuilder().setSigningKey(getSignInKey()).build().parseClaimsJws(token).getBody();
    }

    private Key getSignInKey() {
        return signInKey;
    }


    @Override
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
            .setClaims(extraClaims)
            .setSubject(userDetails.getUsername())
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + 86400000 ))//7jours
            .signWith(getSignInKey(), SignatureAlgorithm.HS256).compact();
    }

    @Override
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extracUserName(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    @Override
    public boolean isTokenExpired(String token) {
        return extractExperiation(token).before(new Date());
    }

    private Date extractExperiation(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    @Override
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<String, Object>(), userDetails);
    }

    

  

    
    
}

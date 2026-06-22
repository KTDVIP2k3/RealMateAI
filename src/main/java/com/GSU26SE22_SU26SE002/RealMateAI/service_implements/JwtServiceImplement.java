package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.repositories.AccountRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtServiceImplement {
    @Value("${app.jwt.secret}")
    private String privateKey;

    private static final long TEMP_TOKEN_VALIDITY = 1000 * 60 * 60 * 5;

    @Autowired
    private AccountRepository accountRepository;

    private Key getSignKey(){return Keys.hmacShaKeyFor(privateKey.getBytes());}

    public String generateToken(String userName, String roleName, String email){
        Map<String, String> claims = new HashMap<>();
        claims.put("role", roleName);
        claims.put("email", email);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userName)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 12))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token, String userDetails, String role) {
        final String username = extractUsername(token);
        final String extractedRole = extractRole(token);
        return (username.equals(userDetails) && extractedRole.equalsIgnoreCase(role) && !isTokenExpired(token));
    }

    public Date extractExpiration(String token) {

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()


                .parseClaimsJws(token)


                .getBody();


        return claims.getExpiration();
    }


    public String generate2faToken(String username, String email) {


        Map<String, Object> claims = new HashMap<>();


        claims.put("token_type", "2FA_PENDING");


        claims.put("email", email);


        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now);
        Date expiration = new Date(now + TEMP_TOKEN_VALIDITY);


        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }


    public boolean isTokenExpired(String token){
        return extractExpiration(token).before(new Date());
    }


    public String extractUsername(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()


                .parseClaimsJws(token)


                .getBody();


        return claims.getSubject();
    }

    public String extractGmail (String token) {

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()

                .parseClaimsJws(token)


                .getBody();

        return claims.get("gmail", String.class);
    }

    public String extractRole(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()

                .parseClaimsJws(token)

                .getBody();

        return claims.get("role", String.class);
    }
}

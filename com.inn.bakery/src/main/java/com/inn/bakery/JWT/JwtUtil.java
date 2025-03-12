package com.inn.bakery.JWT;

import com.google.common.base.Function;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtUtil {

    private String secret = "secret"; // Your secret key for JWT signing

    // Extract username (subject) from the token
    public String extractUsername(String token) {
        return extractClaims(token, Claims::getSubject);
    }

    // Extract expiration date from the token
    public Date extractExpiration(String token) {
        return extractClaims(token, Claims::getExpiration);
    }

    // Extract any claims from the token using a custom claimsResolver function
    public <T> T extractClaims(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        if (claims == null || claims.isEmpty()) {
            // Handle the case where claims are empty or null
            return null; // or throw an exception, or return a default value
        }
        return claimsResolver.apply(claims);
    }

    // Extract all claims from the token
    public Claims extractAllClaims(String token) {
        if(token == null){
            return null;
        }
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    }

    // Check if the token has expired
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Generate a JWT token using username and role
    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role); // Set the role claim

        // Create and sign the token
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username) // Set the subject (username)
                .setIssuedAt(new Date(System.currentTimeMillis())) // Set the issue time
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // Set expiration time (10 hours)
                .signWith(SignatureAlgorithm.HS256, secret) // Sign the token
                .compact();
    }

    // This method is redundant and has issues due to undefined variables (claims, subject)
    // private String createToken(UserDetails userDetails) {
    //     return Jwts.builder()
    //             .setClaims(claims) // claims is not defined in this method
    //             .setSubject(subject) // subject is not defined in this method
    //             .setIssuedAt(new Date(System.currentTimeMillis()))
    //             .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))
    //             .signWith(SignatureAlgorithm.HS256,secret).compact();
    // }

    // Validate the JWT token
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        if(username == null){
            return true;
        }
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

}

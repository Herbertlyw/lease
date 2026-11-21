package com.atguigu.lease.common.utils;

import com.atguigu.lease.common.exception.LeaseException;
import com.atguigu.lease.common.result.ResultCodeEnum;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

public class JwtUtil {

    private static long tokenExpiration = 60 * 60 * 1000L;
    private static final SecretKey tokenSignKey = signingKey();

    private static SecretKey signingKey() {
        String configured = System.getenv("LEASE_JWT_KEY");
        return configured == null ? Keys.secretKeyFor(SignatureAlgorithm.HS256)
                : Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(configured));
    }

    public static String createToken(Long userId, String username) {
        String token = Jwts.builder().
                setSubject("LOGIN_USER").
                setExpiration(new Date(System.currentTimeMillis() + tokenExpiration)).
                claim("userId", userId).
                claim("username", username).
                signWith(tokenSignKey, SignatureAlgorithm.HS256).
                compact();
        return token;
    }

    public static Claims parseToken(String token){

        if (token==null){
            throw new LeaseException(ResultCodeEnum.ADMIN_LOGIN_AUTH);
        }

        try{
            JwtParser jwtParser = Jwts.parserBuilder().setSigningKey(tokenSignKey).build();
            Jws<Claims> claimsJws=jwtParser.parseClaimsJws(token);
            Claims body = claimsJws.getBody();
            return body;
        }catch (ExpiredJwtException e){
            throw new LeaseException(ResultCodeEnum.TOKEN_EXPIRED);
        }catch (JwtException e){
            throw new LeaseException(ResultCodeEnum.TOKEN_INVALID);
        }
    }

}

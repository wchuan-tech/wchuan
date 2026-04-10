package com.wchuan.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

public class JwtUtil {
    public static final Long JWT_TTL = 60 * 60 * 1000L;// 60 * 60 * 1000 有效期为 1h

    public static final String JWT_KEY = "wchuan1234567890wchuan1234567890";


    public static String getUUID(){
        return UUID.randomUUID().toString().replaceAll("-", "");
    }


    // 最基础的方法（真正干活）
    public static String createJWT(String id, String subject, Long ttlMillis) {
        JwtBuilder builder = getJwtBuilder(subject, ttlMillis, id);
        return builder.compact();
    }

    // 重载1：自动生成id
    public static String createJWT(String subject, Long ttlMillis) {
        return createJWT(getUUID(), subject, ttlMillis);
    }

    // 重载2：自动生成id + 使用默认过期时间
    public static String createJWT(String subject) {
        return createJWT(subject, null);
    }

    private static JwtBuilder getJwtBuilder(String subject,Long ttlMillis,String uuid){
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        SecretKey secretKey = generalKey();
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        if(ttlMillis == null){
            ttlMillis = JWT_TTL;
        }

        long expMillis = nowMillis + ttlMillis;
        Date expDate = new Date(expMillis);
        return Jwts.builder()
                .setId(uuid)
                .setSubject(subject)
                .setIssuer("wchuan")
                .setIssuedAt(now)
                .signWith(signatureAlgorithm, secretKey)
                .setExpiration(expDate);


    }

    private static SecretKey generalKey() {
        byte[] encodeKey = Base64.getDecoder().decode(JWT_KEY);
        return new SecretKeySpec(encodeKey, 0, encodeKey.length, "AES");
    }

    public static Claims parseJWT(String jwt){
        SecretKey secretKey = generalKey();
        return Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(jwt)
                .getBody();
    }
}
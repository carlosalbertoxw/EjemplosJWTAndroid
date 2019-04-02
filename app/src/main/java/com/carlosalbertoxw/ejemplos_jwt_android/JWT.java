package com.carlosalbertoxw.ejemplos_jwt_android;

import android.util.Log;

import java.util.Date;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class JWT {
    private static final String KEY = "x3KBDLNlGlZ7C58pViJXbuSxc1ejfgAt9jo20KLQMy6wASjl9CZYnJZCjLYUKUDZoHj9jiBT1dwryXRwzm6Pgdq8SBUH0ZYBdHq0iLDHAPX746qpvZ3l1k1FeoC7mJwyfnhhe3JrnVuHOElBa8M4g8g5yNoG2aKjDTot4mRQaP5ifg4d5ps7hDrnT6S1eZuo6leuCiLa";

    public String createJWT(Integer IdUser) {
        try {
            Date date = new Date();
            date.setTime(date.getTime() + 1000 * 60 * 1);
            String compactJws = Jwts.builder()
                    .setExpiration(date)
                    .claim("id", IdUser)
                    .signWith(SignatureAlgorithm.HS512,KEY.getBytes("UTF-8"))
                    .compact();
            return compactJws;
        } catch (Exception e) {
            Log.e("jwt",e.getMessage());
        }
        return null;
    }

    public Integer verifyJWT(String compactJws) {
        try {
            Claims claims = Jwts.parser().setSigningKey(KEY.getBytes("UTF-8")).parseClaimsJws(compactJws).getBody();
            if (claims.get("id") != null && !claims.get("id").equals("")) {
                return (Integer) claims.get("id");
            }
        } catch (ExpiredJwtException e) {
            Log.e("jwt",e.getMessage());
        } catch (Exception e) {
            Log.e("jwt",e.getMessage());
        }
        return null;
    }
}

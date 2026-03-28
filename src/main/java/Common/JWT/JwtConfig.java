package Common.JWT;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Date;


//A configuration class to manage JWT params
//https://www.cnblogs.com/java-note/p/18787761
@Component
@ConfigurationProperties(prefix = "config.jwt")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class JwtConfig {
    private String secret;
    private long expire;
    private String header;

    public String createToken(String subject) {
        Date nowDate = new Date();
        Date expireDate = new Date(nowDate.getTime() + expire * 1000);

        return Jwts.builder()
                //Header
                .setHeaderParam("typ", "JWT")
                .setSubject(subject)
                .setIssuedAt(nowDate )
                .setExpiration(expireDate)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    public Claims getTokenClaim(String token) {
        try {
            return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isTokenExpired(Date expirationTime) {
        return expirationTime.before(new Date());
    }

    public Date getExpirationDateFromToken(String token) {
        return getTokenClaim(token).getExpiration();
    }

    public String getUsernameFromToken(String token) {
        return getTokenClaim(token).getSubject();
    }

    public Date getIssuedAtDateFromToken(String token) {
        return getTokenClaim(token).getIssuedAt();
    }
}

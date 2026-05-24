package Common.UserContext;

import com.hybriddependcyanlysis.POJO.DTO.UserLoginDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Component
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. Parse logged-in user from request header / Token (example: simplified Token parsing logic)
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            UserLoginDTO user = parseToken(token.substring(7)); // Custom Token parsing logic
            UserContextHolder.setUser(user);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 2. Force cleanup after request completion (critical! prevents data leakage from thread reuse)
        UserContextHolder.clear();
    }

    // Simulate Token parsing
    private UserLoginDTO parseToken(String token) {
        // Actual scenario: query user info from Redis/database
        return new UserLoginDTO(1, "admin", token, Arrays.asList("admin:all"));
    }
}

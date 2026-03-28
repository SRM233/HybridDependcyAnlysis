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
        // 1. 从请求头/Token 解析登录用户（示例：简化 Token 解析逻辑）
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            UserLoginDTO user = parseToken(token.substring(7)); // 自定义 Token 解析逻辑
            UserContextHolder.setUser(user);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 2. 请求结束后强制清理（核心！避免线程复用导致数据串用）
        UserContextHolder.clear();
    }

    // 模拟 Token 解析
    private UserLoginDTO parseToken(String token) {
        // 实际场景：从 Redis/数据库查询用户信息
        return new UserLoginDTO(1, "admin", token, Arrays.asList("admin:all"));
    }
}

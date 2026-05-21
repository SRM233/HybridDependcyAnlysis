package Common.JWT;

import Common.UserContext.UserContextHolder;
import com.hybriddependcyanlysis.POJO.DAO.UserDAO;
import com.hybriddependcyanlysis.POJO.DTO.UserLoginDTO;
import com.hybriddependcyanlysis.Service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


//JwtInterceptor
@Component
public class JwtInterceptor implements HandlerInterceptor {
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        final String authHeader = request.getHeader(jwtConfig.getHeader());

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            final String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {

                String username = jwtUtil.getUsernameFromToken(token);
                UserDAO userDAO = userService.getUserByName(username);
                UserLoginDTO userLoginDTO = new UserLoginDTO();
                userLoginDTO.setUser(userDAO);
                userLoginDTO.setToken(token);
                UserContextHolder.setUser(userLoginDTO);
                return true;
            }
        }

        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        return false;
    }
}
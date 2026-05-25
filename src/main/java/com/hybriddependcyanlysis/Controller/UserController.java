package com.hybriddependcyanlysis.Controller;

import Common.JWT.JwtUtil;
import Common.Result;
import Common.UserContext.UserContextHolder;
import com.github.javaparser.utils.Log;
import com.hybriddependcyanlysis.Mapper.UserMapper;
import com.hybriddependcyanlysis.POJO.DAO.UserDAO;
import com.hybriddependcyanlysis.POJO.DTO.UserDTO;
import com.hybriddependcyanlysis.POJO.DTO.UserLoginDTO;
import com.hybriddependcyanlysis.Service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {


    // Inject UserService layer
    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;


    // User registration request
    @PostMapping("/register")
    public Result<UserLoginDTO> register(@RequestBody  UserDTO userDTO) {
        log.info("Registering user:{}", userDTO);
        try {
            userService.register(userDTO);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    // User login request
    @GetMapping("/login")
    public Result<UserLoginDTO> login(@RequestParam String username, String password) {

        // Accept username and password parameters
//        log.info("Logining username:{}",userLoginDTO);
        try {
            UserDAO userDAO = userService.login(username, password);
            userDAO.setPassword(null);
            String token = jwtUtil.generateToken(userDAO.getUsername());
            UserLoginDTO userLoginDTO = new UserLoginDTO();
            userLoginDTO.setToken(token);
            userLoginDTO.setUser(userDAO);
            return Result.success("Login success", userLoginDTO);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }



    @GetMapping("/getUser")
    public Result<UserDAO> getUser()
    {
        log.info("getUser: {}", UserContextHolder.getUserId());
        Integer userId = UserContextHolder.getUserId();
        UserContextHolder.clear();
        return Result.success(userService.getUser(userId));
    }

    // User logout request
    @PostMapping("/logout")
    public Result logout()
    {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.fail("User not authenticated");
        }
        log.info("User logout: {}", userId);
        userService.logout();
        return Result.success("Logged out successfully");
    }

    @DeleteMapping("/delete")
    public Result deleteUser()
    {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.fail("User not authenticated");
        }
        log.info("Deleting user: {}", userId);
        userService.deleteUser(userId);
        return Result.success("User deleted successfully");
    }
}
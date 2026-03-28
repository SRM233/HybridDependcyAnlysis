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


    //注入UserService层
    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;


    //用户注册请求
    @PostMapping("/register")
    public Result<UserLoginDTO> register(@RequestBody  UserDTO userDTO) {
        log.info("Registering user:{}", userDTO);
        userService.register(userDTO);

        return Result.success();
    }

    //用户登陆请求
    @GetMapping("/login")
    public Result<UserLoginDTO> login(@RequestParam String username, String password) {

        //接受username和password参数
//        log.info("Logining username:{}",userLoginDTO);
        UserDAO userDAO = userService.login(username, password);
        //如果没有获取相应的userDAO则返回错误

        if(userDAO == null)
        {
            return Result.fail("Invalid username or password");
        }
        // 清除密码，不返回给前端
        userDAO.setPassword(null);
        String token = jwtUtil.generateToken(userDAO.getUsername());
        UserLoginDTO userLoginDTO = new UserLoginDTO();
        userLoginDTO.setToken(token);
        userLoginDTO.setUser(userDAO);

        return Result.success("Login success", userLoginDTO);
    }

    //
    @DeleteMapping("/deleteSourdeFolders")
    public Result deleteSourdeFolders(Integer sourceFolderId)
    {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.fail("User not authenticated");
        }
        log.info("user delete: {}", userId);
        userService.deleteSourdeFolders(userId, sourceFolderId);
        return Result.success();
    }

    @DeleteMapping("/deleteAstFiles")
    public Result deleteAstFiles(Integer outputId)
    {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.fail("User not authenticated");
        }
        log.info("user delete: {}", userId);
        userService.deleteASTFiles(userId, outputId);
        return Result.success();
    }

    @DeleteMapping("/deleteJspAstFiles")
    public Result deleteJspAstFiles(Integer outputId)
    {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.fail("User not authenticated");
        }
        log.info("user delete: {}", userId);
        userService.deleteJspAstFiles(userId, outputId);
        return Result.success();
    }

    @GetMapping("/getUser")
    public Result<UserDAO> getUser()
    {
        log.info("getUser: {}", UserContextHolder.getUserId());
        Integer userId = UserContextHolder.getUserId();
        UserContextHolder.clear();
        return Result.success(userService.getUser(userId));
    }

    //用户退出请求
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
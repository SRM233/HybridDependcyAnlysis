package com.hybriddependcyanlysis.Controller;

import Common.Result;
import com.hybriddependcyanlysis.Mapper.UserMapper;
import com.hybriddependcyanlysis.POJO.DAO.UserDAO;
import com.hybriddependcyanlysis.POJO.DTO.UserDTO;
import com.hybriddependcyanlysis.Service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {


    @Autowired
    private UserService userService;


    @PostMapping("register")
    public Result register(@RequestBody  UserDTO userDTO) {
        log.info("Registering user:{}", userDTO);
        userService.register(userDTO);
        return Result.success();
    }

    @GetMapping("/login")
    public Result<String> login(String userName, String password) {

        log.info("Logining username:{}" + ", password:{}", userName, password);
        UserDAO userDAO = userService.login(userName, password);
        if(userDAO == null)
        {
            return Result.fail("No user found");
        }
        return Result.success("login");
    }

    @DeleteMapping("/deleteSourdeFolders")
    public Result deleteSourdeFolders(Integer userId, Integer sourceFolderId)
    {
        log.info("user delete: {}", userId);
        userService.deleteSourdeFolders(userId, sourceFolderId);
        return Result.success();
    }

    @DeleteMapping("/deleteAstFiles")
    public Result deleteAstFiles(Integer userId, Integer outputId)
    {
        log.info("user delete: {}", userId);
        userService.deleteASTFiles(userId, outputId);
        return Result.success();
    }

    @DeleteMapping("/deleteJspAstFiles")
    public Result deleteJspAstFiles(Integer userId, Integer outputId)
    {
        log.info("user delete: {}", userId);
        userService.deleteJspAstFiles(userId, outputId);
        return Result.success();
    }
}
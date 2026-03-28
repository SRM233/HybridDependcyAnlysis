package com.hybriddependcyanlysis.Controller;

import Common.Result;
import Common.UserContext.UserContextHolder;
import com.hybriddependcyanlysis.POJO.DTO.UserDTO;
import com.hybriddependcyanlysis.Service.AstService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/ast")
public class AstController {

    @Autowired
    private AstService astService;

    @GetMapping("/getOutput")
    public Result getOutput(Integer sourceFolderId)
    {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.fail("User not authenticated");
        }
        log.info("userId:{}, sourceFolderId:{}", userId, sourceFolderId);
        astService.getOutPutFile(userId, sourceFolderId);
        return Result.success();
    }

    @PostMapping("/mappingAst")
    public Result mapping(Integer sourceFolderId) throws IOException {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.fail("User not authenticated");
        }
        astService.MappingOutPutFile(userId, sourceFolderId);
        return Result.success();
    }

    @DeleteMapping("/deleteOutPutReport")
    public Result deleteOutputReport(Integer sourceFolderId)
    {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.fail("User not authenticated");
        }
        astService.deleteOutputReport(userId, sourceFolderId);
        return Result.success();
    }

    @PostMapping("/mappingJsp")
    public Result jspMapping(@RequestBody UserDTO userDTO)
    {
        log.info("userDTO:{}", userDTO);
        astService.jspMapping(userDTO);
        return Result.success();
    }

}

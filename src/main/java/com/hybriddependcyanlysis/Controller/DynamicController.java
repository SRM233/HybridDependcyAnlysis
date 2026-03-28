package com.hybriddependcyanlysis.Controller;

import Common.Result;
import Common.UserContext.UserContextHolder;
import com.hybriddependcyanlysis.Service.DynamicAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/dynamic")
public class DynamicController {

    @Autowired
    private DynamicAnalysisService dynamicAnalysisService;

    @PostMapping()
    public Result dynamicAnalysis() throws IOException {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.fail("User not authenticated");
        }
        log.info("user Id: {}", userId);

        dynamicAnalysisService.jarPack(userId);

        return Result.success();
    }

    @PostMapping("/javaAgent")
    public Result javaAgent()
    {
        log.info("Java Agent Execute");

        dynamicAnalysisService.javaAgent();

        return Result.success();
    }


}

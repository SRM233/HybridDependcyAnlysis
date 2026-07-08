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
    public Result dynamicAnalysis(Integer sourceFolderId) throws IOException {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.fail("User not authenticated");
        }
        log.info("dynamic analysis for sourceFolderId: {}", sourceFolderId);

        dynamicAnalysisService.jarPack(sourceFolderId);

        return Result.success();
    }

    @PostMapping("/javaAgent")
    public Result javaAgent(Integer sourceFolderId)
    {
        log.info("Java Agent Execute for sourceFolderId: {}", sourceFolderId);

        dynamicAnalysisService.javaAgent(sourceFolderId);

        return Result.success();
    }


}

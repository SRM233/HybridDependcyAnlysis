package com.hybriddependcyanlysis.Controller;

import Common.Result;
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
    public Result dynamicAnalysis(Integer id) throws IOException {
        log.info("user Id: {}", id);

        dynamicAnalysisService.jarPack(id);

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

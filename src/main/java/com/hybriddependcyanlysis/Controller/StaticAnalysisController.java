package com.hybriddependcyanlysis.Controller;

import Common.Result;
import com.hybriddependcyanlysis.Service.StaticAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@RestController
@RequestMapping("StaticAnalysis")
@Slf4j
public class StaticAnalysisController {


    @Autowired
    private StaticAnalysisService staticAnalysisService;

    @GetMapping("/dependencyCount")
    public Result<HashMap<String, Integer>> dependencyCount(Integer userId, Integer sourceFolderId)
    {
        HashMap<String, Integer> count = staticAnalysisService.dependencyCounting(userId, sourceFolderId);
        return Result.succes(count);
    }
}

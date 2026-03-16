package com.hybriddependcyanlysis.Controller;

import Common.Result;
import com.hybriddependcyanlysis.POJO.DAO.AnalysisResultsDAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/reports")
public class AnalysisReportController {


    @GetMapping("/AnnotationReport")
    public Result<AnalysisResultsDAO> getAnnotationReport()
    {
        return null;
    }
}

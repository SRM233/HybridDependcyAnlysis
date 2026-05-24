package com.hybriddependcyanlysis.Controller;

import Common.Result;
import com.hybriddependcyanlysis.POJO.DTO.AnalysisResultDTO;
import com.hybriddependcyanlysis.Service.AnalysisReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/reports")
public class AnalysisReportController {

    @Autowired
    private AnalysisReportService analysisReportService;

    @SuppressWarnings("unchecked")
    private Result<Object> wrapReport(Object result) {
        if (result instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) result;
            if (map.containsKey("code") && map.containsKey("message")) {
                return Result.fail((String) map.get("message"));
            }
        }
        return Result.success(result);
    }

    @GetMapping("/AnnotationReport")
    public Result<Object> getAnnotationReport(@RequestBody AnalysisResultDTO analysisResultDTO) {
        return wrapReport(analysisReportService.getAnnotationReport(analysisResultDTO));
    }

    @GetMapping("/WebXmlReport")
    public Result<Object> getWebXmlReport(@RequestBody AnalysisResultDTO analysisResultDTO) {
        return wrapReport(analysisReportService.getWebXmlReport(analysisResultDTO));
    }

    @GetMapping("/FileStoreReport")
    public Result<Object> getFileStoreReport(@RequestBody AnalysisResultDTO analysisResultDTO) {
        return wrapReport(analysisReportService.getFileStoreReport(analysisResultDTO));
    }

    @GetMapping("/PersistenceReport")
    public Result<Object> getPersistenceReport(@RequestBody AnalysisResultDTO analysisResultDTO) {
        return wrapReport(analysisReportService.getPersistenceReport(analysisResultDTO));
    }

    @GetMapping("/EjbJarReport")
    public Result<Object> getEjbJarReport(@RequestBody AnalysisResultDTO analysisResultDTO) {
        return wrapReport(analysisReportService.getEjbJarReport(analysisResultDTO));
    }

    @GetMapping("/PomXmlReport")
    public Result<Object> getPomXmlReport(@RequestBody AnalysisResultDTO analysisResultDTO) {
        return wrapReport(analysisReportService.getPomXmlReport(analysisResultDTO));
    }

    @GetMapping("/FacesConfigReport")
    public Result<Object> getFacesConfigReport(@RequestBody AnalysisResultDTO analysisResultDTO) {
        return wrapReport(analysisReportService.getFacesConfigReport(analysisResultDTO));
    }
}

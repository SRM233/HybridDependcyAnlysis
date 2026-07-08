package com.hybriddependcyanlysis.Controller;

import Common.Result;
import com.hybriddependcyanlysis.POJO.DTO.AnalysisResultDTO;
import com.hybriddependcyanlysis.Service.AnalysisReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping("/AnnotationReport")
    public Result<Object> getAnnotationReport(@RequestBody AnalysisResultDTO analysisResultDTO) {
        return wrapReport(analysisReportService.getAnnotationReport(analysisResultDTO));
    }

    @PostMapping("/WebXmlReport")
    public Result<Object> getWebXmlReport(@RequestBody AnalysisResultDTO analysisResultDTO) {
        return wrapReport(analysisReportService.getWebXmlReport(analysisResultDTO));
    }

    @PostMapping("/FileStoreReport")
    public Result<Object> getFileStoreReport(@RequestBody AnalysisResultDTO analysisResultDTO) {
        return wrapReport(analysisReportService.getFileStoreReport(analysisResultDTO));
    }

    @PostMapping("/PersistenceReport")
    public Result<Object> getPersistenceReport(@RequestBody AnalysisResultDTO analysisResultDTO) {
        return wrapReport(analysisReportService.getPersistenceReport(analysisResultDTO));
    }

    @PostMapping("/EjbJarReport")
    public Result<Object> getEjbJarReport(@RequestBody AnalysisResultDTO analysisResultDTO) {
        return wrapReport(analysisReportService.getEjbJarReport(analysisResultDTO));
    }

    @PostMapping("/PomXmlReport")
    public Result<Object> getPomXmlReport(@RequestBody AnalysisResultDTO analysisResultDTO) {
        return wrapReport(analysisReportService.getPomXmlReport(analysisResultDTO));
    }

    @PostMapping("/FacesConfigReport")
    public Result<Object> getFacesConfigReport(@RequestBody AnalysisResultDTO analysisResultDTO) {
        return wrapReport(analysisReportService.getFacesConfigReport(analysisResultDTO));
    }

    @PostMapping("/JspContentReport")
    public Result<Object> getJspContentReport(@RequestBody AnalysisResultDTO analysisResultDTO) {
        return wrapReport(analysisReportService.getJspContentReport(analysisResultDTO));
    }

    @PostMapping("/JsfContentReport")
    public Result<Object> getJsfContentReport(@RequestBody AnalysisResultDTO analysisResultDTO) {
        return wrapReport(analysisReportService.getJsfContentReport(analysisResultDTO));
    }

    @PostMapping("/JspFileCountReport")
    public Result<Object> getJspFileCountReport(@RequestBody AnalysisResultDTO analysisResultDTO) {
        return wrapReport(analysisReportService.getJspFileCountReport(analysisResultDTO));
    }

    @PostMapping("/JsfFileCountReport")
    public Result<Object> getJsfFileCountReport(@RequestBody AnalysisResultDTO analysisResultDTO) {
        return wrapReport(analysisReportService.getJsfFileCountReport(analysisResultDTO));
    }
}

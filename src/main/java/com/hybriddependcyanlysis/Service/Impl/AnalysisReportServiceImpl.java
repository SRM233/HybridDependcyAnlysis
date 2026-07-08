package com.hybriddependcyanlysis.Service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hybriddependcyanlysis.Mapper.AnalysisReportMapper;
import com.hybriddependcyanlysis.POJO.DAO.AnalysisReportDAO;
import com.hybriddependcyanlysis.POJO.DTO.AnalysisResultDTO;
import com.hybriddependcyanlysis.Service.AnalysisReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class AnalysisReportServiceImpl implements AnalysisReportService {

    @Autowired
    private AnalysisReportMapper analysisReportMapper;

    private Object getReportByMapperFunction(AnalysisResultDTO analysisResultDTO, Function<AnalysisResultDTO, AnalysisReportDAO> mapperFunction) {
        AnalysisReportDAO analysisReportDAO = mapperFunction.apply(analysisResultDTO);
        if(analysisReportDAO == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("code", 404);
            error.put("message", "Report not generated. Please run the analysis on the Analysis Results page first");
            return error;
        }

        File filePath = new File(analysisReportDAO.getPath());
        if(!filePath.exists()) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("code", 404);
            error.put("message", "Report file does not exist: " + filePath.getAbsolutePath() + ". Please re-run the analysis");
            return error;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(filePath, Object.class);
        } catch (IOException e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("code", 500);
            error.put("message", "Failed to read report file. File may be corrupted");
            return error;
        }
    }

    @Override
    public Object getAnnotationReport(AnalysisResultDTO analysisResultDTO) {
        return getReportByMapperFunction(analysisResultDTO, analysisReportMapper::getAnnotationReport);
    }

    @Override
    public Object getWebXmlReport(AnalysisResultDTO analysisResultDTO) {
        return getReportByMapperFunction(analysisResultDTO, analysisReportMapper::getWebXmlReport);
    }

    @Override
    public Object getFileStoreReport(AnalysisResultDTO analysisResultDTO) {
        return getReportByMapperFunction(analysisResultDTO, analysisReportMapper::getFileStoreReport);
    }

    @Override
    public Object getPersistenceReport(AnalysisResultDTO analysisResultDTO) {
        return getReportByMapperFunction(analysisResultDTO, analysisReportMapper::getPersistenceReport);
    }

    @Override
    public Object getEjbJarReport(AnalysisResultDTO analysisResultDTO) {
        return getReportByMapperFunction(analysisResultDTO, analysisReportMapper::getEjbJarReport);
    }

    @Override
    public Object getPomXmlReport(AnalysisResultDTO analysisResultDTO) {
        return getReportByMapperFunction(analysisResultDTO, analysisReportMapper::getPomXmlReport);
    }

    @Override
    public Object getFacesConfigReport(AnalysisResultDTO analysisResultDTO) {
        return getReportByMapperFunction(analysisResultDTO, analysisReportMapper::getFacesConfigReport);
    }

    @Override
    public Object getJspContentReport(AnalysisResultDTO analysisResultDTO) {
        return getReportByMapperFunction(analysisResultDTO, analysisReportMapper::getJspContentReport);
    }

    @Override
    public Object getJsfContentReport(AnalysisResultDTO analysisResultDTO) {
        return getReportByMapperFunction(analysisResultDTO, analysisReportMapper::getJsfContentReport);
    }

    @Override
    public Object getJspFileCountReport(AnalysisResultDTO analysisResultDTO) {
        return getReportByMapperFunction(analysisResultDTO, analysisReportMapper::getJspFileCountReport);
    }

    @Override
    public Object getJsfFileCountReport(AnalysisResultDTO analysisResultDTO) {
        return getReportByMapperFunction(analysisResultDTO, analysisReportMapper::getJsfFileCountReport);
    }
}

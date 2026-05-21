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
import java.util.function.Function;

@Service
public class AnalysisReportServiceImpl implements AnalysisReportService {

    @Autowired
    private AnalysisReportMapper analysisReportMapper;

    private Object getReportByMapperFunction(AnalysisResultDTO analysisResultDTO, Function<AnalysisResultDTO, AnalysisReportDAO> mapperFunction) {
        AnalysisReportDAO analysisReportDAO = mapperFunction.apply(analysisResultDTO);
        if(analysisReportDAO == null)
        {
            throw new RuntimeException("AnalysisResultDAO is null");
        }

        File filePath = new File(analysisReportDAO.getPath());

        if(!filePath.exists())
        {
            throw new RuntimeException(filePath + " does not exist");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // 读取JSON文件并解析为Java对象
            return objectMapper.readValue(filePath, Object.class);
        } catch (IOException e) {
            throw new RuntimeException("读取JSON文件失败", e);
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
}

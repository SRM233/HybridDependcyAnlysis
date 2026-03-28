package com.hybriddependcyanlysis.Service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hybriddependcyanlysis.Mapper.AnalysisResultMapper;
import com.hybriddependcyanlysis.POJO.DAO.AnalysisResultDAO;
import com.hybriddependcyanlysis.POJO.DTO.AnalysisResultDTO;
import com.hybriddependcyanlysis.Service.AnalysisResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;

@Service
public class AnalysisResultServiceImpl implements AnalysisResultService {

    @Autowired
    private AnalysisResultMapper analysisResultMapper;

    private Object getReportByMapperFunction(AnalysisResultDTO analysisResultDTO, Function<AnalysisResultDTO, AnalysisResultDAO> mapperFunction) {
        AnalysisResultDAO analysisResultDAO = mapperFunction.apply(analysisResultDTO);
        if(analysisResultDAO == null)
        {
            throw new RuntimeException("AnalysisResultDAO is null");
        }

        File filePath = new File(analysisResultDAO.getPath());

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
        return getReportByMapperFunction(analysisResultDTO, analysisResultMapper::getAnnotationResult);
    }

    @Override
    public Object getWebXmlReport(AnalysisResultDTO analysisResultDTO) {
        return getReportByMapperFunction(analysisResultDTO, analysisResultMapper::getWebXmlResult);
    }

    @Override
    public Object getFileStoreReport(AnalysisResultDTO analysisResultDTO) {
        return getReportByMapperFunction(analysisResultDTO, analysisResultMapper::getFileStoreResult);
    }

    @Override
    public Object getPersistenceReport(AnalysisResultDTO analysisResultDTO) {
        return getReportByMapperFunction(analysisResultDTO, analysisResultMapper::getPersistenceResult);
    }

    @Override
    public Object getEjbJarReport(AnalysisResultDTO analysisResultDTO) {
        return getReportByMapperFunction(analysisResultDTO, analysisResultMapper::getEjbJarResult);
    }

    @Override
    public Object getPomXmlReport(AnalysisResultDTO analysisResultDTO) {
        return getReportByMapperFunction(analysisResultDTO, analysisResultMapper::getPomXmlResult);
    }

    @Override
    public Object getFacesConfigReport(AnalysisResultDTO analysisResultDTO) {
        return getReportByMapperFunction(analysisResultDTO, analysisResultMapper::getFacesConfigResult);
    }
}

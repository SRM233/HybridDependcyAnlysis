package com.hybriddependcyanlysis.Mapper;

import com.hybriddependcyanlysis.POJO.DAO.AnalysisReportDAO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StaticAnalysisMapper {

    AnalysisReportDAO getAnnotationCountReport(AnalysisReportDAO analysisResultDTO);
    
    AnalysisReportDAO getWebXmlReport(AnalysisReportDAO analysisResultDTO);
    
    AnalysisReportDAO getFileStoreReport(AnalysisReportDAO analysisResultDTO);
    
    AnalysisReportDAO getPersistenceReport(AnalysisReportDAO analysisResultDTO);
    
    AnalysisReportDAO getEjbJarReport(AnalysisReportDAO analysisResultDTO);
    
    AnalysisReportDAO getPomXmlReport(AnalysisReportDAO analysisResultDTO);
    
    AnalysisReportDAO getFacesConfigReport(AnalysisReportDAO analysisResultDTO);

    AnalysisReportDAO getJspFileCountReport(AnalysisReportDAO analysisResultDTO);
    
    AnalysisReportDAO getJsfFileCountReport(AnalysisReportDAO analysisResultDTO);

    AnalysisReportDAO getJspContentReport(AnalysisReportDAO analysisResultDTO);
    AnalysisReportDAO getJsfContentReport(AnalysisReportDAO analysisResultDTO);

    void insertResult(AnalysisReportDAO analysisReportDAO);

    void updateResult(AnalysisReportDAO analysisReportDAO);

    void deleteAnnotationAnalysisResult(Integer userId, Integer sourceFolderId);

    void deleteWebXmlAnalysisResult(Integer userId, Integer sourceFolderId);

    void deleteFileStoreAnalysisResult(Integer userId, Integer sourceFolderId);

    void deletePersistenceAnalysisResult(Integer userId, Integer sourceFolderId);

    void deleteEjbJarAnalysisResult(Integer userId, Integer sourceFolderId);

    void deletePomXmlAnalysisResult(Integer userId, Integer sourceFolderId);

    void deleteFacesConfigAnalysisResult(Integer userId, Integer sourceFolderId);

    void deleteJspContentAnalysisResult(Integer userId, Integer sourceFolderId);
    void deleteJsfContentAnalysisResult(Integer userId, Integer sourceFolderId);

}

package com.hybriddependcyanlysis.Mapper;

import com.hybriddependcyanlysis.POJO.DAO.AnalysisReportDAO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    void deleteAnnotationAnalysisResult(@Param("userId") Integer userId, @Param("sourceFolderId") Integer sourceFolderId);

    void deleteWebXmlAnalysisResult(@Param("userId") Integer userId, @Param("sourceFolderId") Integer sourceFolderId);

    void deleteFileStoreAnalysisResult(@Param("userId") Integer userId, @Param("sourceFolderId") Integer sourceFolderId);

    void deletePersistenceAnalysisResult(@Param("userId") Integer userId, @Param("sourceFolderId") Integer sourceFolderId);

    void deleteEjbJarAnalysisResult(@Param("userId") Integer userId, @Param("sourceFolderId") Integer sourceFolderId);

    void deletePomXmlAnalysisResult(@Param("userId") Integer userId, @Param("sourceFolderId") Integer sourceFolderId);

    void deleteFacesConfigAnalysisResult(@Param("userId") Integer userId, @Param("sourceFolderId") Integer sourceFolderId);

    void deleteJspContentAnalysisResult(@Param("userId") Integer userId, @Param("sourceFolderId") Integer sourceFolderId);
    void deleteJsfContentAnalysisResult(@Param("userId") Integer userId, @Param("sourceFolderId") Integer sourceFolderId);

}

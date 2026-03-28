package com.hybriddependcyanlysis.Mapper;

import com.hybriddependcyanlysis.POJO.DAO.AnalysisResultDAO;
import com.hybriddependcyanlysis.POJO.DAO.JavaFilesParseDAO;
import com.hybriddependcyanlysis.POJO.DAO.JspParseOutPutDAO;
import org.apache.ibatis.annotations.Mapper;

import java.util.HashMap;
import java.util.List;

@Mapper
public interface StaticAnalysisMapper {

    AnalysisResultDAO getAnnotationCountAnalysisResult(AnalysisResultDAO analysisResultDTO);
    
    AnalysisResultDAO getWebXmlAnalysisResult(AnalysisResultDAO analysisResultDTO);
    
    AnalysisResultDAO getFileStoreAnalysisResult(AnalysisResultDAO analysisResultDTO);
    
    AnalysisResultDAO getPersistenceAnalysisResult(AnalysisResultDAO analysisResultDTO);
    
    AnalysisResultDAO getEjbJarAnalysisResult(AnalysisResultDAO analysisResultDTO);
    
    AnalysisResultDAO getPomXmlAnalysisResult(AnalysisResultDAO analysisResultDTO);
    
    AnalysisResultDAO getFacesConfigAnalysisResult(AnalysisResultDAO analysisResultDTO);

    AnalysisResultDAO getJspFileCountAnalysisResult(AnalysisResultDAO analysisResultDTO);
    
    AnalysisResultDAO getJsfFileCountAnalysisResult(AnalysisResultDAO analysisResultDTO);

    void insertResult(AnalysisResultDAO analysisResultDAO);

    void updateResult(AnalysisResultDAO analysisResultDAO);

    void deleteAnnotationAnalysisResult(Integer userId, Integer sourceFolderId);

    void deleteWebXmlAnalysisResult(Integer userId, Integer sourceFolderId);

    void deleteFileStoreAnalysisResult(Integer userId, Integer sourceFolderId);

    void deletePersistenceAnalysisResult(Integer userId, Integer sourceFolderId);

    void deleteEjbJarAnalysisResult(Integer userId, Integer sourceFolderId);

    void deletePomXmlAnalysisResult(Integer userId, Integer sourceFolderId);

    void deleteFacesConfigAnalysisResult(Integer userId, Integer sourceFolderId);

}

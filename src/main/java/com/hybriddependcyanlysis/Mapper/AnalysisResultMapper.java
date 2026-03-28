package com.hybriddependcyanlysis.Mapper;

import com.hybriddependcyanlysis.POJO.DAO.AnalysisResultDAO;
import com.hybriddependcyanlysis.POJO.DTO.AnalysisResultDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AnalysisResultMapper {
    AnalysisResultDAO getAnalysisResult(AnalysisResultDAO analysisResultDTO);

    void insertResult(AnalysisResultDAO analysisResultDAO);

    void updateResult(AnalysisResultDAO analysisResultDAO);

    @Select("select * from hybridanalysis.analysis_results where user_id = #{userId} and source_folder_id = #{sourceFolderId} and parse_output_id = #{parseOutputId} and name = 'annotation-statistics'")
    AnalysisResultDAO getAnnotationResult(AnalysisResultDTO analysisResultDTO);

    @Select("select * from hybridanalysis.analysis_results where user_id = #{userId} and source_folder_id = #{sourceFolderId} and parse_output_id = #{parseOutputId} and name = 'web-xml-analysis'")
    AnalysisResultDAO getWebXmlResult(AnalysisResultDTO analysisResultDTO);

    @Select("select * from hybridanalysis.analysis_results where user_id = #{userId} and source_folder_id = #{sourceFolderId} and parse_output_id = #{parseOutputId} and name = 'file-store-analysis'")
    AnalysisResultDAO getFileStoreResult(AnalysisResultDTO analysisResultDTO);

    @Select("select * from hybridanalysis.analysis_results where user_id = #{userId} and source_folder_id = #{sourceFolderId} and parse_output_id = #{parseOutputId} and name = 'persistence-analysis'")
    AnalysisResultDAO getPersistenceResult(AnalysisResultDTO analysisResultDTO);

    @Select("select * from hybridanalysis.analysis_results where user_id = #{userId} and source_folder_id = #{sourceFolderId} and parse_output_id = #{parseOutputId} and name = 'ejb-jar-analysis'")
    AnalysisResultDAO getEjbJarResult(AnalysisResultDTO analysisResultDTO);

    @Select("select * from hybridanalysis.analysis_results where user_id = #{userId} and source_folder_id = #{sourceFolderId} and parse_output_id = #{parseOutputId} and name = 'pom-xml-analysis'")
    AnalysisResultDAO getPomXmlResult(AnalysisResultDTO analysisResultDTO);

    @Select("select * from hybridanalysis.analysis_results where user_id = #{userId} and source_folder_id = #{sourceFolderId} and parse_output_id = #{parseOutputId} and name = 'faces-config-analysis'")
    AnalysisResultDAO getFacesConfigResult(AnalysisResultDTO analysisResultDTO);
}

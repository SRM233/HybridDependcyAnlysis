package com.hybriddependcyanlysis.Mapper;

import com.hybriddependcyanlysis.POJO.DAO.AnalysisReportDAO;
import com.hybriddependcyanlysis.POJO.DTO.AnalysisResultDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AnalysisReportMapper {
    AnalysisReportDAO getAnalysisResultReport(AnalysisReportDAO analysisResultDTO);

    void insertResult(AnalysisReportDAO analysisReportDAO);

    void updateResult(AnalysisReportDAO analysisReportDAO);

    @Select("select * from analysis_report where user_id = #{userId} and source_folder_id = #{sourceFolderId} and parse_output_id = #{parseOutputId} and name = 'annotation-statistics'")
    AnalysisReportDAO getAnnotationReport(AnalysisResultDTO analysisResultDTO);

    @Select("select * from analysis_report where user_id = #{userId} and source_folder_id = #{sourceFolderId} and parse_output_id = #{parseOutputId} and name = 'web-xml-analysis'")
    AnalysisReportDAO getWebXmlReport(AnalysisResultDTO analysisResultDTO);

    @Select("select * from analysis_report where user_id = #{userId} and source_folder_id = #{sourceFolderId} and parse_output_id = #{parseOutputId} and name = 'file-store-analysis'")
    AnalysisReportDAO getFileStoreReport(AnalysisResultDTO analysisResultDTO);

    @Select("select * from analysis_report where user_id = #{userId} and source_folder_id = #{sourceFolderId} and parse_output_id = #{parseOutputId} and name = 'persistence-analysis'")
    AnalysisReportDAO getPersistenceReport(AnalysisResultDTO analysisResultDTO);

    @Select("select * from analysis_report where user_id = #{userId} and source_folder_id = #{sourceFolderId} and parse_output_id = #{parseOutputId} and name = 'ejb-jar-analysis'")
    AnalysisReportDAO getEjbJarReport(AnalysisResultDTO analysisResultDTO);

    @Select("select * from analysis_report where user_id = #{userId} and source_folder_id = #{sourceFolderId} and parse_output_id = #{parseOutputId} and name = 'pom-xml-analysis'")
    AnalysisReportDAO getPomXmlReport(AnalysisResultDTO analysisResultDTO);

    @Select("select * from analysis_report where user_id = #{userId} and source_folder_id = #{sourceFolderId} and parse_output_id = #{parseOutputId} and name = 'faces-config-analysis'")
    AnalysisReportDAO getFacesConfigReport(AnalysisResultDTO analysisResultDTO);

    @Select("select * from analysis_report where user_id = #{userId} and source_folder_id = #{sourceFolderId} and parse_output_id = #{parseOutputId} and name = 'jsp-content-analysis'")
    AnalysisReportDAO getJspContentReport(AnalysisResultDTO analysisResultDTO);

    @Select("select * from analysis_report where user_id = #{userId} and source_folder_id = #{sourceFolderId} and parse_output_id = #{parseOutputId} and name = 'jsf-content-analysis'")
    AnalysisReportDAO getJsfContentReport(AnalysisResultDTO analysisResultDTO);

    @Select("select * from analysis_report where user_id = #{userId} and source_folder_id = #{sourceFolderId} and parse_output_id = #{parseOutputId} and name = 'jsp-file-count'")
    AnalysisReportDAO getJspFileCountReport(AnalysisResultDTO analysisResultDTO);

    @Select("select * from analysis_report where user_id = #{userId} and source_folder_id = #{sourceFolderId} and parse_output_id = #{parseOutputId} and name = 'jsf-file-count'")
    AnalysisReportDAO getJsfFileCountReport(AnalysisResultDTO analysisResultDTO);
}

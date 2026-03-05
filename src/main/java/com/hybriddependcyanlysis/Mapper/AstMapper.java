package com.hybriddependcyanlysis.Mapper;

import com.hybriddependcyanlysis.POJO.DAO.AnalysisResultDAO;
import com.hybriddependcyanlysis.POJO.DAO.JavaFilesParseDAO;
import com.hybriddependcyanlysis.POJO.DAO.AstSchema.ClassDAO;
import com.hybriddependcyanlysis.POJO.DAO.AstSchema.DependencyDAO;
import com.hybriddependcyanlysis.POJO.DAO.AstSchema.MethodDAO;
import com.hybriddependcyanlysis.POJO.DAO.AstSchema.ParameterDAO;
import com.hybriddependcyanlysis.POJO.DAO.JspParseOutPutDAO;
import com.hybriddependcyanlysis.POJO.DAO.ClientSideSchema.ELexpressionDAO;
import com.hybriddependcyanlysis.POJO.DAO.ClientSideSchema.PageDAO;
import com.hybriddependcyanlysis.POJO.DAO.ClientSideSchema.PageImportDAO;
import com.hybriddependcyanlysis.POJO.DAO.ClientSideSchema.TagliDAO;
import com.hybriddependcyanlysis.POJO.DTO.UserDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AstMapper {
    JavaFilesParseDAO getOutPut(UserDTO userDTO);

    void insertClass(ClassDAO classDAO);

    void insertMethod(MethodDAO methodDAO);

    void insertParam(ParameterDAO paramDAO);

    void insertDependency(DependencyDAO dep);

    List<DependencyDAO> getDependencies(JavaFilesParseDAO astOutPut);

    void deleteDependency(DependencyDAO dependencyDAO);

    List<ParameterDAO> getParameters(JavaFilesParseDAO astOutPut);

    void deleteParameter(ParameterDAO parameterDAO);

    List<MethodDAO> getMethods(JavaFilesParseDAO astOutPut);

    void deleteMethod(MethodDAO methodDAO);

    List<ClassDAO> getClasses(JavaFilesParseDAO astOutPut);

    void deleteClass(ClassDAO classDAO);

    void deleteOutputReport(JavaFilesParseDAO astOutPut);

    JspParseOutPutDAO getJspParseOutput(UserDTO userDTO);

    void insertPage(PageDAO page);

    void insertExpression(ELexpressionDAO el);

    void insertPageImport(PageImportDAO imp);

    void insertTaglib(TagliDAO taglib);

    AnalysisResultDAO getAnalysisResultBySourceFolderId(Integer id);

    void insertAnalysisResult(AnalysisResultDAO dao);

    void updateAnalysisResult(AnalysisResultDAO dao);

    @Select("select path from hybridanalysis.java_files_parse_output where user_id = #{id} and source_folder_id = #{sourceFolderId}")
    String getJavaFilesParseOutput(UserDTO userDTO);

    @Select("select path from hybridanalysis.web_xml_parse_output where user_id = #{id} and source_folder_id = #{sourceFolderId}")
    String getWebXmlParseOutput(UserDTO userDTO);

    @Select("select path from hybridanalysis.persistence_xml_parse_output where user_id = #{id} and source_folder_id = #{sourceFolderId}")
    String getPersistenceXmlParseOutput(UserDTO userDTO);

    @Select("select path from hybridanalysis.ejb_jar_xml_parse_output where user_id = #{id} and source_folder_id = #{sourceFolderId}")
    String getEjbJarXmlParseOutput(UserDTO userDTO);

    @Select("select path from hybridanalysis.pom_xml_parse_output where user_id = #{id} and source_folder_id = #{sourceFolderId}")
    String getPomXmlParseOutput(UserDTO userDTO);

    @Select("select path from hybridanalysis.faces_config_xml_parse_output where user_id = #{id} and source_folder_id = #{sourceFolderId}")
    String getFacesXmlParseOutput(UserDTO userDTO);
}

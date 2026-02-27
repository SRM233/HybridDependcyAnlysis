package com.hybriddependcyanlysis.Mapper;

import com.hybriddependcyanlysis.POJO.DAO.*;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;


@Mapper
public interface ParseSourceCodeMapper {

//    String getSourceFolderPathById(Integer id);

    void insertJavaFilesParseOutPut(JavaFilesParseDAO javaFilesParseDAO);

    void insertJavaFilesError(JavaFilesErrorDAO javaFilesErrorDAO);

//    JspParseOutPutDAO getJspParseOutput(Integer sourceFolderId);

    void insertJspParseOutput(JspParseOutPutDAO JspParseOutPutDAO);

//    JspParseErrorDAO getJspParseError(Integer sourceFolderId);

    void insertJspParseError(JspParseErrorDAO JspParseErrorDAO);

    JavaFilesParseDAO getOutPutBySourceFolderId(Integer sourceFolderId);

    JavaFilesErrorDAO getErrorBySourceFolderId(Integer sourceFolderId);

    void updateJavaFilesParseOutput(JavaFilesParseDAO javaFilesParseDAO);

    void updateJavaFilesPError(JavaFilesErrorDAO javaFilesErrorDAO);

    void updateJspParseOutput(JspParseOutPutDAO clientSideOutPutDAO);

    void updateJspParseError(JspParseErrorDAO clientSideErrorDAO);

    @Delete("delete from hybridanalysis.java_files_error where user_id = #{userId} and output_id = #{outputId}")
    void deleteASTErrorlog(Integer userId, Integer outputId);

    @Delete("delete from hybridanalysis.java_files_parse_output where user_id = #{userId} and id = #{outputId}")
    void deleteASTOutput(Integer userId, Integer outputId);

    @Delete("delete from hybridanalysis.jsp_parse_error where user_id = #{userId} and jsp_parse_output_id = #{outputId}")
    void deleteJspAstErrorlog(Integer userId, Integer outputId);

    @Delete("delete from hybridanalysis.jsp_parse_output  where user_id = #{userId} and id = #{outputId}")
    void deleteJspAstOutput(Integer userId, Integer outputId);

    @Select("select * from hybridanalysis.jsp_parse_output where source_folder_id = #{id}")
    JspParseOutPutDAO getJspParseOutputBySourceFolderId(Integer id);

    @Select("select * from hybridanalysis.jsp_parse_error where source_folder_id = #{id}")
    JspParseErrorDAO getJspParseErrorBySourceFolderId(Integer id);

    @Select("select * from hybridanalysis.web_xml_parse_output where source_folder_id = #{id};")
    WebXmlParseOutput getWebXmlParseOutputBySourceFolderId(Integer id);


    void insertWebXmlParseOutput(WebXmlParseOutput webXmlParseOutput);

    void updateWebXmlParseOutput(WebXmlParseOutput webXmlParseOutput);

    @Select("select * from hybridanalysis.persistence_xml_parse_output where source_folder_id = #{id}")
    PersistenceXmlParseOutput getPersistenceXmlParseOutputBySourceFolderId(Integer id);

    void insertPersistenceXmlParseOutput(PersistenceXmlParseOutput persistenceXmlParseOutput);

    void updatePersistenceXmlParseOutput(PersistenceXmlParseOutput persistenceXmlParseOutput);


    @Select("select * from hybridanalysis.ejb_jar_xml_parse_output where source_folder_id = #{id}")
    EjbJarXmlParseOutput getEjbJarXmlParseOutputBySourceFolderId(Integer id);

    @Select("select * from hybridanalysis.faces_config_xml_parse_output where source_folder_id = #{id}")
    FacesConfigXmlParseOutput getFacesConfigXmlParseOutputBySourceFolderId(Integer id);

    @Select("select * from hybridanalysis.application_xml_parse_output where source_folder_id = #{id}")
    ApplicationXmlParseOutput getApplicationXmlParseOutputBySourceFolderId(Integer id);

    void insertEjbJarXmlParseOutput(EjbJarXmlParseOutput ejbJarXmlParseOutput);

    void updateEjbJarXmlParseOutput(EjbJarXmlParseOutput ejbJarXmlParseOutput);

    void insertFacesConfigXmlParseOutput(FacesConfigXmlParseOutput facesConfigXmlParseOutput);

    void updateFacesConfigXmlParseOutput(FacesConfigXmlParseOutput facesConfigXmlParseOutput);

    void insertApplicationXmlParseOutput(ApplicationXmlParseOutput applicationXmlParseOutput);

    void updateApplicationXmlParseOutput(ApplicationXmlParseOutput applicationXmlParseOutput);

    @Select("select * from hybridanalysis.jsf_parse_output  where source_folder_id = #{id}")
    JsfParseOutPutDAO getJsfParseOutputBySourceFolderId(Integer id);

    void insertJsfParseOutput(JsfParseOutPutDAO jsfParseOutputDAO);

    void updateJsfParseOutput(JsfParseOutPutDAO jsfParseOutputDAO);
}

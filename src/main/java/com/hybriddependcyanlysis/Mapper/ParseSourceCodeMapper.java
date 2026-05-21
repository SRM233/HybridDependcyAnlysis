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

    @Delete("delete from java_files_error where user_id = #{userId} and output_id = #{outputId}")
    void deleteASTErrorlog(Integer userId, Integer outputId);

    @Delete("delete from java_files_parse where user_id = #{userId} and id = #{outputId}")
    void deleteASTOutput(Integer userId, Integer outputId);

    @Delete("delete from jsp_parse_error where user_id = #{userId} and jsp_parse_output_id = #{outputId}")
    void deleteJspAstErrorlog(Integer userId, Integer outputId);

    @Delete("delete from jsp_parse_output  where user_id = #{userId} and id = #{outputId}")
    void deleteJspAstOutput(Integer userId, Integer outputId);

    @Select("select * from jsp_parse_output where source_folder_id = #{id}")
    JspParseOutPutDAO getJspParseOutputBySourceFolderId(Integer id);

    @Select("select * from jsp_parse_error where source_folder_id = #{id}")
    JspParseErrorDAO getJspParseErrorBySourceFolderId(Integer id);

    @Select("select * from web_xml_parse_output where source_folder_id = #{id};")
    WebXmlParseOutputDAO getWebXmlParseOutputBySourceFolderId(Integer id);


    void insertWebXmlParseOutput(WebXmlParseOutputDAO webXmlParseOutputDAO);

    void updateWebXmlParseOutput(WebXmlParseOutputDAO webXmlParseOutputDAO);

    @Select("select * from persistence_xml_parse_output where source_folder_id = #{id}")
    PersistenceXmlParseOutputDAO getPersistenceXmlParseOutputBySourceFolderId(Integer id);

    void insertPersistenceXmlParseOutput(PersistenceXmlParseOutputDAO persistenceXmlParseOutputDAO);

    void updatePersistenceXmlParseOutput(PersistenceXmlParseOutputDAO persistenceXmlParseOutputDAO);


    @Select("select * from ejb_jar_xml_parse_output where source_folder_id = #{id}")
    EjbJarXmlParseOutputDAO getEjbJarXmlParseOutputBySourceFolderId(Integer id);

    @Select("select * from faces_config_xml_parse_output where source_folder_id = #{id}")
    FacesConfigXmlParseOutputDAO getFacesConfigXmlParseOutputBySourceFolderId(Integer id);

    @Select("select * from application_xml_parse_output where source_folder_id = #{id}")
    ApplicationXmlParseOutputDAO getApplicationXmlParseOutputBySourceFolderId(Integer id);

    void insertEjbJarXmlParseOutput(EjbJarXmlParseOutputDAO ejbJarXmlParseOutputDAO);

    void updateEjbJarXmlParseOutput(EjbJarXmlParseOutputDAO ejbJarXmlParseOutputDAO);

    void insertFacesConfigXmlParseOutput(FacesConfigXmlParseOutputDAO facesConfigXmlParseOutputDAO);

    void updateFacesConfigXmlParseOutput(FacesConfigXmlParseOutputDAO facesConfigXmlParseOutputDAO);

    void insertApplicationXmlParseOutput(ApplicationXmlParseOutputDAO applicationXmlParseOutputDAO);

    void updateApplicationXmlParseOutput(ApplicationXmlParseOutputDAO applicationXmlParseOutputDAO);

    @Select("select * from jsf_parse_output  where source_folder_id = #{id}")
    JsfParseOutPutDAO getJsfParseOutputBySourceFolderId(Integer id);

    void insertJsfParseOutput(JsfParseOutPutDAO jsfParseOutputDAO);

    void updateJsfParseOutput(JsfParseOutPutDAO jsfParseOutputDAO);

    @Select("select * from pom_xml_parse_output where source_folder_id = #{id}")
    PomXmlParseOutputDAO getPomXmlParseOutputBySourceFolderId(Integer id);

    void insertPomXmlParseOutput(PomXmlParseOutputDAO pomXmlParseOutputDAO);

    void updatePomXmlParseOutput(PomXmlParseOutputDAO pomXmlParseOutputDAO);

    @Delete("delete from web_xml_parse_output where user_id = #{userId} and source_folder_id = #{sourceFolderId}")
    void deleteWebXmlParseOutput(Integer userId, Integer sourceFolderId);

    @Delete("delete from persistence_xml_parse_output where user_id = #{userId} and source_folder_id = #{sourceFolderId}")
    void deletePersistenceXmlParseOutput(Integer userId, Integer sourceFolderId);

    @Delete("delete from ejb_jar_xml_parse_output where user_id = #{userId} and source_folder_id = #{sourceFolderId}")
    void deleteEjbJarXmlParseOutput(Integer userId, Integer sourceFolderId);

    @Delete("delete from faces_config_xml_parse_output where user_id = #{userId} and source_folder_id = #{sourceFolderId}")
    void deleteFacesConfigXmlParseOutput(Integer userId, Integer sourceFolderId);

    @Delete("delete from application_xml_parse_output where user_id = #{userId} and source_folder_id = #{sourceFolderId}")
    void deleteApplicationXmlParseOutput(Integer userId, Integer sourceFolderId);

    @Delete("delete from pom_xml_parse_output where user_id = #{userId} and source_folder_id = #{sourceFolderId}")
    void deletePomXmlParseOutput(Integer userId, Integer sourceFolderId);

    @Delete("delete from jsf_parse_output where user_id = #{userId} and source_folder_id = #{sourceFolderId}")
    void deleteJsfParseOutput(Integer userId, Integer sourceFolderId);

    @Delete("delete from java_files_parse where user_id = #{userId} and source_folder_id = #{sourceFolderId}")
    void deleteJavaParseOutputBySourceFolder(Integer userId, Integer sourceFolderId);

    @Delete("delete from java_files_error where user_id = #{userId} and source_folder_id = #{sourceFolderId}")
    void deleteJavaErrorBySourceFolder(Integer userId, Integer sourceFolderId);

    @Delete("delete from jsp_parse_output where user_id = #{userId} and source_folder_id = #{sourceFolderId}")
    void deleteJspParseOutputBySourceFolder(Integer userId, Integer sourceFolderId);

    @Delete("delete from jsp_parse_error where user_id = #{userId} and source_folder_id = #{sourceFolderId}")
    void deleteJspErrorBySourceFolder(Integer userId, Integer sourceFolderId);
}

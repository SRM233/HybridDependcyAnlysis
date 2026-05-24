package com.hybriddependcyanlysis.Service;

import com.hybriddependcyanlysis.POJO.DTO.UserDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;

public interface StaticAnalysisService {
//    HashMap<String, Integer> dependencyCounting(Integer userId, Integer sourceFolderId);
//
//    HashMap<String, String> ELAnalysis(UserDTO userDTO);

    Object AnnotationCount(UserDTO userDTO) throws IOException;

    Object analyzeWebXml(UserDTO userDTO) throws IOException;

    Object FileStoreAnalysis( UserDTO userDTO) throws Exception;

    Object persistenceAnalysis(UserDTO userDTO) throws IOException;

    Object ejbJarAnalysis(UserDTO userDTO) throws IOException;

    Object pomXmlAnalysis(UserDTO userDTO) throws IOException;

    Object facesXmlAnalysis(UserDTO userDTO) throws IOException;

    void deleteAnnotationAnalysis(Integer userId, Integer sourceFolderId);

    void deleteWebXmlAnalysis(Integer userId, Integer sourceFolderId);

    void deleteFileStoreAnalysis(Integer userId, Integer sourceFolderId);

    void deletePersistenceAnalysis(Integer userId, Integer sourceFolderId);

    void deleteEjbJarAnalysis(Integer userId, Integer sourceFolderId);

    void deletePomXmlAnalysis(Integer userId, Integer sourceFolderId);

    void deleteFacesConfigAnalysis(Integer userId, Integer sourceFolderId);

    void deleteJspContentAnalysis(Integer userId, Integer sourceFolderId);
    void deleteJsfContentAnalysis(Integer userId, Integer sourceFolderId);

    Object JspFileCount(UserDTO userDTO) throws IOException;
    Object JsfFileCount(UserDTO userDTO) throws IOException;
    Object JspContentAnalysis(UserDTO userDTO) throws IOException;
    Object JsfContentAnalysis(UserDTO userDTO) throws IOException;
}

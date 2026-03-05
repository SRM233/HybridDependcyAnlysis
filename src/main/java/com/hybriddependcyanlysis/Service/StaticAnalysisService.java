package com.hybriddependcyanlysis.Service;

import com.hybriddependcyanlysis.POJO.DTO.UserDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;

public interface StaticAnalysisService {
    HashMap<String, Integer> dependencyCounting(Integer userId, Integer sourceFolderId);

    HashMap<String, String> ELAnalysis(UserDTO userDTO);

    void AnnotationCount(UserDTO userDTO) throws IOException;

    void analyzeWebXml(UserDTO userDTO) throws IOException;

    void FileStoreAnalysis( UserDTO userDTO) throws Exception;

    void persistenceAnalysis(UserDTO userDTO) throws IOException;

    void ejbJarAnalysis(UserDTO userDTO) throws IOException;

    void pomXmlAnalysis(UserDTO userDTO) throws IOException;

    void facesXmlAnalysis(UserDTO userDTO) throws IOException;
}

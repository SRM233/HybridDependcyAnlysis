package com.hybriddependcyanlysis.Service;

import com.hybriddependcyanlysis.POJO.DTO.UserDTO;

import java.io.IOException;
import java.util.HashMap;

public interface StaticAnalysisService {
    HashMap<String, Integer> dependencyCounting(Integer userId, Integer sourceFolderId);

    HashMap<String, String> ELAnalysis(UserDTO userDTO);

    void AnnotationCount(UserDTO userDTO) throws IOException;

    void analyzeWebXml(UserDTO userDTO) throws IOException;
}

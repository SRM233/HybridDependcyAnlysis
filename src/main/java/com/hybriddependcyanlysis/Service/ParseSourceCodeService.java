package com.hybriddependcyanlysis.Service;

import com.hybriddependcyanlysis.POJO.DTO.ParseFilesDTO;

import java.io.IOException;

public interface ParseSourceCodeService {
    void staticParsing(Integer sourceFolderId) throws IOException;


    void parseJspFile(Integer sourceFolderId) throws IOException;

    void parseWebXmlFile(Integer sourceFolderId) throws IOException;

    void ParsePersistenceXmlFile(Integer sourceFolderId) throws IOException;

    void ParseEjbJarXmlFile(Integer sourceFolderId) throws IOException;

    void ParseFacesConfigXmlFile(Integer sourceFolderId) throws IOException;

    void ParseApplicationXmlFile(Integer sourceFolderId) throws IOException;

    void parseJsfFile(Integer sourceFolderId) throws IOException;

    void staticParseFile(Integer sourceFolderId) throws IOException;

    void parsePomXmlFile(Integer sourceFolderId) throws IOException;

    void deleteJavaParseResults(Integer sourceFolderId);

    void deleteJspParseResults(Integer sourceFolderId);

    void deleteWebXmlParseResults(Integer sourceFolderId);

    void deletePersistenceXmlParseResults(Integer sourceFolderId);

    void deleteEjbJarXmlParseResults(Integer sourceFolderId);

    void deleteFacesConfigXmlParseResults(Integer sourceFolderId);

    void deleteApplicationXmlParseResults(Integer sourceFolderId);

    void deletePomXmlParseResults(Integer sourceFolderId);

    void deleteJsfParseResults(Integer sourceFolderId);
}

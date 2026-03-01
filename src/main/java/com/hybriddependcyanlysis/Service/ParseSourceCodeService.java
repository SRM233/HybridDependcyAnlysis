package com.hybriddependcyanlysis.Service;

import java.io.IOException;

public interface ParseSourceCodeService {
    void staticParsing(Integer userId, Integer sourceFolderId) throws IOException;


    void parseJspFile(Integer userId, Integer sourceFolderId) throws IOException;

    void parseWebXmlFile(Integer userId, Integer sourceFolderId) throws IOException;

    void ParsePersistenceXmlFile(Integer userId, Integer sourceFolderId) throws IOException;

    void ParseEjbJarXmlFile(Integer userId, Integer sourceFolderId) throws IOException;

    void ParseFacesConfigXmlFile(Integer userId, Integer sourceFolderId) throws IOException;

    void ParseApplicationXmlFile(Integer userId, Integer sourceFolderId) throws IOException;

    void parseJsfFile(Integer userId, Integer sourceFolderId) throws IOException;

    void staticParseFile(Integer userId, Integer sourceFolderId) throws IOException;

    void parsePomXmlFile(Integer userId, Integer sourceFolderId) throws IOException;
}

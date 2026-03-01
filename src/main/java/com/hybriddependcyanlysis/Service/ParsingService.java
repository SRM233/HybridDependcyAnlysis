package com.hybriddependcyanlysis.Service;

import Common.ClassInfos.JavaClassInfo;
import com.hybriddependcyanlysis.POJO.DAO.SourceFolderDAO;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Set;

public interface ParsingService {
    public  void parsing(File serverOutput, File serverError, SourceFolderDAO sourceFolderDAO) throws IOException;

    public void parsePackage(CtModel model, BufferedWriter writer) throws IOException;

    public void parseClass(CtType<?> type) throws IOException;

    public void parseMethod(CtMethod<?> method, JavaClassInfo classInfo) throws IOException;

    public void detectInvocationIssues(CtMethod<?> method, Set<String> classDeps) throws IOException;

    public void detectConstructorIssues(CtType<?> type, Set<String> classDeps) throws IOException;

    public void parseFallbackUnit(CtCompilationUnit unit) throws IOException;

    void parsingJsp(File outputLog, File errorLog, SourceFolderDAO sourceFolderDAO) throws IOException;

    void parseWebXml(File webXmlParseOutput, SourceFolderDAO sourceFolderDAO) throws IOException;

    void parsePersistenceXml(File output, SourceFolderDAO sourceFolderDAO) throws IOException;

    void parseEjbJarXml(File output, SourceFolderDAO sourceFolderDAO) throws IOException;

    void parseFacesConfigXml(File output, SourceFolderDAO sourceFolderDAO) throws IOException;

    void parseApplicationXml(File output, SourceFolderDAO sourceFolderDAO) throws IOException;

    void parsingJsf(File jsfFilesParseOutput, SourceFolderDAO sourceFolderDAO) throws IOException;

    void staticParseFiles(SourceFolderDAO sourceFolderDAO) throws IOException;

    void parsePomXmlFile(File pomOutput, SourceFolderDAO sourceFolderDAO) throws IOException;
}

package com.hybriddependcyanlysis.Service.Impl;
import Common.OutputFileName;
import Common.OutputPath;
import com.hybriddependcyanlysis.Mapper.IngestMapper;
import com.hybriddependcyanlysis.Mapper.AstMapper;
import com.hybriddependcyanlysis.Mapper.FileMapper;
import com.hybriddependcyanlysis.Mapper.ParseSourceCodeMapper;
import com.hybriddependcyanlysis.POJO.DAO.*;
import com.hybriddependcyanlysis.Service.ParseSourceCodeService;
import com.hybriddependcyanlysis.Service.ParsingService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Service
public class ParseSourceCodeServiceImpl implements ParseSourceCodeService {

    @Autowired
    private ParseSourceCodeMapper parseSourceCodeMapper;

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private ParsingService ParsingService;

    @Autowired
    private AstMapper astMapper;

    @Autowired
    private IngestMapper ingestMapper;
    @Autowired
    private ParsingServiceImpl parsingServiceImpl;


    @Override
    @Transactional
    public void staticParsing(Integer userId, Integer sourceFolderId) throws IOException {

        SourceFolderDAO sourceFolderDAO = ingestMapper.getById(sourceFolderId);

//        if(!checkFolderPath(sourceFolderDAO.getDirPath()))
//        {
//            return;
//        }

        Path outputRoot = checkOutputFolder(sourceFolderDAO.getDirPath());


        Path analysisOutputPath = outputRoot.resolve(OutputPath.JAVA_PARSE_RESULT_PATH);
        Path errorLogPath = outputRoot.resolve(OutputPath.JAVA_PARSE_ERROR_LOG_PATH);

        File analysisOutput = analysisOutputPath.toFile();
        File errorLog = errorLogPath.toFile();

        parsing(analysisOutput, errorLog, userId, sourceFolderId, sourceFolderDAO);
    }

    public Path checkOutputFolder(String path) throws IOException {
        Path projectRoot = Paths.get(path).normalize();
        Path outputRoot = projectRoot.resolve(OutputPath.OUTPUT_BASE_DIR);  // 自动处理分隔符

        // 检查并创建 output 目录
        if (!Files.exists(outputRoot)) {
            try {
                Files.createDirectories(outputRoot);
                System.out.println("Created output directory: " + outputRoot);
            } catch (IOException e) {
                throw new IOException("Failed to create output directory: " + outputRoot, e);
            }
        }
        return outputRoot;
    }


//    public boolean checkFolderPath(String folderPath)
//    {
//        File outputFolder = new File(folderPath);
//        return outputFolder.exists();
//    }

    @Override
    public void parseJspFile(Integer userId, Integer sourceFolderId) throws IOException {
        SourceFolderDAO sourceFolderDAO = ingestMapper.getById(sourceFolderId);
        Path outputRoot = checkOutputFolder(sourceFolderDAO.getDirPath());

        Path JspParseOutputPath = outputRoot.resolve(OutputPath.JSP_PARSE_RESULT_PATH);
        Path JspParseErrorPath = outputRoot.resolve(OutputPath.JSP_PARSE_ERROR_LOG_PATH);

        File JspParseOutput = JspParseOutputPath.toFile();
        File JspParseError = JspParseErrorPath.toFile();

        staticParsingJspFiles(JspParseOutput, JspParseError, sourceFolderDAO);
    }

    @Override
    public void parseWebXmlFile(Integer userId, Integer sourceFolderId) throws IOException {
        SourceFolderDAO sourceFolderDAO = ingestMapper.getById(sourceFolderId);
        Path outputRoot = checkOutputFolder(sourceFolderDAO.getDirPath());

        Path webXmlResultPath = outputRoot.resolve(OutputPath.WEB_XML_PARSE_RESULT_PATH);
        // 如果有错误日志： Path errorPath = outputRoot.resolve(OutputPath.WEB_XML_PARSE_ERROR_LOG_PATH);

        File webXmlOutput = webXmlResultPath.toFile();


        WebXmlFiles(webXmlOutput, sourceFolderDAO);
    }

    @Override
    public void ParsePersistenceXmlFile(Integer userId, Integer sourceFolderId) throws IOException {
        SourceFolderDAO sourceFolderDAO = ingestMapper.getById(sourceFolderId);
        Path outputRoot = checkOutputFolder(sourceFolderDAO.getDirPath());

        Path persistenceResultPath = outputRoot.resolve(OutputPath.PERSISTENCE_PARSE_RESULT_PATH);  // 假设你常量是这个名字

        File persistenceOutput = persistenceResultPath.toFile();

        ParsePersistenceXmlFiles(persistenceOutput, sourceFolderDAO);
    }

    private void ParsePersistenceXmlFiles(File parsePersistenceXmlParseOutput, SourceFolderDAO sourceFolderDAO) {
        try {
            ParsingService.parsePersistenceXml(parsePersistenceXmlParseOutput, sourceFolderDAO);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        PersistenceXmlParseOutput persistenceXmlParseOutput = parseSourceCodeMapper.getPersistenceXmlParseOutputBySourceFolderId(sourceFolderDAO.getId());
        if(persistenceXmlParseOutput==null)
        {
            persistenceXmlParseOutput = new PersistenceXmlParseOutput();
            persistenceXmlParseOutput.setPath(parsePersistenceXmlParseOutput.getAbsolutePath());
            persistenceXmlParseOutput.setName(OutputFileName.PERSISTENCE_OUTPUT_FILE_NAME);
            persistenceXmlParseOutput.setUserId(sourceFolderDAO.getUserId());
            persistenceXmlParseOutput.setSourceFolderId(sourceFolderDAO.getId());
            persistenceXmlParseOutput.setCreateTime(LocalDateTime.now());
            persistenceXmlParseOutput.setUpdateTime(LocalDateTime.now());
            parseSourceCodeMapper.insertPersistenceXmlParseOutput(persistenceXmlParseOutput);
        }



        parseSourceCodeMapper.updatePersistenceXmlParseOutput(persistenceXmlParseOutput);


        System.out.println("persistence.xml analysis complete. Results in "
                + parsePersistenceXmlParseOutput.getAbsolutePath());
    }

    @Override
    public void ParseEjbJarXmlFile(Integer userId, Integer sourceFolderId) throws IOException {
        SourceFolderDAO sourceFolderDAO = ingestMapper.getById(sourceFolderId);
        Path outputRoot = checkOutputFolder(sourceFolderDAO.getDirPath());

        Path ejbResultPath = outputRoot.resolve(OutputPath.EJB_JAR_PARSE_RESULT_PATH);

        File ejbOutput = ejbResultPath.toFile();

        ParseEjbJarXmlFiles(ejbOutput, sourceFolderDAO);
    }

    private void ParseEjbJarXmlFiles(File parseEjbJarXmlParseOutput, SourceFolderDAO sourceFolderDAO) {
        try {
            ParsingService.parseEjbJarXml(parseEjbJarXmlParseOutput, sourceFolderDAO);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        EjbJarXmlParseOutput ejbJarXmlParseOutput = parseSourceCodeMapper.getEjbJarXmlParseOutputBySourceFolderId(sourceFolderDAO.getId());
        if(ejbJarXmlParseOutput==null)
        {
            ejbJarXmlParseOutput = new EjbJarXmlParseOutput();
            ejbJarXmlParseOutput.setPath(parseEjbJarXmlParseOutput.getAbsolutePath());
            ejbJarXmlParseOutput.setName(OutputFileName.EJB_JAR_XML_OUTPUT_FILE_NAME);
            ejbJarXmlParseOutput.setUserId(sourceFolderDAO.getUserId());
            ejbJarXmlParseOutput.setSourceFolderId(sourceFolderDAO.getId());
            ejbJarXmlParseOutput.setCreateTime(LocalDateTime.now());
            ejbJarXmlParseOutput.setUpdateTime(LocalDateTime.now());
            parseSourceCodeMapper.insertEjbJarXmlParseOutput(ejbJarXmlParseOutput);
        }

        parseSourceCodeMapper.updateEjbJarXmlParseOutput(ejbJarXmlParseOutput);

        System.out.println("ejb-jar.xml analysis complete. Results in "
                + parseEjbJarXmlParseOutput.getAbsolutePath());
    }
    

    @Override
    public void ParseFacesConfigXmlFile(Integer userId, Integer sourceFolderId) throws IOException {
        SourceFolderDAO sourceFolderDAO = ingestMapper.getById(sourceFolderId);
        Path outputRoot = checkOutputFolder(sourceFolderDAO.getDirPath());

        Path facesResultPath = outputRoot.resolve(OutputPath.FACES_CONFIG_PARSE_RESULT_PATH);

        File facesOutput = facesResultPath.toFile();

        ParseFacesConfigXmlFiles(facesOutput, sourceFolderDAO);
    }

    private void ParseFacesConfigXmlFiles(File parseFacesConfigXmlParseOutput, SourceFolderDAO sourceFolderDAO) {
        try {
            ParsingService.parseFacesConfigXml(parseFacesConfigXmlParseOutput, sourceFolderDAO);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        FacesConfigXmlParseOutput facesConfigXmlParseOutput =
                parseSourceCodeMapper.getFacesConfigXmlParseOutputBySourceFolderId(sourceFolderDAO.getId());

        if (facesConfigXmlParseOutput == null) {
            facesConfigXmlParseOutput = new FacesConfigXmlParseOutput();
            facesConfigXmlParseOutput.setPath(parseFacesConfigXmlParseOutput.getAbsolutePath());
            facesConfigXmlParseOutput.setName(OutputFileName.FACES_CONFIG_OUTPUT_FILE_NAME);
            facesConfigXmlParseOutput.setUserId(sourceFolderDAO.getUserId());
            facesConfigXmlParseOutput.setSourceFolderId(sourceFolderDAO.getId());
            facesConfigXmlParseOutput.setCreateTime(LocalDateTime.now());
            facesConfigXmlParseOutput.setUpdateTime(LocalDateTime.now());
            parseSourceCodeMapper.insertFacesConfigXmlParseOutput(facesConfigXmlParseOutput);
        }

        parseSourceCodeMapper.updateFacesConfigXmlParseOutput(facesConfigXmlParseOutput);

        System.out.println("faces-config.xml analysis complete. Results in "
                + parseFacesConfigXmlParseOutput.getAbsolutePath());
    }

    @Override
    public void ParseApplicationXmlFile(Integer userId, Integer sourceFolderId) throws IOException {
        SourceFolderDAO sourceFolderDAO = ingestMapper.getById(sourceFolderId);
        Path outputRoot = checkOutputFolder(sourceFolderDAO.getDirPath());

        Path appResultPath = outputRoot.resolve(OutputPath.EAR_APPLICATION_PARSE_RESULT_PATH);

        File appOutput = appResultPath.toFile();

        ParseApplicationXmlFiles(appOutput, sourceFolderDAO);
    }

    @Override
    public void parseJsfFile(Integer userId, Integer sourceFolderId) throws IOException {
        SourceFolderDAO sourceFolderDAO = ingestMapper.getById(sourceFolderId);
        Path outputRoot = checkOutputFolder(sourceFolderDAO.getDirPath());

        Path jsfResultPath = outputRoot.resolve(OutputPath.JSF_PARSE_RESULT_PATH);

        File jsfOutput = jsfResultPath.toFile();

        parseJsfFiles(jsfOutput, sourceFolderDAO);
    }


    @Transactional
    @Override
    public void staticParseFile(Integer userId, Integer sourceFolderId) throws IOException {
        SourceFolderDAO sourceFolderDAO = ingestMapper.getById(sourceFolderId);
        parsingServiceImpl.staticParseFiles(sourceFolderDAO);
    }

    private void parseJsfFiles(File jsfFilesParseOutput, SourceFolderDAO sourceFolderDAO) throws IOException {

        // Assuming ParsingService has a method parsingJsf similar to parsingJsp
        ParsingService.parsingJsf(jsfFilesParseOutput, sourceFolderDAO);

        JsfParseOutPutDAO jsfParseOutputDAO = parseSourceCodeMapper.getJsfParseOutputBySourceFolderId(sourceFolderDAO.getId());
        if (jsfParseOutputDAO == null) {
            jsfParseOutputDAO = new JsfParseOutPutDAO();
            jsfParseOutputDAO.setPath(jsfFilesParseOutput.getAbsolutePath());
            jsfParseOutputDAO.setName(OutputFileName.FACES_CONFIG_OUTPUT_FILE_NAME);
            jsfParseOutputDAO.setUserId(sourceFolderDAO.getUserId());
            jsfParseOutputDAO.setSourceFolderId(sourceFolderDAO.getId());
            jsfParseOutputDAO.setCreateTime(LocalDateTime.now());
            jsfParseOutputDAO.setUpdateTime(LocalDateTime.now());
            parseSourceCodeMapper.insertJsfParseOutput(jsfParseOutputDAO);
        }

        jsfParseOutputDAO.setUpdateTime(LocalDateTime.now());

        parseSourceCodeMapper.updateJsfParseOutput(jsfParseOutputDAO);

        System.out.println("JSF parse analysis complete. Results in " + jsfFilesParseOutput.getAbsolutePath());
    }

    private void ParseApplicationXmlFiles(File parseApplicationXmlParseOutput, SourceFolderDAO sourceFolderDAO) {
        try {
            ParsingService.parseApplicationXml(parseApplicationXmlParseOutput, sourceFolderDAO);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ApplicationXmlParseOutput applicationXmlParseOutput =
                parseSourceCodeMapper.getApplicationXmlParseOutputBySourceFolderId(sourceFolderDAO.getId());

        if (applicationXmlParseOutput == null) {
            applicationXmlParseOutput = new ApplicationXmlParseOutput();
            applicationXmlParseOutput.setPath(parseApplicationXmlParseOutput.getAbsolutePath());
            applicationXmlParseOutput.setName(OutputFileName.APPLICATION_CONTEXT_OUTPUT_FILE_NAME);
            applicationXmlParseOutput.setUserId(sourceFolderDAO.getUserId());
            applicationXmlParseOutput.setSourceFolderId(sourceFolderDAO.getId());
            applicationXmlParseOutput.setCreateTime(LocalDateTime.now());
            applicationXmlParseOutput.setUpdateTime(LocalDateTime.now());
            parseSourceCodeMapper.insertApplicationXmlParseOutput(applicationXmlParseOutput);
        }

        parseSourceCodeMapper.updateApplicationXmlParseOutput(applicationXmlParseOutput);

        System.out.println("application.xml analysis complete. Results in "
                + parseApplicationXmlParseOutput.getAbsolutePath());
    }

    private void WebXmlFiles(File webXmlParseoutput, SourceFolderDAO sourceFolderDAO) throws IOException {
        ParsingService.parseWebXml(webXmlParseoutput, sourceFolderDAO);

        WebXmlParseOutput webXmlParseOutput = parseSourceCodeMapper.getWebXmlParseOutputBySourceFolderId(sourceFolderDAO.getId());
        if(webXmlParseOutput==null)
        {
            webXmlParseOutput = new WebXmlParseOutput();
            webXmlParseOutput.setPath(webXmlParseoutput.getAbsolutePath());
            webXmlParseOutput.setName(OutputFileName.WEB_XML_OUTPUT_FILE_NAME);
            webXmlParseOutput.setUserId(sourceFolderDAO.getUserId());
            webXmlParseOutput.setSourceFolderId(sourceFolderDAO.getId());
            webXmlParseOutput.setCreateTime(LocalDateTime.now());
            webXmlParseOutput.setUpdateTime(LocalDateTime.now());
            parseSourceCodeMapper.insertWebXmlParseOutput(webXmlParseOutput);
        }



        parseSourceCodeMapper.updateWebXmlParseOutput(webXmlParseOutput);


        System.out.println("Web.xml analysis complete. Results in "
                + webXmlParseoutput.getAbsolutePath());
    }


    public void staticParsingJspFiles(File outputLog, File errorLog, SourceFolderDAO sourceFolderDAO) throws IOException {

        ParsingService.parsingJsp(outputLog, errorLog, sourceFolderDAO);

        JspParseOutPutDAO jspParseOutPutDAO = parseSourceCodeMapper.getJspParseOutputBySourceFolderId(sourceFolderDAO.getId());
        if(jspParseOutPutDAO==null)
        {
            jspParseOutPutDAO = new JspParseOutPutDAO();
            jspParseOutPutDAO.setPath(outputLog.getAbsolutePath());
            jspParseOutPutDAO.setName(OutputFileName.JSP_OUTPUT_FILE_NAME);
            jspParseOutPutDAO.setUserId(sourceFolderDAO.getUserId());
            jspParseOutPutDAO.setSourceFolderId(sourceFolderDAO.getId());
            jspParseOutPutDAO.setCreateTime(LocalDateTime.now());
            jspParseOutPutDAO.setUpdateTime(LocalDateTime.now());
            parseSourceCodeMapper.insertJspParseOutput(jspParseOutPutDAO);
        }

        JspParseErrorDAO jspParseErrorDAO = parseSourceCodeMapper.getJspParseErrorBySourceFolderId(sourceFolderDAO.getId());

        if(jspParseErrorDAO == null)
        {
            jspParseErrorDAO = new JspParseErrorDAO();
            BeanUtils.copyProperties(jspParseOutPutDAO, jspParseErrorDAO);
            jspParseErrorDAO.setJspParseOutPutId(jspParseOutPutDAO.getId());
            jspParseErrorDAO.setName(OutputFileName.JSP_PARSE_ERROR_FILE_NAME);
            jspParseErrorDAO.setPath(sourceFolderDAO.getDirPath() + "\\" + OutputPath.OUTPUT_BASE_DIR + "\\" + OutputPath.JSP_PARSE_ERROR_LOG_PATH);
            parseSourceCodeMapper.insertJspParseError(jspParseErrorDAO);
        }

        jspParseOutPutDAO.setUpdateTime(LocalDateTime.now());
        jspParseErrorDAO.setUpdateTime(LocalDateTime.now());

        parseSourceCodeMapper.updateJspParseOutput(jspParseOutPutDAO);
        parseSourceCodeMapper.updateJspParseError(jspParseErrorDAO);

                System.out.println("Jsp parse analysis complete. Results in "
                + outputLog.getAbsolutePath() + ", errors in " + errorLog.getAbsolutePath());
    }

    @Transactional
    public void parsing(File output, File errorLog, Integer userId, Integer sourceFolderId, SourceFolderDAO sourceFolderDAO) throws IOException {

        ParsingService.parsing(output, errorLog, sourceFolderDAO);
        JavaFilesParseDAO javaFilesParseDAO = parseSourceCodeMapper.getOutPutBySourceFolderId(sourceFolderId);

        if(javaFilesParseDAO ==null)
        {
            javaFilesParseDAO = new JavaFilesParseDAO();
            javaFilesParseDAO.setPath(output.getAbsolutePath());
            javaFilesParseDAO.setName(OutputFileName.JAVA_FILE_OUTPUT_FILE_NAME);
            javaFilesParseDAO.setSourceFolderId(sourceFolderId);
            javaFilesParseDAO.setUserId(userId);
            javaFilesParseDAO.setCreateTime(LocalDateTime.now());
            javaFilesParseDAO.setUpdateTime(LocalDateTime.now());
            parseSourceCodeMapper.insertJavaFilesParseOutPut(javaFilesParseDAO);
        }

        JavaFilesErrorDAO javaFilesErrorDAO = parseSourceCodeMapper.getErrorBySourceFolderId(sourceFolderId);

        if(javaFilesErrorDAO == null)
        {
            javaFilesErrorDAO = new JavaFilesErrorDAO();
            BeanUtils.copyProperties(javaFilesParseDAO, javaFilesErrorDAO);
            javaFilesErrorDAO.setOutputId(javaFilesParseDAO.getId());
            javaFilesErrorDAO.setName(OutputFileName.JAVA_FILE_ERROR_FILE_NAME);
            javaFilesErrorDAO.setPath(sourceFolderDAO.getDirPath() +  "\\" +OutputPath.OUTPUT_BASE_DIR + "\\" +OutputPath.JAVA_PARSE_ERROR_LOG_PATH);
            parseSourceCodeMapper.insertJavaFilesError(javaFilesErrorDAO);
        }


        javaFilesParseDAO.setUpdateTime(LocalDateTime.now());
        javaFilesErrorDAO.setUpdateTime(LocalDateTime.now());

        parseSourceCodeMapper.updateJavaFilesParseOutput(javaFilesParseDAO);
        parseSourceCodeMapper.updateJavaFilesPError(javaFilesErrorDAO);

        System.out.println("Spoon-based analysis complete. Results in "
                + output.getAbsolutePath() + ", errors in " + errorLog.getAbsolutePath());
//
    }





}

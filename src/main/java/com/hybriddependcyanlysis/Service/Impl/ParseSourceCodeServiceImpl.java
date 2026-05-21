package com.hybriddependcyanlysis.Service.Impl;
import Common.OutputFileName;
import Common.OutputPath;
import Common.UserContext.UserContextHolder;
import com.hybriddependcyanlysis.Mapper.IngestMapper;
import com.hybriddependcyanlysis.Mapper.FileMapper;
import com.hybriddependcyanlysis.Mapper.ParseSourceCodeMapper;
import com.hybriddependcyanlysis.POJO.DAO.*;
import com.hybriddependcyanlysis.Service.JsonFileService;
import com.hybriddependcyanlysis.Service.ParseSourceCodeService;
import Common.Util.ParsingUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.time.LocalDateTime;

@Service
public class ParseSourceCodeServiceImpl implements ParseSourceCodeService {

    @Autowired
    private ParseSourceCodeMapper parseSourceCodeMapper;

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private IngestMapper ingestMapper;

    @Autowired
    private JsonFileService jsonFileService;

    private ParsingUtil parsingUtil;

    @PostConstruct
    public void init() {
        this.parsingUtil = new ParsingUtil(jsonFileService);
    }


    @Override
    @Transactional
    public void staticParsing(Integer sourceFolderId) throws IOException {

        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new RemoteException("User not authenticated");
        }

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
    public void parseJspFile(Integer sourceFolderId) throws IOException {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new RemoteException("User not authenticated");
        }
        SourceFolderDAO sourceFolderDAO = ingestMapper.getById(sourceFolderId);
        Path outputRoot = checkOutputFolder(sourceFolderDAO.getDirPath());

        Path JspParseOutputPath = outputRoot.resolve(OutputPath.JSP_PARSE_RESULT_PATH);
        Path JspParseErrorPath = outputRoot.resolve(OutputPath.JSP_PARSE_ERROR_LOG_PATH);

        File JspParseOutput = JspParseOutputPath.toFile();
        File JspParseError = JspParseErrorPath.toFile();

        staticParsingJspFiles(JspParseOutput, JspParseError, sourceFolderDAO);
    }

    @Override
    public void parseWebXmlFile(Integer sourceFolderId) throws IOException {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new RemoteException("User not authenticated");
        }
        SourceFolderDAO sourceFolderDAO = ingestMapper.getById(sourceFolderId);
        Path outputRoot = checkOutputFolder(sourceFolderDAO.getDirPath());

        Path webXmlResultPath = outputRoot.resolve(OutputPath.WEB_XML_PARSE_RESULT_PATH);
        // 如果有错误日志： Path errorPath = outputRoot.resolve(OutputPath.WEB_XML_PARSE_ERROR_LOG_PATH);

        File webXmlOutput = webXmlResultPath.toFile();


        WebXmlFiles(webXmlOutput, sourceFolderDAO);
    }

    @Override
    public void ParsePersistenceXmlFile(Integer sourceFolderId) throws IOException {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new RemoteException("User not authenticated");
        }
        SourceFolderDAO sourceFolderDAO = ingestMapper.getById(sourceFolderId);
        Path outputRoot = checkOutputFolder(sourceFolderDAO.getDirPath());

        Path persistenceResultPath = outputRoot.resolve(OutputPath.PERSISTENCE_PARSE_RESULT_PATH);  // 假设你常量是这个名字

        File persistenceOutput = persistenceResultPath.toFile();

        ParsePersistenceXmlFiles(persistenceOutput, sourceFolderDAO);
    }

    private void ParsePersistenceXmlFiles(File parsePersistenceXmlParseOutput, SourceFolderDAO sourceFolderDAO) {
        try {
            parsingUtil.parsePersistenceXml(parsePersistenceXmlParseOutput, sourceFolderDAO);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        PersistenceXmlParseOutputDAO persistenceXmlParseOutputDAO = parseSourceCodeMapper.getPersistenceXmlParseOutputBySourceFolderId(sourceFolderDAO.getId());
        if(persistenceXmlParseOutputDAO ==null)
        {
            persistenceXmlParseOutputDAO = new PersistenceXmlParseOutputDAO();
            persistenceXmlParseOutputDAO.setPath(parsePersistenceXmlParseOutput.getAbsolutePath());
            persistenceXmlParseOutputDAO.setName(OutputFileName.PERSISTENCE_OUTPUT_FILE_NAME);
            persistenceXmlParseOutputDAO.setUserId(sourceFolderDAO.getUserId());
            persistenceXmlParseOutputDAO.setSourceFolderId(sourceFolderDAO.getId());
            persistenceXmlParseOutputDAO.setCreateTime(LocalDateTime.now());
            persistenceXmlParseOutputDAO.setUpdateTime(LocalDateTime.now());
            parseSourceCodeMapper.insertPersistenceXmlParseOutput(persistenceXmlParseOutputDAO);
        }



        parseSourceCodeMapper.updatePersistenceXmlParseOutput(persistenceXmlParseOutputDAO);


        System.out.println("persistence.xml analysis complete. Results in "
                + parsePersistenceXmlParseOutput.getAbsolutePath());
    }

    @Override
    public void ParseEjbJarXmlFile(Integer sourceFolderId) throws IOException {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new RemoteException("User not authenticated");
        }
        SourceFolderDAO sourceFolderDAO = ingestMapper.getById(sourceFolderId);
        Path outputRoot = checkOutputFolder(sourceFolderDAO.getDirPath());

        Path ejbResultPath = outputRoot.resolve(OutputPath.EJB_JAR_PARSE_RESULT_PATH);

        File ejbOutput = ejbResultPath.toFile();

        ParseEjbJarXmlFiles(ejbOutput, sourceFolderDAO);
    }

    private void ParseEjbJarXmlFiles(File parseEjbJarXmlParseOutput, SourceFolderDAO sourceFolderDAO) {
        try {
            parsingUtil.parseEjbJarXml(parseEjbJarXmlParseOutput, sourceFolderDAO);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        EjbJarXmlParseOutputDAO ejbJarXmlParseOutputDAO = parseSourceCodeMapper.getEjbJarXmlParseOutputBySourceFolderId(sourceFolderDAO.getId());
        if(ejbJarXmlParseOutputDAO ==null)
        {
            ejbJarXmlParseOutputDAO = new EjbJarXmlParseOutputDAO();
            ejbJarXmlParseOutputDAO.setPath(parseEjbJarXmlParseOutput.getAbsolutePath());
            ejbJarXmlParseOutputDAO.setName(OutputFileName.EJB_JAR_XML_OUTPUT_FILE_NAME);
            ejbJarXmlParseOutputDAO.setUserId(sourceFolderDAO.getUserId());
            ejbJarXmlParseOutputDAO.setSourceFolderId(sourceFolderDAO.getId());
            ejbJarXmlParseOutputDAO.setCreateTime(LocalDateTime.now());
            ejbJarXmlParseOutputDAO.setUpdateTime(LocalDateTime.now());
            parseSourceCodeMapper.insertEjbJarXmlParseOutput(ejbJarXmlParseOutputDAO);
        }

        parseSourceCodeMapper.updateEjbJarXmlParseOutput(ejbJarXmlParseOutputDAO);

        System.out.println("ejb-jar.xml analysis complete. Results in "
                + parseEjbJarXmlParseOutput.getAbsolutePath());
    }
    

    @Override
    public void ParseFacesConfigXmlFile(Integer sourceFolderId) throws IOException {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new RemoteException("User not authenticated");
        }
        SourceFolderDAO sourceFolderDAO = ingestMapper.getById(sourceFolderId);
        Path outputRoot = checkOutputFolder(sourceFolderDAO.getDirPath());

        Path facesResultPath = outputRoot.resolve(OutputPath.FACES_CONFIG_PARSE_RESULT_PATH);

        File facesOutput = facesResultPath.toFile();

        ParseFacesConfigXmlFiles(facesOutput, sourceFolderDAO);
    }

    private void ParseFacesConfigXmlFiles(File parseFacesConfigXmlParseOutput, SourceFolderDAO sourceFolderDAO) {
        try {
            parsingUtil.parseFacesConfigXml(parseFacesConfigXmlParseOutput, sourceFolderDAO);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        FacesConfigXmlParseOutputDAO facesConfigXmlParseOutputDAO =
                parseSourceCodeMapper.getFacesConfigXmlParseOutputBySourceFolderId(sourceFolderDAO.getId());

        if (facesConfigXmlParseOutputDAO == null) {
            facesConfigXmlParseOutputDAO = new FacesConfigXmlParseOutputDAO();
            facesConfigXmlParseOutputDAO.setPath(parseFacesConfigXmlParseOutput.getAbsolutePath());
            facesConfigXmlParseOutputDAO.setName(OutputFileName.FACES_CONFIG_OUTPUT_FILE_NAME);
            facesConfigXmlParseOutputDAO.setUserId(sourceFolderDAO.getUserId());
            facesConfigXmlParseOutputDAO.setSourceFolderId(sourceFolderDAO.getId());
            facesConfigXmlParseOutputDAO.setCreateTime(LocalDateTime.now());
            facesConfigXmlParseOutputDAO.setUpdateTime(LocalDateTime.now());
            parseSourceCodeMapper.insertFacesConfigXmlParseOutput(facesConfigXmlParseOutputDAO);
        }

        parseSourceCodeMapper.updateFacesConfigXmlParseOutput(facesConfigXmlParseOutputDAO);

        System.out.println("faces-config.xml analysis complete. Results in "
                + parseFacesConfigXmlParseOutput.getAbsolutePath());
    }

    @Override
    public void ParseApplicationXmlFile(Integer sourceFolderId) throws IOException {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new RemoteException("User not authenticated");
        }
        SourceFolderDAO sourceFolderDAO = ingestMapper.getById(sourceFolderId);
        Path outputRoot = checkOutputFolder(sourceFolderDAO.getDirPath());

        Path appResultPath = outputRoot.resolve(OutputPath.EAR_APPLICATION_PARSE_RESULT_PATH);

        File appOutput = appResultPath.toFile();

        ParseApplicationXmlFiles(appOutput, sourceFolderDAO);
    }

    @Override
    public void parseJsfFile(Integer sourceFolderId) throws IOException {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new RemoteException("User not authenticated");
        }
        SourceFolderDAO sourceFolderDAO = ingestMapper.getById(sourceFolderId);
        Path outputRoot = checkOutputFolder(sourceFolderDAO.getDirPath());

        Path jsfResultPath = outputRoot.resolve(OutputPath.JSF_PARSE_RESULT_PATH);

        File jsfOutput = jsfResultPath.toFile();

        parseJsfFiles(jsfOutput, sourceFolderDAO);
    }


    @Transactional
    @Override
    public void staticParseFile(Integer sourceFolderId) throws IOException {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new RemoteException("User not authenticated");
        }
        SourceFolderDAO sourceFolderDAO = ingestMapper.getById(sourceFolderId);
        parsingUtil.staticParseFiles(sourceFolderDAO);
    }

    @Override
    public void parsePomXmlFile(Integer sourceFolderId) throws IOException {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new RemoteException("User not authenticated");
        }
        SourceFolderDAO sourceFolderDAO = ingestMapper.getById(sourceFolderId);

        Path outputPath = checkOutputFolder(sourceFolderDAO.getDirPath());

        Path pomResultPath = outputPath.resolve(OutputPath.POM_XML_PARSE_RESULT_PATH);

        File pomOutput = pomResultPath.toFile();

        parsePomXmlFile(pomOutput, sourceFolderDAO);

    }

    private void parsePomXmlFile(File pomOutput, SourceFolderDAO sourceFolderDAO) throws IOException {
        parsingUtil.parsePomXmlFile(pomOutput,sourceFolderDAO);

        PomXmlParseOutputDAO pomXmlParseOutputDAO = parseSourceCodeMapper.getPomXmlParseOutputBySourceFolderId(sourceFolderDAO.getId());

        if (pomXmlParseOutputDAO == null) {
            pomXmlParseOutputDAO = new PomXmlParseOutputDAO();
            pomXmlParseOutputDAO.setPath(pomOutput.getAbsolutePath());           // ← 改用 pomOutput
            pomXmlParseOutputDAO.setName(OutputFileName.POM_XML_OUTPUT_FILE_NAME);  // ← 建議改用適合POM的檔名常量
            pomXmlParseOutputDAO.setUserId(sourceFolderDAO.getUserId());
            pomXmlParseOutputDAO.setSourceFolderId(sourceFolderDAO.getId());
            pomXmlParseOutputDAO.setCreateTime(LocalDateTime.now());
            pomXmlParseOutputDAO.setUpdateTime(LocalDateTime.now());

            // 假設你已經新增對應的 insert 方法
            parseSourceCodeMapper.insertPomXmlParseOutput(pomXmlParseOutputDAO);
        }

        pomXmlParseOutputDAO.setUpdateTime(LocalDateTime.now());

// 假設你已經新增對應的 update 方法
        parseSourceCodeMapper.updatePomXmlParseOutput(pomXmlParseOutputDAO);

        System.out.println("POM parse analysis complete. Results in " + pomOutput.getAbsolutePath());

    }

    private void parseJsfFiles(File jsfFilesParseOutput, SourceFolderDAO sourceFolderDAO) throws IOException {

        // Assuming ParsingService has a method parsingJsf similar to parsingJsp
        parsingUtil.parsingJsf(jsfFilesParseOutput, sourceFolderDAO);

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
            parsingUtil.parseApplicationXml(parseApplicationXmlParseOutput, sourceFolderDAO);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ApplicationXmlParseOutputDAO applicationXmlParseOutputDAO =
                parseSourceCodeMapper.getApplicationXmlParseOutputBySourceFolderId(sourceFolderDAO.getId());

        if (applicationXmlParseOutputDAO == null) {
            applicationXmlParseOutputDAO = new ApplicationXmlParseOutputDAO();
            applicationXmlParseOutputDAO.setPath(parseApplicationXmlParseOutput.getAbsolutePath());
            applicationXmlParseOutputDAO.setName(OutputFileName.APPLICATION_CONTEXT_OUTPUT_FILE_NAME);
            applicationXmlParseOutputDAO.setUserId(sourceFolderDAO.getUserId());
            applicationXmlParseOutputDAO.setSourceFolderId(sourceFolderDAO.getId());
            applicationXmlParseOutputDAO.setCreateTime(LocalDateTime.now());
            applicationXmlParseOutputDAO.setUpdateTime(LocalDateTime.now());
            parseSourceCodeMapper.insertApplicationXmlParseOutput(applicationXmlParseOutputDAO);
        }

        parseSourceCodeMapper.updateApplicationXmlParseOutput(applicationXmlParseOutputDAO);

        System.out.println("application.xml analysis complete. Results in "
                + parseApplicationXmlParseOutput.getAbsolutePath());
    }

    private void WebXmlFiles(File webXmlParseoutput, SourceFolderDAO sourceFolderDAO) throws IOException {
        parsingUtil.parseWebXml(webXmlParseoutput, sourceFolderDAO);

        WebXmlParseOutputDAO webXmlParseOutputDAO = parseSourceCodeMapper.getWebXmlParseOutputBySourceFolderId(sourceFolderDAO.getId());
        if(webXmlParseOutputDAO ==null)
        {
            webXmlParseOutputDAO = new WebXmlParseOutputDAO();
            webXmlParseOutputDAO.setPath(webXmlParseoutput.getAbsolutePath());
            webXmlParseOutputDAO.setName(OutputFileName.WEB_XML_OUTPUT_FILE_NAME);
            webXmlParseOutputDAO.setUserId(sourceFolderDAO.getUserId());
            webXmlParseOutputDAO.setSourceFolderId(sourceFolderDAO.getId());
            webXmlParseOutputDAO.setCreateTime(LocalDateTime.now());
            webXmlParseOutputDAO.setUpdateTime(LocalDateTime.now());
            parseSourceCodeMapper.insertWebXmlParseOutput(webXmlParseOutputDAO);
        }



        parseSourceCodeMapper.updateWebXmlParseOutput(webXmlParseOutputDAO);


        System.out.println("Web.xml analysis complete. Results in "
                + webXmlParseoutput.getAbsolutePath());
    }


    public void staticParsingJspFiles(File outputLog, File errorLog, SourceFolderDAO sourceFolderDAO) throws IOException {

        parsingUtil.parsingJsp(outputLog, errorLog, sourceFolderDAO);

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

        parsingUtil.parsing(output, errorLog, sourceFolderDAO);
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

    @Override
    @Transactional
    public void deleteJavaParseResults(Integer sourceFolderId) {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new RuntimeException("User not authenticated");
        }
        parseSourceCodeMapper.deleteJavaParseOutputBySourceFolder(userId, sourceFolderId);
        parseSourceCodeMapper.deleteJavaErrorBySourceFolder(userId, sourceFolderId);
    }

    @Override
    @Transactional
    public void deleteJspParseResults(Integer sourceFolderId) {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new RuntimeException("User not authenticated");
        }
        parseSourceCodeMapper.deleteJspParseOutputBySourceFolder(userId, sourceFolderId);
        parseSourceCodeMapper.deleteJspErrorBySourceFolder(userId, sourceFolderId);
    }

    @Override
    @Transactional
    public void deleteWebXmlParseResults(Integer sourceFolderId) {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new RuntimeException("User not authenticated");
        }
        parseSourceCodeMapper.deleteWebXmlParseOutput(userId, sourceFolderId);
    }

    @Override
    @Transactional
    public void deletePersistenceXmlParseResults(Integer sourceFolderId) {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new RuntimeException("User not authenticated");
        }
        parseSourceCodeMapper.deletePersistenceXmlParseOutput(userId, sourceFolderId);
    }

    @Override
    @Transactional
    public void deleteEjbJarXmlParseResults(Integer sourceFolderId) {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new RuntimeException("User not authenticated");
        }
        parseSourceCodeMapper.deleteEjbJarXmlParseOutput(userId, sourceFolderId);
    }

    @Override
    @Transactional
    public void deleteFacesConfigXmlParseResults(Integer sourceFolderId) {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new RuntimeException("User not authenticated");
        }
        parseSourceCodeMapper.deleteFacesConfigXmlParseOutput(userId, sourceFolderId);
    }

    @Override
    @Transactional
    public void deleteApplicationXmlParseResults(Integer sourceFolderId) {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new RuntimeException("User not authenticated");
        }
        parseSourceCodeMapper.deleteApplicationXmlParseOutput(userId, sourceFolderId);
    }

    @Override
    @Transactional
    public void deletePomXmlParseResults(Integer sourceFolderId) {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new RuntimeException("User not authenticated");
        }
        parseSourceCodeMapper.deletePomXmlParseOutput(userId, sourceFolderId);
    }

    @Override
    @Transactional
    public void deleteJsfParseResults(Integer sourceFolderId) {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new RuntimeException("User not authenticated");
        }
        parseSourceCodeMapper.deleteJsfParseOutput(userId, sourceFolderId);
    }

}

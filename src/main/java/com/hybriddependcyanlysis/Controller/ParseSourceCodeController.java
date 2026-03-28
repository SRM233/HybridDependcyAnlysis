package com.hybriddependcyanlysis.Controller;

import Common.Result;
import Common.UserContext.UserContextHolder;
import com.hybriddependcyanlysis.POJO.DTO.SourceFolderDTO;
import com.hybriddependcyanlysis.POJO.DTO.UserDTO;
import com.hybriddependcyanlysis.Service.ParseSourceCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/parse")
public class ParseSourceCodeController {

    @Autowired
    private ParseSourceCodeService parseSourceCodeService;


    //用户解析.java源文件请求
    @PostMapping("/parseJavaFiles")
    public Result parseJavaFiles(Integer sourceFolderId)
    {
        try {
            parseSourceCodeService.staticParsing(sourceFolderId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Result.success();
    }

    @PostMapping("/parseJspFiles")
    public Result parseJspFiles(Integer sourceFolderId) throws IOException {
        log.info("Source folder id:{}", sourceFolderId);
        parseSourceCodeService.parseJspFile(sourceFolderId);
        return Result.success();
    }

    @PostMapping("/parseWebXml")
    public Result ParseWebXml(Integer sourceFolderId) throws IOException {
        log.info("Source folder id:{}", sourceFolderId);
        parseSourceCodeService.parseWebXmlFile(sourceFolderId);
        return Result.success();
    }

    @PostMapping("/parsePersistenceXm")
    public Result ParsePersistenceXml(Integer sourceFolderId) throws IOException {
        log.info("Source folder id:{}", sourceFolderId);
        parseSourceCodeService.ParsePersistenceXmlFile(sourceFolderId);
        return Result.success();
    }

    @PostMapping("/parseEjbJarXml")
    public Result ParseEjbJarXml(Integer sourceFolderId) throws IOException {
        log.info("Source folder id:{}", sourceFolderId);
        parseSourceCodeService.ParseEjbJarXmlFile(sourceFolderId);
        return Result.success();
    }

    @PostMapping("/parseFacesConfigXml")
    public Result ParseFacesConfigXml(Integer sourceFolderId) throws IOException {
        log.info("Source folder id:{}", sourceFolderId);
        parseSourceCodeService.ParseFacesConfigXmlFile(sourceFolderId);
        return Result.success();
    }

    @PostMapping("/parseApplicationXml")
    public Result ParseApplicationXml(Integer sourceFolderId) throws IOException {
        log.info("Source folder id:{}", sourceFolderId);
        parseSourceCodeService.ParseApplicationXmlFile(sourceFolderId);
        return Result.success();
    }

    @PostMapping("/parseJsfFile")
    public Result parseJsfFile(Integer sourceFolderId) throws IOException {
        log.info("Source folder id:{}", sourceFolderId);
        parseSourceCodeService.parseJsfFile(sourceFolderId);
        return Result.success();
    }

    @PostMapping("/staticParseFile")
    public Result staticParseFile(Integer sourceFolderId) throws IOException {
        log.info("Source folder id:{}", sourceFolderId);
        parseSourceCodeService.staticParseFile(sourceFolderId);
        return Result.success();
    }

    @PostMapping("/parsePomXmlFile")
    public Result parsePomXmlFile(Integer sourceFolderId) throws IOException {
        log.info("Source folder id:{}", sourceFolderId);
        parseSourceCodeService.parsePomXmlFile(sourceFolderId);
        return Result.success();
    }

    @DeleteMapping("/deleteJavaParseResults")
    public Result deleteJavaParseResults(Integer sourceFolderId) {
        log.info("Deleting Java parse results for source folder id:{}", sourceFolderId);
        parseSourceCodeService.deleteJavaParseResults(sourceFolderId);
        return Result.success();
    }

    @DeleteMapping("/deleteJspParseResults")
    public Result deleteJspParseResults(Integer sourceFolderId) {
        log.info("Deleting JSP parse results for source folder id:{}", sourceFolderId);
        parseSourceCodeService.deleteJspParseResults(sourceFolderId);
        return Result.success();
    }

    @DeleteMapping("/deleteWebXmlParseResults")
    public Result deleteWebXmlParseResults(Integer sourceFolderId) {
        log.info("Deleting Web XML parse results for source folder id:{}", sourceFolderId);
        parseSourceCodeService.deleteWebXmlParseResults(sourceFolderId);
        return Result.success();
    }

    @DeleteMapping("/deletePersistenceXmlParseResults")
    public Result deletePersistenceXmlParseResults(Integer sourceFolderId) {
        log.info("Deleting Persistence XML parse results for source folder id:{}", sourceFolderId);
        parseSourceCodeService.deletePersistenceXmlParseResults(sourceFolderId);
        return Result.success();
    }

    @DeleteMapping("/deleteEjbJarXmlParseResults")
    public Result deleteEjbJarXmlParseResults(Integer sourceFolderId) {
        log.info("Deleting EJB JAR XML parse results for source folder id:{}", sourceFolderId);
        parseSourceCodeService.deleteEjbJarXmlParseResults(sourceFolderId);
        return Result.success();
    }

    @DeleteMapping("/deleteFacesConfigXmlParseResults")
    public Result deleteFacesConfigXmlParseResults(Integer sourceFolderId) {
        log.info("Deleting Faces Config XML parse results for source folder id:{}", sourceFolderId);
        parseSourceCodeService.deleteFacesConfigXmlParseResults(sourceFolderId);
        return Result.success();
    }

    @DeleteMapping("/deleteApplicationXmlParseResults")
    public Result deleteApplicationXmlParseResults(Integer sourceFolderId) {
        log.info("Deleting Application XML parse results for source folder id:{}", sourceFolderId);
        parseSourceCodeService.deleteApplicationXmlParseResults(sourceFolderId);
        return Result.success();
    }

    @DeleteMapping("/deletePomXmlParseResults")
    public Result deletePomXmlParseResults(Integer sourceFolderId) {
        log.info("Deleting POM XML parse results for source folder id:{}", sourceFolderId);
        parseSourceCodeService.deletePomXmlParseResults(sourceFolderId);
        return Result.success();
    }

    @DeleteMapping("/deleteJsfParseResults")
    public Result deleteJsfParseResults(Integer sourceFolderId) {
        log.info("Deleting JSF parse results for source folder id:{}", sourceFolderId);
        parseSourceCodeService.deleteJsfParseResults(sourceFolderId);
        return Result.success();
    }

//    @PostMapping("/parsingClientSide")
//    public Result parsingClientFiles(Integer sourceFolderId)
//    {
//        log.info("Source folder id:{}", sourceFolderId);
//        try {
//            parseSourceCodeService.staticParsingClientSide(sourceFolderId);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        return Result.success();
//    }
}

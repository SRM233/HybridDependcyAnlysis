package com.hybriddependcyanlysis.Controller;

import Common.Result;
import com.hybriddependcyanlysis.Service.ParseSourceCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    @PostMapping("/parseJavaFiles")
    public Result parseJavaFiles(Integer userId, Integer sourceFolderId)
    {
        log.info("Source folder id:{}", sourceFolderId);
        try {
            parseSourceCodeService.staticParsing(userId, sourceFolderId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Result.success();
    }

    @PostMapping("/parseJspFiles")
    public Result parseJspFiles(Integer userId, Integer sourceFolderId) throws IOException {
        log.info("Source folder id:{}", sourceFolderId);
        parseSourceCodeService.parseJspFile(userId, sourceFolderId);
        return Result.success();
    }

    @PostMapping("/parseWebXml")
    public Result ParseWebXml(Integer userId, Integer sourceFolderId) throws IOException {
        log.info("Source folder id:{}", sourceFolderId);
        parseSourceCodeService.parseWebXmlFile(userId, sourceFolderId);
        return Result.success();
    }

    @PostMapping("/parsePersistenceXm")
    public Result ParsePersistenceXml(Integer userId, Integer sourceFolderId) throws IOException {
        log.info("Source folder id:{}", sourceFolderId);
        parseSourceCodeService.ParsePersistenceXmlFile(userId, sourceFolderId);
        return Result.success();
    }

    @PostMapping("/parseEjbJarXml")
    public Result ParseEjbJarXml(Integer userId, Integer sourceFolderId) throws IOException {
        log.info("Source folder id:{}", sourceFolderId);
        parseSourceCodeService.ParseEjbJarXmlFile(userId, sourceFolderId);
        return Result.success();
    }

    @PostMapping("/parseFacesConfigXml")
    public Result ParseFacesConfigXml(Integer userId, Integer sourceFolderId) throws IOException {
        log.info("Source folder id:{}", sourceFolderId);
        parseSourceCodeService.ParseFacesConfigXmlFile(userId, sourceFolderId);
        return Result.success();
    }

    @PostMapping("/parseApplicationXml")
    public Result ParseApplicationXml(Integer userId, Integer sourceFolderId) throws IOException {
        log.info("Source folder id:{}", sourceFolderId);
        parseSourceCodeService.ParseApplicationXmlFile(userId, sourceFolderId);
        return Result.success();
    }

    @PostMapping("/parseJsfFile")
    public Result parseJsfFile(Integer userId, Integer sourceFolderId) throws IOException {
        log.info("Source folder id:{}", sourceFolderId);
        parseSourceCodeService.parseJsfFile(userId, sourceFolderId);
        return Result.success();
    }

    @PostMapping("/staticParseFile")
    public Result staticParseFile(Integer userId, Integer sourceFolderId) throws IOException {
        log.info("Source folder id:{}", sourceFolderId);
        parseSourceCodeService.staticParseFile(userId, sourceFolderId);
        return Result.success();
    }

    @PostMapping("/parsePomXmlFile")
    public Result parsePomXmlFile(Integer userId, Integer sourceFolderId) throws IOException {
        log.info("Source folder id:{}", sourceFolderId);
        parseSourceCodeService.parsePomXmlFile(userId, sourceFolderId);
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

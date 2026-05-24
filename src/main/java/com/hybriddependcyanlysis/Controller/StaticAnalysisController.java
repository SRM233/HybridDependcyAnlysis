package com.hybriddependcyanlysis.Controller;

import Common.Result;
import Common.UserContext.UserContextHolder;
import com.hybriddependcyanlysis.POJO.DTO.UserDTO;
import com.hybriddependcyanlysis.Service.StaticAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;

@RestController
@RequestMapping("/StaticAnalysis")
@Slf4j
public class StaticAnalysisController {


    @Autowired
    private StaticAnalysisService staticAnalysisService;

//    @GetMapping("/dependencyCount")
//    public Result<HashMap<String, Integer>> dependencyCount(Integer userId, Integer sourceFolderId)
//    {
//        HashMap<String, Integer> count = staticAnalysisService.dependencyCounting(userId, sourceFolderId);
//        return Result.succes(count);
//    }
//
//    @GetMapping("/ELExpressionAnalysis")
//    public Result ELexpressionAnalysis(@RequestBody UserDTO userDTO)
//    {
//
//        log.info("ELExpressionAnalysis:{}", userDTO);
//        HashMap<String, String> el =  staticAnalysisService.ELAnalysis(userDTO);
//        return Result.success(el);
//    }


    @PostMapping("/JspFilesCount")
    public Result JspFilesCount(@RequestBody UserDTO userDTO) throws IOException {
        Object report = staticAnalysisService.JspFileCount(userDTO);
        return Result.success(report);
    }

    // User annotation analysis request
    @PostMapping("/annotationAnalysis")
    public Result AnnotationAnalysis(@RequestBody UserDTO userDTO)
    {

        log.info("GetAnnotationCount:{}", userDTO);
        try {
            Object report = staticAnalysisService.AnnotationCount(userDTO);
            return Result.success(report);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/webXMlFileAnalysis")
    public Result webXMlFileAnalysis(@RequestBody UserDTO userDTO) throws IOException {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.fail("User not authenticated");
        }
        userDTO.setId(userId);
        log.info("GetWebXMlFileAnalysis:{}", userDTO);
        Object report = staticAnalysisService.analyzeWebXml(userDTO);
        return Result.success(report);
    }

    @PostMapping("/fileStoreAnalysis")
    public Result FileStoreAnalysis(@RequestBody UserDTO userDTO) throws Exception {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.fail("User not authenticated");
        }
        userDTO.setId(userId);
        log.info("FileStoreAnalysis:{}", userDTO);
        Object report = staticAnalysisService.FileStoreAnalysis(userDTO);
        return Result.success(report);
    }

    @PostMapping("/persistenceAnalysis")
    public Result persistenceAnalysis(@RequestBody UserDTO userDTO) throws Exception {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.fail("User not authenticated");
        }
        userDTO.setId(userId);
        log.info("persistenceAnalysis:{}", userDTO);
        Object report = staticAnalysisService.persistenceAnalysis(userDTO);
        return Result.success(report);
    }

    @PostMapping("/ejbJarAnalysis")
    public Result ejbJarAnalysis(@RequestBody UserDTO userDTO) throws Exception {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.fail("User not authenticated");
        }
        userDTO.setId(userId);
        log.info("ejbJarAnalysis:{}", userDTO);
        Object report = staticAnalysisService.ejbJarAnalysis(userDTO);
        return Result.success(report);
    }

    @PostMapping("/pomXmlAnalysis")
    public Result pomXmlAnalysis(@RequestBody UserDTO userDTO) throws Exception {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.fail("User not authenticated");
        }
        userDTO.setId(userId);
        log.info("pomXmlAnalysis:{}", userDTO);
        Object report = staticAnalysisService.pomXmlAnalysis(userDTO);
        return Result.success(report);
    }

    @PostMapping("/facesXmlAnalysis")
    public Result facesXmlAnalysis(@RequestBody UserDTO userDTO) throws Exception {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.fail("User not authenticated");
        }
        userDTO.setId(userId);
        log.info("facesXmlAnalysis:{}", userDTO);
        Object report = staticAnalysisService.facesXmlAnalysis(userDTO);
        return Result.success(report);
    }

    @DeleteMapping("/deleteAnnotationAnalysis")
    public Result deleteAnnotationAnalysis(Integer sourceFolderId) {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.fail("User not authenticated");
        }
        log.info("Deleting annotation analysis for source folder id:{}", sourceFolderId);
        staticAnalysisService.deleteAnnotationAnalysis(userId, sourceFolderId);
        return Result.success();
    }

    @DeleteMapping("/deleteWebXmlAnalysis")
    public Result deleteWebXmlAnalysis(Integer sourceFolderId) {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.fail("User not authenticated");
        }
        log.info("Deleting Web XML analysis for source folder id:{}", sourceFolderId);
        staticAnalysisService.deleteWebXmlAnalysis(userId, sourceFolderId);
        return Result.success();
    }

    @DeleteMapping("/deleteFileStoreAnalysis")
    public Result deleteFileStoreAnalysis(Integer sourceFolderId) {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.fail("User not authenticated");
        }
        log.info("Deleting file store analysis for source folder id:{}", sourceFolderId);
        staticAnalysisService.deleteFileStoreAnalysis(userId, sourceFolderId);
        return Result.success();
    }

    @DeleteMapping("/deletePersistenceAnalysis")
    public Result deletePersistenceAnalysis(Integer sourceFolderId) {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.fail("User not authenticated");
        }
        log.info("Deleting persistence analysis for source folder id:{}", sourceFolderId);
        staticAnalysisService.deletePersistenceAnalysis(userId, sourceFolderId);
        return Result.success();
    }

    @DeleteMapping("/deleteEjbJarAnalysis")
    public Result deleteEjbJarAnalysis(Integer sourceFolderId) {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.fail("User not authenticated");
        }
        log.info("Deleting EJB JAR analysis for source folder id:{}", sourceFolderId);
        staticAnalysisService.deleteEjbJarAnalysis(userId, sourceFolderId);
        return Result.success();
    }

    @DeleteMapping("/deletePomXmlAnalysis")
    public Result deletePomXmlAnalysis(Integer sourceFolderId) {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.fail("User not authenticated");
        }
        log.info("Deleting POM XML analysis for source folder id:{}", sourceFolderId);
        staticAnalysisService.deletePomXmlAnalysis(userId, sourceFolderId);
        return Result.success();
    }

    @DeleteMapping("/deleteFacesConfigAnalysis")
    public Result deleteFacesConfigAnalysis(Integer sourceFolderId) {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.fail("User not authenticated");
        }
        log.info("Deleting Faces Config analysis for source folder id:{}", sourceFolderId);
        staticAnalysisService.deleteFacesConfigAnalysis(userId, sourceFolderId);
        return Result.success();
    }

    @PostMapping("/jspContentAnalysis")
    public Result JspContentAnalysis(@RequestBody UserDTO userDTO) throws IOException {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.fail("User not authenticated");
        }
        userDTO.setId(userId);
        log.info("JspContentAnalysis:{}", userDTO);
        Object report = staticAnalysisService.JspContentAnalysis(userDTO);
        return Result.success(report);
    }

    @DeleteMapping("/deleteJspContentAnalysis")
    public Result deleteJspContentAnalysis(Integer sourceFolderId) {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.fail("User not authenticated");
        }
        log.info("Deleting JSP Content analysis for source folder id:{}", sourceFolderId);
        staticAnalysisService.deleteJspContentAnalysis(userId, sourceFolderId);
        return Result.success();
    }

    @PostMapping("/jsfContentAnalysis")
    public Result JsfContentAnalysis(@RequestBody UserDTO userDTO) throws IOException {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) return Result.fail("User not authenticated");
        userDTO.setId(userId);
        log.info("JsfContentAnalysis:{}", userDTO);
        return Result.success(staticAnalysisService.JsfContentAnalysis(userDTO));
    }

    @DeleteMapping("/deleteJsfContentAnalysis")
    public Result deleteJsfContentAnalysis(Integer sourceFolderId) {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) return Result.fail("User not authenticated");
        log.info("Deleting JSF Content analysis for source folder id:{}", sourceFolderId);
        staticAnalysisService.deleteJsfContentAnalysis(userId, sourceFolderId);
        return Result.success();
    }

}

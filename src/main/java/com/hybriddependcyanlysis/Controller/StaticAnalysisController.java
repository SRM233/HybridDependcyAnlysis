package com.hybriddependcyanlysis.Controller;

import Common.Result;
import com.hybriddependcyanlysis.POJO.DTO.UserDTO;
import com.hybriddependcyanlysis.Service.StaticAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;

@RestController
@RequestMapping("StaticAnalysis")
@Slf4j
public class StaticAnalysisController {


    @Autowired
    private StaticAnalysisService staticAnalysisService;

    @GetMapping("/dependencyCount")
    public Result<HashMap<String, Integer>> dependencyCount(Integer userId, Integer sourceFolderId)
    {
        HashMap<String, Integer> count = staticAnalysisService.dependencyCounting(userId, sourceFolderId);
        return Result.succes(count);
    }

    @GetMapping("/ELExpressionAnalysis")
    public Result ELexpressionAnalysis(@RequestBody UserDTO userDTO)
    {

        log.info("ELExpressionAnalysis:{}", userDTO);
        HashMap<String, String> el =  staticAnalysisService.ELAnalysis(userDTO);
        return Result.success(el);
    }

    @PostMapping("/annotationAnalysis")
    public Result AnnotationAnalysis(@RequestBody UserDTO userDTO)
    {
        log.info("GetAnnotationCount:{}", userDTO);
        try {
            staticAnalysisService.AnnotationCount(userDTO);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Result.success();
    }

    @PostMapping("/webXMlFileAnalysis")
    public Result webXMlFileAnalysis(@RequestBody UserDTO userDTO) throws IOException {
        log.info("GetWebXMlFileAnalysis:{}", userDTO);
        staticAnalysisService.analyzeWebXml(userDTO);
        return Result.success();
    }

    @PostMapping("/fileStoreAnalysis")
    public Result FileStoreAnalysis(@RequestBody UserDTO userDTO) throws Exception {
        log.info("FileStoreAnalysis:{}", userDTO);
        staticAnalysisService.FileStoreAnalysis(userDTO);
        return Result.success();
    }

    @PostMapping("/persistenceAnalysis")
    public Result persistenceAnalysis(@RequestBody UserDTO userDTO) throws Exception {
        log.info("persistenceAnalysis:{}", userDTO);
        staticAnalysisService.persistenceAnalysis(userDTO);
        return Result.success();
    }

    @PostMapping("/ejbJarAnalysis")
    public Result ejbJarAnalysis(@RequestBody UserDTO userDTO) throws Exception {
        log.info("ejbJarAnalysis:{}", userDTO);
        staticAnalysisService.ejbJarAnalysis(userDTO);
        return Result.success();
    }

    @PostMapping("/pomXmlAnalysis")
    public Result pomXmlAnalysis(@RequestBody UserDTO userDTO) throws Exception {
        log.info("ejbJarAnalysis:{}", userDTO);
        staticAnalysisService.pomXmlAnalysis(userDTO);
        return Result.success();
    }

    @PostMapping("/facesXmlAnalysis")
    public Result facesXmlAnalysis(@RequestBody UserDTO userDTO) throws Exception {
        log.info("facesXmlAnalysis:{}", userDTO);
        staticAnalysisService.facesXmlAnalysis(userDTO);
        return Result.success();
    }




}

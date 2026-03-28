package com.hybriddependcyanlysis.Controller;

import Common.Result;
import Common.UserContext.UserContextHolder;
import com.hybriddependcyanlysis.POJO.DTO.UserFolderDTO;
import com.hybriddependcyanlysis.Service.IngestService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/ingest")
public class IngestController {

    @Autowired
    private IngestService ingestService;

    //用户上传zip档案请求
    @PostMapping("/upload")
    public Result uploadFile(@ModelAttribute UserFolderDTO userFolderDTO)
    {

        log.info("Ingest file:{}", userFolderDTO);
        ingestService.sourceFolderIngest(userFolderDTO);
//        if (file.getSize() > 1024 * 1024)
//        {
//            return Result.fail("File size is larger than 1GB");
//        }
        return Result.success("Ingest file uploaded");

    }



}

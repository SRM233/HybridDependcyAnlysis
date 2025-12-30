package com.hybriddependcyanlysis.IngestModule.Controller;

import Common.Result;
import com.hybriddependcyanlysis.POJO.DTO.UserFolderDTO;
import com.hybriddependcyanlysis.IngestModule.Service.IngestService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/ingest")
public class IngestController {

    @Autowired
    private IngestService ingestService;
    @PostMapping("/upload")
    public Result uploadFile(@ModelAttribute UserFolderDTO userFileDTO)
    {
        log.info("Ingest file:{}", userFileDTO);
        ingestService.sourceFolderIngest(userFileDTO);
//        if (file.getSize() > 1024 * 1024)
//        {
//            return Result.fail("File size is larger than 1GB");
//        }
        return Result.success();

    }



}

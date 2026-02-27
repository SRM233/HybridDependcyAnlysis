package com.hybriddependcyanlysis.Controller;

import Common.Result;
import com.hybriddependcyanlysis.POJO.DTO.UserDTO;
import com.hybriddependcyanlysis.Service.ArchiveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/Archives")
public class ArchiveController {

    @Autowired
    private ArchiveService archiveService;

    @DeleteMapping("/deleteAll")
    public Result deleteAll(Integer sourceFolderId){
        log.info("delete all ");
        archiveService.deleteAll(sourceFolderId);
        return Result.success("Archive deleted");
    }

}

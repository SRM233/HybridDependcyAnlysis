package com.hybriddependcyanlysis.Controller;

import Common.Result;
import com.hybriddependcyanlysis.Service.AstService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/ast")
public class AstController {

    @Autowired
    private AstService astService;

    @GetMapping("/getOutput")
    public Result getOutput(Integer userId, Integer sourceFolderId)
    {
        log.info("userId:{}, sourceFolderId:{}", userId, sourceFolderId);
        astService.getOutPutFile(userId, sourceFolderId);
        return Result.success();
    }

    @PostMapping("/mappingAst")
    public Result mapping(Integer userId, Integer sourceFolderId) throws IOException {
        astService.MappingOutPutFile(userId, sourceFolderId);
        return Result.success();
    }

}

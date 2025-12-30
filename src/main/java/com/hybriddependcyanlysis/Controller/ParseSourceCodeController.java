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

    @PostMapping("/parsing")
    public Result parsing(Integer sourceFolderId)
    {
        log.info("User id:{}", sourceFolderId);
        try {
            parseSourceCodeService.staticParsing(sourceFolderId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Result.success();
    }
}

package com.hybriddependcyanlysis.Controller;

import Common.Result;
import Common.UserContext.UserContextHolder;
import com.github.pagehelper.PageInfo;
import com.hybriddependcyanlysis.POJO.DAO.SourceFolderDAO;
import com.hybriddependcyanlysis.POJO.DTO.UserFolderDTO;
import com.hybriddependcyanlysis.Service.SourceFolderService;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/sourceFolder")
public class SourceFolderController {

    @Autowired
    private SourceFolderService sourceFolderService;

    //用户上传zip档案请求
    @PostMapping("/upload")
    public Result uploadFile(@ModelAttribute UserFolderDTO userFolderDTO)
    {

        log.info("Ingest file:{}", userFolderDTO);
        sourceFolderService.sourceFolderIngest(userFolderDTO);
//        if (file.getSize() > 1024 * 1024)
//        {
//            return Result.fail("File size is larger than 1GB");
//        }
        return Result.success("Ingest file uploaded");

    }

    @GetMapping("/getFolders")
    public Result<PageInfo<SourceFolderDAO>> getFolders(@RequestParam(defaultValue = "1") Integer pageNumber,
                                                        @RequestParam(defaultValue = "10") Integer pageSize)
    {
        PageInfo<SourceFolderDAO> sourceFolderList = sourceFolderService.getSourceFolders(pageNumber, pageSize);

        return Result.success(sourceFolderList);
    }

    @DeleteMapping("/deleteSourceFolders")
    public Result deleteSourdeFolders(@RequestParam Integer sourceFolderId)
    {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.fail("User not authenticated");
        }
        log.info("user delete: {}", userId);
        sourceFolderService.deleteSourdeFolders(userId, sourceFolderId);
        return Result.success();
    }

    @DeleteMapping("/deleteAstFiles")
    public Result deleteAstFiles(Integer outputId)
    {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.fail("User not authenticated");
        }
        log.info("user delete: {}", userId);
        sourceFolderService.deleteASTFiles(userId, outputId);
        return Result.success();
    }

    @DeleteMapping("/deleteJspAstFiles")
    public Result deleteJspAstFiles(Integer outputId)
    {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.fail("User not authenticated");
        }
        log.info("user delete: {}", userId);
        sourceFolderService.deleteJspAstFiles(userId, outputId);
        return Result.success();
    }


}

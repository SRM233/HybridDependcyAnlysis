package com.hybriddependcyanlysis.Service;


import com.github.pagehelper.PageInfo;
import com.hybriddependcyanlysis.POJO.DAO.SourceFolderDAO;
import com.hybriddependcyanlysis.POJO.DTO.UserFolderDTO;
import java.util.List;

public interface SourceFolderService {
    void sourceFolderIngest(UserFolderDTO userFileDTO);

    PageInfo<SourceFolderDAO> getSourceFolders(Integer pageNumber, Integer pageSize);
    
    void deleteSourdeFolders(Integer userId, Integer sourceFolderId);

    void deleteASTFiles(Integer userId, Integer outputId);

    void deleteJspAstFiles(Integer userId, Integer outputId);
}

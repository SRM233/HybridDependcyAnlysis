package com.hybriddependcyanlysis.IngestModule.Service;


import com.hybriddependcyanlysis.POJO.DTO.UserFolderDTO;

public interface IngestService {
    void sourceFolderIngest(UserFolderDTO userFileDTO);
}

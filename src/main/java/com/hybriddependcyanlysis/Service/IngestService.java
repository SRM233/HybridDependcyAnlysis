package com.hybriddependcyanlysis.Service;


import com.hybriddependcyanlysis.POJO.DTO.UserFolderDTO;

public interface IngestService {
    void sourceFolderIngest(UserFolderDTO userFileDTO);
}

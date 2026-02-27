package com.hybriddependcyanlysis.Service;

import com.hybriddependcyanlysis.POJO.DTO.UserDTO;

public interface ArchiveService {
    void deleteArchive(UserDTO userDTO);

    void deleteAll(Integer sourceFolderId);
}

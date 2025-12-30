package com.hybriddependcyanlysis.Service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface AstService {
    public void getOutPutFile(Integer userId, Integer sourceFolderId);

    public void MappingOutPutFile(Integer userId, Integer sourceFolderId) throws IOException;
}

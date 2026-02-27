package com.hybriddependcyanlysis.Service;

import com.hybriddependcyanlysis.POJO.DTO.UserDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface AstService {
    public void getOutPutFile(Integer userId, Integer sourceFolderId);

    public void MappingOutPutFile(Integer userId, Integer sourceFolderId) throws IOException;

    void deleteOutputReport(Integer userId, Integer sourceFolderId);

    void jspMapping(UserDTO userDTO);
}

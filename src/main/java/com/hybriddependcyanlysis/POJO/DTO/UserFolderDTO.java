package com.hybriddependcyanlysis.POJO.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserFolderDTO {
    private Integer id;
    private MultipartFile file;
}

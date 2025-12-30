package com.hybriddependcyanlysis.POJO.DAO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class FileDAO {
    private Integer id;
    private Integer sourceFolderId;
    private Integer outputId;
    private String filePath;
    private String fileName;
    private Long fileSize;
    private String unpackPath;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}

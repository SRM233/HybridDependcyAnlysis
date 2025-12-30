package com.hybriddependcyanlysis.POJO.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class FileDTO {
    private Integer id;
    private String filePath;
    private Integer userId;
    private String fileName;
    private Long fileSize;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

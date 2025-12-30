package com.hybriddependcyanlysis.POJO.DAO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class SourceFolderDAO {
    private Integer id;
    private Integer userId;
    private String zipName;
    private String zipPath;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

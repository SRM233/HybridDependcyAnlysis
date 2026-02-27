package com.hybriddependcyanlysis.POJO.DAO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class JavaFilesErrorDAO {
    private Integer id;
    private String name;
    private Integer userId;
    private Integer sourceFolderId;
    private Integer outputId;
    private String path;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

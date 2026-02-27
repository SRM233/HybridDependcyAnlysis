package com.hybriddependcyanlysis.POJO.DAO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class WebXmlParseOutput {
    private Integer id;
    private Integer userId;
    private Integer sourceFolderId;
    private String name;
    private String path;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

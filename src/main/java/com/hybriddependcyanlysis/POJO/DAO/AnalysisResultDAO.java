package com.hybriddependcyanlysis.POJO.DAO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisResultDAO {
    private Integer id;
    private Integer sourceFolderId;
    private Integer userId;

    private String projectPath;
    private LocalDateTime analysisTime;

    private String dependencyGraph;
    private String javaClasses;
    private String xmlConfigs;
    private String jsfFiles;
    private String issues;

    private Integer totalJavaFiles = 0;
    private Integer totalXhtmlFiles = 0;
    private Integer totalXmlFiles = 0;
    private Integer totalIssues = 0;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

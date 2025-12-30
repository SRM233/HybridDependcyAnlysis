package com.hybriddependcyanlysis.POJO.DAO;

import java.time.LocalDateTime;

enum ModuleType
{
    COMPILE, STATIC, DYNAMIC, REPORT
}

enum Status
{
    PENDING, RUNNING, SUCCESS, FAIL
}

public class AnalysisJobsDAO {
    private Integer id;
    private Integer sourceFolderId;
    private Integer userId;
    private ModuleType moduleType;
    private Status status;
    private String logPath;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

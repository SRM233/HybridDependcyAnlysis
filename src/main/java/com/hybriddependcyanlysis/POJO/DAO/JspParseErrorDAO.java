package com.hybriddependcyanlysis.POJO.DAO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class JspParseErrorDAO {
    private Integer id;
    private Integer userId;
    private Integer sourceFolderId;
    private Integer JspParseOutPutId;
    private String name;
    private String path;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}

package com.hybriddependcyanlysis.POJO.DAO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisResultsDAO {
    private Integer id;
    private String name;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer userId;
    private Integer sourceFolderId;
    private Integer parseOutputId;



}

package com.hybriddependcyanlysis.POJO;

import Common.ClassInfos.IssueInfo;
import Common.ClassInfos.JavaClassInfo;
import Common.JsfFileInfo.JsfFileInfo;
import Common.JspFileInfo;
import Common.XmlFileInfo.XmlFileInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisResultReport {
    private String projectPath;
    private LocalDateTime analysisTime;

    // 依赖图（直接用你现有的 Map）
    private Map<String, Set<String>> dependencyGraph = new HashMap<>();

    // Java 类信息（你在 parseClass 中收集的）
    private List<JavaClassInfo> javaClasses = new ArrayList<>();

    // XML 配置（按类型分组）
    private Map<String, List<XmlFileInfo>> xmlConfigs = new HashMap<>();

    // JSF 文件信息
    private List<JsfFileInfo> jsfFiles = new ArrayList<>();

    // JSP 文件信息
    private List<JspFileInfo> jspInfos = new ArrayList<>();

    // 所有 Issue（全局收集）
    private List<IssueInfo> issues = new ArrayList<>();

    // 统计
    private int totalJavaFiles = 0;
    private int totalXhtmlFiles = 0;
    private int totalXmlFiles = 0;
    private int totalIssues = 0;
}
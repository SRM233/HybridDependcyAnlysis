package com.hybriddependcyanlysis.POJO;

import Common.ClassInfos.IssueInfo;
import Common.ClassInfos.JavaClassInfo;
import Common.JsfFileInfo.JsfFileInfo;
import Common.JspFileInfo.JspFileInfo;
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

    // Dependency graph (using existing Map)
    private Map<String, Set<String>> dependencyGraph = new HashMap<>();

    // Java class info (collected in parseClass)
    private List<JavaClassInfo> javaClasses = new ArrayList<>();

    // XML configurations (grouped by type)
    private Map<String, List<XmlFileInfo>> xmlConfigs = new HashMap<>();

    // JSF file info
    private List<JsfFileInfo> jsfFiles = new ArrayList<>();

    // JSP file info
    private List<JspFileInfo> jspInfos = new ArrayList<>();

    // All Issues (globally collected)
    private List<IssueInfo> issues = new ArrayList<>();

    // Statistics
    private int totalJavaFiles = 0;
    private int totalXhtmlFiles = 0;
    private int totalXmlFiles = 0;
    private int totalIssues = 0;
}
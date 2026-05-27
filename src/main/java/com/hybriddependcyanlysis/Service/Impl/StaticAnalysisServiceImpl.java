package com.hybriddependcyanlysis.Service.Impl;

import Common.ClassInfos.IssueInfo;
import Common.DectectedProblems;
import Common.OutputPath;
import Common.UserContext.UserContextHolder;
import Common.XmlFileInfo.XmlFileInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.hybriddependcyanlysis.Mapper.ParseSourceCodeMapper;
import com.hybriddependcyanlysis.Mapper.StaticAnalysisMapper;
import com.hybriddependcyanlysis.POJO.DAO.*;
import com.hybriddependcyanlysis.POJO.DTO.UserDTO;
import com.hybriddependcyanlysis.Service.AnalysisReportService;
import com.hybriddependcyanlysis.Service.StaticAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StaticAnalysisServiceImpl implements StaticAnalysisService {


    @Autowired
    private StaticAnalysisMapper staticAnalysisMapper;

//    @Autowired
//    private AnalysisReportService analysisReportService;

    @Autowired
    private ParseSourceCodeMapper parseSourceCodeMapper;



    //web xml check fields

    //<session-config>
    //<security-constraint></security-constraint> , user-data-constraint，<transport-guarantee>CONFIDENTIAL</transport-guarantee>。
    //<context-param>
    private static final Set<String> TARGET_WEB_XML_SECTIONS = Set.of(
            "contextParams", "filters", "servlets", "listeners",
            "sessionConfig", "welcomeFiles", "taglibs",
            "resourceRefs", "securityConstraints", "securityRoles",
            "loginConfig", "errorPages", "mimeMappings", "filterMapping",
            "servletMapping"
    );


    @Transactional
    public Object JspFileCount(UserDTO userDTO) throws IOException {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new RuntimeException("User not authenticated");
        }
        userDTO.setId(userId);
        System.out.println("📊 Starting JSP file count statistics (based on simplified parsing)");

        // Get JSP parse JSON file path from database
        JspParseOutPutDAO jspDao = parseSourceCodeMapper.getJspParseOutputBySourceFolderId(userDTO.getSourceFolderId());
        if (jspDao == null) {
            return noDataReport("JSP");
        }
        String jspOutputPath = jspDao.getPath();
        File jspJsonFile = new File(jspOutputPath);
        checkJsonFile(jspJsonFile);

        // Read JSP parse JSON and parse as List<Map>
        ObjectMapper objectMapper = new ObjectMapper();
        String content = Files.readString(jspJsonFile.toPath()).trim();
        if (content.isEmpty() || content.equals("[]") || content.equals("{}")) {
            throw new RuntimeException("JSP file content is empty or null structure: " + jspOutputPath);
        }

        List<Map<String, Object>> jspClasses = objectMapper.readValue(jspJsonFile,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        // Count JSP file count
        int jspFileCount = jspClasses.size();

        // Generate report JSON file
        Path outputRoot = jspJsonFile.toPath().getParent();
        File reportFile = outputRoot.resolve("jsp-file-count-report.json").toFile();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalJspFiles", jspFileCount);
        report.put("jspFiles", jspClasses.stream().map(cls -> {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("filePath", cls.get("filePath"));
            info.put("namespaces", cls.getOrDefault("namespaces", List.of()));
            info.put("customTaglibs", cls.getOrDefault("customTaglibs", List.of()));
            info.put("javaCodeBlockCount", cls.getOrDefault("javaCodeBlockCount", 0));
            return info;
        }).collect(Collectors.toList()));
        report.put("analysisTime", LocalDateTime.now().toString());

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportFile, report);

        // Save analysis results to database
        AnalysisReportDAO analysisResultDTO = new AnalysisReportDAO();
        analysisResultDTO.setUserId(userDTO.getId());
        analysisResultDTO.setSourceFolderId(userDTO.getSourceFolderId());
        analysisResultDTO.setName("jsp-file-count");
        analysisResultDTO.setParseOutputId(jspDao.getId());
        analysisResultDTO.setPath(reportFile.getAbsolutePath());
        analysisResultDTO.setCreateTime(LocalDateTime.now());
        analysisResultDTO.setUpdateTime(LocalDateTime.now());

        AnalysisReportDAO existingResult = staticAnalysisMapper.getJspFileCountReport(analysisResultDTO);
        if (existingResult == null) {
            staticAnalysisMapper.insertResult(analysisResultDTO);
        } else {
            existingResult.setPath(reportFile.getAbsolutePath());
            existingResult.setUpdateTime(LocalDateTime.now());
            staticAnalysisMapper.updateResult(existingResult);
        }

        System.out.println("✅ JSP file count complete -> " + reportFile.getAbsolutePath());
        System.out.println("   Total found: " + jspFileCount + " JSP files");
        return report;
    }

    @Transactional
    public Object JsfFileCount(UserDTO userDTO) throws IOException {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new RuntimeException("User not authenticated");
        }
        userDTO.setId(userId);
        System.out.println("📊 Starting JSF file count statistics (based on simplified parsing)");

        // Get JSF parse JSON file path from database
        JsfParseOutPutDAO jsfDao = parseSourceCodeMapper.getJsfParseOutputBySourceFolderId(userDTO.getSourceFolderId());
        if (jsfDao == null) {
            return noDataReport("JSF");
        }
        String jsfOutputPath = jsfDao.getPath();
        File jsfJsonFile = new File(jsfOutputPath);
        checkJsonFile(jsfJsonFile);

        // Read JSF parse JSON and parse as List<Map>
        ObjectMapper objectMapper = new ObjectMapper();
        String content = Files.readString(jsfJsonFile.toPath()).trim();
        if (content.isEmpty() || content.equals("[]") || content.equals("{}")) {
            throw new RuntimeException("JSF file content is empty or null structure: " + jsfOutputPath);
        }

        List<Map<String, Object>> jsfFiles = objectMapper.readValue(jsfJsonFile,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        // Count JSF file count
        int jsfFileCount = jsfFiles.size();

        // Generate report JSON file
        Path outputRoot = jsfJsonFile.toPath().getParent();
        File reportFile = outputRoot.resolve("jsf-file-count-report.json").toFile();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalJsfFiles", jsfFileCount);
        report.put("jsfFiles", jsfFiles.stream().map(jsf -> {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("filePath", jsf.get("filePath"));
            info.put("namespaces", jsf.getOrDefault("namespaces", List.of()));
            info.put("includes", jsf.getOrDefault("includes", List.of()));
            info.put("beans", jsf.getOrDefault("beans", List.of()));
            info.put("componentCount", jsf.getOrDefault("componentCount", 0));
            info.put("maxComponentDepth", jsf.getOrDefault("maxComponentDepth", 0));
            info.put("elExpressionCount", ((List<?>) jsf.getOrDefault("elExpressions", List.of())).size());
            info.put("hardcodedPaths", jsf.getOrDefault("hardcodedPaths", List.of()));
            info.put("hasAjax", jsf.getOrDefault("hasAjax", false));
            info.put("hasBinding", jsf.getOrDefault("hasBinding", false));
            info.put("hasInputFile", jsf.getOrDefault("hasInputFile", false));
            info.put("isTransientView", jsf.getOrDefault("isTransientView", true));
            info.put("hasSubview", jsf.getOrDefault("hasSubview", false));
            info.put("hasSessionAccess", jsf.getOrDefault("hasSessionAccess", false));
            info.put("hasFacesContextAccess", jsf.getOrDefault("hasFacesContextAccess", false));
            info.put("hasApplicationAccess", jsf.getOrDefault("hasApplicationAccess", false));
            return info;
        }).collect(Collectors.toList()));
        report.put("analysisTime", LocalDateTime.now().toString());

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportFile, report);

        // Save analysis results to database
        AnalysisReportDAO analysisResultDTO = new AnalysisReportDAO();
        analysisResultDTO.setUserId(userDTO.getId());
        analysisResultDTO.setSourceFolderId(userDTO.getSourceFolderId());
        analysisResultDTO.setName("jsf-file-count");
        analysisResultDTO.setParseOutputId(jsfDao.getId());
        analysisResultDTO.setPath(reportFile.getAbsolutePath());
        analysisResultDTO.setCreateTime(LocalDateTime.now());
        analysisResultDTO.setUpdateTime(LocalDateTime.now());

        AnalysisReportDAO existingResult = staticAnalysisMapper.getJsfFileCountReport(analysisResultDTO);
        if (existingResult == null) {
            staticAnalysisMapper.insertResult(analysisResultDTO);
        } else {
            existingResult.setPath(reportFile.getAbsolutePath());
            existingResult.setUpdateTime(LocalDateTime.now());
            staticAnalysisMapper.updateResult(existingResult);
        }

        System.out.println("✅ JSF file count complete -> " + reportFile.getAbsolutePath());
        System.out.println("   Total found: " + jsfFileCount + " JSF files");
        return report;
    }

    @Override
    @Transactional
    public Object AnnotationCount(UserDTO userDTO) throws IOException {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new RemoteException("User not authenticated");
        }
        userDTO.setId(userId);
        AnalysisReportDAO analysisResultDTO = new AnalysisReportDAO();
        analysisResultDTO.setUserId(userDTO.getId());
        analysisResultDTO.setSourceFolderId(userDTO.getSourceFolderId());
        analysisResultDTO.setName("annotation-statistics");
        AnalysisReportDAO analysisReportDAO = staticAnalysisMapper.getAnnotationCountReport(analysisResultDTO);

        // get java parse output path
        ObjectMapper objectMapper = new ObjectMapper();
        JavaFilesParseDAO javaDao = parseSourceCodeMapper.getOutPutBySourceFolderId(userDTO.getSourceFolderId());
        if (javaDao == null) {
            return noDataReport("Java");
        }
        String outputPath = javaDao.getPath();

        File jsonFile = new File(outputPath);

        checkJsonFile(jsonFile);

// Read first few lines or entire content (small files can be read directly)
        String content = Files.readString(jsonFile.toPath()).trim();

        if (content.isEmpty() || content.equals("[]") || content.equals("{}")) {
            throw new RuntimeException("File content is empty or null structure: " + outputPath);
        }

        // Read directly as List<Map<String, Object>>
        List<Map<String, Object>> classes = objectMapper.readValue(jsonFile,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        if(classes.isEmpty())
        {
            throw new RuntimeException("Parse result file is null: " + outputPath);
        }

//        System.out.println("✅ Successfully read Java parse results!");
//        System.out.println("Total class count: " + classes.size());
//        System.out.println("First class example: " + classes.get(0));


        Map<String, Integer> countMap = new LinkedHashMap<>();
        Map<String, List<String>> detailMap = new LinkedHashMap<>();

        for (Map<String, Object> cls : classes) {
            String fullName = (String) cls.get("fullName");
            if (fullName == null) continue;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> annos = (List<Map<String, Object>>) cls.getOrDefault("annotations", List.of());

            // Match TARGET_ANNOTATIONS list one by one
            for (Map<String, Object> ann : annos) {
                String annoName = (String) ann.get("name");
                if (annoName == null) continue;

                if (DectectedProblems.TARGET_ANNOTATIONS.contains(annoName)) {
                    // Count by annotation name
                    countMap.merge(annoName, 1, Integer::sum);
                    // Record which class each annotation belongs to
                    detailMap.computeIfAbsent(annoName, k -> new ArrayList<>()).add(fullName);
                }
            }
        }

        // ====================== Output results ======================
//        System.out.println("\n🎉 Annotation statistics complete! Scanned " + classes.size() + " classes");
//        countMap.forEach((anno, cnt) -> {
//            System.out.printf("   %-45s : %d%n", anno, cnt);
//        });

        // ====================== Generate report JSON ======================
        Path outputRoot = jsonFile.toPath().getParent();
        File reportFile = outputRoot.resolve("annotation-statistics.json").toFile();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalClasses", classes.size());
        report.put("annotationCount", countMap);
        report.put("annotationDetails", detailMap);
        report.put("analysisTime", LocalDateTime.now().toString());
//        report.put("suggestions", new Class[]);

        Map<String, String> suggestions = new LinkedHashMap<>();
        for (String annotation : countMap.keySet()) {
            String advice = DectectedProblems.SUGGESTIONS_MAP.get(annotation);
            if (advice != null) {
                suggestions.put(annotation, advice);
            }
        }
        report.put("suggestions", suggestions);

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportFile, report);

        if(analysisReportDAO == null)
        {
            analysisResultDTO.setPath(reportFile.getAbsolutePath());
            analysisResultDTO.setParseOutputId(javaDao.getId());
            analysisResultDTO.setCreateTime(LocalDateTime.now());
            analysisResultDTO.setUpdateTime(LocalDateTime.now());

            staticAnalysisMapper.insertResult(analysisResultDTO);
//            analysisResultDAO = analysisResultDTO;
        } else {
            analysisReportDAO.setPath(reportFile.getAbsolutePath());
            analysisReportDAO.setUpdateTime(LocalDateTime.now());
            staticAnalysisMapper.updateResult(analysisReportDAO);
        }

        return report;

//        System.out.println("\n📁 Detailed statistics report saved -> " + reportFile.getAbsolutePath());

    }

    @Override
    public Object analyzeWebXml(UserDTO userDTO) throws IOException {
        WebXmlParseOutputDAO webXmlDao = parseSourceCodeMapper.getWebXmlParseOutputBySourceFolderId(userDTO.getSourceFolderId());
        if (webXmlDao == null) {
            return noDataReport("web.xml");
        }
        String outputPath = webXmlDao.getPath();
        File webXmlJsonFile = new File(outputPath);
        checkJsonFile(webXmlJsonFile);

        //read web.xml json file
        ObjectMapper objectMapper = new ObjectMapper();
        List<XmlFileInfo> webDataList = objectMapper.readValue(webXmlJsonFile,
                new TypeReference<List<XmlFileInfo>>() {});

        if (webDataList.isEmpty()) {
            System.err.println("⚠️ web.xml JSON is empty: " + outputPath);
            return Map.of();
        }

        XmlFileInfo fileInfo = webDataList.get(0);
        Map<String, Object> data = fileInfo.data;

        Path reportPath = webXmlJsonFile.toPath().getParent();
        File reportFile = reportPath.resolve("web.xml-analysis.json").toFile();
        Map<String, Object> analysis = new LinkedHashMap<>();
        List<String> migrationSuggestions = new ArrayList<>();

        // ====================== 1. Basic info (new) ======================
        analysis.put("fileType", fileInfo.fileType);
        analysis.put("filePath", fileInfo.filePath);
        analysis.put("analyzedAt", LocalDateTime.now().toString());

        // Version check: Servlet 2.x version recommend upgrade
        String version = (String) data.getOrDefault("version", "unknown");
        boolean metadataComplete = Boolean.parseBoolean(String.valueOf(data.get("metadataComplete")));
        analysis.put("webXmlVersion", version);
        analysis.put("metadataComplete", data.get("metadataComplete"));

        if (version.compareTo("3.0") < 0 && !version.equals("unknown")) {
            migrationSuggestions.add("⚠️ web.xml version " + version + " (pre-Servlet 2.x/3.0), Strongly recommend upgrading to Jakarta EE 9+ (web.xml version=6.0 or replace entirely with annotations)");
        }
        if (metadataComplete) {
            migrationSuggestions.add("ℹ️ metadata-complete=true, annotation scanning is disabled. Recommend setting to false and using @WebServlet/@WebFilter/@WebListener");
        }

        // ====================== 2. Config section count statistics ======================
        for (String section : TARGET_WEB_XML_SECTIONS) {
            Object val = data.get(section);
            int count = 0;
            if (val instanceof List) count = ((List<?>) val).size();
            else if (val instanceof Map) count = ((Map<?, ?>) val).size();

            analysis.put(section + "Count", count);
        }

        // ====================== 3. Core config deep analysis ======================

        // Context Params (hardcoded configuration)
        List<Map<String, Object>> contextParams = getList(data, "contextParams");
        analysis.put("contextParams", contextParams);
        if (!contextParams.isEmpty()) {
            Set<String> sensitiveKeys = Set.of("jdbc.", "db.", "mail.", "redis.", "spring.", "contextConfigLocation");
            boolean hasSensitive = contextParams.stream()
                    .anyMatch(p -> sensitiveKeys.stream().anyMatch(s ->
                            ((String) p.getOrDefault("name", "")).toLowerCase().contains(s)));
            if (hasSensitive) {
                migrationSuggestions.add("🚨 Hardcoded context-param found (containing sensitive DB/email config). Must be fully externalized to application.yml + Kubernetes ConfigMap/Secret");
            }
        }

        // Servlet framework detection: FacesServlet (JSF) / DispatcherServlet (Spring MVC)
        List<Map<String, Object>> servlets = getList(data, "servlets");
        boolean hasFacesServlet = servlets.stream()
                .anyMatch(s -> ((String) s.getOrDefault("servletClass", "")).contains("FacesServlet"));
        boolean hasSpringDispatcher = servlets.stream()
                .anyMatch(s -> ((String) s.getOrDefault("servletClass", "")).contains("DispatcherServlet"));
        if (hasFacesServlet) {
            migrationSuggestions.add("🔄 FacesServlet (JSF) detected. Recommend migrating to Jakarta Faces or switching to Spring Boot + Thymeleaf/React");
        }
        if (hasSpringDispatcher) {
            migrationSuggestions.add("🌱 Spring MVC DispatcherServlet detected. Recommend upgrading directly to Spring Boot 3.x (embedded Tomcat + zero XML)");
        }

        // Filters / Listeners
        List<Map<String, Object>> filters = getList(data, "filters");
        List<String> listeners = getListString(data, "listeners");
        analysis.put("filters", filters);
        analysis.put("listeners", listeners);

        // Session timeout check: over 30 min requires distributed Session
        @SuppressWarnings("unchecked")
        Map<String, Object> sessionConfig = (Map<String, Object>) data.getOrDefault("sessionConfig", Map.of());
        analysis.put("sessionConfig", sessionConfig);
        String timeout = (String) sessionConfig.getOrDefault("sessionTimeoutMinutes", "30");
        if (Integer.parseInt(timeout) > 30) {
            migrationSuggestions.add("♻️ session-timeout = " + timeout + " min -> Must use Redis / Hazelcast / Spring Session distributed session in cloud");
        }

        // Login auth method check: FORM/BASIC recommend migrating to OAuth2
        @SuppressWarnings("unchecked")
        Map<String, Object> loginConfig = (Map<String, Object>) data.getOrDefault("loginConfig", Map.of());
        String authMethod = (String) loginConfig.getOrDefault("authMethod", "");
        if (!authMethod.isEmpty()) {
            migrationSuggestions.add("🔐 detected " + authMethod + " authentication (FORM/BASIC). Recommend migrating to OAuth2 / OpenID Connect + Keycloak / Auth0");
        }

        // Taglib / Welcome Files (legacy technology)
        List<Map<String, Object>> taglibs = getList(data, "taglibs");
        List<String> welcomeFiles = getListString(data, "welcomeFiles");
        if (!taglibs.isEmpty()) {
            migrationSuggestions.add("📚 Found " + taglibs.size() + " <taglib> tags (legacy JSP tag libraries). Recommend replacing with JSTL or migrating fully to Facelets/Thymeleaf");
        }

        // Resource Refs (JNDI)
        List<Map<String, Object>> resourceRefs = getList(data, "resourceRefs");
        if (!resourceRefs.isEmpty()) {
            migrationSuggestions.add("☁️ Found " + resourceRefs.size() + " <resource-ref> (JNDI) entries. Replace all with @Value + Kubernetes Secret / AWS Secrets Manager");
            analysis.put("resourceRefDetails", resourceRefs);
        }

        // Security & Error Pages (preserving original logic)
        List<Map<String, Object>> securityConstraints = getList(data, "securityConstraints");
        List<String> securityRoles = getListString(data, "securityRoles");
        List<Map<String, Object>> errorPages = getList(data, "errorPages");

        if (!securityConstraints.isEmpty()) {
            migrationSuggestions.add("🔐 Found " + securityConstraints.size() + " security-constraint entries. Recommend migrating to Spring Security 6 / Keycloak + Istio");
        }
        boolean hasExceptionPages = errorPages.stream().anyMatch(ep -> "exception".equals(ep.get("type")));
        if (hasExceptionPages) {
            migrationSuggestions.add("📄 Custom exception-page. Recommend using @ControllerAdvice + GlobalExceptionHandler");
        }

        // ====================== 4. Aggregate migration suggestions and calculate score ======================
        analysis.put("migrationSuggestions", migrationSuggestions);
        analysis.put("totalSuggestions", migrationSuggestions.size());
        analysis.put("modernizationScore", calculateScore(migrationSuggestions.size(), version)); // More suggestions = lower score = more modernization needed

        // Save to database (recommended)
        // astMapper.saveWebXmlAnalysis(userDTO.getId(), analysis);

        //Output
        System.out.println("✅ web.xml analysis complete! File: " + fileInfo.filePath);
        System.out.println("   Version: " + version + " | Servlets: " + servlets.size() + " | Filters: " + filters.size());
        System.out.println("   Migration suggestions: " + migrationSuggestions.size() + " items | Modernization score: " + analysis.get("modernizationScore") + "/100");

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportFile, analysis);
        
        // Save analysis results to database
        AnalysisReportDAO analysisResultDTO = new AnalysisReportDAO();
        analysisResultDTO.setUserId(userDTO.getId());
        analysisResultDTO.setSourceFolderId(userDTO.getSourceFolderId());
        analysisResultDTO.setName("web-xml-analysis");
        AnalysisReportDAO analysisReportDAO = staticAnalysisMapper.getWebXmlReport(analysisResultDTO);
        
        if (analysisReportDAO == null) {
            analysisResultDTO.setParseOutputId(webXmlDao.getId());
            analysisResultDTO.setPath(reportFile.getAbsolutePath());
            analysisResultDTO.setCreateTime(LocalDateTime.now());
            analysisResultDTO.setUpdateTime(LocalDateTime.now());
            staticAnalysisMapper.insertResult(analysisResultDTO);
        } else {
            analysisReportDAO.setPath(reportFile.getAbsolutePath());
            analysisReportDAO.setUpdateTime(LocalDateTime.now());
            staticAnalysisMapper.updateResult(analysisReportDAO);
        }

        return analysis;
    }

    @Override
    public Object FileStoreAnalysis(UserDTO userDTO) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();

        JavaFilesParseDAO fileStoreJavaDao = parseSourceCodeMapper.getOutPutBySourceFolderId(userDTO.getSourceFolderId());
        if (fileStoreJavaDao == null) {
            return noDataReport("Java");
        }
        String javaFilesParseOutputPath = fileStoreJavaDao.getPath();

        File javaParseOutputFile = new File(javaFilesParseOutputPath);

        checkJsonFile(javaParseOutputFile);

        String content = Files.readString(javaParseOutputFile.toPath()).trim();

        if (content.isEmpty() || content.equals("[]") || content.equals("{}")) {
            throw new RuntimeException("File content is empty or null structure: " + javaFilesParseOutputPath);
        }

        // Read directly as List<Map<String, Object>>
        List<Map<String, Object>> classes = objectMapper.readValue(javaParseOutputFile,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        if(classes.isEmpty())
        {
            throw new RuntimeException("Parse result file is null: " + javaFilesParseOutputPath);
        }

        // Define high-risk file I/O class set
        Set<String> fileRelatedTypes = Set.of(
                "java.io.File", "java.io.FileWriter", "java.io.FileReader",
                "java.nio.file.Path", "java.nio.file.Files",
                "java.io.BufferedWriter", "java.io.OutputStream"  // Example, add common file operation classes
        );

//        Set<String> fileRelatedMethods = Set.of(
//                "write", "createNewFile", "delete", "mkdir", "exists",
//                "getAbsolutePath", "listFiles"  // Example, common file methods
//        );

        List<Map<String, Object>> fileRiskClasses = new ArrayList<>();  // Store risk class details
        int totalRiskCount = 0;  // Total risk point count

        for (Map<String, Object> cls : classes) {
            String fullName = (String) cls.get("fullName");
            if (fullName == null) continue;

            boolean hasFileRisk = false;
            List<String> riskDetails = new ArrayList<>();  // Risk point list for this class

            // Check fields (fields array)
            List<String> imports = (List<String>) cls.getOrDefault("imports", List.of());

//            System.out.println("Class " + fullName + " has " + imports.size() + " imports");

            // Scan each class imports list
            for(String importClss : imports)
            {
                // Match file I/O related imports
                if(fileRelatedTypes.stream().anyMatch(importClss::contains))
                {
                    hasFileRisk = true;
                    riskDetails.add("import risk: " + importClss);
                    totalRiskCount++;
                }
            }

            // If risk exists, record class details
            if (hasFileRisk) {
                Map<String, Object> riskCls = new HashMap<>();
                riskCls.put("className", fullName);  // Risk class name
                riskCls.put("classType", cls.get("kind"));  // Class type (e.g., "Class" or "Interface")
                riskCls.put("riskDetails", riskDetails);  // Risk detail list

                // Record "what class references this class" (assuming you have dependencyGraph or reverseDependencies map)
                // If no dependencyGraph, you can comment this line or build a reverseGraph later
                // List<String> referencedBy = getReferencedBy(fullName);  // Reverse lookup from dependencyGraph
                // riskCls.put("referencedBy", referencedBy);

                fileRiskClasses.add(riskCls);
            }


        }
        Path outputRoot = javaParseOutputFile.toPath().getParent();
        File reportFile = outputRoot.resolve("file-store-analysis-report.json").toFile();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalRiskCount", totalRiskCount);
        report.put("riskClasses", fileRiskClasses);

        // Generate suggestions (optional, add based on your needs)
//        List<String> suggestions = new ArrayList<>();
//        if (totalRiskCount > 0) {
//            suggestions.add("⚠️ Detected " + totalRiskCount + " file storage related risk point(s)");
//            suggestions.add("Suggestion: Migrate to cloud storage service (e.g. AWS S3), avoid local file operations");
//        } else {
//            suggestions.add("✅ No file storage risk detected");
//        }
//        report.put("suggestions", suggestions);

        // Write report
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportFile, report);

        System.out.println("🎉 File store analysis complete! Report generated -> " + reportFile.getAbsolutePath());

        // Save analysis results to database
        AnalysisReportDAO analysisResultDTO = new AnalysisReportDAO();
        analysisResultDTO.setUserId(userDTO.getId());
        analysisResultDTO.setSourceFolderId(userDTO.getSourceFolderId());
        analysisResultDTO.setName("file-store-analysis");
        AnalysisReportDAO analysisReportDAO = staticAnalysisMapper.getFileStoreReport(analysisResultDTO);
        
        if (analysisReportDAO == null) {
            analysisResultDTO.setParseOutputId(fileStoreJavaDao.getId());
            analysisResultDTO.setPath(reportFile.getAbsolutePath());
            analysisResultDTO.setCreateTime(LocalDateTime.now());
            analysisResultDTO.setUpdateTime(LocalDateTime.now());
            staticAnalysisMapper.insertResult(analysisResultDTO);
        } else {
            analysisReportDAO.setPath(reportFile.getAbsolutePath());
            analysisReportDAO.setUpdateTime(LocalDateTime.now());
            staticAnalysisMapper.updateResult(analysisReportDAO);
        }

        return report;
    }

    @Override
    public Object persistenceAnalysis(UserDTO userDTO) throws IOException {
        PersistenceXmlParseOutputDAO persistenceDao = parseSourceCodeMapper.getPersistenceXmlParseOutputBySourceFolderId(userDTO.getSourceFolderId());
        if (persistenceDao == null) {
            return noDataReport("persistence.xml");
        }
        String outputPath = persistenceDao.getPath();
        File persistenceJsonFile = new File(outputPath);
        checkJsonFile(persistenceJsonFile);

        ObjectMapper objectMapper = new ObjectMapper();
        List<XmlFileInfo> persistenceDataList = objectMapper.readValue(persistenceJsonFile,
                new TypeReference<List<XmlFileInfo>>() {});

        if (persistenceDataList.isEmpty()) {
            System.err.println("⚠️ persistence.xml JSON is empty: " + outputPath);
            return Map.of();
        }

        Map<String, Object> analysis = new LinkedHashMap<>();
        Set<String> migrationSuggestions = new LinkedHashSet<>();

        int totalUnits = 0;
        int jndiCount = 0;
        int hardcodedDbCount = 0;

        // Basic info
        analysis.put("fileType", persistenceDataList.get(0).fileType);  // Use first as representative
        analysis.put("analyzedAt", LocalDateTime.now().toString());

        for (XmlFileInfo fileInfo : persistenceDataList) {
            Map<String, Object> data = fileInfo.data;
            List<Map<String, Object>> units = getList(data, "persistenceUnits");
            totalUnits += units.size();

            for (Map<String, Object> unit : units) {
                String transactionType = (String) unit.getOrDefault("transactionType", "unknown");
                String jtaDataSource = (String) unit.getOrDefault("jtaDataSource", "");
                @SuppressWarnings("unchecked")
                Map<String, String> properties = (Map<String, String>) unit.getOrDefault("properties", Map.of());

                // 1. JTA transaction type check
                if (transactionType.equals("JTA")) {
                    migrationSuggestions.add("⚠️ transaction-type=JTA. For containerization, recommend using Spring @Transactional or Jakarta Transactions");
                }

                // 2. JNDI data source detection (should be changed to env vars for containers)
                if (!jtaDataSource.isEmpty()) {
                    jndiCount++;
                    migrationSuggestions.add("☁️ Found JNDI data source (" + jtaDataSource + "). Recommend changing to environment variable injection (e.g. SPRING_DATASOURCE_URL)");
                }

                // 3. Hardcoded database info detection
                boolean hasHardcodedDb = properties.keySet().stream()
                        .anyMatch(k -> k.contains("jdbc.url") || k.contains("username") || k.contains("password"));
                if (hasHardcodedDb) {
                    hardcodedDbCount++;
                    migrationSuggestions.add("🚨 Hardcoded database URL / username / password found in properties. Must be fully externalizedto Kubernetes Secret");
                }

                // 4. schema-generation auto table creation check (extremely dangerous in production)
                String schemaAction = (String) properties.getOrDefault("javax.persistence.schema-generation.database.action", "");
                if (schemaAction.contains("drop-and-create")) {
                    migrationSuggestions.add("⚠️ schema-generation.database.action = drop-and-create. Extremely dangerous in production. Recommend changing to none or validate");
                }

                // 5. Provider version check
                String provider = (String) unit.getOrDefault("provider", "unknown");
                if (provider.contains("hibernate") && provider.compareTo("5.0") < 0) {
                    migrationSuggestions.add("⚠️ provider = " + provider + "(legacy Hibernate). Recommend upgrading to 6.x+ for container compatibility");
                }
            }
        }

        // ====================== Final report ======================
        analysis.put("totalPersistenceUnits", totalUnits);
        analysis.put("jndiDataSourceCount", jndiCount);
        analysis.put("hardcodedDbConfigCount", hardcodedDbCount);
        analysis.put("migrationSuggestions", new ArrayList<>(migrationSuggestions));
        analysis.put("totalSuggestions", migrationSuggestions.size());

        Path reportPath = persistenceJsonFile.toPath().getParent();
        File reportFile = reportPath.resolve("persistence-analysis-report.json").toFile();

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportFile, analysis);

        System.out.println("✅ persistence.xml analysis complete! Report generated -> " + reportFile.getAbsolutePath());

        // Save analysis results to database
        AnalysisReportDAO analysisResultDTO = new AnalysisReportDAO();
        analysisResultDTO.setUserId(userDTO.getId());
        analysisResultDTO.setSourceFolderId(userDTO.getSourceFolderId());
        analysisResultDTO.setName("persistence-analysis");
        AnalysisReportDAO analysisReportDAO = staticAnalysisMapper.getPersistenceReport(analysisResultDTO);
        
        if (analysisReportDAO == null) {
            analysisResultDTO.setParseOutputId(persistenceDao.getId());
            analysisResultDTO.setPath(reportFile.getAbsolutePath());
            analysisResultDTO.setCreateTime(LocalDateTime.now());
            analysisResultDTO.setUpdateTime(LocalDateTime.now());
            staticAnalysisMapper.insertResult(analysisResultDTO);
        } else {
            analysisReportDAO.setPath(reportFile.getAbsolutePath());
            analysisReportDAO.setUpdateTime(LocalDateTime.now());
            staticAnalysisMapper.updateResult(analysisReportDAO);
        }
        return analysis;
    }
    @Override
    public Object ejbJarAnalysis(UserDTO userDTO) throws IOException {
        EjbJarXmlParseOutputDAO ejbDao = parseSourceCodeMapper.getEjbJarXmlParseOutputBySourceFolderId(userDTO.getSourceFolderId());
        if (ejbDao == null) {
            return noDataReport("ejb-jar.xml");
        }
        String outputPath = ejbDao.getPath();
        File ejbJarJsonFile = new File(outputPath);
        checkJsonFile(ejbJarJsonFile);

        ObjectMapper objectMapper = new ObjectMapper();
        List<XmlFileInfo> ejbDataList = objectMapper.readValue(ejbJarJsonFile,
                new TypeReference<List<XmlFileInfo>>() {});

        if (ejbDataList.isEmpty()) {
            System.err.println("⚠️ ejb-jar.xml JSON is empty: " + outputPath);
            return Map.of();
        }

        XmlFileInfo fileInfo = ejbDataList.get(0);
        Map<String, Object> data = fileInfo.data;

        Path reportPath = ejbJarJsonFile.toPath().getParent();
        File reportFile = reportPath.resolve("ejb-jar.xml-analysis.json").toFile();
        Map<String, Object> analysis = new LinkedHashMap<>();
        List<String> migrationSuggestions = new ArrayList<>();

        // Basic info
        analysis.put("fileType", fileInfo.fileType);
        analysis.put("filePath", fileInfo.filePath);
        analysis.put("analyzedAt", LocalDateTime.now().toString());

        // Enterprise Beans
        List<Map<String, Object>> beans = getList(data, "sessionBeans");
        analysis.put("beanCount", beans.size());

        // Iterate Session Beans, detect Stateful / JNDI / Bean-managed transactions / security roles
        int statefulCount = 0;
        for (Map<String, Object> bean : beans) {
            String sessionType = (String) bean.getOrDefault("sessionType", "");

            // Detect Stateful Session Bean (high containerization risk)
            if (sessionType.equals("Stateful")) {
                statefulCount++;
                migrationSuggestions.add("⚠️ Found Stateful Session Bean: " + bean.get("ejbName") + ", high containerization risk. Recommend changing to Stateless + Redis state storage");
            }

            // Container compatibility: check JNDI references
            List<Map<String, Object>> refs = getList(bean, "ejbRef");
            if (!refs.isEmpty()) {
                migrationSuggestions.add("☁️ Found ejb-ref (JNDI). Recommend changing to @Inject or CDI");
            }
            // Bean-managed transaction check
            String transactionType = (String) bean.getOrDefault("transactionType", "");
            if (transactionType.equals("Bean")) {
                migrationSuggestions.add("ℹ️ transaction-type=Bean (user-managed transactions). Recommend changing to Container to leverage Jakarta Transactions");
            }
            // Security role check
            List<String> securityRoles = getListString(bean, "securityRoles");
            if (!securityRoles.isEmpty()) {
                migrationSuggestions.add("🔐 Found security-role. Recommend migrating to Keycloak or Spring Security RBAC");
            }
        }

        // MDB (Message Driven Bean) detection
        List<Map<String, Object>> mdbBeans = getList(data, "messageDrivenBeans");
        analysis.put("mdbBeanCount", mdbBeans.size());

        for (Map<String, Object> mdb : mdbBeans) {
            String ejbName = (String) mdb.getOrDefault("ejbName", "unknown");
            migrationSuggestions.add("📨 Found Message Driven Bean: " + ejbName + ". Recommend verifying JMS config migration to Kafka or Spring JMS");
        }

        // Interceptor detection: recommend migrating to CDI Interceptor
        List<Map<String, Object>> interceptors = getList(data, "interceptors");
        List<Map<String, Object>> bindings = getList(data, "interceptorBindings");

        analysis.put("interceptorCount", interceptors.size());
        analysis.put("interceptorBindings", bindings);

        if (interceptors.size() > 0) {
            migrationSuggestions.add("📌 Found " + interceptors.size() + " Interceptor(s). Recommend migrating to CDI Interceptor (better suited for container environments, reduces XML config)");
        }

        // Container transaction attribute check
        List<Map<String, Object>> transactions = getList(data, "containerTransactions");
        analysis.put("transactionCount", transactions.size());

        for (Map<String, Object> tx : transactions) {
            String transAttribute = (String) tx.getOrDefault("transAttribute", "unknown");
            if (transAttribute.equals("NotSupported")) {
                migrationSuggestions.add("⚠️ trans-attribute=NotSupported. May cause transaction inconsistency. Recommend verifying container transaction manager");
            }
        }

        // ejbClientJar (using "ejbClientJar" from your parse result)
        String ejbClientJar = (String) data.getOrDefault("ejbClientJar", "");
        analysis.put("ejbClientJar", ejbClientJar);
        if (!ejbClientJar.isEmpty()) {
            migrationSuggestions.add("ℹ️ Found ejb-client-jar = " + ejbClientJar + ". For containerization, recommend removing or converting to Maven dependency");
        }

        analysis.put("statefulCount", statefulCount);
        analysis.put("migrationSuggestions", migrationSuggestions);
        analysis.put("totalSuggestions", migrationSuggestions.size());

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportFile, analysis);

        System.out.println("✅ ejb-jar.xml analysis complete -> " + reportFile.getAbsolutePath());

        // Save analysis results to database
        AnalysisReportDAO analysisResultDTO = new AnalysisReportDAO();
        analysisResultDTO.setUserId(userDTO.getId());
        analysisResultDTO.setSourceFolderId(userDTO.getSourceFolderId());
        analysisResultDTO.setName("ejb-jar-analysis");
        AnalysisReportDAO analysisReportDAO = staticAnalysisMapper.getEjbJarReport(analysisResultDTO);
        
        if (analysisReportDAO == null) {
            analysisResultDTO.setParseOutputId(ejbDao.getId());
            analysisResultDTO.setPath(reportFile.getAbsolutePath());
            analysisResultDTO.setCreateTime(LocalDateTime.now());
            analysisResultDTO.setUpdateTime(LocalDateTime.now());
            staticAnalysisMapper.insertResult(analysisResultDTO);
        } else {
            analysisReportDAO.setPath(reportFile.getAbsolutePath());
            analysisReportDAO.setUpdateTime(LocalDateTime.now());
            staticAnalysisMapper.updateResult(analysisReportDAO);
        }
        return analysis;
    }

    @Override
    public Object pomXmlAnalysis(UserDTO userDTO) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        PomXmlParseOutputDAO pomDao = parseSourceCodeMapper.getPomXmlParseOutputBySourceFolderId(userDTO.getSourceFolderId());
        if (pomDao == null) {
            return noDataReport("pom.xml");
        }
        String pomParseOutputPath = pomDao.getPath();

        File pomXmlFile = new File(pomParseOutputPath);

        checkJsonFile(pomXmlFile);

        Path reportPath = pomXmlFile.toPath().getParent();
        File analysisReportFile = reportPath.resolve("pom.xml-analysis.json").toFile();

        List<XmlFileInfo> xmlFileInfoList = objectMapper.readValue(pomXmlFile, new TypeReference<List<XmlFileInfo>>() {});

        if (xmlFileInfoList.isEmpty()) {
            throw new RuntimeException("pomXmlAnalysis is empty");
        }

        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("totalPomFiles", xmlFileInfoList.size());
        analysis.put("analyzedAt", LocalDateTime.now().toString());

        List<String> migrationSuggestions = new ArrayList<>();

        // Global aggregation variables
        Set<String> allJavaVersions = new LinkedHashSet<>();
        Set<String> allDependencies = new LinkedHashSet<>();
        Set<String> allPlugins = new LinkedHashSet<>();
        int totalProfileCount = 0;
        boolean hasLegacyServerDep = false;
        boolean hasLowJavaVersion = false;

        // Iterate all pom.xml, aggregate Java version, dependencies, plugins, profiles
        for (XmlFileInfo xmlFileInfo : xmlFileInfoList) {
            Map<String, Object> data = xmlFileInfo.data;

            // Collect javaVersion and check for low version
            String javaVersion = (String) data.getOrDefault("javaVersion", "unknown");
            allJavaVersions.add(javaVersion);
            if (!javaVersion.equals("unknown") && Integer.parseInt(javaVersion) < 11) {
                hasLowJavaVersion = true;
            }

            // Collect dependencies and check for legacy app server dependencies
            @SuppressWarnings("unchecked")
            List<String> deps = (List<String>) data.getOrDefault("dependencies", List.of());
            allDependencies.addAll(deps);
            if (deps.stream().anyMatch(d -> d.contains("weblogic") || d.contains("websphere") || d.contains("glassfish"))) {
                hasLegacyServerDep = true;
            }

            // Collect plugins
            @SuppressWarnings("unchecked")
            List<String> plugins = (List<String>) data.getOrDefault("plugins", List.of());
            allPlugins.addAll(plugins);

            // Collect profileCount
            int profileCount = (int) data.getOrDefault("profileCount", 0);
            totalProfileCount += profileCount;
        }

        // Generate migration suggestions: legacy server deps / profile externalization etc.
//        if (hasLowJavaVersion) {
//            migrationSuggestions.add("⚠️ Java version too low detected (below 11). Recommend upgrading to Java 17+ to support modern containers (e.g. Liberty/WildFly)");
//        }
        if (hasLegacyServerDep) {
            migrationSuggestions.add("⚠️ Legacy application server dependencies detected (e.g. WebLogic/WebSphere). Recommend removing and migrating to lightweight servers");
        }
        if (totalProfileCount > 0) {
            migrationSuggestions.add("ℹ️ detected " + totalProfileCount + " profile(s). Recommend externalizing multi-environment config to Kubernetes ConfigMap");
        }
//        if (allDependencies.stream().anyMatch(d -> d.contains("jakarta.jakartaee-api") && d.contains("1.0"))) {
//            migrationSuggestions.add("⚠️ Legacy Jakarta EE API dependency detected. Recommend upgrading to 10.0+ for container compatibility");
//        }

        // Deduplicate and clean up: replace all-"unknown" with single entry
        allJavaVersions.remove("unknown");
        if (allJavaVersions.isEmpty()) allJavaVersions.add("Not specified");

        // Aggregate into analysis report
        analysis.put("allJavaVersions", new ArrayList<>(allJavaVersions));
        analysis.put("allDependencies", new ArrayList<>(allDependencies));
        analysis.put("allPlugins", new ArrayList<>(allPlugins));
        analysis.put("totalProfileCount", totalProfileCount);
        analysis.put("migrationSuggestions", migrationSuggestions);
        analysis.put("totalSuggestions", migrationSuggestions.size());

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(analysisReportFile, analysis);

        System.out.println("✅ pom.xml integration analysis complete! Report generated -> " + analysisReportFile.getAbsolutePath());

        // Save analysis results to database
        AnalysisReportDAO analysisResultDTO = new AnalysisReportDAO();
        analysisResultDTO.setUserId(userDTO.getId());
        analysisResultDTO.setSourceFolderId(userDTO.getSourceFolderId());
        analysisResultDTO.setName("pom-xml-analysis");
        AnalysisReportDAO analysisReportDAO = staticAnalysisMapper.getPomXmlReport(analysisResultDTO);
        
        if (analysisReportDAO == null) {
            analysisResultDTO.setParseOutputId(pomDao.getId());
            analysisResultDTO.setPath(analysisReportFile.getAbsolutePath());
            analysisResultDTO.setCreateTime(LocalDateTime.now());
            analysisResultDTO.setUpdateTime(LocalDateTime.now());
            staticAnalysisMapper.insertResult(analysisResultDTO);
        } else {
            analysisReportDAO.setPath(analysisReportFile.getAbsolutePath());
            analysisReportDAO.setUpdateTime(LocalDateTime.now());
            staticAnalysisMapper.updateResult(analysisReportDAO);
        }
        return analysis;
    }

    @Override
    public Object facesXmlAnalysis(UserDTO userDTO) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        FacesConfigXmlParseOutputDAO facesDao = parseSourceCodeMapper.getFacesConfigXmlParseOutputBySourceFolderId(userDTO.getSourceFolderId());
        if (facesDao == null) {
            return noDataReport("faces-config.xml");
        }
        String facesXmlParseOutputPath = facesDao.getPath();

        File facesJsonFile = new File(facesXmlParseOutputPath);
        checkJsonFile(facesJsonFile);
        Path reportPath = facesJsonFile.toPath().getParent();
        File analysisReportFile = reportPath.resolve("faces-config.xml-analysis.json").toFile();

        List<XmlFileInfo> dataList = objectMapper.readValue(facesJsonFile,
                new TypeReference<List<XmlFileInfo>>() {});

        if (dataList.isEmpty()) {
            System.err.println("⚠️ faces-config.xml JSON is empty: " + facesXmlParseOutputPath);
            return Map.of();
        }

        Map<String, Object> analysis = new LinkedHashMap<>();
        List<String> suggestions = new ArrayList<>();
        List<IssueInfo> detectedIssues = new ArrayList<>();

        int sessionScopeCount = 0;

        for (XmlFileInfo fileInfo : dataList) {
            Map<String, Object> data = fileInfo.data;
            List<Map<String, Object>> managedBeans = (List<Map<String, Object>>) data.getOrDefault("managedBeans", List.of());

            for (Map<String, Object> bean : managedBeans) {
                String scope = (String) bean.getOrDefault("scope", "unknown");
                // session/application scope detection: high containerization risk
                if (scope.equals("session") || scope.equals("application")) {
                    sessionScopeCount++;
                    suggestions.add("⚠️ Found scope=" + scope + " managed-bean (" + bean.get("name") + "), high containerization risk. Recommend changing to request/view scope or migrating to CDI (@RequestScoped)");
                }
            }

            // Custom PhaseListener detection: tightly coupled to JSF request lifecycle
            List<String> phaseListeners = (List<String>) data.getOrDefault("phaseListeners", List.of());
            for (String pl : phaseListeners) {
                IssueInfo issue = new IssueInfo();
                issue.setSeverity("High");
                issue.setMessage("Custom PhaseListener detected: " + pl + " — tightly coupled to JSF request lifecycle, prevents stateless cloud migration");
                issue.setLocation(fileInfo.getFilePath());
                issue.setClassName(pl);
                issue.setSource("jsf");
                issue.setType("StatefulSession");
                detectedIssues.add(issue);
            }

            // Custom ViewHandler detection: tightly coupled to JSF rendering lifecycle
            List<String> viewHandlers = (List<String>) data.getOrDefault("viewHandlers", List.of());
            for (String vh : viewHandlers) {
                IssueInfo issue = new IssueInfo();
                issue.setSeverity("High");
                issue.setMessage("Custom ViewHandler detected: " + vh + " — tightly coupled to JSF rendering lifecycle, requires refactoring for cloud-native frontend");
                issue.setLocation(fileInfo.getFilePath());
                issue.setClassName(vh);
                issue.setSource("jsf");
                issue.setType("StatefulSession");
                detectedIssues.add(issue);
            }
        }

        analysis.put("sessionScopeCount", sessionScopeCount);
        analysis.put("migrationSuggestions", suggestions);
        analysis.put("totalSuggestions", suggestions.size());
        analysis.put("phaseListenerCount", detectedIssues.stream().filter(i -> i.getMessage().contains("PhaseListener")).count());
        analysis.put("viewHandlerCount", detectedIssues.stream().filter(i -> i.getMessage().contains("ViewHandler")).count());

        File reportFile = reportPath.resolve("faces-config-analysis-report.json").toFile();

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportFile, analysis);

        System.out.println("✅ faces-config.xml analysis complete -> " + reportFile.getAbsolutePath());

        // Save analysis results to database
        AnalysisReportDAO analysisResultDTO = new AnalysisReportDAO();
        analysisResultDTO.setUserId(userDTO.getId());
        analysisResultDTO.setSourceFolderId(userDTO.getSourceFolderId());
        analysisResultDTO.setName("faces-config-analysis");
        AnalysisReportDAO analysisReportDAO = staticAnalysisMapper.getFacesConfigReport(analysisResultDTO);
        
        if (analysisReportDAO == null) {
            analysisResultDTO.setParseOutputId(facesDao.getId());
            analysisResultDTO.setPath(reportFile.getAbsolutePath());
            analysisResultDTO.setCreateTime(LocalDateTime.now());
            analysisResultDTO.setUpdateTime(LocalDateTime.now());
            staticAnalysisMapper.insertResult(analysisResultDTO);
        } else {
            analysisReportDAO.setPath(reportFile.getAbsolutePath());
            analysisReportDAO.setUpdateTime(LocalDateTime.now());
            staticAnalysisMapper.updateResult(analysisReportDAO);
        }
        return analysis;
    }

    @Override
    @Transactional
    public Object JspContentAnalysis(UserDTO userDTO) throws IOException {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new RuntimeException("User not authenticated");
        }
        userDTO.setId(userId);
        System.out.println("📊 Starting JSP content analysis (EL expressions, Java code blocks, dependencies, etc.)");

        JspParseOutPutDAO jspDao = parseSourceCodeMapper.getJspParseOutputBySourceFolderId(userDTO.getSourceFolderId());
        if (jspDao == null) {
            return noDataReport("JSP");
        }
        String jspOutputPath = jspDao.getPath();
        File jspJsonFile = new File(jspOutputPath);
        checkJsonFile(jspJsonFile);

        ObjectMapper objectMapper = new ObjectMapper();
        String content = Files.readString(jspJsonFile.toPath()).trim();
        if (content.isEmpty() || content.equals("[]") || content.equals("{}")) {
            throw new RuntimeException("JSP file content is empty or null structure: " + jspOutputPath);
        }

        List<Map<String, Object>> jspClasses = objectMapper.readValue(jspJsonFile,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        int totalJspFiles = jspClasses.size();
        int totalJavaCodeBlocks = 0;
        int totalElExpressions = 0;
        int totalScriptlets = 0;
        int totalDeclarations = 0;
        int totalDirectives = 0;
        int totalCustomTags = 0;
        List<String> allNamespaces = new ArrayList<>();
        List<String> allCustomTaglibs = new ArrayList<>();
        List<Map<String, Object>> findings = new ArrayList<>();

        for (Map<String, Object> jsp : jspClasses) {
            String filePath = (String) jsp.getOrDefault("filePath", "unknown");
            int codeBlocks = ((Number) jsp.getOrDefault("javaCodeBlockCount", 0)).intValue();
            int elCount = ((List<?>) jsp.getOrDefault("elExpressions", List.of())).size();
            int scriptletCount = ((List<?>) jsp.getOrDefault("scriptlets", List.of())).size();
            int declCount = ((List<?>) jsp.getOrDefault("declarations", List.of())).size();
            int directiveCount = ((List<?>) jsp.getOrDefault("directives", List.of())).size();

            totalJavaCodeBlocks += codeBlocks;
            totalElExpressions += elCount;
            totalScriptlets += scriptletCount;
            totalDeclarations += declCount;
            totalDirectives += directiveCount;
            @SuppressWarnings("unchecked")
            List<String> namespaces = (List<String>) jsp.getOrDefault("namespaces", List.of());
            @SuppressWarnings("unchecked")
            List<String> customTaglibs = (List<String>) jsp.getOrDefault("customTaglibs", List.of());
            allNamespaces.addAll(namespaces);
            allCustomTaglibs.addAll(customTaglibs);
            totalCustomTags += customTaglibs.size();

            Map<String, Object> f = new LinkedHashMap<>();
            f.put("filePath", filePath);
            f.put("scriptletCount", scriptletCount);
            f.put("elExpressionCount", elCount);
            f.put("declarationCount", declCount);
            f.put("directiveCount", directiveCount);
            f.put("javaCodeBlockCount", codeBlocks);
            f.put("customTaglibs", customTaglibs);
            findings.add(f);
        }

        // Deduplicate namespaces and taglibs
        Set<String> uniqueNamespaces = new LinkedHashSet<>(allNamespaces);
        Set<String> uniqueCustomTaglibs = new LinkedHashSet<>(allCustomTaglibs);

        // Generate migration suggestions: scriptlet / EL / custom tags / declarations
        List<String> suggestions = new ArrayList<>();
        if (totalScriptlets > 0) {
            suggestions.add("⚠️ Found " + totalScriptlets + " Scriptlet(s) (Java code embedded in JSP). Recommend migrating to Servlet or MVC Controller to avoid business logic in JSP");
        }
        if (totalElExpressions > 0) {
            suggestions.add("ℹ️ Found " + totalElExpressions + " EL expression(s). In cloud environments, ensure Expression Language engine is compatible with Jakarta EL");
        }
        if (totalCustomTags > 0) {
            suggestions.add("📚 Found " + totalCustomTags + " custom taglib reference(s). Recommend evaluating replacement with standard JSTL or migrating to Facelets/Thymeleaf");
        }
        if (totalDeclarations > 0) {
            suggestions.add("⚠️ Found " + totalDeclarations + " JSP Declaration(s) (<%! %>). This is unsafe in multi-threaded environments. Strongly recommend removal");
        }
        if (totalJavaCodeBlocks > 10) {
            suggestions.add("🚨 Too many Java code blocks in JSP (" + totalJavaCodeBlocks + "). Severely violates MVC layering principles. Must be refactored");
        }

        // Generate report
        Path outputRoot = jspJsonFile.toPath().getParent();
        File reportFile = outputRoot.resolve("jsp-content-analysis.json").toFile();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalJspFiles", totalJspFiles);
        report.put("totalJavaCodeBlocks", totalJavaCodeBlocks);
        report.put("totalElExpressions", totalElExpressions);
        report.put("totalScriptlets", totalScriptlets);
        report.put("totalDeclarations", totalDeclarations);
        report.put("totalDirectives", totalDirectives);
        report.put("totalCustomTaglibs", totalCustomTags);
        report.put("uniqueNamespaces", uniqueNamespaces);
        report.put("uniqueCustomTaglibs", uniqueCustomTaglibs);
        report.put("findings", findings);
        report.put("suggestions", suggestions);
        report.put("totalSuggestions", suggestions.size());
        report.put("analysisTime", LocalDateTime.now().toString());

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportFile, report);

        // Save to database
        AnalysisReportDAO analysisResultDTO = new AnalysisReportDAO();
        analysisResultDTO.setUserId(userDTO.getId());
        analysisResultDTO.setSourceFolderId(userDTO.getSourceFolderId());
        analysisResultDTO.setName("jsp-content-analysis");
        AnalysisReportDAO existingResult = staticAnalysisMapper.getJspContentReport(analysisResultDTO);
        if (existingResult == null) {
            analysisResultDTO.setParseOutputId(jspDao.getId());
            analysisResultDTO.setPath(reportFile.getAbsolutePath());
            analysisResultDTO.setCreateTime(LocalDateTime.now());
            analysisResultDTO.setUpdateTime(LocalDateTime.now());
            staticAnalysisMapper.insertResult(analysisResultDTO);
        } else {
            existingResult.setPath(reportFile.getAbsolutePath());
            existingResult.setUpdateTime(LocalDateTime.now());
            staticAnalysisMapper.updateResult(existingResult);
        }

        System.out.println("✅ JSP content analysis complete -> " + reportFile.getAbsolutePath());
        System.out.println("   Total analyzed: " + totalJspFiles + " JSP files");
        return report;
    }

    @Override
    @Transactional
    public Object JsfContentAnalysis(UserDTO userDTO) throws IOException {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new RuntimeException("User not authenticated");
        }
        userDTO.setId(userId);
        System.out.println("📊 Starting JSF content analysis (components, EL expressions, bindings, etc.)");

        JsfParseOutPutDAO jsfDao = parseSourceCodeMapper.getJsfParseOutputBySourceFolderId(userDTO.getSourceFolderId());
        if (jsfDao == null) {
            return noDataReport("JSF");
        }
        String jsfOutputPath = jsfDao.getPath();
        File jsfJsonFile = new File(jsfOutputPath);
        checkJsonFile(jsfJsonFile);

        ObjectMapper objectMapper = new ObjectMapper();
        String content = Files.readString(jsfJsonFile.toPath()).trim();
        if (content.isEmpty() || content.equals("[]") || content.equals("{}")) {
            throw new RuntimeException("JSF file content is empty or null structure: " + jsfOutputPath);
        }

        List<Map<String, Object>> jsfFiles = objectMapper.readValue(jsfJsonFile,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        int totalJsfFiles = jsfFiles.size();
        int totalComponents = 0;
        int totalElExpressions = 0;
        int totalHardcodedPaths = 0;
        int totalBeans = 0;
        int transientsViews = 0;
        int sessionAccessCount = 0;
        int facesContextAccessCount = 0;
        int applicationAccessCount = 0;
        int maxComponentDepth = 0;
        List<String> allNamespaces = new ArrayList<>();
        List<String> allBeans = new ArrayList<>();
        List<String> allHardcodedPaths = new ArrayList<>();
        List<Map<String, Object>> findings = new ArrayList<>();

        // Compute project root prefix for relative paths
        File outDir = jsfJsonFile.getParentFile();
        String rootPrefix = outDir != null ? outDir.getParentFile().getAbsolutePath().replace("\\", "/") + "/" : null;

        for (Map<String, Object> jsf : jsfFiles) {
            String filePath = (String) jsf.getOrDefault("filePath", "unknown");
            filePath = filePath.replace("\\", "/");
            if (rootPrefix != null && filePath.startsWith(rootPrefix)) {
                filePath = filePath.substring(rootPrefix.length());
            }
            int compCount = ((Number) jsf.getOrDefault("componentCount", 0)).intValue();
            int elCount = ((List<?>) jsf.getOrDefault("elExpressions", List.of())).size();
            int hcCount = ((List<?>) jsf.getOrDefault("hardcodedPaths", List.of())).size();
            int beanCount = ((List<?>) jsf.getOrDefault("beans", List.of())).size();
            int depth = ((Number) jsf.getOrDefault("maxComponentDepth", 0)).intValue();
            boolean isTransient = Boolean.TRUE.equals(jsf.getOrDefault("isTransientView", false));
            boolean hasSession = Boolean.TRUE.equals(jsf.getOrDefault("hasSessionAccess", false));
            boolean hasFacesCtx = Boolean.TRUE.equals(jsf.getOrDefault("hasFacesContextAccess", false));
            boolean hasAppCtx = Boolean.TRUE.equals(jsf.getOrDefault("hasApplicationAccess", false));

            totalComponents += compCount;
            totalElExpressions += elCount;
            totalHardcodedPaths += hcCount;
            totalBeans += beanCount;
            if (depth > maxComponentDepth) maxComponentDepth = depth;
            if (isTransient) transientsViews++;
            if (hasSession) sessionAccessCount++;
            if (hasFacesCtx) facesContextAccessCount++;
            if (hasAppCtx) applicationAccessCount++;

            Map<String, Object> f = new LinkedHashMap<>();
            f.put("filePath", filePath);
            f.put("componentCount", compCount);
            f.put("elExpressionCount", elCount);
            f.put("hardcodedPathCount", hcCount);
            f.put("beanCount", beanCount);
            f.put("maxDepth", depth);
            f.put("isTransientView", isTransient);
            f.put("hasSessionAccess", hasSession);
            f.put("hasFacesContextAccess", hasFacesCtx);
            f.put("hasApplicationAccess", hasAppCtx);
            findings.add(f);

            @SuppressWarnings("unchecked")
            List<String> namespaces = (List<String>) jsf.getOrDefault("namespaces", List.of());
            @SuppressWarnings("unchecked")
            List<String> beans = (List<String>) jsf.getOrDefault("beans", List.of());
            @SuppressWarnings("unchecked")
            List<String> hardcodedPaths = (List<String>) jsf.getOrDefault("hardcodedPaths", List.of());
            allNamespaces.addAll(namespaces);
            allBeans.addAll(beans);
            allHardcodedPaths.addAll(hardcodedPaths);
        }

        // Deduplicate namespaces and Beans
        Set<String> uniqueNamespaces = new LinkedHashSet<>(allNamespaces);
        Set<String> uniqueBeans = new LinkedHashSet<>(allBeans);

        // Generate migration suggestions: components / EL / hardcoded paths / Session / FacesContext
        List<String> suggestions = new ArrayList<>();
        if (totalComponents > 0) {
            suggestions.add("ℹ️ Found " + totalComponents + " JSF component(s). Ensure Jakarta Faces compatibility during migration");
        }
        if (totalElExpressions > 0) {
            suggestions.add("ℹ️ Found " + totalElExpressions + " EL expression(s). Verify EL parser compatibility in cloud environments");
        }
        if (totalHardcodedPaths > 0) {
            suggestions.add("🚨 Found " + totalHardcodedPaths + " hardcoded path(s). Must be externalized to config files or environment variables");
        }
        if (sessionAccessCount > 0) {
            suggestions.add("⚠️ " + sessionAccessCount + " page(s) directly accessing Session. Containerization requires migration to Redis/Hazelcast distributed Session");
        }
        if (facesContextAccessCount > 0) {
            suggestions.add("⚠️ " + facesContextAccessCount + " page(s) directly accessing FacesContext. Thread safety issue. Recommend accessing via CDI Bean");
        }
        if (maxComponentDepth > 5) {
            suggestions.add("📌 Maximum component nesting depth is " + maxComponentDepth + ". May impact rendering performance");
        }

        // Generate report
        Path outputRoot = jsfJsonFile.toPath().getParent();
        File reportFile = outputRoot.resolve("jsf-content-analysis.json").toFile();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalJsfFiles", totalJsfFiles);
        report.put("totalComponents", totalComponents);
        report.put("totalElExpressions", totalElExpressions);
        report.put("totalHardcodedPaths", totalHardcodedPaths);
        report.put("totalBeans", totalBeans);
        report.put("maxComponentDepth", maxComponentDepth);
        report.put("transientViewCount", transientsViews);
        report.put("sessionAccessCount", sessionAccessCount);
        report.put("facesContextAccessCount", facesContextAccessCount);
        report.put("applicationAccessCount", applicationAccessCount);
        report.put("uniqueNamespaces", uniqueNamespaces);
        report.put("uniqueBeans", uniqueBeans);
        report.put("hardcodedPaths", allHardcodedPaths);
        report.put("findings", findings);
        report.put("suggestions", suggestions);
        report.put("totalSuggestions", suggestions.size());
        report.put("analysisTime", LocalDateTime.now().toString());

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportFile, report);

        // Save to database
        AnalysisReportDAO analysisResultDTO = new AnalysisReportDAO();
        analysisResultDTO.setUserId(userDTO.getId());
        analysisResultDTO.setSourceFolderId(userDTO.getSourceFolderId());
        analysisResultDTO.setName("jsf-content-analysis");
        AnalysisReportDAO existingResult = staticAnalysisMapper.getJsfContentReport(analysisResultDTO);
        if (existingResult == null) {
            analysisResultDTO.setParseOutputId(jsfDao.getId());
            analysisResultDTO.setPath(reportFile.getAbsolutePath());
            analysisResultDTO.setCreateTime(LocalDateTime.now());
            analysisResultDTO.setUpdateTime(LocalDateTime.now());
            staticAnalysisMapper.insertResult(analysisResultDTO);
        } else {
            existingResult.setPath(reportFile.getAbsolutePath());
            existingResult.setUpdateTime(LocalDateTime.now());
            staticAnalysisMapper.updateResult(existingResult);
        }

        System.out.println("✅ JSF content analysis complete -> " + reportFile.getAbsolutePath());
        System.out.println("   Total analyzed: " + totalJsfFiles + " JSF files");
        return report;
    }

    private void checkJsonFile(File file) {
        if (!file.exists()) {
            throw new RuntimeException("Parse output not found. Please run Analysis on Source Folders page first.");
        }
        if (file.length() == 0) {
            throw new RuntimeException("File is empty (0 bytes): " + file);
        }
    }


    // Generic helper to get List<Map>
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return (val instanceof List) ? (List<Map<String, Object>>) val : List.of();
    }

    // Specific helper to get List<String> (securityRoles)
    @SuppressWarnings("unchecked")
    private List<String> getListString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return (val instanceof List) ? (List<String>) val : List.of();
    }

    private Map<String, Object> noDataReport(String fileType) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("status", "no_data");
        report.put("message", "No " + fileType + " files in project, skipping analysis");
        report.put("suggestions", List.of("No " + fileType + " files detected, no migration suggestions needed"));
        report.put("totalSuggestions", 0);
        return report;
    }

    // Simple scoring example (can be extended with more complex rules)
    private int calculateScore(int suggestionCount, String version) {
        return Math.max(100 - suggestionCount * 8, 30); // More suggestions = lower score = more modernization needed
    }

    //TODO Consolidate into a single Common Class as static methods
    private Path checkOutputFile(String path) throws IOException {
        Path projectRoot = Paths.get(path).normalize();   // need import java.nio.file.Paths;
        Path outputRoot = projectRoot.resolve(OutputPath.OUTPUT_BASE_DIR);

        if (!Files.exists(outputRoot)) {
            try {
                Files.createDirectories(outputRoot);
                System.out.println("Created output directory: " + outputRoot);
            } catch (IOException e) {
                throw new IOException("Failed to create output directory: " + outputRoot, e);
            }
        }
        return outputRoot;
    }

    @Override
    public void deleteAnnotationAnalysis(Integer userId, Integer sourceFolderId) {
        staticAnalysisMapper.deleteAnnotationAnalysisResult(userId, sourceFolderId);
    }

    @Override
    public void deleteWebXmlAnalysis(Integer userId, Integer sourceFolderId) {
        staticAnalysisMapper.deleteWebXmlAnalysisResult(userId, sourceFolderId);
    }

    @Override
    public void deleteFileStoreAnalysis(Integer userId, Integer sourceFolderId) {
        staticAnalysisMapper.deleteFileStoreAnalysisResult(userId, sourceFolderId);
    }

    @Override
    public void deletePersistenceAnalysis(Integer userId, Integer sourceFolderId) {
        staticAnalysisMapper.deletePersistenceAnalysisResult(userId, sourceFolderId);
    }

    @Override
    public void deleteEjbJarAnalysis(Integer userId, Integer sourceFolderId) {
        staticAnalysisMapper.deleteEjbJarAnalysisResult(userId, sourceFolderId);
    }

    @Override
    public void deletePomXmlAnalysis(Integer userId, Integer sourceFolderId) {
        staticAnalysisMapper.deletePomXmlAnalysisResult(userId, sourceFolderId);
    }

    @Override
    public void deleteFacesConfigAnalysis(Integer userId, Integer sourceFolderId) {
        staticAnalysisMapper.deleteFacesConfigAnalysisResult(userId, sourceFolderId);
    }

    @Override
    public void deleteJspContentAnalysis(Integer userId, Integer sourceFolderId) {
        staticAnalysisMapper.deleteJspContentAnalysisResult(userId, sourceFolderId);
    }

    @Override
    public void deleteJsfContentAnalysis(Integer userId, Integer sourceFolderId) {
        staticAnalysisMapper.deleteJsfContentAnalysisResult(userId, sourceFolderId);
    }

}


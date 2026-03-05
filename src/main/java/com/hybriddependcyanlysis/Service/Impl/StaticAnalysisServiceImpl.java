package com.hybriddependcyanlysis.Service.Impl;

import Common.OutputPath;
import Common.XmlFileInfo.XmlFileInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hybriddependcyanlysis.Mapper.AstMapper;
import com.hybriddependcyanlysis.Mapper.StaticAnalysisMapper;
import com.hybriddependcyanlysis.POJO.DAO.JavaFilesParseDAO;
import com.hybriddependcyanlysis.POJO.DAO.JspParseOutPutDAO;
import com.hybriddependcyanlysis.POJO.DTO.UserDTO;
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
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class StaticAnalysisServiceImpl implements StaticAnalysisService {


    @Autowired
    private StaticAnalysisMapper staticAnalysisMapper;

    @Autowired
    private AstMapper astMapper;

    private static final Set<String> TARGET_WEB_XML_SECTIONS = Set.of(
            "contextParams", "filters", "servlets", "listeners",
            "sessionConfig", "welcomeFiles", "taglibs",
            "resourceRefs", "securityConstraints", "securityRoles",
            "loginConfig", "errorPages", "mimeMappings", "filterMapping",
            "servletMapping"
    );

    @Override
    @Transactional
    public HashMap<String, Integer> dependencyCounting(Integer userId, Integer sourceFolderId) {

        UserDTO userDTO = new UserDTO();
        userDTO.setId(userId);
        userDTO.setSourceFolderId(sourceFolderId);

        JavaFilesParseDAO javaFilesParseDAO = astMapper.getOutPut(userDTO);

        List<HashMap<String, Object>> resultList = staticAnalysisMapper.getDependencyCountGroupByType(javaFilesParseDAO);

        HashMap<String, Integer> resultMap = new HashMap<>();
        for (Map<String, Object> row : resultList) {
            String type = (String) row.get("dependency_type");
            Integer count = ((Number) row.get("calledNums")).intValue(); // 确保 SQL 中别名是 calledNums
            resultMap.put(type, count);
        }

        return resultMap;
    }

    @Override
    @Transactional
    public HashMap<String, String> ELAnalysis(UserDTO userDTO) {
        JspParseOutPutDAO jspParseOutPutDAO = astMapper.getJspParseOutput(userDTO);

        List<HashMap<String, Object>> temp = staticAnalysisMapper.getELexpression(jspParseOutPutDAO);

        log.info("EL Analysis");

        HashMap<String, String> resultMap = new HashMap<>();
        for (Map<String, Object> row : temp) {
            String pageName = (String) row.get("pageName");
            String expression = (String) row.get("expression"); // 确保 SQL 中别名是 calledNums
            resultMap.put(pageName, expression);
        }

        return resultMap;

    }

    @Override
    public void AnnotationCount(UserDTO userDTO) throws IOException {
        // get java parse output path
        ObjectMapper objectMapper = new ObjectMapper();   // 需要 import com.fasterxml.jackson.databind.ObjectMapper;
        String outputPath = astMapper.getJavaFilesParseOutput(userDTO);

        File jsonFile = new File(outputPath);

        checkJsonFile(jsonFile);

// 读取前几行或整个内容（小文件可以直接读）
        String content = Files.readString(jsonFile.toPath()).trim();

        if (content.isEmpty() || content.equals("[]") || content.equals("{}")) {
            throw new RuntimeException("文件内容为空或空结构: " + outputPath);
        }

        // 直接读取为 List<Map<String, Object>>
        List<Map<String, Object>> classes = objectMapper.readValue(jsonFile,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        if(classes.isEmpty())
        {
            throw new RuntimeException("解析结果文件 is null: " + outputPath);
        }

//        System.out.println("✅ 成功读取 Java 解析结果！");
//        System.out.println("总类数量: " + classes.size());
//        System.out.println("第一个类示例: " + classes.get(0));

        Set<String> targetAnnotations = Set.of(
                // Session State 重點（容器化痛點）
                "jakarta.ejb.Stateful", "javax.ejb.Stateful",
                "jakarta.ejb.Stateless", "javax.ejb.Stateless",
                "jakarta.ejb.Singleton", "javax.ejb.Singleton",

                // 其他 EJB 相關
                "jakarta.ejb.MessageDriven", "javax.ejb.MessageDriven",
                "jakarta.ejb.ApplicationException", "javax.ejb.ApplicationException",
                "jakarta.ejb.EJB", "javax.ejb.EJB",
                "jakarta.ejb.Remote", "javax.ejb.Remote",
                "jakarta.ejb.Local", "javax.ejb.Local",

                // JPA / 持久化（數據源配置影響容器化）
                "jakarta.persistence.Entity", "javax.persistence.Entity",
                "jakarta.persistence.Table", "javax.persistence.Table",
                "jakarta.persistence.PersistenceContext", "javax.persistence.PersistenceContext",

                // 注入相關（常見遷移問題）
                "jakarta.inject.Inject", "javax.inject.Inject",
                "jakarta.annotation.Resource", "javax.annotation.Resource"
        );

        Map<String, Integer> countMap = new LinkedHashMap<>();
        Map<String, List<String>> detailMap = new LinkedHashMap<>();

        for (Map<String, Object> cls : classes) {
            String fullName = (String) cls.get("fullName");
            if (fullName == null) continue;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> annos = (List<Map<String, Object>>) cls.getOrDefault("annotations", List.of());

            for (Map<String, Object> ann : annos) {
                String annoName = (String) ann.get("name");
                if (annoName == null) continue;

                if (targetAnnotations.contains(annoName)) {
                    countMap.merge(annoName, 1, Integer::sum);
                    detailMap.computeIfAbsent(annoName, k -> new ArrayList<>()).add(fullName);
                }
            }
        }

        // ====================== 输出结果 ======================
//        System.out.println("\n🎉 Annotation 统计完成！共扫描 " + classes.size() + " 个类");
//        countMap.forEach((anno, cnt) -> {
//            System.out.printf("   %-45s : %d 个%n", anno, cnt);
//        });

        // ====================== 生成报告 JSON ======================
        Path outputRoot = jsonFile.toPath().getParent();
        File reportFile = outputRoot.resolve("annotation-statistics.json").toFile();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalClasses", classes.size());
        report.put("annotationCount", countMap);
        report.put("annotationDetails", detailMap);
        report.put("analysisTime", LocalDateTime.now().toString());

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportFile, report);

//        System.out.println("\n📁 详细统计报告已保存 → " + reportFile.getAbsolutePath());

    }

    @Override
    public void analyzeWebXml(UserDTO userDTO) throws IOException {
        String outputPath = astMapper.getWebXmlParseOutput(userDTO);
        File webXmlJsonFile = new File(outputPath);
        checkJsonFile(webXmlJsonFile);

        //read web.xml json file
        ObjectMapper objectMapper = new ObjectMapper();
        List<XmlFileInfo> webDataList = objectMapper.readValue(webXmlJsonFile,
                new TypeReference<List<XmlFileInfo>>() {});

        if (webDataList.isEmpty()) {
            System.err.println("⚠️ web.xml JSON 为空: " + outputPath);
            return;
        }

        XmlFileInfo fileInfo = webDataList.get(0);
        Map<String, Object> data = fileInfo.data;

        Path reportPath = webXmlJsonFile.toPath().getParent();
        File reportFile = reportPath.resolve("web.xml-analysis.json").toFile();
        Map<String, Object> analysis = new LinkedHashMap<>();
        List<String> migrationSuggestions = new ArrayList<>();

        // ====================== 1. 基础信息（新增） ======================
        analysis.put("fileType", fileInfo.fileType);
        analysis.put("filePath", fileInfo.filePath);
        analysis.put("analyzedAt", LocalDateTime.now().toString());

        // 版本 & metadata-complete（最重要！）
        String version = (String) data.getOrDefault("version", "unknown");
        boolean metadataComplete = Boolean.parseBoolean(String.valueOf(data.get("metadataComplete")));
        analysis.put("webXmlVersion", version);
        analysis.put("metadataComplete", data.get("metadataComplete"));

        if (version.compareTo("3.0") < 0 && !version.equals("unknown")) {
            migrationSuggestions.add("⚠️ web.xml 版本 " + version + "（Servlet 2.x/3.0 前），强烈建议升级到 Jakarta EE 9+（web.xml version=6.0 或使用注解完全取代）");
        }
        if (metadataComplete) {
            migrationSuggestions.add("ℹ️ metadata-complete=true，注解扫描被禁用，建议改为 false 并使用 @WebServlet/@WebFilter/@WebListener");
        }

        // ====================== 2. TARGET_WEB_XML_SECTIONS 自动统计 ======================
        for (String section : TARGET_WEB_XML_SECTIONS) {
            Object val = data.get(section);
            int count = 0;
            if (val instanceof List) count = ((List<?>) val).size();
            else if (val instanceof Map) count = ((Map<?, ?>) val).size();

            analysis.put(section + "Count", count);
        }

        // ====================== 3. 核心配置深度分析 ======================

        // Context Params（硬编码配置）
        List<Map<String, Object>> contextParams = getList(data, "contextParams");
        analysis.put("contextParams", contextParams);
        if (!contextParams.isEmpty()) {
            Set<String> sensitiveKeys = Set.of("jdbc.", "db.", "mail.", "redis.", "spring.", "contextConfigLocation");
            boolean hasSensitive = contextParams.stream()
                    .anyMatch(p -> sensitiveKeys.stream().anyMatch(s ->
                            ((String) p.getOrDefault("name", "")).toLowerCase().contains(s)));
            if (hasSensitive) {
                migrationSuggestions.add("🚨 发现硬编码 context-param（含数据库/邮件等敏感配置），必须全部外置到 application.yml + Kubernetes ConfigMap/Secret");
            }
        }

        // Servlets（检测框架）
        List<Map<String, Object>> servlets = getList(data, "servlets");
        boolean hasFacesServlet = servlets.stream()
                .anyMatch(s -> ((String) s.getOrDefault("servletClass", "")).contains("FacesServlet"));
        boolean hasSpringDispatcher = servlets.stream()
                .anyMatch(s -> ((String) s.getOrDefault("servletClass", "")).contains("DispatcherServlet"));
        if (hasFacesServlet) {
            migrationSuggestions.add("🔄 检测到 FacesServlet（JSF），建议迁移到 Jakarta Faces 或转向 Spring Boot + Thymeleaf/React");
        }
        if (hasSpringDispatcher) {
            migrationSuggestions.add("🌱 检测到 Spring MVC DispatcherServlet，推荐直接升级到 Spring Boot 3.x（内嵌 Tomcat + 零 XML）");
        }

        // Filters / Listeners
        List<Map<String, Object>> filters = getList(data, "filters");
        List<String> listeners = getListString(data, "listeners");
        analysis.put("filters", filters);
        analysis.put("listeners", listeners);

        // Session Config（已解析为 Map）
        @SuppressWarnings("unchecked")
        Map<String, Object> sessionConfig = (Map<String, Object>) data.getOrDefault("sessionConfig", Map.of());
        analysis.put("sessionConfig", sessionConfig);
        String timeout = (String) sessionConfig.getOrDefault("sessionTimeoutMinutes", "30");
        if (Integer.parseInt(timeout) > 30) {
            migrationSuggestions.add("♻️ session-timeout = " + timeout + " 分钟 → 云上必须使用 Redis / Hazelcast / Spring Session 分布式 Session");
        }

        // Login Config（认证方式）
        @SuppressWarnings("unchecked")
        Map<String, Object> loginConfig = (Map<String, Object>) data.getOrDefault("loginConfig", Map.of());
        String authMethod = (String) loginConfig.getOrDefault("authMethod", "");
        if (!authMethod.isEmpty()) {
            migrationSuggestions.add("🔐 检测到 " + authMethod + " 认证（FORM/BASIC），建议迁移到 OAuth2 / OpenID Connect + Keycloak / Auth0");
        }

        // Taglib / Welcome Files（老技术）
        List<Map<String, Object>> taglibs = getList(data, "taglibs");
        List<String> welcomeFiles = getListString(data, "welcomeFiles");
        if (!taglibs.isEmpty()) {
            migrationSuggestions.add("📚 发现 " + taglibs.size() + " 个 <taglib>，老式 JSP 标签库，建议替换为 JSTL 或完全转向 Facelets/Thymeleaf");
        }

        // Resource Refs（JNDI）
        List<Map<String, Object>> resourceRefs = getList(data, "resourceRefs");
        if (!resourceRefs.isEmpty()) {
            migrationSuggestions.add("☁️ 发现 " + resourceRefs.size() + " 个 <resource-ref>（JNDI），全部改为 @Value + Kubernetes Secret / AWS Secrets Manager");
            analysis.put("resourceRefDetails", resourceRefs);
        }

        // Security & Error Pages（保留你原来的优秀逻辑）
        List<Map<String, Object>> securityConstraints = getList(data, "securityConstraints");
        List<String> securityRoles = getListString(data, "securityRoles");
        List<Map<String, Object>> errorPages = getList(data, "errorPages");

        if (!securityConstraints.isEmpty()) {
            migrationSuggestions.add("🔐 发现 " + securityConstraints.size() + " 个 security-constraint，建议迁移到 Spring Security 6 / Keycloak + Istio");
        }
        boolean hasExceptionPages = errorPages.stream().anyMatch(ep -> "exception".equals(ep.get("type")));
        if (hasExceptionPages) {
            migrationSuggestions.add("📄 自定义 exception-page 建议统一用 @ControllerAdvice + GlobalExceptionHandler");
        }

        // ====================== 4. 最终汇总 ======================
        analysis.put("migrationSuggestions", migrationSuggestions);
        analysis.put("totalSuggestions", migrationSuggestions.size());
        analysis.put("modernizationScore", calculateScore(migrationSuggestions.size(), version)); // 升级版评分

        // 保存到数据库（建议）
        // astMapper.saveWebXmlAnalysis(userDTO.getId(), analysis);

        // ====================== 输出 ======================
        System.out.println("✅ web.xml 分析完成！文件: " + fileInfo.filePath);
        System.out.println("   版本: " + version + " | Servlets: " + servlets.size() + " | Filters: " + filters.size());
        System.out.println("   迁移建议: " + migrationSuggestions.size() + " 条 | 现代化评分: " + analysis.get("modernizationScore") + "/100");

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportFile, analysis);

    }

    @Override
    public void FileStoreAnalysis(UserDTO userDTO) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();

        String javaFilesParseOutputPath = astMapper.getJavaFilesParseOutput(userDTO);

        File javaParseOutputFile = new File(javaFilesParseOutputPath);

        checkJsonFile(javaParseOutputFile);

        String content = Files.readString(javaParseOutputFile.toPath()).trim();

        if (content.isEmpty() || content.equals("[]") || content.equals("{}")) {
            throw new RuntimeException("文件内容为空或空结构: " + javaFilesParseOutputPath);
        }

        // 直接读取为 List<Map<String, Object>>
        List<Map<String, Object>> classes = objectMapper.readValue(javaParseOutputFile,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        if(classes.isEmpty())
        {
            throw new RuntimeException("解析结果文件 is null: " + javaFilesParseOutputPath);
        }

        Set<String> fileRelatedTypes = Set.of(
                "java.io.File", "java.io.FileWriter", "java.io.FileReader",
                "java.nio.file.Path", "java.nio.file.Files",
                "java.io.BufferedWriter", "java.io.OutputStream"  // 示例，添加常见文件操作类
        );

//        Set<String> fileRelatedMethods = Set.of(
//                "write", "createNewFile", "delete", "mkdir", "exists",
//                "getAbsolutePath", "listFiles"  // 示例，常见文件方法
//        );

        List<Map<String, Object>> fileRiskClasses = new ArrayList<>();  // 存储有风险的类详情
        int totalRiskCount = 0;  // 总风险点计数

        for (Map<String, Object> cls : classes) {
            String fullName = (String) cls.get("fullName");
            if (fullName == null) continue;

            boolean hasFileRisk = false;
            List<String> riskDetails = new ArrayList<>();  // 这个类的风险点列表

            // 检查字段（fields 数组）
            List<String> imports = (List<String>) cls.getOrDefault("imports", List.of());

//            System.out.println("類別 " + fullName + " 的 imports 有 " + imports.size() + " 個");

            for(String importClss : imports)
            {
                if(fileRelatedTypes.stream().anyMatch(importClss::contains))
                {
                    hasFileRisk = true;
                    riskDetails.add("import风险: " + importClss);
                    totalRiskCount++;
                }
            }

            // 如果有风险，记录这个类详情
            if (hasFileRisk) {
                Map<String, Object> riskCls = new HashMap<>();
                riskCls.put("className", fullName);  // 风险类名
                riskCls.put("classType", cls.get("kind"));  // 类类型 (e.g., "Class" or "Interface")
                riskCls.put("riskDetails", riskDetails);  // 风险详情列表

                // 记录 "什么类引用了这个类" (假设你有 dependencyGraph 或 reverseDependencies 地图)
                // 如果没有 dependencyGraph，你可以先注释这一行，或后续构建一个 reverseGraph
                // List<String> referencedBy = getReferencedBy(fullName);  // 从 dependencyGraph 反查
                // riskCls.put("referencedBy", referencedBy);

                fileRiskClasses.add(riskCls);
            }


        }
        Path outputRoot = javaParseOutputFile.toPath().getParent();
        File reportFile = outputRoot.resolve("file-store-analysis-report.json").toFile();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalRiskCount", totalRiskCount);
        report.put("riskClasses", fileRiskClasses);

        // 生成建议（可选，根据你的需求添加）
//        List<String> suggestions = new ArrayList<>();
//        if (totalRiskCount > 0) {
//            suggestions.add("⚠️ 检测到 " + totalRiskCount + " 个文件存储相关风险点");
//            suggestions.add("建议: 迁移到云存储服务（如 AWS S3），避免本地文件操作");
//        } else {
//            suggestions.add("✅ 未检测到文件存储风险");
//        }
//        report.put("suggestions", suggestions);

        // 写报告
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportFile, report);

        System.out.println("🎉 文件存储分析完成！报告生成 → " + reportFile.getAbsolutePath());

    }

    @Override
    public void persistenceAnalysis(UserDTO userDTO) throws IOException {
        String outputPath = astMapper.getPersistenceXmlParseOutput(userDTO);
        File persistenceJsonFile = new File(outputPath);
        checkJsonFile(persistenceJsonFile);

        ObjectMapper objectMapper = new ObjectMapper();
        List<XmlFileInfo> persistenceDataList = objectMapper.readValue(persistenceJsonFile,
                new TypeReference<List<XmlFileInfo>>() {});

        if (persistenceDataList.isEmpty()) {
            System.err.println("⚠️ persistence.xml JSON 为空: " + outputPath);
            return;
        }

        Map<String, Object> analysis = new LinkedHashMap<>();
        List<String> migrationSuggestions = new ArrayList<>();

        int totalUnits = 0;
        int jndiCount = 0;
        int hardcodedDbCount = 0;

        // 基础信息
        analysis.put("fileType", persistenceDataList.get(0).fileType);  // 取第一個作為代表
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

                // 1. JTA 事務
                if (transactionType.equals("JTA")) {
                    migrationSuggestions.add("⚠️ transaction-type=JTA，容器化建議用 Spring @Transactional 或 Jakarta Transactions");
                }

                // 2. JNDI 資料源（最重要！）
                if (!jtaDataSource.isEmpty()) {
                    jndiCount++;
                    migrationSuggestions.add("☁️ 發現 JNDI 資料源 (" + jtaDataSource + ")，建議改為環境變數注入 (e.g. SPRING_DATASOURCE_URL)");
                }

                // 3. 硬編碼資料庫資訊
                boolean hasHardcodedDb = properties.keySet().stream()
                        .anyMatch(k -> k.contains("jdbc.url") || k.contains("username") || k.contains("password"));
                if (hasHardcodedDb) {
                    hardcodedDbCount++;
                    migrationSuggestions.add("🚨 properties 中發現硬編碼資料庫 URL / 用戶名 / 密碼，必須全部外部化到 Kubernetes Secret");
                }

                // 4. schema-generation 自動建表（生產環境危險）
                String schemaAction = (String) properties.getOrDefault("javax.persistence.schema-generation.database.action", "");
                if (schemaAction.contains("drop-and-create")) {
                    migrationSuggestions.add("⚠️ schema-generation.database.action = drop-and-create，生產環境極度危險，建議改為 none 或 validate");
                }

                // 5. provider 版本（可選）
                String provider = (String) unit.getOrDefault("provider", "unknown");
                if (provider.contains("hibernate") && provider.compareTo("5.0") < 0) {
                    migrationSuggestions.add("⚠️ provider = " + provider + "（舊版本 Hibernate），建議升級到 6.x+ 以兼容容器");
                }
            }
        }

        // ====================== 最終報告 ======================
        analysis.put("totalPersistenceUnits", totalUnits);
        analysis.put("jndiDataSourceCount", jndiCount);
        analysis.put("hardcodedDbConfigCount", hardcodedDbCount);
        analysis.put("migrationSuggestions", migrationSuggestions);
        analysis.put("totalSuggestions", migrationSuggestions.size());

        Path reportPath = persistenceJsonFile.toPath().getParent();
        File reportFile = reportPath.resolve("persistence-analysis-report.json").toFile();

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportFile, analysis);

        System.out.println("✅ persistence.xml 分析完成！報告生成 → " + reportFile.getAbsolutePath());
    }
    @Override
    public void ejbJarAnalysis(UserDTO userDTO) throws IOException {
        String outputPath = astMapper.getEjbJarXmlParseOutput(userDTO);
        File ejbJarJsonFile = new File(outputPath);
        checkJsonFile(ejbJarJsonFile);

        ObjectMapper objectMapper = new ObjectMapper();
        List<XmlFileInfo> ejbDataList = objectMapper.readValue(ejbJarJsonFile,
                new TypeReference<List<XmlFileInfo>>() {});

        if (ejbDataList.isEmpty()) {
            System.err.println("⚠️ ejb-jar.xml JSON 为空: " + outputPath);
            return;
        }

        XmlFileInfo fileInfo = ejbDataList.get(0);
        Map<String, Object> data = fileInfo.data;

        Path reportPath = ejbJarJsonFile.toPath().getParent();
        File reportFile = reportPath.resolve("ejb-jar.xml-analysis.json").toFile();
        Map<String, Object> analysis = new LinkedHashMap<>();
        List<String> migrationSuggestions = new ArrayList<>();

        // 基础信息
        analysis.put("fileType", fileInfo.fileType);
        analysis.put("filePath", fileInfo.filePath);
        analysis.put("analyzedAt", LocalDateTime.now().toString());

        // Enterprise Beans
        List<Map<String, Object>> beans = getList(data, "sessionBeans");
        analysis.put("beanCount", beans.size());

        int statefulCount = 0;
        for (Map<String, Object> bean : beans) {
            String sessionType = (String) bean.getOrDefault("sessionType", "");
            if (sessionType.equals("Stateful")) {
                statefulCount++;
                migrationSuggestions.add("⚠️ 发现 Stateful Session Bean: " + bean.get("ejbName") + "，容器化风险高，建议改为 Stateless + Redis 状态存储");
            }
            List<Map<String, Object>> refs = getList(bean, "ejbRef");
            if (!refs.isEmpty()) {
                migrationSuggestions.add("☁️ 发现 ejb-ref（JNDI），建议改为 @Inject 或 CDI");
            }
            String transactionType = (String) bean.getOrDefault("transactionType", "");
            if (transactionType.equals("Bean")) {
                migrationSuggestions.add("ℹ️ transaction-type=Bean（用户管理事务），建议改为 Container 以利用 Jakarta Transactions");
            }
            List<String> securityRoles = getListString(bean, "securityRoles");
            if (!securityRoles.isEmpty()) {
                migrationSuggestions.add("🔐 发现 security-role，建议迁移到 Keycloak 或 Spring Security RBAC");
            }
        }

        List<Map<String, Object>> mdbBeans = getList(data, "messageDrivenBeans");
        analysis.put("mdbBeanCount", mdbBeans.size());

        for (Map<String, Object> mdb : mdbBeans) {
            String ejbName = (String) mdb.getOrDefault("ejbName", "unknown");
            migrationSuggestions.add("📨 發現 Message Driven Bean: " + ejbName + "，建議確認 JMS 配置是否遷移到 Kafka 或 Spring JMS");
        }

        List<Map<String, Object>> interceptors = getList(data, "interceptors");
        List<Map<String, Object>> bindings = getList(data, "interceptorBindings");

        analysis.put("interceptorCount", interceptors.size());
        analysis.put("interceptorBindings", bindings);

        if (interceptors.size() > 0) {
            migrationSuggestions.add("📌 發現 " + interceptors.size() + " 個 Interceptor，建議遷移到 CDI Interceptor（更適合容器環境，減少 XML 配置）");
        }

        List<Map<String, Object>> transactions = getList(data, "containerTransactions");
        analysis.put("transactionCount", transactions.size());

        for (Map<String, Object> tx : transactions) {
            String transAttribute = (String) tx.getOrDefault("transAttribute", "unknown");
            if (transAttribute.equals("NotSupported")) {
                migrationSuggestions.add("⚠️ trans-attribute=NotSupported，可能導致事務不一致，建議確認容器事務管理器");
            }
        }

        // ejbClientJar (使用你的解析结果中的 "ejbClientJar")
        String ejbClientJar = (String) data.getOrDefault("ejbClientJar", "");
        analysis.put("ejbClientJar", ejbClientJar);
        if (!ejbClientJar.isEmpty()) {
            migrationSuggestions.add("ℹ️ 發現 ejb-client-jar = " + ejbClientJar + "，容器化建議移除或改成 Maven 依賴");
        }

        analysis.put("statefulCount", statefulCount);
        analysis.put("migrationSuggestions", migrationSuggestions);
        analysis.put("totalSuggestions", migrationSuggestions.size());

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportFile, analysis);

        System.out.println("✅ ejb-jar.xml 分析完成 → " + reportFile.getAbsolutePath());
    }

    @Override
    public void pomXmlAnalysis(UserDTO userDTO) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        String pomParseOutputPath = astMapper.getPomXmlParseOutput(userDTO);

        File pomXmlFile = new File(pomParseOutputPath);

        checkJsonFile(pomXmlFile);

        Path reportPath = pomXmlFile.toPath().getParent();
        File analysisReportFile = reportPath.resolve("pom.xml-analysis.json").toFile();

        List<XmlFileInfo> xmlFileInfoList = objectMapper.readValue(pomXmlFile, new TypeReference<List<XmlFileInfo>>() {});

        if (xmlFileInfoList.isEmpty()) {
            throw new RuntimeException("pomXmlAnalysis 为空");
        }

        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("totalPomFiles", xmlFileInfoList.size());
        analysis.put("analyzedAt", LocalDateTime.now().toString());

        List<String> migrationSuggestions = new ArrayList<>();

        // 全局汇总变量
        List<String> allJavaVersions = new ArrayList<>();
        List<String> allDependencies = new ArrayList<>();
        List<String> allPlugins = new ArrayList<>();
        int totalProfileCount = 0;
        boolean hasLegacyServerDep = false;
        boolean hasLowJavaVersion = false;

        // 遍历所有 pom.xml 文件
        for (XmlFileInfo xmlFileInfo : xmlFileInfoList) {
            Map<String, Object> data = xmlFileInfo.data;

            // 收集 javaVersion
            String javaVersion = (String) data.getOrDefault("javaVersion", "unknown");
            allJavaVersions.add(javaVersion);
            if (!javaVersion.equals("unknown") && Integer.parseInt(javaVersion) < 11) {
                hasLowJavaVersion = true;
            }

            // 收集 dependencies 并检查传统服务器依赖
            @SuppressWarnings("unchecked")
            List<String> deps = (List<String>) data.getOrDefault("dependencies", List.of());
            allDependencies.addAll(deps);
            if (deps.stream().anyMatch(d -> d.contains("weblogic") || d.contains("websphere") || d.contains("glassfish"))) {
                hasLegacyServerDep = true;
            }

            // 收集 plugins
            @SuppressWarnings("unchecked")
            List<String> plugins = (List<String>) data.getOrDefault("plugins", List.of());
            allPlugins.addAll(plugins);

            // 收集 profileCount
            int profileCount = (int) data.getOrDefault("profileCount", 0);
            totalProfileCount += profileCount;
        }

        // 生成迁移建议
//        if (hasLowJavaVersion) {
//            migrationSuggestions.add("⚠️ 检测到 Java 版本过低（低于 11），建议升级到 Java 17+ 以支持现代容器（如 Liberty/WildFly）");
//        }
//        if (hasLegacyServerDep) {
//            migrationSuggestions.add("⚠️ 检测到传统应用服务器依赖（如 WebLogic/WebSphere），建议移除并迁移到轻量服务器");
//        }
//        if (totalProfileCount > 0) {
//            migrationSuggestions.add("ℹ️ 检测到 " + totalProfileCount + " 个 profile，建议将多环境配置外部化到 Kubernetes ConfigMap");
//        }
//        if (allDependencies.stream().anyMatch(d -> d.contains("jakarta.jakartaee-api") && d.contains("1.0"))) {
//            migrationSuggestions.add("⚠️ 检测到旧版 Jakarta EE API 依赖，建议升级到 10.0+ 以兼容容器");
//        }

        // 汇总到分析报告
        analysis.put("allJavaVersions", allJavaVersions);
        analysis.put("allDependencies", allDependencies);
        analysis.put("allPlugins", allPlugins);
        analysis.put("totalProfileCount", totalProfileCount);
        analysis.put("migrationSuggestions", migrationSuggestions);
        analysis.put("totalSuggestions", migrationSuggestions.size());

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(analysisReportFile, analysis);

        System.out.println("✅ pom.xml 整合分析完成！报告生成 → " + analysisReportFile.getAbsolutePath());
    }

    @Override
    public void facesXmlAnalysis(UserDTO userDTO) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String facesXmlParseOutputPath = astMapper.getFacesXmlParseOutput(userDTO);

        File facesJsonFile = new File(facesXmlParseOutputPath);
        checkJsonFile(facesJsonFile);
        Path reportPath = facesJsonFile.toPath().getParent();
        File analysisReportFile = reportPath.resolve("faces-config.xml-analysis.json").toFile();

        List<XmlFileInfo> dataList = objectMapper.readValue(facesJsonFile,
                new TypeReference<List<XmlFileInfo>>() {});

        if (dataList.isEmpty()) {
            System.err.println("⚠️ faces-config.xml JSON 为空: " + facesXmlParseOutputPath);
            return;
        }

        Map<String, Object> analysis = new LinkedHashMap<>();
        List<String> suggestions = new ArrayList<>();

        int sessionScopeCount = 0;

        for (XmlFileInfo fileInfo : dataList) {
            Map<String, Object> data = fileInfo.data;
            List<Map<String, Object>> managedBeans = (List<Map<String, Object>>) data.getOrDefault("managedBeans", List.of());

            for (Map<String, Object> bean : managedBeans) {
                String scope = (String) bean.getOrDefault("scope", "unknown");
                if (scope.equals("session") || scope.equals("application")) {
                    sessionScopeCount++;
                    suggestions.add("⚠️ 發現 scope=" + scope + " 的 managed-bean (" + bean.get("name") + ")，容器化風險高，建議改為 request/view scope 或遷移到 CDI (@RequestScoped)");
                }
            }
        }

        analysis.put("sessionScopeCount", sessionScopeCount);
        analysis.put("migrationSuggestions", suggestions);
        analysis.put("totalSuggestions", suggestions.size());

        File reportFile = reportPath.resolve("faces-config-analysis-report.json").toFile();

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportFile, analysis);

        System.out.println("✅ faces-config.xml 分析完成 → " + reportFile.getAbsolutePath());
    }



    private void checkJsonFile(File file)
    {

        if (!file.exists()) {
            throw new RuntimeException("文件不存在: " + file);
        }

        if (file.length() == 0) {
            throw new RuntimeException("文件为空（0字节）: " + file);
        }
    }


    // 通用获取 List<Map>
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return (val instanceof List) ? (List<Map<String, Object>>) val : List.of();
    }

    // 专用获取 List<String>（securityRoles）
    @SuppressWarnings("unchecked")
    private List<String> getListString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return (val instanceof List) ? (List<String>) val : List.of();
    }

    // 简单打分示例（你可以扩展成更复杂的规则）
    private int calculateScore(int suggestionCount, String version) {
        return Math.max(100 - suggestionCount * 8, 30); // 建议越多，分数越低，代表越需要现代化
    }

    //TODO 整合在一个Common Class 里为静态方法
    private Path checkOutputFile(String path) throws IOException {
        Path projectRoot = Paths.get(path).normalize();   // 需要 import java.nio.file.Paths;
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

}


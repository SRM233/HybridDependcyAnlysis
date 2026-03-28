package com.hybriddependcyanlysis.Service.Impl;


import Common.ClassInfos.*;
import Common.JsfFileInfo.JsfFileInfo;
import Common.OutputPath;
import Common.XmlFileInfo.XmlFileInfo;
import com.hybriddependcyanlysis.Mapper.AstMapper;
import com.hybriddependcyanlysis.POJO.AnalysisResult;
import com.hybriddependcyanlysis.POJO.DAO.SourceFolderDAO;
import com.hybriddependcyanlysis.Service.ParsingService;


import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.cu.CompilationUnit;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static Common.JspDependencyAnalyzer.analyzeSingleJsp;

@Service
public class ParsingServiceImpl implements ParsingService {

    private Map<String, Set<String>> dependencyGraph = new HashMap<>();

    @Autowired
    private JsonFileServiceImpl jsonFileService;

    private  List<JavaClassInfo> javaClasses = new ArrayList<>();

//    private final List<XmlFileInfo> xmlFileInfos = new ArrayList<>();
    public AnalysisResult analysisResult = new AnalysisResult();
    private  List<IssueInfo> allIssues = new ArrayList<>();
    List<JsfFileInfo> jsfFiles = new ArrayList<>();

    @Autowired
    private AstMapper astMapper;
//    public Map<String, List<XmlFileInfo>> xmlConfigs = new HashMap<>();

    public void parsing(File serverOutput, File serverError,SourceFolderDAO sourceFolderDAO) throws IOException {
        dependencyGraph.clear();
        javaClasses.clear();

        Path root = Paths.get(sourceFolderDAO.getDirPath());
        if (!Files.exists(root) || !Files.isDirectory(root)) return;

        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".java"))
                    .forEach(p -> {
                        try {
                            parseJavaFile(p.toFile());  // 只传 DAO，不再传 writer
                         } catch (Exception e) {
                             System.err.println("解析Java文件失败: " + p + " - " + e.getMessage());
                         }
                    });
        }

        // 【只生成 JSON】使用正确的输出路径
        Path outputRoot = root.resolve(OutputPath.OUTPUT_BASE_DIR);
        File jsonFile = outputRoot.resolve(OutputPath.JAVA_PARSE_RESULT_PATH).toFile();
        jsonFileService.generateJsonArray(javaClasses, jsonFile.getAbsolutePath());
    }

    @Override
    public void parsingJsp(File outputLog, File errorLog, SourceFolderDAO sourceFolderDAO) throws IOException {
        Path root = Paths.get(sourceFolderDAO.getDirPath());
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            System.out.println("❌ 项目目录不存在，跳过 JSP 解析");
            return;
        }

        Set<String> processed = new HashSet<>();

        System.out.println("=== 开始扫描 JSP 文件 ===");

        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".jsp"))
                    .forEach(p -> {
                        String absPath = p.toAbsolutePath().toString();
                        String lower = absPath.toLowerCase();

                        if (lower.contains("\\target\\") || lower.contains("/target/") ||
                                lower.contains("\\build\\") || lower.contains("/build/") ||
                                lower.contains("\\.mvn\\") || lower.contains("/.mvn/")) {
                            return;
                        }

                        try {
                            long size = Files.size(p);
                            String uniqueKey = absPath + "|" + size;
                            if (processed.add(uniqueKey)) {
                                System.out.println("   → 开始解析: " + p.getFileName());
                                parseJspFiles(p.toFile());   // ← 调用修改后的方法
                            }
                        } catch (IOException ignored) {}
                    });
        }

        System.out.println("=== JSP 扫描结束 ===\n");

        // ====================== 生成独立的 JSP JSON 报告 ======================
        Path outputRoot = root.resolve(OutputPath.OUTPUT_BASE_DIR);
        File jspJsonFile = outputRoot.resolve(OutputPath.JSP_PARSE_RESULT_PATH).toFile();

        // 只提取 JSP 生成的类（可选：如果你想把所有类都放一个 JSON，也可以直接用 javaClasses）
        List<JavaClassInfo> jspGeneratedClasses = javaClasses.stream()
                .filter(c -> c.getIsGeneratedFromJsp())
                .collect(Collectors.toList());

        jsonFileService.generateJsonArray(jspGeneratedClasses, jspJsonFile.getAbsolutePath());

        System.out.println("✅ JSP JSON 报告生成完成 → " + jspJsonFile.getAbsolutePath());
        System.out.println("   共包含 " + jspGeneratedClasses.size() + " 个 JSP 生成的 Servlet 类");
    }

    public void parseJavaFile(File javaFile) throws IOException {

        try {
            Launcher launcher = new Launcher();
            launcher.getEnvironment().setNoClasspath(true);
            launcher.addInputResource(javaFile.getAbsolutePath());
            launcher.buildModel();

            CtModel model = launcher.getModel();

            for (CtType<?> type : model.getAllTypes()) {
                parseClass(type);   // 不再传 writer
            }
        } catch (Exception e) {
            System.err.println("解析Java文件失败（Spoon）: " + javaFile.getAbsolutePath() + " - " + e.getMessage());
        }


    }

    public void parseClass(CtType<?> type) throws IOException {

        JavaClassInfo classInfo = new JavaClassInfo();

        classInfo.setFullName(type.getQualifiedName());
        classInfo.setSimpleName(type.getSimpleName());
        classInfo.setPackageName(type.getPackage() != null ? type.getPackage().getQualifiedName() : "(default)");
        classInfo.setKind((type instanceof CtInterface) ? "Interface" : "Class");

        // 修饰符
        classInfo.setModifiers(type.getModifiers().stream()
                .map(ModifierKind::toString)
                .map(String::toLowerCase)
                .collect(Collectors.toList()));

        // ==================== 注解（保留） ====================
        for (CtAnnotation<?> annotation : type.getAnnotations()) {
            AnnotationInfo annInfo = new AnnotationInfo();
            annInfo.name = annotation.getAnnotationType().getQualifiedName();
            annotation.getValues().forEach((k, v) -> annInfo.values.put(k, v.toString()));
            classInfo.addAnnotation(annInfo);
        }

        // ==================== 字段 ====================
        for (CtField<?> field : type.getFields()) {
            FieldInfo f = new FieldInfo();
            f.name = field.getSimpleName();
            f.type = field.getType() != null ? field.getType().getQualifiedName() : "(unresolved)";
            f.modifiers = field.getModifiers().stream()
                    .map(ModifierKind::toString)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            for (CtAnnotation<?> ann : field.getAnnotations()) {
                AnnotationInfo annInfo = new AnnotationInfo();
                annInfo.name = ann.getAnnotationType().getQualifiedName();
                f.annotations.add(annInfo);
            }
            classInfo.addField(f);
        }

        // ==================== 依赖检测 ====================
        Set<String> classDeps = new HashSet<>();
        detectConstructorIssues(type, classDeps);
        detectMemoryReplicationIssues(type, classDeps);
        
        // ==================== 方法 ====================
        for (CtMethod<?> method : type.getMethods()) {
            parseMethodWithDeps(method, classInfo, classDeps);
        }


//        String fullSourceCode = type.toStringWithImports();
//        classInfo.setFullSourceCode(fullSourceCode);

        List<String> importList = new ArrayList<>();
        Collection<CompilationUnit> allCUs = type.getFactory().CompilationUnit().getMap().values();
        if (!allCUs.isEmpty()) {
            CtCompilationUnit cu = allCUs.iterator().next();
            for (CtImport imp : cu.getImports()) {
                importList.add(imp.getReference().getSimpleName());
            }
        }
        classInfo.setImports(importList);

        // 存储依赖关系到 dependencyGraph
        if (!classDeps.isEmpty()) {
            dependencyGraph.computeIfAbsent(classInfo.getFullName(), k -> new HashSet<>()).addAll(classDeps);
        }

        javaClasses.add(classInfo);


    }

    private void parseMethodWithDeps(CtMethod<?> method, JavaClassInfo classInfo, Set<String> classDeps) throws IOException {
        MethodInfo m = new MethodInfo();
        m.name = method.getSimpleName();
        m.returnType = method.getType() != null ? method.getType().getQualifiedName() : "(unresolved)";
        m.modifiers = method.getModifiers().stream()
                .map(ModifierKind::toString)
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        for (CtParameter<?> param : method.getParameters()) {
            ParameterInfo p = new ParameterInfo();
            p.name = param.getSimpleName();
            p.type = param.getType() != null ? param.getType().getQualifiedName() : "(unresolved)";
            p.modifiers = param.getModifiers().stream()
                    .map(ModifierKind::toString)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
            m.parameters.add(p);
        }

        classInfo.addMethod(m);
        
        detectInvocationIssues(method, classDeps);
    }

    @Override
    public void parseMethod(CtMethod<?> method, JavaClassInfo classInfo) throws IOException {
        parseMethodWithDeps(method, classInfo, new HashSet<>());
    }

    public void parsePackage(CtModel model, BufferedWriter writer) throws IOException {
//            model.getAllTypes().stream().findFirst().ifPresent(type -> {
//                try {
//                    String pkg = type.getPackage().getQualifiedName();
//                    if (!pkg.isEmpty()) {
//                        writer.write("Package: " + pkg + "\n");
//                    }
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            });
        }

    public void detectInvocationIssues(CtMethod<?> method, Set<String> classDeps) throws IOException {   // Detect import problem
        String className = method.getDeclaringType().getQualifiedName();
        boolean isGeneratedJsp = className.contains("_jsp") || className.contains("org.apache.jsp");

        method.filterChildren(new TypeFilter<>(CtInvocation.class)).forEach(inv -> {
            try {
                CtInvocation<?> call = (CtInvocation<?>) inv;
                CtExecutableReference<?> exec = call.getExecutable();
                CtTypeReference<?> declaringType = exec.getDeclaringType();

                String declaringTypeName = (declaringType != null) ? declaringType.getQualifiedName() : "(unknown)";
                String signature = exec.getSignature();

                if (declaringType != null) {
                    classDeps.add(declaringType.getQualifiedName());
                }

                // ==================== Issue 判断（保留数据收集） ====================
                boolean shouldReport = false;
                String issueType = "";

                if (isGeneratedJsp) {
                    if (declaringTypeName.startsWith("java.io.File") ||
                            declaringTypeName.startsWith("java.nio.file")) {
                        shouldReport = true;
                        issueType = "FileSystemDependency (from JSP generated code)";
                    }
                } else {
                    if ("getenv(java.lang.String)".equals(signature)) {
                        shouldReport = true;
                        issueType = "PortabilityRisk";
                    } else if ("exit(int)".equals(signature) && "java.lang.System".equals(declaringTypeName)) {
                        shouldReport = true;
                        issueType = "AbruptTermination";
                    } else if ((declaringTypeName.startsWith("java.io") || declaringTypeName.startsWith("java.nio.file"))
                            && !declaringTypeName.contains("Writer") && !declaringTypeName.contains("PrintWriter")) {
                        shouldReport = true;
                        issueType = "FileSystemDependency";
                    }
                }

                if (shouldReport) {
                    // 【已注释：文本输出】
                    // writer.write(String.format("    ⚠️ Issue: Uses %s → %s%s\n", ...));

                    IssueInfo issue = new IssueInfo();
                    issue.severity = issueType.contains("High") ? "High" : "Medium";
                    issue.message = "Uses " + declaringTypeName + "." + signature + " → " + issueType;
                    issue.location = "";   // 如果后面需要位置信息可以再加
                    issue.className = className;
                    issue.source = "invocation";
                    allIssues.add(issue);
                }

            } catch (Exception e) {
                System.err.println("检测调用问题时出错: " + e.getMessage());
            }
        });
    }

    // ====================== 3. parseFallbackUnit ======================
    public void parseFallbackUnit(CtCompilationUnit unit) throws IOException {   // ← 去掉 writer 参数
        // 【已注释：文本输出】
    /*
    CtPackageDeclaration pkgDecl = unit.getPackageDeclaration();
    if (pkgDecl != null && pkgDecl.getReference() != null) {
        String pkgName = pkgDecl.getReference().getQualifiedName();
        if (!pkgName.isEmpty()) {
            writer.write("Package: " + pkgName + "\n");
        }
    } else {
        writer.write("Package: (default)\n");
    }
    */

        for (CtType<?> type : unit.getDeclaredTypes()) {
            parseClass(type);   // 继续正常收集数据
        }
    }

    // ====================== 4. detectConstructorIssues ======================
    public void detectConstructorIssues(CtType<?> type, Set<String> classDeps) throws IOException {   // ← 去掉 writer 参数
        type.filterChildren(new TypeFilter<>(CtConstructorCall.class)).forEach(ctor -> {
            try {
                CtConstructorCall<?> call = (CtConstructorCall<?>) ctor;
                CtTypeReference<?> ref = call.getType();
                String typeName = (ref != null) ? ref.getQualifiedName() : "(unknown)";

                if (ref != null) {
                    classDeps.add(ref.getQualifiedName());
                }

                switch (typeName) {
                    case "javax.servlet.http.HttpSession":
                        // 【已注释：文本输出】
                        // writer.write("  ⚠️ Issue: Uses HttpSession → StatefulSession\n");

                        IssueInfo issue1 = new IssueInfo();
                        issue1.severity = "High";
                        issue1.message = "Uses HttpSession → StatefulSession";
                        issue1.location = "in class " + type.getSimpleName();
                        issue1.className = type.getQualifiedName();
                        issue1.source = "constructor";
                        allIssues.add(issue1);
                        break;

                    case "java.io.FileInputStream":
                    case "java.io.File":
                        // 【已注释：文本输出】
                        // writer.write("  ⚠️ Issue: Uses " + typeName + " → FileSystemDependency\n");

                        IssueInfo issue2 = new IssueInfo();
                        issue2.severity = "High";
                        issue2.message = "Uses " + typeName + " → FileSystemDependency";
                        issue2.location = "in class " + type.getSimpleName();
                        issue2.className = type.getQualifiedName();
                        issue2.source = "constructor";
                        allIssues.add(issue2);
                        break;

                    case "java.net.Socket":
                        // 【已注释：文本输出】
                        // writer.write("  ⚠️ Issue: Uses Socket → PortBindingRisk\n");

                        IssueInfo issue3 = new IssueInfo();
                        issue3.severity = "High";
                        issue3.message = "Uses Socket → PortBindingRisk";
                        issue3.location = "in class " + type.getSimpleName();
                        issue3.className = type.getQualifiedName();
                        issue3.source = "constructor";
                        allIssues.add(issue3);
                        break;
                }
            } catch (Exception e) {
                System.err.println("检测构造器问题时出错: " + e.getMessage());
            }
        });
    }

    // ====================== 5. detectMemoryReplicationIssues ======================
    @Override
    public void detectMemoryReplicationIssues(CtType<?> type, Set<String> classDeps) throws IOException {
        String className = type.getQualifiedName();
        
        // 1. Check for Serializable interface
        for (CtTypeReference<?> interfaceRef : type.getSuperInterfaces()) {
            String interfaceName = interfaceRef.getQualifiedName();
            if (interfaceName.equals("java.io.Serializable")) {
                IssueInfo issue = new IssueInfo();
                issue.severity = "Medium";
                issue.message = "Implements Serializable - Object serialization may cause memory replication";
                issue.location = "in class " + type.getSimpleName();
                issue.className = className;
                issue.source = "memory_replication";
                allIssues.add(issue);
                break;
            }
        }
        
        // 2. Check for Cloneable interface  
        for (CtTypeReference<?> interfaceRef : type.getSuperInterfaces()) {
            String interfaceName = interfaceRef.getQualifiedName();
            if (interfaceName.equals("java.lang.Cloneable")) {
                IssueInfo issue = new IssueInfo();
                issue.severity = "Medium";
                issue.message = "Implements Cloneable - Object cloning may cause memory replication";
                issue.location = "in class " + type.getSimpleName();
                issue.className = className;
                issue.source = "memory_replication";
                allIssues.add(issue);
                break;
            }
        }
        
        // 3. Check for MappedByteBuffer usage
        type.filterChildren(new TypeFilter<>(CtConstructorCall.class)).forEach(ctor -> {
            CtConstructorCall<?> call = (CtConstructorCall<?>) ctor;
            CtTypeReference<?> ref = call.getType();
            if (ref != null && ref.getQualifiedName().equals("java.nio.MappedByteBuffer")) {
                IssueInfo issue = new IssueInfo();
                issue.severity = "High";
                issue.message = "Uses MappedByteBuffer - Memory-mapped files can cause memory replication";
                issue.location = "in class " + type.getSimpleName();
                issue.className = className;
                issue.source = "memory_replication";
                allIssues.add(issue);
            }
        });
        
        // 4. Check for FileChannel.map() invocations
        type.filterChildren(new TypeFilter<>(CtInvocation.class)).forEach(inv -> {
            CtInvocation<?> invocation = (CtInvocation<?>) inv;
            CtExecutableReference<?> execRef = invocation.getExecutable();
            if (execRef != null && execRef.getSignature().contains("java.nio.channels.FileChannel.map")) {
                IssueInfo issue = new IssueInfo();
                issue.severity = "High";
                issue.message = "Uses FileChannel.map() - Memory-mapped files can cause memory replication";
                issue.location = "in class " + type.getSimpleName();
                issue.className = className;
                issue.source = "memory_replication";
                allIssues.add(issue);
            }
        });
        
        // 5. Check for distributed caching frameworks
        String[] distributedFrameworks = {
            "org.jgroups", "com.hazelcast", "net.sf.ehcache", "org.infinispan", 
            "redis.clients.jedis", "org.apache.ignite", "org.ehcache"
        };
        
        for (CtTypeReference<?> dep : type.getReferencedTypes()) {
            String depName = dep.getQualifiedName();
            for (String framework : distributedFrameworks) {
                if (depName.startsWith(framework)) {
                    IssueInfo issue = new IssueInfo();
                    issue.severity = "High";
                    issue.message = "Uses distributed caching framework '" + depName + "' - May cause memory replication across nodes";
                    issue.location = "in class " + type.getSimpleName();
                    issue.className = className;
                    issue.source = "memory_replication";
                    allIssues.add(issue);
                    break;
                }
            }
        }
        
        // 6. Check for RMI usage
        String[] rmiClasses = {
            "java.rmi", "javax.rmi", "java.rmi.server", "java.rmi.registry"
        };
        
        for (CtTypeReference<?> dep : type.getReferencedTypes()) {
            String depName = dep.getQualifiedName();
            for (String rmiClass : rmiClasses) {
                if (depName.startsWith(rmiClass)) {
                    IssueInfo issue = new IssueInfo();
                    issue.severity = "High";
                    issue.message = "Uses RMI '" + depName + "' - Remote method invocation can cause object serialization/replication";
                    issue.location = "in class " + type.getSimpleName();
                    issue.className = className;
                    issue.source = "memory_replication";
                    allIssues.add(issue);
                    break;
                }
            }
        }
        
        // 7. Check for JMS usage
        String[] jmsClasses = {
            "javax.jms", "jakarta.jms"
        };
        
        for (CtTypeReference<?> dep : type.getReferencedTypes()) {
            String depName = dep.getQualifiedName();
            for (String jmsClass : jmsClasses) {
                if (depName.startsWith(jmsClass)) {
                    IssueInfo issue = new IssueInfo();
                    issue.severity = "Medium";
                    issue.message = "Uses JMS '" + depName + "' - Message passing may involve object serialization";
                    issue.location = "in class " + type.getSimpleName();
                    issue.className = className;
                    issue.source = "memory_replication";
                    allIssues.add(issue);
                    break;
                }
            }
        }
    }

    public void writeModifiers(Set<ModifierKind> modifiers, BufferedWriter writer, String indent, String context) throws IOException {
        if (modifiers != null && !modifiers.isEmpty()) {
            String modifierStr = modifiers.stream()
                    .map(ModifierKind::toString)
                    .map(String::toLowerCase)
                    .reduce((a, b) -> a + " " + b)
                    .orElse("");
            writer.write(indent + context + " Modifier: " + modifierStr + "\n");
        }


    }


    public void parseJspFiles(File jspFile) {   // ← 改成只接收 File
        try {
            System.out.println("📝 记录 JSP 文件: " + jspFile.getName());
            
            // 创建简单的 JavaClassInfo 对象，仅记录原始 JSP 文件
            JavaClassInfo jspClassInfo = new JavaClassInfo();
            jspClassInfo.setFullName(jspFile.getAbsolutePath());
            jspClassInfo.setSimpleName(jspFile.getName());
            jspClassInfo.setPackageName("");
            jspClassInfo.setKind("JSP File");
            jspClassInfo.setIsGeneratedFromJsp(true);
            jspClassInfo.setOriginalJspFile(jspFile.getAbsolutePath());
            
            // 添加到 javaClasses 列表
            javaClasses.add(jspClassInfo);
            
            // 添加警告 issue
            IssueInfo warning = new IssueInfo();
            warning.setSeverity("Medium");
            warning.setMessage("使用 JSP 文件可能导致扩展性问题，建议使用现代前端技术");
            warning.setLocation(jspFile.getAbsolutePath());
            warning.setClassName(jspFile.getName());
            warning.setSource("jsp");
            
            analysisResult.getIssues().add(warning);
            allIssues.add(warning);

            // ==================== 解析 JSP 生成的 Servlet 代码 ====================
            // 创建临时输出目录
            Path tempDir = Files.createTempDirectory("jsp_analysis");
            try {
                String generatedJavaPath = analyzeSingleJsp(jspFile, tempDir.toString());
                if (generatedJavaPath != null) {
                    File generatedJavaFile = new File(generatedJavaPath);
                    if (generatedJavaFile.exists()) {
                        System.out.println("🔍 解析 JSP 生成的 Servlet: " + generatedJavaFile.getName());
                        parseJavaFile(generatedJavaFile);
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠️ JSP 代码分析失败: " + jspFile.getName() + " - " + e.getMessage());
            } finally {
                // 清理临时目录
                try {
                    Files.walk(tempDir)
                         .sorted(Comparator.reverseOrder())
                         .map(Path::toFile)
                         .forEach(File::delete);
                } catch (IOException e) {
                    // 忽略清理错误
                }
            }

        } catch (Exception e) {
            System.out.println("❌ JSP 记录异常: " + jspFile.getName() + " → " + e.getMessage());
        }
    }




    @Override
    public void parseWebXml(File webXmlParseoutput, SourceFolderDAO sourceFolderDAO) throws IOException {

        parseGenericXml(
                webXmlParseoutput,
                sourceFolderDAO,
                "web.xml",
                "web.xml",
                (doc, xmlData) -> {
                    // ====== 以下是原来 parseWebXml 中的核心解析逻辑 ======
                    // 1. Security Constraints（修正嵌套結構）
                    List<Map<String, Object>> securityList = new ArrayList<>();
                    NodeList securityConstraints = doc.getElementsByTagName("security-constraint");
                    for (int i = 0; i < securityConstraints.getLength(); i++) {
                        Element sc = (Element) securityConstraints.item(i);

                        // web-resource-collection
                        Element wrc = (Element) sc.getElementsByTagName("web-resource-collection").item(0);
                        if (wrc == null) continue;

                        Map<String, Object> scMap = new HashMap<>();
                        scMap.put("resourceName", getTagValue(wrc, "web-resource-name"));

                        // url-patterns
                        List<String> urlPatterns = new ArrayList<>();
                        NodeList urlNodes = wrc.getElementsByTagName("url-pattern");
                        for (int j = 0; j < urlNodes.getLength(); j++) {
                            urlPatterns.add(urlNodes.item(j).getTextContent().trim());
                        }
                        scMap.put("urlPatterns", urlPatterns);

                        // auth-constraint → roles
                        Element auth = (Element) sc.getElementsByTagName("auth-constraint").item(0);
                        List<String> roles = new ArrayList<>();
                        if (auth != null) {
                            NodeList roleNodes = auth.getElementsByTagName("role-name");
                            for (int j = 0; j < roleNodes.getLength(); j++) {
                                roles.add(roleNodes.item(j).getTextContent().trim());
                            }
                        }
                        scMap.put("roles", roles);

                        securityList.add(scMap);
                    }
                    xmlData.put("securityConstraints", securityList);

                    // 2. Security Roles（已正確，可保留或簡化）
                    List<String> rolesList = new ArrayList<>();
                    NodeList securityRoles = doc.getElementsByTagName("security-role");
                    for (int i = 0; i < securityRoles.getLength(); i++) {
                        Element roleElem = (Element) securityRoles.item(i);
                        String roleName = getTagValue(roleElem, "role-name");
                        if (!roleName.isEmpty()) {
                            rolesList.add(roleName);
                        }
                    }
                    xmlData.put("securityRoles", rolesList);

                    // 3. Error Pages（已大致正確，可小優化）
                    List<Map<String, Object>> errorPagesList = new ArrayList<>();
                    NodeList errorPages = doc.getElementsByTagName("error-page");
                    for (int i = 0; i < errorPages.getLength(); i++) {
                        Element ep = (Element) errorPages.item(i);
                        Map<String, Object> epMap = new HashMap<>();

                        String exType  = getTagValue(ep, "exception-type");
                        String errCode = getTagValue(ep, "error-code");
                        String location = getTagValue(ep, "location");

                        if (!exType.isEmpty()) {
                            epMap.put("type", "exception");
                            epMap.put("value", exType);
                        } else if (!errCode.isEmpty()) {
                            epMap.put("type", "http");
                            epMap.put("value", errCode);
                        }
                        if (!location.isEmpty()) {
                            epMap.put("location", location);
                        }
                        if (!epMap.isEmpty()) {
                            errorPagesList.add(epMap);
                        }
                    }
                    xmlData.put("errorPages", errorPagesList);

                    // 4. 其他常見但這個 XML 沒有的（可選保留）
                    // Servlets、Filters → 這個 XML 沒有，保持原邏輯即可（會得到空列表）

                    // 5. 關鍵配置（修正 session-timeout）
                    Map<String, Object> keyConfigs = new HashMap<>();
                    NodeList sessionConfigs = doc.getElementsByTagName("session-config");
                    if (sessionConfigs.getLength() > 0) {
                        Element sc = (Element) sessionConfigs.item(0);
                        String timeout = getTagValue(sc, "session-timeout");
                        if (!timeout.isEmpty()) {
                            keyConfigs.put("sessionTimeoutMinutes", timeout);
                            keyConfigs.put("note", "建議移到環境變數或外部配置");
                        }
                    }
                    // 如果還有其他想統一收集的（如 welcome-file-list、context-param 等）可繼續加

                    xmlData.put("keyConfigs", keyConfigs);

                    // 7. Resource Refs
                    List<Map<String, Object>> resourcesList = new ArrayList<>();
                    NodeList resources = doc.getElementsByTagName("resource-ref");
                    for (int i = 0; i < resources.getLength(); i++) {
                        Element r = (Element) resources.item(i);
                        Map<String, Object> rMap = new HashMap<>();
                        rMap.put("resRefName", getTagValue(r, "res-ref-name"));
                        rMap.put("resType", getTagValue(r, "res-type"));
                        rMap.put("risk", "JNDI 必须改成环境变量");
                        resourcesList.add(rMap);
                    }
                    xmlData.put("resourceRefs", resourcesList);

                    Element root = doc.getDocumentElement();

                    // 1. 版本检测（老项目最重要！）
                    String version = root.getAttribute("version");
                    if (version.isEmpty() && doc.getDoctype() != null) {
                        String dtd = doc.getDoctype().getSystemId();
                        if (dtd.contains("2.3")) version = "2.3";
                        else if (dtd.contains("2.4")) version = "2.4";
                        else if (dtd.contains("2.5")) version = "2.5";
                        else if (dtd.contains("3.0")) version = "3.0";
                        else if (dtd.contains("3.1")) version = "3.1";
                    }
                    xmlData.put("version", version.isEmpty() ? "unknown" : version);
                    xmlData.put("metadataComplete", "true".equals(root.getAttribute("metadata-complete")));

                    // 2. Context Param（硬编码配置大户）
                    List<Map<String, Object>> contextParams = new ArrayList<>();
                    NodeList cpList = doc.getElementsByTagName("context-param");
                    for (int i = 0; i < cpList.getLength(); i++) {
                        Element cp = (Element) cpList.item(i);
                        Map<String, Object> m = new HashMap<>();
                        m.put("name", getTagValue(cp, "param-name"));
                        m.put("value", getTagValue(cp, "param-value"));
                        contextParams.add(m);
                    }
                    xmlData.put("contextParams", contextParams);

                    // 3. Filter + Filter-Mapping（合并到同一个列表）
                    List<Map<String, Object>> filters = new ArrayList<>();
                    NodeList filterList = doc.getElementsByTagName("filter");
                    for (int i = 0; i < filterList.getLength(); i++) {
                        Element f = (Element) filterList.item(i);
                        Map<String, Object> fm = new HashMap<>();
                        fm.put("filterName", getTagValue(f, "filter-name"));
                        fm.put("filterClass", getTagValue(f, "filter-class"));
                        // init-param 可继续加...
                        filters.add(fm);
                    }
                    xmlData.put("filters", filters);

                    // 4. Servlet + Servlet-Mapping
                    List<Map<String, Object>> servlets = new ArrayList<>();
                    NodeList servletList = doc.getElementsByTagName("servlet");
                    for (int i = 0; i < servletList.getLength(); i++) {
                        Element s = (Element) servletList.item(i);
                        Map<String, Object> sm = new HashMap<>();
                        sm.put("servletName", getTagValue(s, "servlet-name"));
                        sm.put("servletClass", getTagValue(s, "servlet-class"));
                        sm.put("loadOnStartup", getTagValue(s, "load-on-startup"));
                        servlets.add(sm);
                    }
                    xmlData.put("servlets", servlets);

                    // 5. Listener
                    List<String> listeners = new ArrayList<>();
                    NodeList listenerList = doc.getElementsByTagName("listener");
                    for (int i = 0; i < listenerList.getLength(); i++) {
                        listeners.add(getTagValue((Element) listenerList.item(i), "listener-class"));
                    }
                    xmlData.put("listeners", listeners);

                    // 6. Session Config（增强版）
                    Map<String, Object> sessionConfig = new HashMap<>();
                    NodeList scList = doc.getElementsByTagName("session-config");
                    if (scList.getLength() > 0) {
                        Element sc = (Element) scList.item(0);
                        sessionConfig.put("sessionTimeoutMinutes", getTagValue(sc, "session-timeout"));
                    }
                    xmlData.put("sessionConfig", sessionConfig);

                    // 7. Welcome File List
                    List<String> welcomeFiles = new ArrayList<>();
                    NodeList wfList = doc.getElementsByTagName("welcome-file");
                    for (int i = 0; i < wfList.getLength(); i++) {
                        welcomeFiles.add(wfList.item(i).getTextContent().trim());
                    }
                    xmlData.put("welcomeFiles", welcomeFiles);

                    // 8. Taglib（老项目常见）
                    List<Map<String, Object>> taglibs = new ArrayList<>();
                    NodeList tList = doc.getElementsByTagName("taglib");
                    for (int i = 0; i < tList.getLength(); i++) {
                        Element t = (Element) tList.item(i);
                        Map<String, Object> m = new HashMap<>();
                        m.put("uri", getTagValue(t, "taglib-uri"));
                        m.put("location", getTagValue(t, "taglib-location"));
                        taglibs.add(m);
                    }
                    xmlData.put("taglibs", taglibs);

                    // 9. Login Config（安全）
                    Map<String, Object> loginConfig = new HashMap<>();
                    NodeList lcList = doc.getElementsByTagName("login-config");
                    if (lcList.getLength() > 0) {
                        Element lc = (Element) lcList.item(0);
                        loginConfig.put("authMethod", getTagValue(lc, "auth-method"));
                        loginConfig.put("formLoginPage", getTagValue(lc, "form-login-page"));
                    }
                    xmlData.put("loginConfig", loginConfig);

                    // 10. Mime Mappings
                    List<Map<String, Object>> mimeMappings = new ArrayList<>();
                    NodeList mmList = doc.getElementsByTagName("mime-mapping");
                    for (int i = 0; i < mmList.getLength(); i++) {
                        Element mm = (Element) mmList.item(i);
                        Map<String, Object> m = new HashMap<>();
                        m.put("extension", getTagValue(mm, "extension"));
                        m.put("mimeType", getTagValue(mm, "mime-type"));
                        mimeMappings.add(m);
                    }
                    xmlData.put("mimeMappings", mimeMappings);

                }
        );
    }

    @Override
    public void parsePersistenceXml(File output, SourceFolderDAO sourceFolderDAO) throws IOException {
        parseGenericXml(output, sourceFolderDAO, "persistence.xml", "persistence.xml", (doc, xmlData) -> {
            List<Map<String, Object>> unitsList = new ArrayList<>();
            NodeList units = doc.getElementsByTagName("persistence-unit");

            for (int i = 0; i < units.getLength(); i++) {
                Element unit = (Element) units.item(i);
                Map<String, Object> m = new HashMap<>();

                // 1. persistence-unit 的基本属性
                m.put("name", unit.getAttribute("name"));                    // 强烈建议加上
                m.put("jtaDataSource", getTagValue(unit, "jta-data-source"));
                m.put("nonJtaDataSource", getTagValue(unit, "non-jta-data-source"));

                // 2. 处理多个 <class>（关键修复）
                List<String> classList = new ArrayList<>();
                NodeList classNodes = unit.getElementsByTagName("class");   // 注意：用 unit 而不是 doc
                for (int j = 0; j < classNodes.getLength(); j++) {
                    String className = classNodes.item(j).getTextContent().trim();
                    if (!className.isEmpty()) {
                        classList.add(className);
                    }
                }
                m.put("classes", classList);   // 改成复数更清晰

                // 3. 处理 <properties> 里的多个 <property>（关键修复）
                m.put("properties", parseProperties(unit));

                unitsList.add(m);
            }

            xmlData.put("persistenceUnits", unitsList);
        });
    }

    /**
     * 解析 persistence-unit 下的 <properties> 标签，返回 Map<name, value>
     */
    private Map<String, String> parseProperties(Element persistenceUnit) {
        Map<String, String> props = new HashMap<>();

        NodeList propContainers = persistenceUnit.getElementsByTagName("properties");
        if (propContainers.getLength() == 0) {
            return props;  // 没有 properties 标签就返回空 Map
        }

        Element propertiesElement = (Element) propContainers.item(0);
        NodeList propertyNodes = propertiesElement.getElementsByTagName("property");

        for (int i = 0; i < propertyNodes.getLength(); i++) {
            Element prop = (Element) propertyNodes.item(i);
            String name = prop.getAttribute("name");
            String value = prop.getAttribute("value");

            if (!name.isEmpty()) {
                props.put(name, value != null ? value : "");
            }
        }
        return props;
    }

    @Override
    public void parseEjbJarXml(File output, SourceFolderDAO sourceFolderDAO) throws IOException {
        parseGenericXml(output, sourceFolderDAO, "ejb-jar.xml", "ejb-jar.xml", (doc, xmlData) -> {
            List<Map<String, Object>> sessionBeans = new ArrayList<>();
            NodeList sessions = doc.getElementsByTagName("session");   // 直接找所有 session（enterprise-beans 下面）
            for (int i = 0; i < sessions.getLength(); i++) {
                Element session = (Element) sessions.item(i);
                Map<String, Object> m = new HashMap<>();
                m.put("ejbName", getTagValue(session, "ejb-name"));
                m.put("sessionType", getTagValue(session, "session-type"));   // Stateful / Stateless
                m.put("transactionType", getTagValue(session, "transaction-type"));
                sessionBeans.add(m);
            }
            xmlData.put("sessionBeans", sessionBeans);

            // ==================== Message Driven Bean ====================
            List<Map<String, Object>> mdbBeans = new ArrayList<>();
            NodeList mdbNodes = doc.getElementsByTagName("message-driven");
            for (int i = 0; i < mdbNodes.getLength(); i++) {
                Element mdb = (Element) mdbNodes.item(i);
                Map<String, Object> m = new HashMap<>();
                m.put("ejbName", getTagValue(mdb, "ejb-name"));
                m.put("destinationType", getTagValue(mdb, "destination-type"));
                mdbBeans.add(m);
            }
            xmlData.put("messageDrivenBeans", mdbBeans);

            // ==================== 2. Assembly Descriptor（事務 + 安全） ====================
            List<Map<String, Object>> transactions = new ArrayList<>();
            NodeList txNodes = doc.getElementsByTagName("container-transaction");
            for (int i = 0; i < txNodes.getLength(); i++) {
                Element tx = (Element) txNodes.item(i);
                Map<String, Object> m = new HashMap<>();
                m.put("transAttribute", getTagValue(tx, "trans-attribute"));
                transactions.add(m);
            }
            xmlData.put("containerTransactions", transactions);

            // ==================== 3. ejb-client-jar ====================
            xmlData.put("ejbClientJar", getTagValue(doc, "ejb-client-jar"));

            // ==================== 4. Interceptors（你原本就有的部分，保留） ====================
            List<Map<String, Object>> interceptors = new ArrayList<>();
            NodeList interceptorNodes = doc.getElementsByTagName("interceptor");
            for (int i = 0; i < interceptorNodes.getLength(); i++) {
                Element interceptor = (Element) interceptorNodes.item(i);
                Map<String, Object> m = new HashMap<>();
                m.put("interceptorClass", getTagValue(interceptor, "interceptor-class"));
                interceptors.add(m);
            }
            xmlData.put("interceptors", interceptors);

            // Interceptor Binding
            List<Map<String, Object>> bindings = new ArrayList<>();
            NodeList bindingNodes = doc.getElementsByTagName("interceptor-binding");
            for (int i = 0; i < bindingNodes.getLength(); i++) {
                Element binding = (Element) bindingNodes.item(i);
                Map<String, Object> m = new HashMap<>();
                m.put("ejbName", getTagValue(binding, "ejb-name"));
                m.put("interceptorClass", getTagValue(binding, "interceptor-class"));
                bindings.add(m);
            }
            xmlData.put("interceptorBindings", bindings);
        });
    }

    @Override
    public void parseFacesConfigXml(File output, SourceFolderDAO sourceFolderDAO) throws IOException {
        parseGenericXml(output, sourceFolderDAO, "faces-config.xml", "faces-config.xml", (doc, xmlData) -> {
            List<Map<String, Object>> beansList = new ArrayList<>();
            NodeList managedBeans = doc.getElementsByTagName("managed-bean");
            for (int i = 0; i < managedBeans.getLength(); i++) {
                Element mb = (Element) managedBeans.item(i);
                Map<String, Object> m = new HashMap<>();
                m.put("name", getTagValue(mb, "managed-bean-name"));
                m.put("class", getTagValue(mb, "managed-bean-class"));
                m.put("scope", getTagValue(mb, "managed-bean-scope"));
                beansList.add(m);
            }
            xmlData.put("managedBeans", beansList);
        });
    }

    @Override
    public void parseApplicationXml(File output, SourceFolderDAO sourceFolderDAO) throws IOException {

        parseGenericXml(output, sourceFolderDAO, "application.xml", "application.xml", (doc, xmlData) -> {
            List<Map<String, Object>> modulesList = new ArrayList<>();
            NodeList modules = doc.getElementsByTagName("module");
            // 【新增】收集 Modules
            for (int i = 0; i < modules.getLength(); i++) {
                Element module = (Element) modules.item(i);
                Map<String, Object> moduleMap = new HashMap<>();
                String type = "";
                if (module.getElementsByTagName("web").getLength() > 0) type = "web";
                else if (module.getElementsByTagName("ejb").getLength() > 0) type = "ejb";
                else if (module.getElementsByTagName("java").getLength() > 0) type = "java";
                else type = "unknown";

                String uri = "";
                if ("web".equals(type)) uri = getTagValue(module, "web-uri");
                else if ("ejb".equals(type)) uri = getTagValue(module, "ejb");
                else if ("java".equals(type)) uri = getTagValue(module, "java");

                moduleMap.put("type", type);
                moduleMap.put("uri", uri);
                modulesList.add(moduleMap);
            }
            xmlData.put("modules", modulesList);
        });
    }

    @Override
    public void parsingJsf(File jsfFilesParseOutput, SourceFolderDAO sourceFolderDAO) throws IOException {
        Path root = Paths.get(sourceFolderDAO.getDirPath());
        if (!Files.exists(root) || !Files.isDirectory(root)) return;

        List<Path> paths = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".xhtml"))
                    .filter(p -> {
                        String lower = p.toString().toLowerCase();
                        return !lower.contains("\\build\\") && !lower.contains("/build/") &&
                                !lower.contains("\\target\\") && !lower.contains("/target/") &&
                                !lower.contains("\\.mvn\\") && !lower.contains("/.mvn/");
                    })
                    .forEach(paths::add);
        }

        if (paths.isEmpty()) {
            System.out.println("=== 无 .xhtml JSF 文件找到（跳过） ===");
            return;
        }

        for (Path path : paths) {
            File file = path.toFile();
            System.out.println("📝 记录 JSF 文件: " + file.getName());
            
            // 创建简单的 JSF 文件信息，仅记录文件路径
            JsfFileInfo jsfInfo = new JsfFileInfo();
            jsfInfo.filePath = file.getAbsolutePath();
            // namespaces, includes, beans 列表保持为空
            
            jsfFiles.add(jsfInfo);
            analysisResult.getJsfFiles().add(jsfInfo);
            
            // 添加警告 issue
            IssueInfo warning = new IssueInfo();
            warning.setSeverity("Medium");
            warning.setMessage("使用 JSF 文件可能导致扩展性问题，建议使用现代前端框架");
            warning.setLocation(file.getAbsolutePath());
            warning.setClassName(file.getName());
            warning.setSource("jsf");
            
            analysisResult.getIssues().add(warning);
            allIssues.add(warning);
        }

        // ====================== 生成 JSF JSON ======================
        Path outputRoot = root.resolve(OutputPath.OUTPUT_BASE_DIR);
        File jsfJsonFile = outputRoot.resolve(OutputPath.JSF_PARSE_RESULT_PATH).toFile();
        jsonFileService.generateJsonArray(jsfFiles, jsfJsonFile.getAbsolutePath());

        System.out.println("✅ JSF JSON 报告生成完成 → " + jsfJsonFile.getAbsolutePath());
        System.out.println("   共记录 " + paths.size() + " 个 JSF 文件（仅文件名）");
    }

    @Override
    @Transactional
    public void staticParseFiles(SourceFolderDAO sourceFolderDAO) throws IOException {

        String rootPath = sourceFolderDAO.getDirPath();
        File rootDir = new File(rootPath);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            throw new IOException("项目目录不存在: " + rootPath);
        }

        // 创建 output 目录
        File outputFolder = new File(rootPath, OutputPath.OUTPUT_BASE_DIR);
        if (!outputFolder.exists()) {
            if (!outputFolder.mkdirs()) {
                throw new IOException("无法创建 output 目录: " + outputFolder.getAbsolutePath());
            }
        }

        // 1. 定义所有报告文件路径
        File analysisOutput     = new File(outputFolder, OutputPath.JAVA_PARSE_RESULT_PATH);
        File errorLog           = new File(outputFolder, OutputPath.JAVA_PARSE_ERROR_LOG_PATH);
        File jspOutput          = new File(outputFolder, OutputPath.JSP_PARSE_RESULT_PATH);
        File jspError           = new File(outputFolder, OutputPath.JSP_PARSE_ERROR_LOG_PATH);
        File webXmlOutput       = new File(outputFolder, OutputPath.WEB_XML_PARSE_RESULT_PATH);
        File persistenceXmlOutput = new File(outputFolder, OutputPath.PERSISTENCE_PARSE_RESULT_PATH);
        File ejbJarXmlOutput    = new File(outputFolder, OutputPath.EJB_JAR_PARSE_RESULT_PATH);
        File facesConfigXmlOutput = new File(outputFolder, OutputPath.FACES_CONFIG_PARSE_RESULT_PATH);
        File applicationXmlOutput = new File(outputFolder, OutputPath.EAR_APPLICATION_PARSE_RESULT_PATH);
        File jsfOutput          = new File(outputFolder, OutputPath.JSF_PARSE_RESULT_PATH);
        File issuesOutput       = new File(outputFolder, OutputPath.ISSUES_PARSE_RESULT_PATH);

        analysisResult.getJavaClasses().clear();
        if (analysisResult.getJsfFiles() != null) analysisResult.getJsfFiles().clear();
        if (analysisResult.getXmlConfigs() != null) analysisResult.getXmlConfigs().clear();
        analysisResult.getIssues().clear();
        analysisResult.setTotalJavaFiles(0);
        analysisResult.setTotalXhtmlFiles(0);
        analysisResult.setTotalXmlFiles(0);
        analysisResult.setTotalIssues(0);
        allIssues.clear();
        jsfFiles.clear();

// 重新初始化（防止 null）
//        if (analysisResult.getXmlConfigs() == null) {
//            analysisResult.setXmlConfigs(new HashMap<>());
//        }
//        if (analysisResult.getJsfFiles() == null) {
//            analysisResult.setJsfFiles(new ArrayList<>());
//        }
//        analysisResult.setDependencyGraph(dependencyGraph);

        // 3. 依次运行所有解析（按依赖顺序）
        // 先 Java（含 JSP 生成的 Servlet）
        parsing(analysisOutput, errorLog, sourceFolderDAO);

        // 再 JSP（会调用 parseJavaFile 处理生成代码）
        parsingJsp(jspOutput, jspError, sourceFolderDAO);

        // 然后 JSF
        parsingJsf(jsfOutput, sourceFolderDAO);

        // 最后各种 XML（顺序不严格影响）
        parseWebXml(webXmlOutput, sourceFolderDAO);
        parsePersistenceXml(persistenceXmlOutput, sourceFolderDAO);
        parseEjbJarXml(ejbJarXmlOutput, sourceFolderDAO);
        parseFacesConfigXml(facesConfigXmlOutput, sourceFolderDAO);
        parseApplicationXml(applicationXmlOutput, sourceFolderDAO);

        // 生成 issues JSON 报告
        if (!allIssues.isEmpty()) {
            jsonFileService.generateJsonArray(allIssues, issuesOutput.getAbsolutePath());
            System.out.println("✅ Issues JSON 报告生成完成 → " + issuesOutput.getAbsolutePath());
            System.out.println("   共包含 " + allIssues.size() + " 个问题");
        } else {
            System.out.println("ℹ️ 未发现任何问题，跳过 Issues JSON 报告生成");
        }

        // 4. 统一生成 JSON + 存数据库


        // 5. 打印完成信息（可返回给前端）
        System.out.println("完整静态分析完成！");
        System.out.println(" - TXT 报告目录: " + outputFolder.getAbsolutePath());
        System.out.println(" - JSON 报告: analysis_result_" + sourceFolderDAO.getId() + "*.json");
        System.out.println(" - 数据库记录已更新，source_folder_id: " + sourceFolderDAO.getId());
    }

    @Override
    public void parsePomXmlFile(File pomOutput, SourceFolderDAO sourceFolderDAO) throws IOException {
        parseGenericXml(pomOutput, sourceFolderDAO, "pom.xml", "pom.xml", (doc, xmlData) -> {
            String javaVersion = getTagValue(doc, "java.version");

            xmlData.put("javaVersion", javaVersion.isEmpty() ? "unknown" : javaVersion);

            NodeList parents = doc.getElementsByTagName("parent");

            for(int i = 0; i < parents.getLength(); i++)
            {
                Element parent = (Element) parents.item(i);
                Map<String, String> parentInfo = new HashMap<>();
                parentInfo.put("groupId", getTagValue(parent, "groupId"));
                parentInfo.put("artifactId", getTagValue(parent, "artifactId"));
                parentInfo.put("version", getTagValue(parent, "version"));
                xmlData.put("parent", parentInfo);
            }

            List<String> deps = new ArrayList<>();
            NodeList dependencies = doc.getElementsByTagName("dependencies");
            for (int i = 0; i < dependencies.getLength(); i++) {
                Element dep = (Element) dependencies.item(i);
                String g = getTagValue(dep, "groupId");
                String a = getTagValue(dep, "artifactId");
                String v = getTagValue(dep, "version");
                deps.add(g + ":" + a + ":" + (v.isEmpty() ? "未指定" : v));
            }
            xmlData.put("dependencies", deps);

            List<String> plugins = new ArrayList<>();
            NodeList pluginNodes = doc.getElementsByTagName("plugin");
            for (int i = 0; i < pluginNodes.getLength(); i++) {
                Element plugin = (Element) pluginNodes.item(i);
                String artifactId = getTagValue(plugin, "artifactId");
                if (!artifactId.isEmpty()) {
                    plugins.add(artifactId);
                }
            }
            xmlData.put("plugins", plugins);

            // ==================== 应用服务器兼容性分析 ====================
            Map<String, Object> compatibility = new HashMap<>();
            
            // 1. Java 版本兼容性分析
            List<String> supportedJavaVersions = Arrays.asList("8", "11", "17", "21");
            boolean javaVersionCompatible = false;
            String javaCompatibilityAssessment = "未知";
            
            if (javaVersion.isEmpty()) {
                javaCompatibilityAssessment = "未指定 Java 版本";
                
                IssueInfo javaIssue = new IssueInfo();
                javaIssue.severity = "Medium";
                javaIssue.message = "未指定 Java 版本，可能导致构建或运行问题";
                javaIssue.location = "pom.xml";
                javaIssue.className = "pom.xml";
                javaIssue.source = "pom_analysis";
                allIssues.add(javaIssue);
            } else {
                // 提取主要版本号（例如 "1.8" -> "8", "11" -> "11"）
                String majorVersion = javaVersion;
                if (javaVersion.startsWith("1.")) {
                    majorVersion = javaVersion.substring(2);
                }
                // 移除非数字字符
                majorVersion = majorVersion.replaceAll("[^0-9]", "");
                
                if (supportedJavaVersions.contains(majorVersion)) {
                    javaVersionCompatible = true;
                    javaCompatibilityAssessment = "兼容 (Java " + majorVersion + ")";
                } else {
                    javaCompatibilityAssessment = "不兼容 (Java " + majorVersion + "，支持版本: " + String.join(", ", supportedJavaVersions) + ")";
                    
                    // 添加问题报告
                    IssueInfo javaIssue = new IssueInfo();
                    javaIssue.severity = "High";
                    javaIssue.message = "Java 版本 " + majorVersion + " 可能与应用服务器不兼容";
                    javaIssue.location = "pom.xml";
                    javaIssue.className = "pom.xml";
                    javaIssue.source = "pom_analysis";
                    allIssues.add(javaIssue);
                }
            }
            
            compatibility.put("javaVersionCompatible", javaVersionCompatible);
            compatibility.put("javaCompatibilityAssessment", javaCompatibilityAssessment);
            
            // 2. 依赖大小和性能影响分析
            int dependencyCount = deps.size();
            String dependencySizeAssessment;
            if (dependencyCount > 50) {
                dependencySizeAssessment = "大型依赖 (" + dependencyCount + " 个依赖)，可能影响 WAR 包大小和启动时间";
                
                IssueInfo depIssue = new IssueInfo();
                depIssue.severity = "Medium";
                depIssue.message = "项目包含大量依赖 (" + dependencyCount + " 个)，可能导致 WAR 包超过 500MB";
                depIssue.location = "pom.xml";
                depIssue.className = "pom.xml";
                depIssue.source = "pom_analysis";
                allIssues.add(depIssue);
            } else if (dependencyCount > 20) {
                dependencySizeAssessment = "中等依赖 (" + dependencyCount + " 个依赖)";
            } else {
                dependencySizeAssessment = "小型依赖 (" + dependencyCount + " 个依赖)";
            }
            
            compatibility.put("dependencyCount", dependencyCount);
            compatibility.put("dependencySizeAssessment", dependencySizeAssessment);
            
            // 3. Fat Jar/Uber Jar 插件检测
            boolean hasFatJarPlugin = plugins.stream().anyMatch(plugin -> 
                plugin.contains("spring-boot-maven-plugin") || 
                plugin.contains("maven-shade-plugin") ||
                plugin.contains("maven-assembly-plugin")
            );
            
            if (hasFatJarPlugin) {
                compatibility.put("fatJarWarning", "检测到 Fat Jar/Uber Jar 插件，可能不适用于传统应用服务器部署");
                
                IssueInfo fatJarIssue = new IssueInfo();
                fatJarIssue.severity = "High";
                fatJarIssue.message = "检测到 Fat Jar/Uber Jar 插件，传统应用服务器需要 WAR 部署";
                fatJarIssue.location = "pom.xml";
                fatJarIssue.className = "pom.xml";
                fatJarIssue.source = "pom_analysis";
                allIssues.add(fatJarIssue);
            } else {
                compatibility.put("fatJarWarning", "未检测到 Fat Jar 插件，适合传统应用服务器部署");
            }
            
            // 4. 整体容器兼容性评估
            boolean overallCompatible = javaVersionCompatible && !hasFatJarPlugin && dependencyCount <= 100;
            String overallAssessment;
            
            if (overallCompatible) {
                overallAssessment = "项目结构与应用服务器兼容性良好";
            } else {
                overallAssessment = "项目可能存在与应用服务器的兼容性问题";
                
                IssueInfo overallIssue = new IssueInfo();
                overallIssue.severity = "Medium";
                overallIssue.message = "项目结构可能与应用服务器存在兼容性问题";
                overallIssue.location = "pom.xml";
                overallIssue.className = "pom.xml";
                overallIssue.source = "pom_analysis";
                allIssues.add(overallIssue);
            }
            
            compatibility.put("overallCompatible", overallCompatible);
            compatibility.put("overallAssessment", overallAssessment);
            
            xmlData.put("containerCompatibility", compatibility);

            NodeList profiles = doc.getElementsByTagName("profile");
            xmlData.put("profileCount", profiles.getLength());

        });
    }

    // Helper method for getTagValue (Element)
    private String getTagValue(Element element, String tagName) {
        NodeList list = element.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            Node node = list.item(0);
            return node.getTextContent().trim();
        }
        return "";
    }

    // Helper method for getTagValue (Document)
    private String getTagValue(Document doc, String tagName) {
        NodeList list = doc.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            return list.item(0).getTextContent().trim();
        }
        return "";
    }




    /**
     * 通用 XML 解析方法（大幅减少重复代码）
     * @param outputFile       TXT 输出文件
     * @param sourceFolderDAO  项目信息
     * @param xmlType          类型名称（如 "persistence.xml"）
     * @param fileNamePattern  要查找的文件名（如 "persistence.xml"）
     * @param dataParser       具体解析逻辑（把 Document 转成 Map）
     */
    private void parseGenericXml(
            File outputFile,              // 改名更清楚：這是要輸出的 JSON 檔案
            SourceFolderDAO sourceFolderDAO,
            String xmlType,
            String fileNamePattern,
            BiConsumer<Document, Map<String, Object>> dataParser) throws IOException {

        List<XmlFileInfo> xmlFileInfos = new ArrayList<>();
        String projectDir = sourceFolderDAO.getDirPath();

        // 1. 掃描找到所有符合檔名的 XML 檔案
        List<Path> xmlPaths = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(Paths.get(projectDir))) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equalsIgnoreCase(fileNamePattern))
                    .filter(p -> {
                        String lower = p.toString().toLowerCase();
                        return !lower.contains("\\build\\") && !lower.contains("/build/") &&
                                !lower.contains("\\target\\") && !lower.contains("/target/") &&
                                !lower.contains("\\.mvn\\") && !lower.contains("/.mvn/");
                    })
                    .forEach(xmlPaths::add);
        }

        if (xmlPaths.isEmpty()) {
            System.out.println("=== " + xmlType + " 未找到（跳過） ===");
            return;
        }

        System.out.println("找到 " + xmlPaths.size() + " 個 " + xmlType + " 檔案");

        for (Path path : xmlPaths) {
            XmlFileInfo xmlFileInfo = new XmlFileInfo();
            Map<String, Object> xmlData = new HashMap<>();

            File xmlFile = path.toFile();
            xmlFileInfo.setFilePath(xmlFile.getAbsolutePath());
            xmlFileInfo.setFileType(xmlType);

            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(xmlFile);
                doc.getDocumentElement().normalize();

                dataParser.accept(doc, xmlData);

                // 把解析結果綁定到 XmlFileInfo（很重要！）
                xmlFileInfo.setData(xmlData);   // 假設 XmlFileInfo 有 setData(Map) 方法

            } catch (Exception e) {
                System.out.println(xmlType + " 解析失敗: " + xmlFile.getName() + " → " + e.getMessage());
            }

            xmlFileInfos.add(xmlFileInfo);
        }

        // 3. 存储到 analysisResult.xmlConfigs
        analysisResult.getXmlConfigs().computeIfAbsent(xmlType, k -> new ArrayList<>()).addAll(xmlFileInfos);

        // 2. 真正使用 outputJsonFile 來寫入結果
        // 確保父目錄存在
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("無法創建輸出目錄: " + parentDir.getAbsolutePath());
            }
        }

        jsonFileService.generateJsonArray(xmlFileInfos, outputFile.getAbsolutePath());

        System.out.println("✅ " + xmlType + " 解析完成，已寫入 → " + outputFile.getAbsolutePath());
    }

}

package Common.Util;

import Common.ClassInfos.*;
import Common.JasperJspAnalyzer;
import Common.JsfAnalyzer;
import Common.JsfFileInfo.JsfFileInfo;
import Common.JsfMigrationIssueDetector;
import Common.JspContentAnalyzer;
import Common.JspFileInfo;
import Common.OutputPath;
import Common.XmlFileInfo.XmlFileInfo;
import com.hybriddependcyanlysis.POJO.AnalysisResultReport;
import com.hybriddependcyanlysis.POJO.DAO.SourceFolderDAO;
import com.hybriddependcyanlysis.Service.JsonFileService;

import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtReference;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParsingUtil {

    private final JsonFileService jsonFileService;

    private List<JavaClassInfo> javaClasses = new ArrayList<>();
    private List<JspFileInfo> jspInfos = new ArrayList<>();

    public AnalysisResultReport analysisResult = new AnalysisResultReport();
    private List<IssueInfo> allIssues = new ArrayList<>();
    List<JsfFileInfo> jsfFiles = new ArrayList<>();

    public ParsingUtil(JsonFileService jsonFileService) {
        this.jsonFileService = jsonFileService;
    }

    public void parsing(File serverOutput, File serverError, SourceFolderDAO sourceFolderDAO) throws IOException {
        javaClasses.clear();

        Path root = Paths.get(sourceFolderDAO.getDirPath());
        if (!Files.exists(root) || !Files.isDirectory(root)) return;
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".java"))
                    .forEach(p -> {
                        try {
                            parseJavaFile(p.toFile());
                         } catch (Exception e) {
                             System.err.println("解析Java文件失败: " + p + " - " + e.getMessage());
                         }
                    });
        }

        Path outputRoot = root.resolve(OutputPath.OUTPUT_BASE_DIR);
        File jsonFile = outputRoot.resolve(OutputPath.JAVA_PARSE_RESULT_PATH).toFile();
        jsonFileService.generateJsonArray(javaClasses, jsonFile.getAbsolutePath());
    }

    public void parsingJsp(File outputLog, File errorLog, SourceFolderDAO sourceFolderDAO) throws IOException {
        Path root = Paths.get(sourceFolderDAO.getDirPath());
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            System.out.println("项目目录不存在，跳过 JSP 解析");
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
                                parseJspFiles(p.toFile());
                            }
                        } catch (IOException ignored) {}
                    });
        }

        System.out.println("=== JSP 扫描结束 ===\n");

        Path outputRoot = root.resolve(OutputPath.OUTPUT_BASE_DIR);
        File jspJsonFile = outputRoot.resolve(OutputPath.JSP_PARSE_RESULT_PATH).toFile();

        jsonFileService.generateJsonArray(jspInfos, jspJsonFile.getAbsolutePath());

        System.out.println("JSP JSON 报告生成完成 → " + jspJsonFile.getAbsolutePath());
        System.out.println("   共包含 " + jspInfos.size() + " 个 JSP 文件");
    }

    public void parseJavaFile(File javaFile) throws IOException {
        try {
            Launcher launcher = new Launcher();
            launcher.getEnvironment().setNoClasspath(true);
            launcher.addInputResource(javaFile.getAbsolutePath());
            launcher.buildModel();

            CtModel model = launcher.getModel();

            for (CtType<?> type : model.getAllTypes()) {
                JavaClassInfo javaClassInfo = new JavaClassInfo();
                parseClass(type, javaClassInfo);
            }
        } catch (Exception e) {
            System.err.println("Parsing Java file failed (Spoon): " + javaFile.getAbsolutePath() + " - " + e.getMessage());
        }
    }

    public void parseClass(CtType<?> type, JavaClassInfo javaClassInfo) throws IOException {
        if (javaClassInfo == null) {
            javaClassInfo = new JavaClassInfo();
        }

        Set<String> classDeps = new HashSet<>();

        javaClassInfo.setFullName(type.getQualifiedName());
        javaClassInfo.setSimpleName(type.getSimpleName());
        javaClassInfo.setPackageName(type.getPackage() != null ? type.getPackage().getQualifiedName() : "(default)");
        javaClassInfo.setKind((type instanceof CtInterface) ? "Interface" : "Class");

        List<String> modifiers = type.getModifiers().stream()
                .map(ModifierKind::toString)
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        javaClassInfo.setModifiers(modifiers.isEmpty() ? null : modifiers);

        List<AnnotationInfo> annotations = new ArrayList<>();
        for (CtAnnotation<?> annotation : type.getAnnotations()) {
            AnnotationInfo annInfo = new AnnotationInfo();
            annInfo.setName(annotation.getAnnotationType().getQualifiedName());
            annotation.getValues().forEach((k, v) -> annInfo.addValue(k, v.toString()));
            annotations.add(annInfo);
        }
        javaClassInfo.setAnnotations(annotations.isEmpty() ? null : annotations);

        List<FieldInfo> fields = new ArrayList<>();
        for (CtField<?> field : type.getFields()) {
            FieldInfo f = new FieldInfo();
            f.name = field.getSimpleName();
            f.type = field.getType() != null ? field.getType().getQualifiedName() : "(unresolved)";
            f.modifiers = field.getModifiers().stream()
                    .map(ModifierKind::toString)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            List<AnnotationInfo> fieldAnnotations = new ArrayList<>();
            for (CtAnnotation<?> ann : field.getAnnotations()) {
                AnnotationInfo annInfo = new AnnotationInfo();
                annInfo.setName(ann.getAnnotationType().getQualifiedName());
                fieldAnnotations.add(annInfo);
            }
            f.annotations = fieldAnnotations.isEmpty() ? null : fieldAnnotations;
            fields.add(f);
        }
        javaClassInfo.setFields(fields.isEmpty() ? null : fields);

        for (CtMethod<?> method : type.getMethods()) {
            try {
                parseMethodWithDeps(method, javaClassInfo, classDeps);
            } catch (IOException e) {
                System.err.println("解析方法失败: " + method.getSimpleName() + " - " + e.getMessage());
            }
        }
        if (javaClassInfo.getMethods() != null && javaClassInfo.getMethods().isEmpty()) {
            javaClassInfo.setMethods(null);
        }

        detectConstructorIssues(type, classDeps);
        detectMemoryReplicationIssues(type, classDeps);

        List<String> importList = new ArrayList<>();
        try {
            CtCompilationUnit cu = type.getPosition().getCompilationUnit();
            if (cu == null) {
                System.out.println("无法通过position获取编译单元，尝试工厂方法: " + type.getQualifiedName());
                cu = type.getFactory().CompilationUnit().getOrCreate(type);
            }

            if (cu != null) {
                System.out.println("找到编译单元，提取imports: " + type.getQualifiedName());
                List<CtImport> imports = cu.getImports();
                System.out.println("  找到 " + imports.size() + " 个import语句");

                for (int i = 0; i < imports.size(); i++) {
                    CtImport imp = imports.get(i);
                    System.out.println("  处理第 " + (i+1) + " 个import: " + imp.toString());

                    CtReference ref = imp.getReference();
                    if (ref != null) {
                        String importName = ref.getSimpleName();
                        String qualifiedName = null;
                        try {
                            if (ref instanceof CtTypeReference) {
                                qualifiedName = ((CtTypeReference<?>) ref).getQualifiedName();
                            } else if (ref instanceof CtPackageReference) {
                                qualifiedName = ((CtPackageReference) ref).getQualifiedName();
                            } else if (ref instanceof CtExecutableReference) {
                                qualifiedName = ((CtExecutableReference<?>) ref).getSignature();
                            }
                        } catch (Exception e) {
                            System.out.println("  获取完整限定名失败: " + e.getMessage());
                        }

                        importList.add(qualifiedName != null ? qualifiedName : importName);
                        System.out.println("  → 导入[" + (i+1) + "]: " + importName +
                                         (qualifiedName != null ? " (完整: " + qualifiedName + ")" : ""));
                    } else {
                        String importString = imp.toString();
                        if (importString.startsWith("import ")) {
                            String importContent = importString.substring(7).trim();
                            if (importContent.endsWith(";")) {
                                importContent = importContent.substring(0, importContent.length() - 1);
                            }
                            importList.add(importContent);
                            System.out.println("  → 无法解析的导入[" + (i+1) + "]，使用字符串: " + importContent);
                        } else {
                            importList.add("(unresolved import: " + importString + ")");
                            System.out.println("  → 无法解析的导入[" + (i+1) + "]: " + importString);
                        }
                    }
                }
            } else {
                System.err.println("无法获取编译单元: " + type.getQualifiedName());
            }
        } catch (Exception e) {
            System.err.println("提取 imports 失败: " + type.getQualifiedName() + " - " + e.getMessage());
            e.printStackTrace();
        }

        javaClassInfo.setImports(importList.isEmpty() ? null : importList);
        javaClassInfo.setIssues(null);

        javaClasses.add(javaClassInfo);
    }

    public void parseClass(CtType<?> type) throws IOException {
        parseClass(type, new JavaClassInfo());
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

    public void parseMethod(CtMethod<?> method, JavaClassInfo classInfo) throws IOException {
        parseMethodWithDeps(method, classInfo, new HashSet<>());
    }

    public void parsePackage(CtModel model, BufferedWriter writer) throws IOException {
    }

    public void detectInvocationIssues(CtMethod<?> method, Set<String> classDeps) throws IOException {
        String className = method.getDeclaringType().getQualifiedName();

        boolean isGeneratedJsp = className.contains("_jsp") || className.contains("org.apache.jsp");

        method.filterChildren(new TypeFilter<>(CtInvocation.class)).forEach(inv -> {
            try {
                CtInvocation<?> call = (CtInvocation<?>) inv;
                CtExecutableReference<?> exec = call.getExecutable();
                CtTypeReference<?> declaringType = exec.getDeclaringType();

                String declaringTypeName = (declaringType != null) ? declaringType.getQualifiedName() : "(unknown)";
                String signature = exec.getSignature();
                String methodName = exec.getSimpleName();

                if (declaringType != null) {
                    classDeps.add(declaringType.getQualifiedName());
                }

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

                    } else if ("lookup".equals(methodName) &&
                            (declaringTypeName.startsWith("javax.naming") || declaringTypeName.startsWith("jakarta.naming"))) {
                        shouldReport = true;
                        issueType = "JndiLookup (hardcoded JNDI name likely)";
                    }
                    else if (declaringTypeName.equals("java.lang.ThreadLocal") &&
                            ("set".equals(methodName) || "get".equals(methodName))) {
                        shouldReport = true;
                        issueType = "ThreadLocalUsage (may cause memory leak in pooled threads)";
                    }
                }

                if (shouldReport) {
                    IssueInfo issue = new IssueInfo();
                    issue.severity = issueType.contains("High") ? "High" : "Medium";
                    issue.message = "Uses " + declaringTypeName + "." + signature + " → " + issueType;
                    issue.location = "";
                    issue.className = className;
                    issue.source = "invocation";
                    allIssues.add(issue);
                }

            } catch (Exception e) {
                System.err.println("检测调用问题时出错: " + e.getMessage());
            }
        });
    }

    public void detectConstructorIssues(CtType<?> type, Set<String> classDeps) throws IOException {
        type.filterChildren(new TypeFilter<>(CtConstructorCall.class)).forEach(ctor -> {
            try {
                CtConstructorCall<?> call = (CtConstructorCall<?>) ctor;
                CtTypeReference<?> ref = call.getType();
                String typeName = (ref != null) ? ref.getQualifiedName() : "(unknown)";
                int line = call.getPosition() != null ? call.getPosition().getLine() : 0;
                String loc = type.getQualifiedName() + ":" + line;

                if (ref != null) {
                    classDeps.add(ref.getQualifiedName());
                }

                switch (typeName) {
                    case "javax.servlet.http.HttpSession":
                        IssueInfo issue1 = new IssueInfo();
                        issue1.severity = "High";
                        issue1.message = "Uses HttpSession → StatefulSession";
                        issue1.location = loc;
                        issue1.className = type.getQualifiedName();
                        issue1.source = loc;
                        issue1.type = "StatefulSession";
                        allIssues.add(issue1);
                        break;

                    case "java.io.FileInputStream":
                    case "java.io.File":
                    case "java.io.FileReader":
                    case "java.io.FileOutputStream":
                    case "java.io.FileWriter":
                    case "java.io.RandomAccessFile":
                        IssueInfo issue2 = new IssueInfo();
                        issue2.severity = "High";
                        issue2.message = "Uses " + typeName + " → FileSystemDependency";
                        issue2.location = loc;
                        issue2.className = type.getQualifiedName();
                        issue2.source = loc;
                        issue2.type = "FileSystemDependency";
                        allIssues.add(issue2);
                        break;

                    case "javax.naming.InitialContext":
                    case "jakarta.naming.InitialContext":
                        IssueInfo issue3 = new IssueInfo();
                        issue3.severity = "High";
                        issue3.message = "Uses " + typeName + " → JNDIDependency";
                        issue3.location = loc;
                        issue3.className = type.getQualifiedName();
                        issue3.source = loc;
                        issue3.type = "JNDIDependency";
                        allIssues.add(issue3);
                        break;

                    case "java.net.Socket":
                    case "java.net.ServerSocket":
                    case "java.net.DatagramSocket":
                        IssueInfo issue4 = new IssueInfo();
                        issue4.severity = "High";
                        issue4.message = "Uses Socket → PortBindingRisk";
                        issue4.location = loc;
                        issue4.className = type.getQualifiedName();
                        issue4.source = loc;
                        issue4.type = "PortBindingRisk";
                        allIssues.add(issue4);
                        break;

                    case "java.lang.ThreadLocal":
                        IssueInfo issue5 = new IssueInfo();
                        issue5.severity = "High";
                        issue5.message = "Uses ThreadLocal → ThreadLocalUse";
                        issue5.location = loc;
                        issue5.className = type.getQualifiedName();
                        issue5.source = loc;
                        issue5.type = "ThreadLocalUse";
                        allIssues.add(issue5);
                        break;
                }
            } catch (Exception e) {
                System.err.println("检测构造器问题时出错: " + e.getMessage());
            }
        });
    }

    public void detectMemoryReplicationIssues(CtType<?> type, Set<String> classDeps) throws IOException {
        String className = type.getQualifiedName();
        String classLoc = className;

        for (CtTypeReference<?> interfaceRef : type.getSuperInterfaces()) {
            String interfaceName = interfaceRef.getQualifiedName();
            if (interfaceName.equals("java.io.Serializable")) {
                IssueInfo issue = new IssueInfo();
                issue.severity = "Medium";
                issue.message = "Implements Serializable - Object serialization may cause memory replication";
                issue.location = classLoc;
                issue.className = className;
                issue.source = "memory_replication";
                issue.type = "Serializable";
                allIssues.add(issue);
                break;
            }
            else if (interfaceName.equals("java.lang.Cloneable")) {
                IssueInfo issue = new IssueInfo();
                issue.severity = "Medium";
                issue.message = "Implements Cloneable - Object cloning may cause memory replication";
                issue.location = classLoc;
                issue.className = className;
                issue.source = "memory_replication";
                issue.type = "Cloneable";
                allIssues.add(issue);
                break;
            }
        }

        type.filterChildren(new TypeFilter<>(CtInvocation.class)).forEach(inv -> {
            CtInvocation<?> invocation = (CtInvocation<?>) inv;
            CtExecutableReference<?> execRef = invocation.getExecutable();
            if (execRef != null && execRef.getSignature().contains("java.nio.channels.FileChannel.map") && "map".equals(execRef.getSimpleName())) {
                int line = invocation.getPosition() != null ? invocation.getPosition().getLine() : 0;
                IssueInfo issue = new IssueInfo();
                issue.severity = "High";
                issue.message = "Uses FileChannel.map() - Memory-mapped files can cause memory replication";
                issue.location = className + ":" + line;
                issue.className = className;
                issue.source = "memory_replication";
                issue.type = "MemoryMappedFile";
                allIssues.add(issue);
            }
        });

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
                    issue.location = classLoc;
                    issue.className = className;
                    issue.source = "memory_replication";
                    issue.type = "DistributedCaching";
                    allIssues.add(issue);
                    break;
                }
            }
        }

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
                    issue.location = classLoc;
                    issue.className = className;
                    issue.source = "memory_replication";
                    issue.type = "RMI";
                    allIssues.add(issue);
                    break;
                }
            }
        }

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
                    issue.location = classLoc;
                    issue.className = className;
                    issue.source = "memory_replication";
                    issue.type = "JMS";
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

    public void parseJspFiles(File jspFile) {
        JspFileInfo jspAnalysisResult;
        boolean usingJasper = false;

        try {
            System.out.println("📝 [Jasper] 分析 JSP 文件: " + jspFile.getName());
            jspAnalysisResult = JasperJspAnalyzer.analyze(jspFile.toPath());
            usingJasper = true;

            System.out.println("✅ [Jasper] 分析成功: " + jspFile.getName());
            System.out.println("   引入的命名空间: " + jspAnalysisResult.namespaces.size());
            System.out.println("   Java代码块: " + jspAnalysisResult.getJavaCodeBlockCount());
            System.out.println("   自定义标签: " + jspAnalysisResult.getCustomTaglibs().size());

        } catch (Exception e) {
            System.out.println("⚠️ [Jasper] 分析失败，回退到正则方案: " + jspFile.getName() + " → " + e.getMessage());

            try {
                jspAnalysisResult = JspContentAnalyzer.analyzeJspContent(jspFile);
            } catch (Exception ex) {
                System.out.println("❌ 正则回退也失败: " + ex.getMessage());
                return;
            }
        }

        jspInfos.add(jspAnalysisResult);

        IssueInfo warning = new IssueInfo();
        warning.setSeverity("Medium");
        warning.setMessage("使用 JSP 文件可能导致扩展性问题，建议使用现代前端技术");
        warning.setLocation(jspFile.getAbsolutePath());
        warning.setClassName(jspFile.getName());
        warning.setSource("jsp");

        analysisResult.getIssues().add(warning);
        allIssues.add(warning);

        String tag = usingJasper ? "Jasper" : "Regex";
        System.out.println("✅ [" + tag + "] JSP 内容分析完成: " + jspFile.getName());
        System.out.println("   代码块数量: " + jspAnalysisResult.getJavaCodeBlockCount());
        System.out.println("   代码行数: " + jspAnalysisResult.getJavaCodeLineCount());
        System.out.println("   自定义标签: " + jspAnalysisResult.getCustomTaglibs().size());
        System.out.println("   服务器引用: " + jspAnalysisResult.getServerSpecificReferences().size());
        System.out.println("   EL表达式: " + jspAnalysisResult.getElExpressions().size());
    }

    public void parseWebXml(File webXmlParseoutput, SourceFolderDAO sourceFolderDAO) throws IOException {
        parseGenericXml(
                webXmlParseoutput,
                sourceFolderDAO,
                "web.xml",
                "web.xml",
                (doc, xmlData) -> {
                    List<Map<String, Object>> securityList = new ArrayList<>();
                    NodeList securityConstraints = doc.getElementsByTagName("security-constraint");
                    for (int i = 0; i < securityConstraints.getLength(); i++) {
                        Element sc = (Element) securityConstraints.item(i);

                        Element wrc = (Element) sc.getElementsByTagName("web-resource-collection").item(0);
                        if (wrc == null) continue;

                        Map<String, Object> scMap = new HashMap<>();
                        scMap.put("resourceName", getTagValue(wrc, "web-resource-name"));

                        List<String> urlPatterns = new ArrayList<>();
                        NodeList urlNodes = wrc.getElementsByTagName("url-pattern");
                        for (int j = 0; j < urlNodes.getLength(); j++) {
                            urlPatterns.add(urlNodes.item(j).getTextContent().trim());
                        }
                        scMap.put("urlPatterns", urlPatterns);

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

                    xmlData.put("keyConfigs", keyConfigs);

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

                    List<Map<String, Object>> filters = new ArrayList<>();
                    NodeList filterList = doc.getElementsByTagName("filter");
                    for (int i = 0; i < filterList.getLength(); i++) {
                        Element f = (Element) filterList.item(i);
                        Map<String, Object> fm = new HashMap<>();
                        fm.put("filterName", getTagValue(f, "filter-name"));
                        fm.put("filterClass", getTagValue(f, "filter-class"));
                        filters.add(fm);
                    }
                    xmlData.put("filters", filters);

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

                    List<String> listeners = new ArrayList<>();
                    NodeList listenerList = doc.getElementsByTagName("listener");
                    for (int i = 0; i < listenerList.getLength(); i++) {
                        listeners.add(getTagValue((Element) listenerList.item(i), "listener-class"));
                    }
                    xmlData.put("listeners", listeners);

                    Map<String, Object> sessionConfig = new HashMap<>();
                    NodeList scList = doc.getElementsByTagName("session-config");
                    if (scList.getLength() > 0) {
                        Element sc = (Element) scList.item(0);
                        sessionConfig.put("sessionTimeoutMinutes", getTagValue(sc, "session-timeout"));
                    }
                    xmlData.put("sessionConfig", sessionConfig);

                    List<String> welcomeFiles = new ArrayList<>();
                    NodeList wfList = doc.getElementsByTagName("welcome-file");
                    for (int i = 0; i < wfList.getLength(); i++) {
                        welcomeFiles.add(wfList.item(i).getTextContent().trim());
                    }
                    xmlData.put("welcomeFiles", welcomeFiles);

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

                    Map<String, Object> loginConfig = new HashMap<>();
                    NodeList lcList = doc.getElementsByTagName("login-config");
                    if (lcList.getLength() > 0) {
                        Element lc = (Element) lcList.item(0);
                        loginConfig.put("authMethod", getTagValue(lc, "auth-method"));
                        loginConfig.put("formLoginPage", getTagValue(lc, "form-login-page"));
                    }
                    xmlData.put("loginConfig", loginConfig);

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

    public void parsePersistenceXml(File output, SourceFolderDAO sourceFolderDAO) throws IOException {
        parseGenericXml(output, sourceFolderDAO, "persistence.xml", "persistence.xml", (doc, xmlData) -> {
            List<Map<String, Object>> unitsList = new ArrayList<>();
            NodeList units = doc.getElementsByTagName("persistence-unit");

            for (int i = 0; i < units.getLength(); i++) {
                Element unit = (Element) units.item(i);
                Map<String, Object> m = new HashMap<>();

                m.put("name", unit.getAttribute("name"));
                m.put("jtaDataSource", getTagValue(unit, "jta-data-source"));
                m.put("nonJtaDataSource", getTagValue(unit, "non-jta-data-source"));

                List<String> classList = new ArrayList<>();
                NodeList classNodes = unit.getElementsByTagName("class");
                for (int j = 0; j < classNodes.getLength(); j++) {
                    String className = classNodes.item(j).getTextContent().trim();
                    if (!className.isEmpty()) {
                        classList.add(className);
                    }
                }
                m.put("classes", classList);

                m.put("properties", parseProperties(unit));

                unitsList.add(m);
            }

            xmlData.put("persistenceUnits", unitsList);
        });
    }

    private Map<String, String> parseProperties(Element persistenceUnit) {
        Map<String, String> props = new HashMap<>();

        NodeList propContainers = persistenceUnit.getElementsByTagName("properties");
        if (propContainers.getLength() == 0) {
            return props;
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

    public void parseEjbJarXml(File output, SourceFolderDAO sourceFolderDAO) throws IOException {
        parseGenericXml(output, sourceFolderDAO, "ejb-jar.xml", "ejb-jar.xml", (doc, xmlData) -> {
            List<Map<String, Object>> sessionBeans = new ArrayList<>();
            NodeList sessions = doc.getElementsByTagName("session");
            for (int i = 0; i < sessions.getLength(); i++) {
                Element session = (Element) sessions.item(i);
                Map<String, Object> m = new HashMap<>();
                m.put("ejbName", getTagValue(session, "ejb-name"));
                m.put("sessionType", getTagValue(session, "session-type"));
                m.put("transactionType", getTagValue(session, "transaction-type"));
                sessionBeans.add(m);
            }
            xmlData.put("sessionBeans", sessionBeans);

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

            List<Map<String, Object>> transactions = new ArrayList<>();
            NodeList txNodes = doc.getElementsByTagName("container-transaction");
            for (int i = 0; i < txNodes.getLength(); i++) {
                Element tx = (Element) txNodes.item(i);
                Map<String, Object> m = new HashMap<>();
                m.put("transAttribute", getTagValue(tx, "trans-attribute"));
                transactions.add(m);
            }
            xmlData.put("containerTransactions", transactions);

            xmlData.put("ejbClientJar", getTagValue(doc, "ejb-client-jar"));

            List<Map<String, Object>> interceptors = new ArrayList<>();
            NodeList interceptorNodes = doc.getElementsByTagName("interceptor");
            for (int i = 0; i < interceptorNodes.getLength(); i++) {
                Element interceptor = (Element) interceptorNodes.item(i);
                Map<String, Object> m = new HashMap<>();
                m.put("interceptorClass", getTagValue(interceptor, "interceptor-class"));
                interceptors.add(m);
            }
            xmlData.put("interceptors", interceptors);

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

            List<Map<String, Object>> entityBeans = new ArrayList<>();
            NodeList entityNodes = doc.getElementsByTagName("entity");
            for (int i = 0; i < entityNodes.getLength(); i++) {
                Element entity = (Element) entityNodes.item(i);
                Map<String, Object> m = new HashMap<>();
                m.put("ejbName", getTagValue(entity, "ejb-name"));
                m.put("persistenceType", getTagValue(entity, "persistence-type"));
                m.put("primKeyClass", getTagValue(entity, "prim-key-class"));
                entityBeans.add(m);
            }
            xmlData.put("entityBeans", entityBeans);
        });
    }

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

            List<String> phaseListeners = new ArrayList<>();
            NodeList plNodes = doc.getElementsByTagName("phase-listener");
            for (int i = 0; i < plNodes.getLength(); i++) {
                phaseListeners.add(plNodes.item(i).getTextContent().trim());
            }
            xmlData.put("phaseListeners", phaseListeners);

            Element lifecycleEl = (Element) doc.getElementsByTagName("lifecycle").item(0);
            if (lifecycleEl != null) {
                NodeList vhNodes = lifecycleEl.getElementsByTagName("view-handler");
                List<String> viewHandlers = new ArrayList<>();
                for (int i = 0; i < vhNodes.getLength(); i++) {
                    viewHandlers.add(vhNodes.item(i).getTextContent().trim());
                }
                xmlData.put("viewHandlers", viewHandlers);
            }
        });
    }

    public void parseApplicationXml(File output, SourceFolderDAO sourceFolderDAO) throws IOException {
        parseGenericXml(output, sourceFolderDAO, "application.xml", "application.xml", (doc, xmlData) -> {
            List<Map<String, Object>> modulesList = new ArrayList<>();
            NodeList modules = doc.getElementsByTagName("module");
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
            System.out.println("分析 JSF 文件: " + file.getName());

            JsfFileInfo jsfInfo = JsfAnalyzer.analyze(file);

            jsfFiles.add(jsfInfo);
            analysisResult.getJsfFiles().add(jsfInfo);

            List<IssueInfo> detected = JsfMigrationIssueDetector.detect(jsfInfo);
            for (IssueInfo issue : detected) {
                analysisResult.getIssues().add(issue);
                allIssues.add(issue);
            }
        }

        List<XmlFileInfo> facesConfigs = analysisResult.getXmlConfigs().get("faces-config.xml");
        if (facesConfigs != null) {
            for (XmlFileInfo fc : facesConfigs) {
                Map<String, Object> data = fc.data;
                if (data == null) continue;

                List<String> phaseListeners = (List<String>) data.getOrDefault("phaseListeners", List.of());
                for (String pl : phaseListeners) {
                    IssueInfo issue = new IssueInfo();
                    issue.setSeverity("High");
                    issue.setMessage("Custom PhaseListener detected: " + pl + " — tightly coupled to JSF request lifecycle, prevents stateless cloud migration");
                    issue.setLocation(fc.getFilePath());
                    issue.setClassName(pl);
                    issue.setSource("jsf");
                    issue.setType("StatefulSession");
                    analysisResult.getIssues().add(issue);
                    allIssues.add(issue);
                }

                List<String> viewHandlers = (List<String>) data.getOrDefault("viewHandlers", List.of());
                for (String vh : viewHandlers) {
                    IssueInfo issue = new IssueInfo();
                    issue.setSeverity("High");
                    issue.setMessage("Custom ViewHandler detected: " + vh + " — tightly coupled to JSF rendering lifecycle, requires refactoring for cloud-native frontend");
                    issue.setLocation(fc.getFilePath());
                    issue.setClassName(vh);
                    issue.setSource("jsf");
                    issue.setType("StatefulSession");
                    analysisResult.getIssues().add(issue);
                    allIssues.add(issue);
                }
            }
        }

        Path outputRoot = root.resolve(OutputPath.OUTPUT_BASE_DIR);
        File jsfJsonFile = outputRoot.resolve(OutputPath.JSF_PARSE_RESULT_PATH).toFile();
        jsonFileService.generateJsonArray(jsfFiles, jsfJsonFile.getAbsolutePath());

        System.out.println("JSF JSON 报告生成完成 → " + jsfJsonFile.getAbsolutePath());
        System.out.println("   共分析 " + paths.size() + " 个 JSF 文件");
    }

    @Transactional
    public void staticParseFiles(SourceFolderDAO sourceFolderDAO) throws IOException {
        String rootPath = sourceFolderDAO.getDirPath();
        File rootDir = new File(rootPath);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            throw new IOException("项目目录不存在: " + rootPath);
        }

        File outputFolder = new File(rootPath, OutputPath.OUTPUT_BASE_DIR);
        if (!outputFolder.exists()) {
            if (!outputFolder.mkdirs()) {
                throw new IOException("无法创建 output 目录: " + outputFolder.getAbsolutePath());
            }
        }

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
        jspInfos.clear();

        parsing(analysisOutput, errorLog, sourceFolderDAO);

        parsingJsp(jspOutput, jspError, sourceFolderDAO);

        parseFacesConfigXml(facesConfigXmlOutput, sourceFolderDAO);

        parsingJsf(jsfOutput, sourceFolderDAO);

        parseWebXml(webXmlOutput, sourceFolderDAO);
        parsePersistenceXml(persistenceXmlOutput, sourceFolderDAO);
        parseEjbJarXml(ejbJarXmlOutput, sourceFolderDAO);
        parseApplicationXml(applicationXmlOutput, sourceFolderDAO);

        if (!allIssues.isEmpty()) {
            jsonFileService.generateJsonArray(allIssues, issuesOutput.getAbsolutePath());
            System.out.println("Issues JSON 报告生成完成 → " + issuesOutput.getAbsolutePath());
            System.out.println("   共包含 " + allIssues.size() + " 个问题");
        } else {
            System.out.println("未发现任何问题，跳过 Issues JSON 报告生成");
        }

        System.out.println("完整静态分析完成！");
        System.out.println(" - TXT 报告目录: " + outputFolder.getAbsolutePath());
        System.out.println(" - JSON 报告: analysis_result_" + sourceFolderDAO.getId() + "*.json");
        System.out.println(" - 数据库记录已更新，source_folder_id: " + sourceFolderDAO.getId());
    }

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

            Map<String, Object> compatibility = new HashMap<>();

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
                String majorVersion = javaVersion;
                if (javaVersion.startsWith("1.")) {
                    majorVersion = javaVersion.substring(2);
                }
                majorVersion = majorVersion.replaceAll("[^0-9]", "");

                if (supportedJavaVersions.contains(majorVersion)) {
                    javaVersionCompatible = true;
                    javaCompatibilityAssessment = "兼容 (Java " + majorVersion + ")";
                } else {
                    javaCompatibilityAssessment = "不兼容 (Java " + majorVersion + "，支持版本: " + String.join(", ", supportedJavaVersions) + ")";

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

    private String getTagValue(Element element, String tagName) {
        NodeList list = element.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            Node node = list.item(0);
            return node.getTextContent().trim();
        }
        return "";
    }

    private String getTagValue(Document doc, String tagName) {
        NodeList list = doc.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            return list.item(0).getTextContent().trim();
        }
        return "";
    }

    private void parseGenericXml(
            File outputFile,
            SourceFolderDAO sourceFolderDAO,
            String xmlType,
            String fileNamePattern,
            BiConsumer<Document, Map<String, Object>> dataParser) throws IOException {

        List<XmlFileInfo> xmlFileInfos = new ArrayList<>();
        String projectDir = sourceFolderDAO.getDirPath();

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
            System.out.println("=== " + xmlType + " 未找到（跳过） ===");
            return;
        }

        System.out.println("找到 " + xmlPaths.size() + " 个 " + xmlType + " 文件");

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

                xmlFileInfo.setData(xmlData);

            } catch (Exception e) {
                System.out.println(xmlType + " 解析失败: " + xmlFile.getName() + " → " + e.getMessage());
            }

            xmlFileInfos.add(xmlFileInfo);
        }

        analysisResult.getXmlConfigs().computeIfAbsent(xmlType, k -> new ArrayList<>()).addAll(xmlFileInfos);

        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("无法创建输出目录: " + parentDir.getAbsolutePath());
            }
        }

        jsonFileService.generateJsonArray(xmlFileInfos, outputFile.getAbsolutePath());

        System.out.println(xmlType + " 解析完成，已写入 → " + outputFile.getAbsolutePath());
    }
}

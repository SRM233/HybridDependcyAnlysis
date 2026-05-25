package Common.Util;

import Common.ClassInfos.*;
import Common.JasperJspAnalyzer;
import Common.JsfAnalyzer;
import Common.JsfFileInfo.JsfFileInfo;
import Common.JsfMigrationIssueDetector;
import Common.JspContentAnalyzer;
import Common.JspFileInfo.JspFileInfo;
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
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

        List<String> errorLines = new ArrayList<>();

        Path root = Paths.get(sourceFolderDAO.getDirPath());
        if (!Files.exists(root) || !Files.isDirectory(root)) return;
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".java"))
                    .forEach(p -> {
                        try {
                            parseJavaFile(p.toFile());
                         } catch (Exception e) {
                             String err = "Failed to parse Java file: " + p + " - " + e.getMessage();
                             System.err.println(err);
                             errorLines.add(err);
                         }
                    });
        }

        jsonFileService.generateJsonArray(javaClasses, serverOutput.getAbsolutePath());

        if (!errorLines.isEmpty()) {
            Files.write(serverError.toPath(), errorLines, StandardCharsets.UTF_8);
        }
    }

    public void parsingJsp(File outputLog, File errorLog, SourceFolderDAO sourceFolderDAO) throws IOException {
        Path root = Paths.get(sourceFolderDAO.getDirPath());
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            System.out.println("Project directory does not exist, skipping JSP parsing");
            return;
        }

        Set<String> processed = new HashSet<>();
        List<String> errorLines = new ArrayList<>();

        System.out.println("=== Start scanning JSP files ===");

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
                                System.out.println("   -> Starting parse: " + p.getFileName());
                                parseJspFiles(p.toFile());
                            }
                        } catch (IOException e) {
                            errorLines.add("JSP file access error: " + p + " - " + e.getMessage());
                        }
                    });
        }

        System.out.println("=== JSP scan complete ===\n");

        jsonFileService.generateJsonArray(jspInfos, outputLog.getAbsolutePath());

        System.out.println("JSP JSON report generated -> " + outputLog.getAbsolutePath());
        System.out.println("   Total: " + jspInfos.size() + " JSP files");

        if (!errorLines.isEmpty()) {
            Files.write(errorLog.toPath(), errorLines, StandardCharsets.UTF_8);
        }
    }

    public void parseJavaFile(File javaFile) throws IOException {
        try {
            //Using Spoon to parse java files Reference link: https://spoon.gforge.inria.fr/launcher.html
            //Building the Spoon Model
            Launcher launcher = new Launcher();
            //Set noClasspath true for avoid error when parsing class missing dependency or incompleted code
            launcher.getEnvironment().setNoClasspath(true);
            launcher.addInputResource(javaFile.getAbsolutePath());
            launcher.buildModel();

            CtModel model = launcher.getModel();

            // Extract all types including class, inteface and enum
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

        //Extract information fully qualified name, simple name, package name, and type.
        javaClassInfo.setFullName(type.getQualifiedName());
        javaClassInfo.setSimpleName(type.getSimpleName());
        javaClassInfo.setPackageName(type.getPackage() != null ? type.getPackage().getQualifiedName() : "(default)");
        javaClassInfo.setKind((type instanceof CtInterface) ? "Interface" : "Class");

        //Extract modifiers
        List<String> modifiers = type.getModifiers().stream()
                .map(ModifierKind::toString)
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        javaClassInfo.setModifiers(modifiers.isEmpty() ? null : modifiers);

        //Extract annotations
        List<AnnotationInfo> annotations = new ArrayList<>();
        for (CtAnnotation<?> annotation : type.getAnnotations()) {
            AnnotationInfo annInfo = new AnnotationInfo();
            annInfo.setName(annotation.getAnnotationType().getQualifiedName());
            annotation.getValues().forEach((k, v) -> annInfo.addValue(k, v.toString()));
            annotations.add(annInfo);
        }
        javaClassInfo.setAnnotations(annotations.isEmpty() ? null : annotations);

        //Extract fields
        List<FieldInfo> fields = new ArrayList<>();
        for (CtField<?> field : type.getFields()) {
            FieldInfo f = new FieldInfo();
            //Extract field name like : username
            f.name = field.getSimpleName();
            //Extract the type of field like : String
            f.type = field.getType() != null ? field.getType().getQualifiedName() : "(unresolved)";
            //Extract the modifiers
            f.modifiers = field.getModifiers().stream()
                    .map(ModifierKind::toString)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            //Extract annotations of field
            List<AnnotationInfo> fieldAnnotations = new ArrayList<>();
            for (CtAnnotation<?> ann : field.getAnnotations()) {
                AnnotationInfo annInfo = new AnnotationInfo();
                //Extract annotation name
                annInfo.setName(ann.getAnnotationType().getQualifiedName());
                fieldAnnotations.add(annInfo);
            }
            //Set the annotation to the field
            f.annotations = fieldAnnotations.isEmpty() ? null : fieldAnnotations;
            fields.add(f);
        }
        //Set the
        javaClassInfo.setFields(fields.isEmpty() ? null : fields);

        //Parse each method and collect dependencies
        for (CtMethod<?> method : type.getMethods()) {
            try {
                parseMethodWithDeps(method, javaClassInfo, classDeps);
            } catch (IOException e) {
                System.err.println("Parsing method failed: " + method.getSimpleName() + " - " + e.getMessage());
            }
        }
        if (javaClassInfo.getMethods() != null && javaClassInfo.getMethods().isEmpty()) {
            javaClassInfo.setMethods(null);
        }

        // Run three problem checks: constructor call, memory copy, and call chain.
        detectConstructorIssues(type, classDeps);
        detectMemoryReplicationIssues(type, classDeps);


        List<String> importList = new ArrayList<>();
        try {
            CtCompilationUnit cu = type.getPosition().getCompilationUnit();
            if (cu == null) {
                System.out.println("Unable to obtain the compilation unit by position, try the factory method: " + type.getQualifiedName());
                cu = type.getFactory().CompilationUnit().getOrCreate(type);
            }

            if (cu != null) {
                System.out.println("Locate the compilation unit and extract the imports: " + type.getQualifiedName());
                List<CtImport> imports = cu.getImports();
                System.out.println("  Found " + imports.size() + " imports");

                for (int i = 0; i < imports.size(); i++) {
                    CtImport imp = imports.get(i);
                    System.out.println("  process " + (i+1) + " import: " + imp.toString());

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
                            System.out.println("  Failed to retrieve full qualified name: " + e.getMessage());
                        }

                        importList.add(qualifiedName != null ? qualifiedName : importName);
//                        System.out.println("  → import[" + (i+1) + "]: " + importName +
//                                         (qualifiedName != null ? " (full: " + qualifiedName + ")" : ""));
                    } else {
                        String importString = imp.toString();
                        if (importString.startsWith("import ")) {
                            String importContent = importString.substring(7).trim();
                            if (importContent.endsWith(";")) {
                                importContent = importContent.substring(0, importContent.length() - 1);
                            }
                            importList.add(importContent);
//                            System.out.println("  → Unresolved import[" + (i+1) + "], using string: " + importContent);
                        } else {
                            importList.add("(unresolved import: " + importString + ")");
//                            System.out.println("  → Unresolved import[" + (i+1) + "]: " + importString);
                        }
                    }
                }
            } else {
                System.err.println("Unable to obtain compilation unit: " + type.getQualifiedName());
            }
        } catch (Exception e) {
            System.err.println("Failed to extract imports: " + type.getQualifiedName() + " - " + e.getMessage());
            e.printStackTrace();
        }

        javaClassInfo.setImports(importList.isEmpty() ? null : importList);
        javaClassInfo.setIssues(null);

        javaClasses.add(javaClassInfo);
    }


    private void parseMethodWithDeps(CtMethod<?> method, JavaClassInfo classInfo, Set<String> classDeps) throws IOException {
        MethodInfo m = new MethodInfo();
        m.name = method.getSimpleName();
        m.returnType = method.getType() != null ? method.getType().getQualifiedName() : "(unresolved)";
        m.modifiers = method.getModifiers().stream()
                .map(ModifierKind::toString)
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        //Extract parameters of method
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

        //Detect invocation issues in a method
        detectInvocationIssues(method, classDeps);
    }

    public void detectInvocationIssues(CtMethod<?> method, Set<String> classDeps) throws IOException {
        String className = method.getDeclaringType().getQualifiedName();

        boolean isGeneratedJsp = className.contains("_jsp") || className.contains("org.apache.jsp");

        //Traversing the invocation in a method
        method.filterChildren(new TypeFilter<>(CtInvocation.class)).forEach(inv -> {
            try {
                //Extract the invocation
                CtInvocation<?> call = (CtInvocation<?>) inv;
                //Get the invocation method
                CtExecutableReference<?> exec = call.getExecutable();
                //Get the invocation belong to which class
                CtTypeReference<?> declaringType = exec.getDeclaringType();

                //Set the invocation class name if null then set the unknown which maybe the user custom
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
                    if ("exit(int)".equals(signature) && "java.lang.System".equals(declaringTypeName)) {
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
                System.err.println("Error detecting invocation issues: " + e.getMessage());
            }
        });
    }

    public void detectConstructorIssues(CtType<?> type, Set<String> classDeps) throws IOException {
        //Traversing the constructor call in a class like: new File(...)
        type.filterChildren(new TypeFilter<>(CtConstructorCall.class)).forEach(ctor -> {
            try {
                CtConstructorCall<?> call = (CtConstructorCall<?>) ctor;
                //Extract the class
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

                }
            } catch (Exception e) {
                System.err.println("Error detecting constructor problem: " + e.getMessage());
            }
        });
    }

    public void detectMemoryReplicationIssues(CtType<?> type, Set<String> classDeps) throws IOException {
        String className = type.getQualifiedName();
        String classLoc = className;

        //Extract implements interface
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
    }


    public void parseJspFiles(File jspFile) {
        JspFileInfo jspAnalysisResult;
        boolean usingJasper = false;

        try {
            // Track 1: Jasper compile JSP -> extract dependency info (imports, namespaces, customTaglibs, server refs)
            System.out.println("📝 [Jasper] Analyzing JSP file: " + jspFile.getName());
            jspAnalysisResult = JasperJspAnalyzer.analyze(jspFile.toPath());
            usingJasper = true;

            System.out.println("✅ [Jasper] Analysis successful: " + jspFile.getName());
            System.out.println("   Imported namespaces: " + jspAnalysisResult.namespaces.size());
            System.out.println("   Java code blocks: " + jspAnalysisResult.getJavaCodeBlockCount());
            System.out.println("   Custom tags: " + jspAnalysisResult.getCustomTaglibs().size());

            // When Jasper succeeds, additionally analyze original JSP source with regex to correct content fields
            // scriptlets/declarations/elExpressions in Jasper output are generated Java code, not original JSP content
            try {
                JspFileInfo regexResult = JspContentAnalyzer.analyzeJspContent(jspFile);
                jspAnalysisResult.setElExpressions(regexResult.getElExpressions());
                jspAnalysisResult.setScriptlets(regexResult.getScriptlets());
                jspAnalysisResult.setDeclarations(regexResult.getDeclarations());
                jspAnalysisResult.setExpressions(regexResult.getExpressions());
                jspAnalysisResult.setDirectives(regexResult.getDirectives());
                jspAnalysisResult.setBeans(regexResult.getBeans());
                jspAnalysisResult.setIncludes(regexResult.getIncludes());
                jspAnalysisResult.setJavaCodeBlockCount(regexResult.getJavaCodeBlockCount());
                jspAnalysisResult.setJavaCodeLineCount(regexResult.getJavaCodeLineCount());
            } catch (Exception e) {
                System.out.println("   ⚠️ Regex supplementary analysis failed, using Jasper result: " + e.getMessage());
            }

        } catch (Exception e) {
            // Track 2: Jasper failed, fallback to pure regex analysis
            System.out.println("⚠️ [Jasper] Analysis failed, falling back to regex: " + jspFile.getName() + " → " + e.getMessage());

            try {
                jspAnalysisResult = JspContentAnalyzer.analyzeJspContent(jspFile);
            } catch (Exception ex) {
                System.out.println("❌ Regex fallback also failed: " + ex.getMessage());
                return;
            }
        }

        jspInfos.add(jspAnalysisResult);

        IssueInfo warning = new IssueInfo();
        warning.setSeverity("Medium");
        warning.setMessage("Using JSP files may cause scalability issues, recommend using modern frontend technologies");
        warning.setLocation(jspFile.getAbsolutePath());
        warning.setClassName(jspFile.getName());
        warning.setSource("jsp");

        analysisResult.getIssues().add(warning);
        allIssues.add(warning);

        String tag = usingJasper ? "Jasper" : "Regex";
        System.out.println("✅ [" + tag + "] JSP content analysis complete: " + jspFile.getName());
        System.out.println("   Code block count: " + jspAnalysisResult.getJavaCodeBlockCount());
        System.out.println("   Code line count: " + jspAnalysisResult.getJavaCodeLineCount());
        System.out.println("   Custom tags: " + jspAnalysisResult.getCustomTaglibs().size());
        System.out.println("   Server references: " + jspAnalysisResult.getServerSpecificReferences().size());
        System.out.println("   EL expressions: " + jspAnalysisResult.getElExpressions().size());
    }

    public void parseWebXml(File webXmlParseoutput, SourceFolderDAO sourceFolderDAO) throws IOException {
        parseGenericXml(
                webXmlParseoutput,
                sourceFolderDAO,
                "web.xml",
                "web.xml",
                (doc, xmlData) -> {
                    // Extract <security-constraint> security constraints (URL pattern + roles)
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

                    // Extract <security-role> security role list
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

                    // Extract <error-page> error page mapping (exception type / HTTP status code -> page)
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

                    // Extract <session-config> session timeout configuration
                    Map<String, Object> keyConfigs = new HashMap<>();
                    NodeList sessionConfigs = doc.getElementsByTagName("session-config");
                    if (sessionConfigs.getLength() > 0) {
                        Element sc = (Element) sessionConfigs.item(0);
                        String timeout = getTagValue(sc, "session-timeout");
                        if (!timeout.isEmpty()) {
                            keyConfigs.put("sessionTimeoutMinutes", timeout);
                            keyConfigs.put("note", "Recommend moving to environment variables or external configuration");
                        }
                    }

                    xmlData.put("keyConfigs", keyConfigs);

                    // Extract <resource-ref> JNDI resource references
                    List<Map<String, Object>> resourcesList = new ArrayList<>();
                    NodeList resources = doc.getElementsByTagName("resource-ref");
                    for (int i = 0; i < resources.getLength(); i++) {
                        Element r = (Element) resources.item(i);
                        Map<String, Object> rMap = new HashMap<>();
                        rMap.put("resRefName", getTagValue(r, "res-ref-name"));
                        rMap.put("resType", getTagValue(r, "res-type"));
                        rMap.put("risk", "JNDI must be changed to environment variables");
                        resourcesList.add(rMap);
                    }
                    xmlData.put("resourceRefs", resourcesList);

                    // Extract web.xml version (with DTD fallback parsing)
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

                    // Extract <context-param> context parameters
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

                    // Extract <filter> filter definitions
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

                    // Extract <servlet> definitions
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

                    // Extract <listener> listener classes
                    List<String> listeners = new ArrayList<>();
                    NodeList listenerList = doc.getElementsByTagName("listener");
                    for (int i = 0; i < listenerList.getLength(); i++) {
                        listeners.add(getTagValue((Element) listenerList.item(i), "listener-class"));
                    }
                    xmlData.put("listeners", listeners);

                    // Extract session-config (kept again for backward compatibility)
                    Map<String, Object> sessionConfig = new HashMap<>();
                    NodeList scList = doc.getElementsByTagName("session-config");
                    if (scList.getLength() > 0) {
                        Element sc = (Element) scList.item(0);
                        sessionConfig.put("sessionTimeoutMinutes", getTagValue(sc, "session-timeout"));
                    }
                    xmlData.put("sessionConfig", sessionConfig);

                    // Extract <welcome-file-list> welcome files
                    List<String> welcomeFiles = new ArrayList<>();
                    NodeList wfList = doc.getElementsByTagName("welcome-file");
                    for (int i = 0; i < wfList.getLength(); i++) {
                        welcomeFiles.add(wfList.item(i).getTextContent().trim());
                    }
                    xmlData.put("welcomeFiles", welcomeFiles);

                    // Extract <taglib> tag library references
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

                    // Extract <login-config> login configuration (auth method / form page)
                    Map<String, Object> loginConfig = new HashMap<>();
                    NodeList lcList = doc.getElementsByTagName("login-config");
                    if (lcList.getLength() > 0) {
                        Element lc = (Element) lcList.item(0);
                        loginConfig.put("authMethod", getTagValue(lc, "auth-method"));
                        loginConfig.put("formLoginPage", getTagValue(lc, "form-login-page"));
                    }
                    xmlData.put("loginConfig", loginConfig);

                    // Extract <mime-mapping> MIME type mappings
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
            // Extract all persistence-unit configurations
            List<Map<String, Object>> unitsList = new ArrayList<>();
            NodeList units = doc.getElementsByTagName("persistence-unit");

            for (int i = 0; i < units.getLength(); i++) {
                Element unit = (Element) units.item(i);
                Map<String, Object> m = new HashMap<>();

                // Extract JTA/non-JTA data sources
                m.put("name", unit.getAttribute("name"));
                m.put("jtaDataSource", getTagValue(unit, "jta-data-source"));
                m.put("nonJtaDataSource", getTagValue(unit, "non-jta-data-source"));

                // Extract entity class list
                List<String> classList = new ArrayList<>();
                NodeList classNodes = unit.getElementsByTagName("class");
                for (int j = 0; j < classNodes.getLength(); j++) {
                    String className = classNodes.item(j).getTextContent().trim();
                    if (!className.isEmpty()) {
                        classList.add(className);
                    }
                }
                m.put("classes", classList);

                // Extract properties attributes
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
            // Extract <session> Bean (Stateless/Stateful) configuration
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

            // Extract <message-driven> Bean configuration
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

            // Extract <container-transaction> transaction attributes
            List<Map<String, Object>> transactions = new ArrayList<>();
            NodeList txNodes = doc.getElementsByTagName("container-transaction");
            for (int i = 0; i < txNodes.getLength(); i++) {
                Element tx = (Element) txNodes.item(i);
                Map<String, Object> m = new HashMap<>();
                m.put("transAttribute", getTagValue(tx, "trans-attribute"));
                transactions.add(m);
            }
            xmlData.put("containerTransactions", transactions);

            // Extract ejb-client-jar reference
            xmlData.put("ejbClientJar", getTagValue(doc, "ejb-client-jar"));

            // Extract <interceptor> and <interceptor-binding>
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

            // Extract <entity> Bean (EJB 2.x CMP/BMP)
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
            // Extract <managed-bean> managed beans and their scopes
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

            // Extract <phase-listener> lifecycle listeners
            List<String> phaseListeners = new ArrayList<>();
            NodeList plNodes = doc.getElementsByTagName("phase-listener");
            for (int i = 0; i < plNodes.getLength(); i++) {
                phaseListeners.add(plNodes.item(i).getTextContent().trim());
            }
            xmlData.put("phaseListeners", phaseListeners);

            // Extract <lifecycle><view-handler> custom view handlers
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
            // Walk all <module> and detect module type (web/ejb/java)
            List<Map<String, Object>> modulesList = new ArrayList<>();
            NodeList modules = doc.getElementsByTagName("module");
            for (int i = 0; i < modules.getLength(); i++) {
                Element module = (Element) modules.item(i);
                Map<String, Object> moduleMap = new HashMap<>();
                String type = "";
                // Determine module type by child element tag
                if (module.getElementsByTagName("web").getLength() > 0) type = "web";
                else if (module.getElementsByTagName("ejb").getLength() > 0) type = "ejb";
                else if (module.getElementsByTagName("java").getLength() > 0) type = "java";
                else type = "unknown";

                // Get corresponding URI by type
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

        // Walk all .xhtml files, skip build/target output
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
            System.out.println("+No .xhtml JSF files found (skipped) ===");
            return;
        }

        for (Path path : paths) {
            File file = path.toFile();
            System.out.println("Analyzing JSF file: " + file.getName());

            // Use Jsoup to analyze each JSF file
            JsfFileInfo jsfInfo = JsfAnalyzer.analyze(file);

            jsfFiles.add(jsfInfo);
            analysisResult.getJsfFiles().add(jsfInfo);

            // Detect migration issues
            List<IssueInfo> detected = JsfMigrationIssueDetector.detect(jsfInfo);
            for (IssueInfo issue : detected) {
                analysisResult.getIssues().add(issue);
                allIssues.add(issue);
            }
        }

        // Cross-reference PhaseListener and ViewHandler from faces-config.xml
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

        // Generate JSF JSON report
        jsonFileService.generateJsonArray(jsfFiles, jsfFilesParseOutput.getAbsolutePath());

        System.out.println("JSF JSON report generated -> " + jsfFilesParseOutput.getAbsolutePath());
        System.out.println("   Total analyzed: " + paths.size() + " JSF files");
    }

    @Transactional
    public void staticParseFiles(SourceFolderDAO sourceFolderDAO) throws IOException {
        String rootPath = sourceFolderDAO.getDirPath();
        File rootDir = new File(rootPath);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            throw new IOException("Project directory does not exist: " + rootPath);
        }

        // Create output directory structure
        File outputFolder = new File(rootPath, OutputPath.OUTPUT_BASE_DIR);
        if (!outputFolder.exists()) {
            if (!outputFolder.mkdirs()) {
                throw new IOException("Unable to create output directory: " + outputFolder.getAbsolutePath());
            }
        }

        // Define all output file paths
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

        // Clear all previous analysis results
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

        // Execute all parsers in sequence
        parsing(analysisOutput, errorLog, sourceFolderDAO);

        parsingJsp(jspOutput, jspError, sourceFolderDAO);

        parseFacesConfigXml(facesConfigXmlOutput, sourceFolderDAO);

        parsingJsf(jsfOutput, sourceFolderDAO);

        parseWebXml(webXmlOutput, sourceFolderDAO);
        parsePersistenceXml(persistenceXmlOutput, sourceFolderDAO);
        parseEjbJarXml(ejbJarXmlOutput, sourceFolderDAO);
        parseApplicationXml(applicationXmlOutput, sourceFolderDAO);

        // Aggregate all detected issues and write to JSON
        if (!allIssues.isEmpty()) {
            jsonFileService.generateJsonArray(allIssues, issuesOutput.getAbsolutePath());
            System.out.println("Issues JSON report generated -> " + issuesOutput.getAbsolutePath());
            System.out.println("   Total: " + allIssues.size() + " issues");
        } else {
            System.out.println("No issues found, skipping Issues JSON report generation");
        }

        System.out.println("Full static analysis complete!");
        System.out.println(" - TXT report directory: " + outputFolder.getAbsolutePath());
        System.out.println(" - JSON report: analysis_result_" + sourceFolderDAO.getId() + "*.json");
        System.out.println(" - Database record updated, source_folder_id: " + sourceFolderDAO.getId());
    }

    public void parsePomXmlFile(File pomOutput, SourceFolderDAO sourceFolderDAO) throws IOException {
        parseGenericXml(pomOutput, sourceFolderDAO, "pom.xml", "pom.xml", (doc, xmlData) -> {
            // Extract Java compile version
            String javaVersion = getTagValue(doc, "java.version");

            xmlData.put("javaVersion", javaVersion.isEmpty() ? "unknown" : javaVersion);

            // Extract <parent> POM parent info
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

            // Extract <dependencies> dependency list
            List<String> deps = new ArrayList<>();
            NodeList dependencies = doc.getElementsByTagName("dependencies");
            for (int i = 0; i < dependencies.getLength(); i++) {
                Element dep = (Element) dependencies.item(i);
                String g = getTagValue(dep, "groupId");
                String a = getTagValue(dep, "artifactId");
                String v = getTagValue(dep, "version");
                deps.add(g + ":" + a + ":" + (v.isEmpty() ? "unspecified" : v));
            }
            xmlData.put("dependencies", deps);

            // Extract <plugin> build plugin list
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

            // Compatibility assessment: Java version / dependency count / Fat Jar detection
            Map<String, Object> compatibility = new HashMap<>();

            List<String> supportedJavaVersions = Arrays.asList("8", "11", "17", "21");
            boolean javaVersionCompatible = false;
            String javaCompatibilityAssessment = "unknown";

            if (javaVersion.isEmpty()) {
                javaCompatibilityAssessment = "Java version not specified";

                IssueInfo javaIssue = new IssueInfo();
                javaIssue.severity = "Medium";
                javaIssue.message = "Java version not specified, may cause build or runtime issues";
                javaIssue.location = "pom.xml";
                javaIssue.className = "pom.xml";
                javaIssue.source = "pom_analysis";
                allIssues.add(javaIssue);
            } else {
                // Extract major version number (supports 1.8 format)
                String majorVersion = javaVersion;
                if (javaVersion.startsWith("1.")) {
                    majorVersion = javaVersion.substring(2);
                }
                majorVersion = majorVersion.replaceAll("[^0-9]", "");

                if (supportedJavaVersions.contains(majorVersion)) {
                    javaVersionCompatible = true;
                    javaCompatibilityAssessment = "Compatible (Java " + majorVersion + ")";
                } else {
                    javaCompatibilityAssessment = "Incompatible (Java " + majorVersion + ", supported versions: " + String.join(", ", supportedJavaVersions) + ")";

                    IssueInfo javaIssue = new IssueInfo();
                    javaIssue.severity = "High";
                    javaIssue.message = "Java version " + majorVersion + " may be incompatible with the application server";
                    javaIssue.location = "pom.xml";
                    javaIssue.className = "pom.xml";
                    javaIssue.source = "pom_analysis";
                    allIssues.add(javaIssue);
                }
            }

            compatibility.put("javaVersionCompatible", javaVersionCompatible);
            compatibility.put("javaCompatibilityAssessment", javaCompatibilityAssessment);

            // Dependency count assessment: Large / Medium / Small
            int dependencyCount = deps.size();
            String dependencySizeAssessment;
            if (dependencyCount > 50) {
                dependencySizeAssessment = "Large dependency (" + dependencyCount + " dependencies), may affect WAR file size and startup time";

                IssueInfo depIssue = new IssueInfo();
                depIssue.severity = "Medium";
                depIssue.message = "Project contains a large number of dependencies (" + dependencyCount + "), may cause WAR file to exceed 500MB";
                depIssue.location = "pom.xml";
                depIssue.className = "pom.xml";
                depIssue.source = "pom_analysis";
                allIssues.add(depIssue);
            } else if (dependencyCount > 20) {
                dependencySizeAssessment = "Medium dependency (" + dependencyCount + " dependencies)";
            } else {
                dependencySizeAssessment = "Small dependency (" + dependencyCount + " dependencies)";
            }

            compatibility.put("dependencyCount", dependencyCount);
            compatibility.put("dependencySizeAssessment", dependencySizeAssessment);

            // Detect Fat Jar / Uber Jar plugins (not suitable for traditional application servers)
            boolean hasFatJarPlugin = plugins.stream().anyMatch(plugin ->
                plugin.contains("spring-boot-maven-plugin") ||
                plugin.contains("maven-shade-plugin") ||
                plugin.contains("maven-assembly-plugin")
            );

            if (hasFatJarPlugin) {
                compatibility.put("fatJarWarning", "Fat Jar/Uber Jar plugin detected, may not be suitable for traditional application server deployment");

                IssueInfo fatJarIssue = new IssueInfo();
                fatJarIssue.severity = "High";
                fatJarIssue.message = "Fat Jar/Uber Jar plugin detected, traditional application servers require WAR deployment";
                fatJarIssue.location = "pom.xml";
                fatJarIssue.className = "pom.xml";
                fatJarIssue.source = "pom_analysis";
                allIssues.add(fatJarIssue);
            } else {
                compatibility.put("fatJarWarning", "No Fat Jar plugin detected, suitable for traditional application server deployment");
            }

            // Comprehensive compatibility assessment
            boolean overallCompatible = javaVersionCompatible && !hasFatJarPlugin && dependencyCount <= 100;
            String overallAssessment;

            if (overallCompatible) {
                overallAssessment = "Project structure is well compatible with application servers";
            } else {
                overallAssessment = "Project may have compatibility issues with application servers";

                IssueInfo overallIssue = new IssueInfo();
                overallIssue.severity = "Medium";
                overallIssue.message = "Project structure may have compatibility issues with application servers";
                overallIssue.location = "pom.xml";
                overallIssue.className = "pom.xml";
                overallIssue.source = "pom_analysis";
                allIssues.add(overallIssue);
            }

            compatibility.put("overallCompatible", overallCompatible);
            compatibility.put("overallAssessment", overallAssessment);

            xmlData.put("containerCompatibility", compatibility);

            // Count <profile> elements
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
            System.out.println("=== " + xmlType + " not found (skipped) ===");
            return;
        }

        System.out.println("Found " + xmlPaths.size() + " " + xmlType + " files");

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
                System.out.println(xmlType + " parse failed: " + xmlFile.getName() + " → " + e.getMessage());
            }

            xmlFileInfos.add(xmlFileInfo);
        }

        analysisResult.getXmlConfigs().computeIfAbsent(xmlType, k -> new ArrayList<>()).addAll(xmlFileInfos);

        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Unable to create output directory: " + parentDir.getAbsolutePath());
            }
        }

        jsonFileService.generateJsonArray(xmlFileInfos, outputFile.getAbsolutePath());

        System.out.println(xmlType + " parse complete, written to -> " + outputFile.getAbsolutePath());
    }
}

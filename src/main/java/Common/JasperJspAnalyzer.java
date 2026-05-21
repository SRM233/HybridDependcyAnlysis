package Common;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import org.apache.jasper.JspC;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

public class JasperJspAnalyzer {

    private static final Pattern TAG_HANDLER_PATTERN =
            Pattern.compile("_jspx_th_([a-zA-Z0-9]+)_");

    public static JspFileInfo analyze(Path jspFile) throws Exception {
        String javaSource = compileJspToJava(jspFile);
        CompilationUnit cu = StaticJavaParser.parse(javaSource);

        JspFileInfo result = new JspFileInfo();
        result.filePath = jspFile.toAbsolutePath().toString();

        // 1. Extract imports as namespaces
        Set<String> namespaces = new HashSet<>();
        for (var imp : cu.getImports()) {
            namespaces.add(imp.getNameAsString());
        }
        result.namespaces = new ArrayList<>(namespaces);

        // 2. Extract methods from generated class
        List<String> elExpressions = new ArrayList<>();
        Set<String> customTaglibs = new HashSet<>();

        if (!cu.getTypes().isEmpty()) {
            var type = cu.getType(0);
            for (MethodDeclaration method : type.getMethods()) {
                String methodName = method.getNameAsString();
                if (methodName.equals("_jspService")) {
                    if (method.getBody().isPresent()) {
                        String bodyStr = method.getBody().get().toString();
                        result.addScriptlet(bodyStr);

                        // Extract EL evaluation calls
                        method.findAll(MethodCallExpr.class).stream()
                                .filter(mc -> mc.getNameAsString().contains("evaluate")
                                        || mc.getNameAsString().contains("el_"))
                                .forEach(mc -> elExpressions.add(mc.toString()));

                        // Extract dynamic out.write/print calls (non-literal args)
                        method.findAll(MethodCallExpr.class).stream()
                                .filter(mc -> {
                                    String name = mc.getNameAsString();
                                    return (name.equals("write") || name.equals("print"))
                                            && mc.getArguments().size() == 1
                                            && !(mc.getArgument(0) instanceof StringLiteralExpr);
                                })
                                .forEach(mc -> elExpressions.add(mc.getArgument(0).toString()));
                    }
                } else {
                    if (method.getBody().isPresent()) {
                        String decl = method.getDeclarationAsString(true, true, true)
                                + "\n" + method.getBody().get().toString();
                        result.addDeclaration(decl);
                    }
                }
            }

            // Extract custom taglib prefixes from tag handler method names
            var handlerMatcher = TAG_HANDLER_PATTERN.matcher(javaSource);
            while (handlerMatcher.find()) {
                customTaglibs.add(handlerMatcher.group(1));
            }
        }

        // 3. Fill remaining result
        for (String e : elExpressions) result.addElExpression(e);
        for (String t : customTaglibs) result.addCustomTaglib(t);

        return result;
    }

    private static String compileJspToJava(Path jspPath) throws Exception {
        Path tempDir = Files.createTempDirectory("jsp-gen-");
        try {
            File outputDir = tempDir.toFile();
            String jspBaseName = jspPath.getFileName().toString();

            // Copy JSP to temp dir so JspC can find it
            File tempJsp = outputDir.toPath().resolve(jspBaseName).toFile();
            Files.copy(jspPath, tempJsp.toPath(), StandardCopyOption.REPLACE_EXISTING);

            JspC jspc = new JspC();
            jspc.setUriroot(outputDir.getAbsolutePath());
            jspc.setOutputDir(outputDir.getAbsolutePath());
            jspc.setJspFiles(jspBaseName);
            jspc.setCompile(false);
            jspc.setClassDebugInfo(true);
            jspc.setFailOnError(false);
            jspc.execute();

            // Find generated _jsp.java
            File generatedJavaFile = null;
            try (var stream = Files.walk(tempDir)) {
                generatedJavaFile = stream
                        .filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith("_jsp.java"))
                        .findFirst()
                        .map(Path::toFile)
                        .orElse(null);
            }

            if (generatedJavaFile == null || !generatedJavaFile.exists()) {
                throw new RuntimeException("Jasper compilation failed, no _jsp.java generated for: " + jspPath);
            }

            return Files.readString(generatedJavaFile.toPath());
        } finally {
            deleteDir(tempDir);
        }
    }

    private static void deleteDir(Path dir) {
        try {
            if (Files.exists(dir)) {
                Files.walk(dir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException ignored) {
        }
    }
}

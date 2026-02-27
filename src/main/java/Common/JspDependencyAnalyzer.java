package Common;

import org.apache.jasper.JspC;
import org.apache.jasper.JasperException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class JspDependencyAnalyzer {

    public static String analyzeSingleJsp(File jspFile, String outputDirPath) {
        Path outputDir = Path.of(outputDirPath);
        try {
            Files.createDirectories(outputDir);

            // Step 1: 读取原 JSP 内容
            String jspContent = Files.readString(jspFile.toPath());

            // Step 2: 只取文件名（basename），避免全路径问题
            String jspBaseName = jspFile.getName(); // e.g., "index.jsp"

            // Step 3: 写入临时 JSP 文件（只用 basename）
            File tempJspFile = outputDir.resolve(jspBaseName).toFile();
            Files.writeString(tempJspFile.toPath(), jspContent);

            // Step 4: 配置 JspC
            JspC jspc = new JspC();
            jspc.setOutputDir(outputDir.toAbsolutePath().toString());
            jspc.setUriroot(outputDir.toAbsolutePath().toString()); // uriroot 指向 outputDir（临时文件所在）
            jspc.setJspFiles(jspBaseName); // 只传 basename
            jspc.setCompile(false);
            jspc.setClassDebugInfo(true);

            jspc.execute(); // 生成 _jsp.java

            // Step 5: 查找生成的 .java 文件（递归，通用，优先推荐）
            File generatedJavaFile = null;
            try (var stream = Files.walk(outputDir)) {
                generatedJavaFile = stream
                        .filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith("_jsp.java"))
                        .findFirst()
                        .map(Path::toFile)
                        .orElse(null);
            }

            if (generatedJavaFile == null || !generatedJavaFile.exists()) {
                System.err.println("JSP 编译失败，未生成任何 _jsp.java 文件: " + jspFile.getAbsolutePath());
                System.err.println("outputDir 内容: " + Arrays.toString(outputDir.toFile().list()));
                // 可选：列出子目录内容，帮助调试
                Files.walk(outputDir, 3).filter(Files::isRegularFile).forEach(p -> System.err.println("  文件: " + p));
                System.err.println();
//                return new HashSet<>(); // 返回空，继续下一个
            }

            System.out.println("找到生成的 Servlet: " + generatedJavaFile.getAbsolutePath());
//
//            // Step 6: 用 JavaParser 解析 import（如果你后续要换 Spoon，可直接传 generatedJavaFile）
//            com.github.javaparser.ast.CompilationUnit cu = com.github.javaparser.StaticJavaParser.parse(generatedJavaFile);
//            Set<String> dependencies = new HashSet<>();
//            for (com.github.javaparser.ast.ImportDeclaration imp : cu.getImports()) {
//                String importName = imp.getNameAsString();
//                if (!importName.startsWith("java.lang.") &&
//                        !importName.startsWith("javax.servlet.") &&
//                        !importName.startsWith("jakarta.servlet.")) {
//                    dependencies.add(importName);
//                }
//            }
//
//            System.out.println("成功分析: " + jspFile.getAbsolutePath() + " → 依赖数: " + dependencies.size());
//            return dependencies;

            return generatedJavaFile.getAbsolutePath();
        } catch (Exception e) {
            System.err.println("分析失败: " + jspFile.getAbsolutePath());
            System.err.println("错误原因: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace(); // 建议打开，便于定位
//            return new HashSet<>();
            return null;
        }
    }

    // 批量分析
//    public static void analyzeBatch(File[] jspFiles, String outputDirPath) {
//        for (File jspFile : jspFiles) {
//            Set<String> deps = analyzeSingleJsp(jspFile, outputDirPath);
//            if (!deps.isEmpty()) {
//                System.out.println("依赖: " + deps);
//            }
//        }
//    }
}
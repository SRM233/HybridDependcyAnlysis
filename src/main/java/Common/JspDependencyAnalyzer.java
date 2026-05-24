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

            // Step 1: Read original JSP content
            String jspContent = Files.readString(jspFile.toPath());

            // Step 2: Use only filename (basename) to avoid full path issues
            String jspBaseName = jspFile.getName(); // e.g., "index.jsp"

            // Step 3: Write temp JSP file (using basename only)
            File tempJspFile = outputDir.resolve(jspBaseName).toFile();
            Files.writeString(tempJspFile.toPath(), jspContent);

            // Step 4: Configure JspC
            JspC jspc = new JspC();
            jspc.setOutputDir(outputDir.toAbsolutePath().toString());
            jspc.setUriroot(outputDir.toAbsolutePath().toString()); // uriroot points to outputDir (where temp file is)
            jspc.setJspFiles(jspBaseName); // Pass basename only
            jspc.setCompile(false);
            jspc.setClassDebugInfo(true);

            jspc.execute(); // Generate _jsp.java

            // Step 5: Find generated .java file (recursive, universal, recommended)
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
                System.err.println("JSP compilation failed, no _jsp.java file generated: " + jspFile.getAbsolutePath());
                System.err.println("outputDir contents: " + Arrays.toString(outputDir.toFile().list()));
                // Optional: list subdirectory contents for debugging
                Files.walk(outputDir, 3).filter(Files::isRegularFile).forEach(p -> System.err.println("  File: " + p));
                System.err.println();
//                return new HashSet<>(); // Return empty, continue to next
            }

            System.out.println("Found generated Servlet: " + generatedJavaFile.getAbsolutePath());
//
//            // Step 6: Use JavaParser to parse imports (if switching to Spoon later, can pass generatedJavaFile directly)
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
//            System.out.println("Analysis successful: " + jspFile.getAbsolutePath() + " -> dependency count: " + dependencies.size());
//            return dependencies;

            return generatedJavaFile.getAbsolutePath();
        } catch (Exception e) {
            System.err.println("Analysis failed: " + jspFile.getAbsolutePath());
            System.err.println("Error reason: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace(); // Recommended to enable for debugging
//            return new HashSet<>();
            return null;
        }
    }

    // Batch analysis
//    public static void analyzeBatch(File[] jspFiles, String outputDirPath) {
//        for (File jspFile : jspFiles) {
//            Set<String> deps = analyzeSingleJsp(jspFile, outputDirPath);
//            if (!deps.isEmpty()) {
//                System.out.println("Dependencies: " + deps);
//            }
//        }
//    }
}
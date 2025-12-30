package com.hybriddependcyanlysis.Service.Impl;


import com.hybriddependcyanlysis.POJO.DAO.FileDAO;
import com.hybriddependcyanlysis.Service.SpoonServices;

import org.springframework.stereotype.Service;

import spoon.Launcher;
import spoon.compiler.ModelBuildingException;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.cu.CompilationUnit;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Service
public class SpoonServicesImpl implements SpoonServices {
        public void parsing(File output, File errorLog, List<FileDAO> files) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(output));
                 BufferedWriter errorWriter = new BufferedWriter(new FileWriter(errorLog))) {

                for (FileDAO fileDAO : files) {
                    File javaFile = new File(fileDAO.getFilePath());

                    if (fileDAO.getFileName().contains("package-info.java"))
                    {
                        continue;
                    }

                    Launcher launcher = new Launcher();
                    launcher.getEnvironment().setCommentEnabled(false);
                    launcher.getEnvironment().setNoClasspath(true);
                    launcher.addInputResource(javaFile.getAbsolutePath());

                    try {
                        launcher.buildModel();
                        CtModel model = launcher.getModel();

                        writer.write("File: " + javaFile.getName() + "\n");
                        parsePackage(model, writer);
                        writer.write("Imports:\n"); // Spoon doesn't retain imports directly

                        for (CtType<?> type : model.getAllTypes()) {
                            parseClass(type, writer);
                        }

                        writer.write("\n");

                    } catch (ModelBuildingException mbe) {
                        errorWriter.write("Spoon parse error in file: " + javaFile.getAbsolutePath() + "\n");
                        errorWriter.write(" Reason: " + mbe.getMessage() + "\n\n");

                        try {
                            Collection<CompilationUnit> units = launcher.getFactory().CompilationUnit().getMap().values();
                            for (CtCompilationUnit unit : units) {
                                writer.write("File (partial): " + javaFile.getName() + "\n");
                                parseFallbackUnit(unit, writer);
                            }
                        } catch (Exception fallbackEx) {
                            errorWriter.write("Fallback parse also failed for file: " + javaFile.getAbsolutePath() + "\n");
                            errorWriter.write("  Reason: " + fallbackEx.getMessage() + "\n\n");
                        }
                    }
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void parsePackage(CtModel model, BufferedWriter writer) throws IOException {
            model.getAllTypes().stream().findFirst().ifPresent(type -> {
                try {
                    String pkg = type.getPackage().getQualifiedName();
                    if (!pkg.isEmpty()) {
                        writer.write("Package: " + pkg + "\n");
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        public void parseClass(CtType<?> type, BufferedWriter writer) throws IOException {
            String kind = (type instanceof CtInterface) ? "Interface" : "Class";
            writer.write(kind + ": " + type.getSimpleName() + "\n");

            writeModifiers(type.getModifiers(), writer, "", "Class");

            for (CtAnnotation<?> annotation : type.getAnnotations()) {
                String ann = annotation.getAnnotationType().getQualifiedName();
                if (ann != null && !ann.isEmpty()) {
                    writer.write("  Annotation: " + ann + "\n");
                }
            }

            for (CtMethod<?> method : type.getMethods()) {
                parseMethod(method, writer);
            }

            detectConstructorIssues(type, writer);
        }

        public void parseMethod(CtMethod<?> method, BufferedWriter writer) throws IOException {
            writer.write("  Method: " + method.getSimpleName() + "\n");

            writeModifiers(method.getModifiers(), writer, " ", "Method");

            CtTypeReference<?> returnType = method.getType();
            writer.write("    Return: " + (returnType != null ? returnType.getQualifiedName() : "(unresolved)") + "\n");

            for (CtParameter<?> param : method.getParameters()) {
                CtTypeReference<?> paramType = param.getType();
                if (paramType != null && paramType.getQualifiedName() != null) {
                    String paramModifiers = param.getModifiers().stream()
                            .map(ModifierKind::toString)
                            .map(String::toLowerCase)
                            .reduce((a, b) -> a + " " + b)
                            .orElse("");
                    writer.write("    Param: " + paramModifiers + " " + paramType.getQualifiedName() + " " + param.getSimpleName() + "\n");

                }
            }

            detectInvocationIssues(method, writer);
        }

    public void detectInvocationIssues(CtMethod<?> method, BufferedWriter writer) throws IOException {
        method.filterChildren(new TypeFilter<>(CtInvocation.class)).forEach(inv -> {
            try {
                CtInvocation<?> call = (CtInvocation<?>) inv;
                CtExecutableReference<?> exec = call.getExecutable();

                String signature = exec.getSignature(); // e.g. getenv(java.lang.String)
                CtTypeReference<?> declaringType = exec.getDeclaringType();

                String declaringTypeName = (declaringType != null) ? declaringType.getQualifiedName() : "(unknown)";
                String fullCall = declaringTypeName + "." + signature;

                // 你可以根据需要添加更多规则
                if ("getenv(java.lang.String)".equals(signature)) {
                    writer.write("    ⚠️ Issue: Uses " + fullCall + " → PortabilityRisk\n");
                }

                if ("exit(int)".equals(signature) && "java.lang.System".equals(declaringTypeName)) {
                    writer.write("    ⚠️ Issue: Uses " + fullCall + " → AbruptTermination\n");
                }

                if (declaringTypeName.startsWith("java.io") || declaringTypeName.startsWith("java.nio.file")) {
                    writer.write("    ⚠️ Issue: Uses " + fullCall + " → FileSystemDependency\n");
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }



    public void detectConstructorIssues(CtType<?> type, BufferedWriter writer) throws IOException {
            type.filterChildren(new TypeFilter<>(CtConstructorCall.class)).forEach(ctor -> {
                try {
                    CtConstructorCall<?> call = (CtConstructorCall<?>) ctor;
                    CtTypeReference<?> ref = call.getType();
                    String typeName = (ref != null) ? ref.getQualifiedName() : "(unknown)";

                    switch (typeName) {
                        case "javax.servlet.http.HttpSession":
                            writer.write("  ⚠️ Issue: Uses HttpSession → StatefulSession\n");
                            break;
                        case "java.io.FileInputStream":
                        case "java.io.File":
                            writer.write("  ⚠️ Issue: Uses " + typeName + " → FileSystemDependency\n");
                            break;
                        case "java.net.Socket":
                            writer.write("  ⚠️ Issue: Uses Socket → PortBindingRisk\n");
                            break;
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        public void parseFallbackUnit(CtCompilationUnit unit, BufferedWriter writer) throws IOException {
            CtPackageDeclaration pkgDecl = unit.getPackageDeclaration();
            if (pkgDecl != null && pkgDecl.getReference() != null) {
                String pkgName = pkgDecl.getReference().getQualifiedName();
                if (!pkgName.isEmpty()) {
                    writer.write("Package: " + pkgName + "\n");
                }
            } else {
                writer.write("Package: (default)\n");
            }

            for (CtType<?> type : unit.getDeclaredTypes()) {
                parseClass(type, writer);
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



}

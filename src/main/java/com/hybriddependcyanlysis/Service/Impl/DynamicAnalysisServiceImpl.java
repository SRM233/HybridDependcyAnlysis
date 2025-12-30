package com.hybriddependcyanlysis.Service.Impl;

import com.hybriddependcyanlysis.Service.DynamicAnalysisService;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

@Service
public class DynamicAnalysisServiceImpl implements DynamicAnalysisService {
    @Override
    public void jarPack(Integer id) throws IOException {
        String unpackPath = "E:\\FYP\\ProgramStorage\\glassfish-master_1765590110860\\glassfish-master";
        // Always create a JAR
        createJar(unpackPath, "E:/FYP/ProgramStorage/output_" + id + ".jar");

        // If web files exist, also create a WAR
        boolean hasWebFiles = Files.walk(Paths.get(unpackPath))
                .anyMatch(p -> p.toString().endsWith(".jsp")
                        || p.toString().endsWith(".jsf")
                        || p.toString().contains("WEB-INF"));

        if (hasWebFiles) {
            createWar(unpackPath, "E:/FYP/ProgramStorage/output_" + id + ".war");
        }

        System.out.println("Packaging complete for ID: " + id);
    }

    @Override
    public void javaAgent() {
        try {
            // Paths to your packaged program and agent
            String agentPath = "E:/FYP/Agent/myAgent.jar";
            String jarPath   = "E:/FYP/ProgramStorage/output_1.jar"; // or .war if needed

            // Build the command: java -javaagent:agent.jar -jar program.jar
            ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    "-javaagent:" + agentPath,
                    "-jar",
                    jarPath
            );

            // Redirect output and error to console
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);

            // Start the process
            Process process = pb.start();
            System.out.println("Started program with agent: " + jarPath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createWar(String path, String fileName) throws IOException {
        Path basePath = Paths.get(path);
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(fileName))) {
            Files.walk(Paths.get(path))
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        String relativePath = basePath.relativize(file).toString();
                        JarEntry entry = new JarEntry(relativePath.replace("\\", "/"));
                        try {
                            jos.putNextEntry(entry);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        try {
                            Files.copy(file, jos);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        try {
                            jos.closeEntry();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    public void createJar(String path, String fileName) throws IOException {
        Path basePath = Paths.get(path);
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(fileName))) {
            Files.walk(basePath)
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            // relative path inside JAR
                            String relativePath = basePath.relativize(file).toString().replace("\\", "/");
                            JarEntry entry = new JarEntry(relativePath);
                            jos.putNextEntry(entry);
                            Files.copy(file, jos);
                            jos.closeEntry();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
    }

}

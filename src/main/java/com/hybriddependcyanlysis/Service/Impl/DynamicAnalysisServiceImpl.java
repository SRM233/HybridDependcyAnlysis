package com.hybriddependcyanlysis.Service.Impl;

import com.hybriddependcyanlysis.Mapper.IngestMapper;
import com.hybriddependcyanlysis.POJO.DAO.SourceFolderDAO;
import com.hybriddependcyanlysis.Service.DynamicAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${storage.program-storage-path:../ProgramStorage}")
    private String programStoragePath;

    @Autowired
    private IngestMapper ingestMapper;

    private Path getStorageRoot() {
        Path root = Paths.get(programStoragePath).toAbsolutePath().normalize();
        if (!Files.exists(root)) {
            try {
                Files.createDirectories(root);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return root;
    }

    @Override
    public void jarPack(Integer sourceFolderId) throws IOException {
        SourceFolderDAO sourceFolderDAO = ingestMapper.getById(sourceFolderId);
        if (sourceFolderDAO == null || sourceFolderDAO.getDirPath() == null) {
            throw new RuntimeException("Source folder not found for id: " + sourceFolderId);
        }
        String unpackPath = sourceFolderDAO.getDirPath();
        Path storageRoot = getStorageRoot();
        createJar(unpackPath, storageRoot.resolve("output_" + sourceFolderId + ".jar").toString());

        boolean hasWebFiles = Files.walk(Paths.get(unpackPath))
                .anyMatch(p -> p.toString().endsWith(".jsp")
                        || p.toString().endsWith(".jsf")
                        || p.toString().contains("WEB-INF"));

        if (hasWebFiles) {
            createWar(unpackPath, storageRoot.resolve("output_" + sourceFolderId + ".war").toString());
        }

        System.out.println("Packaging complete for ID: " + sourceFolderId);
    }

    @Override
    public void javaAgent(Integer sourceFolderId) {
        try {
            String agentPath = Paths.get("").toAbsolutePath().getParent().resolve("Agent/myAgent.jar").normalize().toString();
            String jarPath   = getStorageRoot().resolve("output_" + sourceFolderId + ".jar").toString();

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

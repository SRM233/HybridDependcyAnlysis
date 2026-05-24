package com.hybriddependcyanlysis.Service.Impl;

import Common.ClassInfos.JavaClassInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hybriddependcyanlysis.Service.JsonFileService;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@Service
@Data

public class JsonFileServiceImpl implements JsonFileService {

    private ObjectMapper mapper;


    public JsonFileServiceImpl(ObjectMapper mapper) {
        this.mapper = mapper;


    }


    @Override
    public <T> void generateJsonArray(T[] array, String outputPath) throws IOException {
        generateJsonArray(Arrays.asList(array), outputPath);
    }

    @Override
    public <T> void generateJsonArray(List<T> list, String outputPath) throws IOException {
        if (list == null || list.isEmpty()) {
            System.out.println("List is empty");
            return;
        }

        String finalPath = outputPath;
        if (!finalPath.toLowerCase().endsWith(".json")) {
            if (finalPath.endsWith("/") || finalPath.endsWith("\\")) {
                finalPath += "result-" + System.currentTimeMillis() + ".json";
            } else {
                finalPath += ".json";
            }
        }

        File file = new File(finalPath);
        file.getParentFile().mkdirs();

        // Write entire List directly as JSON array
        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(file, list);

    }


}

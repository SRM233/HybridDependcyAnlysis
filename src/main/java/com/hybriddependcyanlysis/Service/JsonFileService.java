package com.hybriddependcyanlysis.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public interface JsonFileService {
    public <T> void generateJsonArray(T[] array, String outputPath) throws IOException;

    public <T> void generateJsonArray(List<T> list, String outputPath) throws IOException;
}

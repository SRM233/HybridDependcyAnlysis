package com.hybriddependcyanlysis.Service;

import com.hybriddependcyanlysis.POJO.DAO.FileDAO;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;

public interface SpoonServices {
    public void parsing(File output, File errorLog, List<FileDAO> files);

    public void parsePackage(CtModel model, BufferedWriter writer) throws IOException;

    public void parseClass(CtType<?> type, BufferedWriter writer) throws IOException;

    public void parseMethod(CtMethod<?> method, BufferedWriter writer) throws IOException;

    public void detectInvocationIssues(CtMethod<?> method, BufferedWriter writer) throws IOException;

    public void detectConstructorIssues(CtType<?> type, BufferedWriter writer) throws IOException;

    public void parseFallbackUnit(CtCompilationUnit unit, BufferedWriter writer) throws IOException;
}

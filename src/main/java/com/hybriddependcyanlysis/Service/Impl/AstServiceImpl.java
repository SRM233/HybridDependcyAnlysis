package com.hybriddependcyanlysis.Service.Impl;

import com.hybriddependcyanlysis.Mapper.AstMapper;
import com.hybriddependcyanlysis.Mapper.FileMapper;
import com.hybriddependcyanlysis.POJO.DAO.JavaFilesParseDAO;
import com.hybriddependcyanlysis.POJO.DAO.AstSchema.ClassDAO;
import com.hybriddependcyanlysis.POJO.DAO.AstSchema.DependencyDAO;
import com.hybriddependcyanlysis.POJO.DAO.AstSchema.MethodDAO;
import com.hybriddependcyanlysis.POJO.DAO.AstSchema.ParameterDAO;
import com.hybriddependcyanlysis.POJO.DAO.JspParseOutPutDAO;
import com.hybriddependcyanlysis.POJO.DAO.ClientSideSchema.ELexpressionDAO;
import com.hybriddependcyanlysis.POJO.DAO.ClientSideSchema.PageDAO;
import com.hybriddependcyanlysis.POJO.DAO.ClientSideSchema.PageImportDAO;
import com.hybriddependcyanlysis.POJO.DAO.ClientSideSchema.TagliDAO;
import com.hybriddependcyanlysis.POJO.DTO.UserDTO;
import com.hybriddependcyanlysis.Service.AstService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AstServiceImpl implements AstService {

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private AstMapper astMapper;

    @Override
    public void getOutPutFile(Integer userId, Integer sourceFolderId) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(userId);
        userDTO.setSourceFolderId(sourceFolderId);

        JavaFilesParseDAO astOutPut = astMapper.getOutPut(userDTO);


    }

    @Override
    @Transactional
    public void MappingOutPutFile(Integer userId, Integer sourceFolderId) throws IOException {
        String currentFileName = null;
        String currentPackage = null;
        String currentClassName = null;
        String currentMethodName = null;
        String currentReturnType = null;
        String currentClassModifier = null;
        String currentMethodModifier = null;
        List<String> currentParams = new ArrayList<>();

        UserDTO userDTO = new UserDTO();
        userDTO.setId(userId);
        userDTO.setSourceFolderId(sourceFolderId);


        JavaFilesParseDAO astOutPut = astMapper.getOutPut(userDTO);

        Path filePath = Paths.get(astOutPut.getPath());
        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);


        //class DAO
        ClassDAO classDAO = new ClassDAO();
        classDAO.setOutputId(astOutPut.getId());


        DependencyDAO dependencyDAO = new DependencyDAO();
        dependencyDAO.setOutputId(astOutPut.getId());


        MethodDAO methodDAO = new MethodDAO();


        for (String s : lines) {
            String line = s.trim();

            if (line.startsWith("File: ")) {
                currentFileName = line.substring(6).trim();

            } else if (line.startsWith("Package: ")) {
                currentPackage = line.substring(9).trim();

            } else if (line.startsWith("Imports: ")) {
                // 可选处理

            } else if (line.startsWith("Class: ") || line.startsWith("Interface: ")) {
                // 提交上一个类（如果有）
//                if (classDAO.getClassName() != null) {
//
//                    astMapper.insertClass(classDAO);
//
//                }

                currentClassName = line.substring(line.indexOf(" ") + 1).trim();
                classDAO = new ClassDAO();
                classDAO.setClassName(currentClassName);
                classDAO.setInterface(line.startsWith("Interface: "));
//                classDAO.setFile_id(fileMapper.getFileIdByName(currentFileName));
                classDAO.setPackageName(currentPackage);

                classDAO.setOutputId(astOutPut.getId());


            } else if (line.startsWith("Method: ")) {

                if (currentMethodName != null) {
                    methodDAO.setMethodName(currentMethodName);
                    methodDAO.setReturnType(currentReturnType);
                    methodDAO.setModifier(currentMethodModifier);
                    methodDAO.setClassId(classDAO.getId());
                    methodDAO.setOutputId(astOutPut.getId());
                    astMapper.insertMethod(methodDAO);
                    currentMethodName = null;

                    for (String param : currentParams) {
                        String[] parts = param.split(" ");
                        if (parts.length == 2) {
                            ParameterDAO paramDAO = new ParameterDAO();
                            paramDAO.setMethodId(methodDAO.getId());
                            paramDAO.setParamType(parts[0]);
                            paramDAO.setParamName(parts[1]);
                            paramDAO.setOutputId(astOutPut.getId());
                            astMapper.insertParam(paramDAO);
                        }
                    }
                }

                // 初始化新方法状态
                currentMethodName = line.substring(8).trim();
                currentReturnType = null;
                currentMethodModifier = null;
                currentParams.clear();
                methodDAO = new MethodDAO();

            } else if (line.startsWith("Class Modifier: ")) {
                currentClassModifier = line.substring(16).trim();
                classDAO.setModifier(currentClassModifier);
                astMapper.insertClass(classDAO); // 假设你有 classMapper

            } else if (line.startsWith("Method Modifier: ")) {
                currentMethodModifier = line.substring(17).trim();
            } else if (line.startsWith("Return: ")) {
                currentReturnType = line.substring(8).trim();

            } else if (line.startsWith("Param: ")) {
                currentParams.add(line.substring(7).trim());

            } else if (line.contains("Issue: ")) {
                String issueLine = line.substring(7).trim();
                if (issueLine.contains("→")) {
                    String[] parts = issueLine.split("→");
                    String usage = parts[0].replace("Uses", "").trim(); // e.g. java.io.PrintStream.println(char[])
                    String dependencyType = parts[1].trim();

                    String targetClass = null;
                    String targetMethod = null;

                    int lastDot = usage.lastIndexOf(".");
                    if (lastDot != -1) {
                        targetClass = usage.substring(0, lastDot).trim(); // e.g. java.io.PrintStream
                        targetMethod = usage.substring(lastDot + 1).trim(); // e.g. println(char[])
                    } else {
                        targetClass = usage; // fallback
                    }

                    DependencyDAO dep = new DependencyDAO();
                    dep.setSourceClass(classDAO.getClassName());
                    dep.setSourceMethod(currentMethodName);
                    dep.setTargetClass(targetClass);
                    dep.setTargetMethod(targetMethod); // ✅ 新增字段
                    dep.setDependencyType(dependencyType);
                    dep.setOutputId(astOutPut.getId());
                    astMapper.insertDependency(dep);
                }
            }

        }

// ✅ 处理最后一个方法（如果有）
        if (currentMethodName != null && methodDAO != null) {
            methodDAO.setMethodName(currentMethodName);
            methodDAO.setReturnType(currentReturnType);
            methodDAO.setModifier(String.join(" ", currentMethodModifier));
            methodDAO.setClassId(classDAO.getId());
            methodDAO.setOutputId(astOutPut.getId());
            astMapper.insertMethod(methodDAO);

            for (String param : currentParams) {
                String[] parts = param.split(" ");
                if (parts.length == 2) {
                    ParameterDAO paramDAO = new ParameterDAO();
                    paramDAO.setMethodId(methodDAO.getId());
                    paramDAO.setParamType(parts[0]);
                    paramDAO.setParamName(parts[1]);
                    paramDAO.setOutputId(astOutPut.getId());
                    astMapper.insertParam(paramDAO);
                }
            }
        }


        log.info("Complete");
    }

    @Override
    @Transactional
    public void deleteOutputReport(Integer userId, Integer sourceFolderId) {

        UserDTO userDTO = new UserDTO();
        userDTO.setId(userId);
        userDTO.setSourceFolderId(sourceFolderId);

        JavaFilesParseDAO astOutPut = astMapper.getOutPut(userDTO);

        deleteAll(astOutPut);

        deleteFile(astOutPut);

        astMapper.deleteOutputReport(astOutPut);

        log.info("Delete Complete");

    }

    @Override
    @Transactional
    public void jspMapping(UserDTO userDTO) {
        JspParseOutPutDAO outputDAO = astMapper.getJspParseOutput(userDTO);
        Path path = Paths.get(outputDAO.getPath());

        String currentPageName = null;
        String currentPagePath = null;
        List<ELexpressionDAO> elList = new ArrayList<>();
        List<PageImportDAO> importList = new ArrayList<>();
        List<TagliDAO> taglibList = new ArrayList<>();

        try {
            List<String> lines = Files.readAllLines(path);

            for (String line : lines) {
                line = line.trim();

                if (line.startsWith("Page:")) {
                    // 如果已有页面信息，先写入数据库
                    if (currentPageName != null && currentPagePath != null) {


                        persistPage(currentPageName, currentPagePath, elList, importList, taglibList);
                    }

                    // 重置变量
                    currentPageName = line.substring("Page:".length()).trim();
                    currentPagePath = null;
                    elList.clear();
                    importList.clear();
                    taglibList.clear();

                } else if (line.startsWith("Path:")) {
                    currentPagePath = line.substring("Path:".length()).trim();

                } else if (line.startsWith("EL:")) {
                    Pattern pattern = Pattern.compile("\\$\\{(.+?)}.*\\(line (\\d+)\\)");
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        String expr = matcher.group(1);
                        int lineNum = Integer.parseInt(matcher.group(2));

                        ELexpressionDAO el = new ELexpressionDAO();
                        el.setExpression("${" + expr + "}");
                        el.setLineNumber(lineNum);
                        elList.add(el);
                    }

                } else if (line.startsWith("Import:")) {
                    String importClass = line.substring("Import:".length()).trim();
                    PageImportDAO imp = new PageImportDAO();
                    imp.setImportClass(importClass);
                    importList.add(imp);

                } else if (line.startsWith("Taglib:")) {
                    Pattern pattern = Pattern.compile("prefix=(.*), uri=(.*)");
                    Matcher matcher = pattern.matcher(line.substring("Taglib:".length()).trim());
                    if (matcher.find()) {
                        TagliDAO taglib = new TagliDAO();
                        taglib.setPrefix(matcher.group(1).trim());
                        taglib.setUri(matcher.group(2).trim());
                        taglibList.add(taglib);
                    }
                }
            }

            // 处理最后一个页面
            if (currentPageName != null && currentPagePath != null) {
                persistPage(currentPageName, currentPagePath, elList, importList, taglibList);
            }

            log.info("Complete");

        } catch (IOException e) {
            throw new RuntimeException("Failed to read client-side analysis output file: " + path, e);
        }
    }

    private void persistPage(String name, String path, List<ELexpressionDAO> els,
                             List<PageImportDAO> imports, List<TagliDAO> taglibs) {

        PageDAO page = new PageDAO();
        page.setPageName(name);
        page.setPath(path);
        page.setType(path.endsWith(".jsp") ? "jsp" : "jsf");

        astMapper.insertPage(page);

        for (ELexpressionDAO el : els) {
            el.setPageId(page.getId());
            astMapper.insertExpression(el);
        }

        for (PageImportDAO imp : imports) {
            imp.setPageId(page.getId());
            astMapper.insertPageImport(imp);
        }

        for (TagliDAO taglib : taglibs) {
            taglib.setPageId(page.getId());
            astMapper.insertTaglib(taglib);
        }
    }



    public void deleteFile(JavaFilesParseDAO astOutPut) {
        String path = astOutPut.getPath();
        Path filePath = Paths.get(path);
        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                System.out.println("文件已删除: " + path);
            } else {
                System.out.println("文件不存在: " + path);
            }
        } catch (IOException e) {
            System.err.println("删除文件时出错: " + e.getMessage());
        }
    }

    private void deleteDependency(JavaFilesParseDAO astOutPut)
    {
        List<DependencyDAO> dependencyDAOS = astMapper.getDependencies(astOutPut);
        while(!dependencyDAOS.isEmpty())
        {
            for(DependencyDAO dependencyDAO : dependencyDAOS)
            {
                astMapper.deleteDependency(dependencyDAO);
            }

            dependencyDAOS = astMapper.getDependencies(astOutPut);
        }
    }

    private void deleteParameters(JavaFilesParseDAO astOutPut) {
        List<ParameterDAO> parameterDAOs = astMapper.getParameters(astOutPut);
        while(!parameterDAOs.isEmpty())
        {
            for(ParameterDAO parameterDAO : parameterDAOs)
            {
                astMapper.deleteParameter(parameterDAO);
            }

            parameterDAOs = astMapper.getParameters(astOutPut);
        }
    }

    private void deleteMethods(JavaFilesParseDAO astOutPut) {
        List<MethodDAO> methodDAOs = astMapper.getMethods(astOutPut);
        while(!methodDAOs.isEmpty())
        {
            for(MethodDAO methodDAO : methodDAOs)
            {
                astMapper.deleteMethod(methodDAO);
            }

            methodDAOs = astMapper.getMethods(astOutPut);
        }
    }

    private void deleteClasses(JavaFilesParseDAO astOutPut) {
        List<ClassDAO> classDAOs = astMapper.getClasses(astOutPut);
        while(!classDAOs.isEmpty())
        {
            for(ClassDAO classDAO : classDAOs)
            {
                astMapper.deleteClass(classDAO);
            }

            classDAOs = astMapper.getClasses(astOutPut);
        }
    }

    private void deleteAll(JavaFilesParseDAO astOutPut)
    {
        deleteDependency(astOutPut);
        deleteParameters(astOutPut);
        deleteMethods(astOutPut);
        deleteClasses(astOutPut);
    }


}

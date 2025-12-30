package com.hybriddependcyanlysis.Service.Impl;

import com.hybriddependcyanlysis.Mapper.AstMapper;
import com.hybriddependcyanlysis.Mapper.FileMapper;
import com.hybriddependcyanlysis.POJO.DAO.AstOutPutDAO;
import com.hybriddependcyanlysis.POJO.DAO.AstSchema.ClassDAO;
import com.hybriddependcyanlysis.POJO.DAO.AstSchema.DependencyDAO;
import com.hybriddependcyanlysis.POJO.DAO.AstSchema.MethodDAO;
import com.hybriddependcyanlysis.POJO.DAO.AstSchema.ParameterDAO;
import com.hybriddependcyanlysis.POJO.DAO.FileDAO;
import com.hybriddependcyanlysis.POJO.DTO.UserDTO;
import com.hybriddependcyanlysis.Service.AstService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class AstServiceImpl implements AstService {

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private AstMapper astMapper;

    public void getOutPutFile(Integer userId, Integer sourceFolderId) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(userId);
        userDTO.setSourceFolderId(sourceFolderId);

        AstOutPutDAO astOutPut = astMapper.getOutPut(userDTO);


    }

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


        AstOutPutDAO astOutPut = astMapper.getOutPut(userDTO);

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
//                    classDAO.setFile_id(fileMapper.getFileIdByName(currentFileName));
//
//                }

                currentClassName = line.substring(line.indexOf(" ") + 1).trim();
                classDAO = new ClassDAO();
                classDAO.setClassName(currentClassName);
                classDAO.setInterface(line.startsWith("Interface: "));
//                classDAO.setFile_id(fileMapper.getFileIdByName(currentFileName));
                classDAO.setPackageName(currentPackage);
                classDAO.setModifier(currentClassModifier);
                classDAO.setOutputId(astOutPut.getId());
                astMapper.insertClass(classDAO); // 假设你有 classMapper

            } else if (line.startsWith("Method: ")) {
                // 提交上一个方法（如果有）
                if (currentMethodName != null) {
                    methodDAO.setMethodName(currentMethodName);
                    methodDAO.setReturnType(currentReturnType);
                    methodDAO.setModifier(currentMethodModifier);
                    methodDAO.setClassId(classDAO.getId());
                    methodDAO.setOutputId(astOutPut.getId());
                    astMapper.insertMethod(methodDAO); // 假设你有 methodMapper

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
                    astMapper.insertParam(paramDAO);
                }
            }
        }


        log.info("Complete");
    }
}

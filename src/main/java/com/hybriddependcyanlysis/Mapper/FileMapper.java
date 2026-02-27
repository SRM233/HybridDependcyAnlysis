package com.hybriddependcyanlysis.Mapper;

import com.hybriddependcyanlysis.POJO.DAO.FileDAO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;

@Mapper
public interface FileMapper {


    void insertFile(FileDAO file);

    ArrayList<FileDAO> getFileBySourceFolderId(Integer sourceFolderId);

    Integer getFileIdByName(String name);

    void deleteFile(FileDAO fileDAO);
}

package com.hybriddependcyanlysis.Mapper;


import com.hybriddependcyanlysis.POJO.DAO.SourceFolderDAO;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;


@Mapper
public interface IngestMapper {

    void insertSourceFolder(SourceFolderDAO sourceFolderDAO);

    @Select("select zip_path from source_folder where id = #{sourceFolderId}")
    String getPathById(Integer sourceFolderId);

    @Select("select dir_path from source_folder where id = #{sourceFolderId}")
    String getDirectoryPathById(Integer sourceFolderId);


    SourceFolderDAO getById(Integer sourceFolderId);

    @Delete("delete from source_folder where id = #{sourceFolderId} and user_id = #{userId}")
    void deleteById(Integer userId, Integer sourceFolderId);


    List<SourceFolderDAO> getSourceFolder(SourceFolderDAO sourceFolderDAO);
}

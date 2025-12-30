package com.hybriddependcyanlysis.IngestModule.Mapper;


import com.hybriddependcyanlysis.POJO.DAO.SourceFolderDAO;

import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface IngestMapper {

    void insertSourceFolder(SourceFolderDAO sourceFolderDAO);
}

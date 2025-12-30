package com.hybriddependcyanlysis.Mapper;

import com.hybriddependcyanlysis.POJO.DAO.AstErrorDAO;
import com.hybriddependcyanlysis.POJO.DAO.AstOutPutDAO;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface ParseSoruceCodeMapper {

    String getSourceFolderPathById(Integer id);

    void insertAstOutPut(AstOutPutDAO astOutPutDAO);

    void insertAstError(AstErrorDAO astErrorDAO);
}

package com.hybriddependcyanlysis.Mapper;

import com.hybriddependcyanlysis.POJO.DAO.AstErrorDAO;
import com.hybriddependcyanlysis.POJO.DAO.AstOutPutDAO;
import com.hybriddependcyanlysis.POJO.DAO.AstSchema.ClassDAO;
import com.hybriddependcyanlysis.POJO.DAO.AstSchema.DependencyDAO;
import com.hybriddependcyanlysis.POJO.DAO.AstSchema.MethodDAO;
import com.hybriddependcyanlysis.POJO.DAO.AstSchema.ParameterDAO;
import com.hybriddependcyanlysis.POJO.DTO.UserDTO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AstMapper {
    AstOutPutDAO getOutPut(UserDTO userDTO);

    AstOutPutDAO getOutPutBySourceFolderId(Integer sourceFolderId);

    AstErrorDAO getErrorBySourceFolderId(Integer sourceFolderId);

    void updateOutput(AstOutPutDAO astOutPutDAO);

    void updateError(AstOutPutDAO astOutPutDAO);

    void insertClass(ClassDAO classDAO);

    void insertMethod(MethodDAO methodDAO);

    void insertParam(ParameterDAO paramDAO);

    void insertDependency(DependencyDAO dep);
}

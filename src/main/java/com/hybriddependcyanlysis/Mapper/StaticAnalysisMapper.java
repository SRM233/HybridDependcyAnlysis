package com.hybriddependcyanlysis.Mapper;

import com.hybriddependcyanlysis.POJO.DAO.AstOutPutDAO;
import org.apache.ibatis.annotations.Mapper;

import java.util.HashMap;
import java.util.List;

@Mapper
public interface StaticAnalysisMapper {
    List<HashMap<String, Object>> getDependencyCountGroupByType(AstOutPutDAO userDTO);
}

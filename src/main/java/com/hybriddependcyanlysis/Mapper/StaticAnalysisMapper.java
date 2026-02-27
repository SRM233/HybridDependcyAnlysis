package com.hybriddependcyanlysis.Mapper;

import com.hybriddependcyanlysis.POJO.DAO.JavaFilesParseDAO;
import com.hybriddependcyanlysis.POJO.DAO.JspParseOutPutDAO;
import org.apache.ibatis.annotations.Mapper;

import java.util.HashMap;
import java.util.List;

@Mapper
public interface StaticAnalysisMapper {
    List<HashMap<String, Object>> getDependencyCountGroupByType(JavaFilesParseDAO userDTO);

    List<HashMap<String, Object>> getELexpression(JspParseOutPutDAO clientSideOutPutDAO);
}

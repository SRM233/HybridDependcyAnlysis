package com.hybriddependcyanlysis.Service.Impl;

import com.hybriddependcyanlysis.Mapper.AstMapper;
import com.hybriddependcyanlysis.Mapper.StaticAnalysisMapper;
import com.hybriddependcyanlysis.POJO.DAO.AstOutPutDAO;
import com.hybriddependcyanlysis.POJO.DAO.UserDAO;
import com.hybriddependcyanlysis.POJO.DTO.UserDTO;
import com.hybriddependcyanlysis.Service.StaticAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class StaticAnalysisServiceImpl implements StaticAnalysisService {


    @Autowired
    private StaticAnalysisMapper staticAnalysisMapper;

    @Autowired
    private AstMapper astMapper;

    @Override
    @Transactional
    public HashMap<String, Integer> dependencyCounting(Integer userId, Integer sourceFolderId) {

        UserDTO userDTO = new UserDTO();
        userDTO.setId(userId);
        userDTO.setSourceFolderId(sourceFolderId);

        AstOutPutDAO astOutPutDAO = astMapper.getOutPut(userDTO);

        List<HashMap<String, Object>> resultList = staticAnalysisMapper.getDependencyCountGroupByType(astOutPutDAO);

        HashMap<String, Integer> resultMap = new HashMap<>();
        for (Map<String, Object> row : resultList) {
            String type = (String) row.get("dependency_type");
            Integer count = ((Number) row.get("calledNums")).intValue(); // 确保 SQL 中别名是 calledNums
            resultMap.put(type, count);
        }

        return resultMap;
    }

}


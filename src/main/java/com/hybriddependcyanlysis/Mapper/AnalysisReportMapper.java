package com.hybriddependcyanlysis.Mapper;

import com.hybriddependcyanlysis.POJO.DAO.AnalysisResultDAO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnalysisReportMapper {
    AnalysisResultDAO getAnalysisResult(AnalysisResultDAO analysisResultDTO);

    void insertResult(AnalysisResultDAO analysisResultDAO);

    void updateResult(AnalysisResultDAO analysisResultDAO);
}

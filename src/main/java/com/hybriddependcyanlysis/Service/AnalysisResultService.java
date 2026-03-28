package com.hybriddependcyanlysis.Service;

import com.hybriddependcyanlysis.POJO.DTO.AnalysisResultDTO;

public interface AnalysisResultService {

    Object getAnnotationReport(AnalysisResultDTO analysisResultDTO);
    Object getWebXmlReport(AnalysisResultDTO analysisResultDTO);
    Object getFileStoreReport(AnalysisResultDTO analysisResultDTO);
    Object getPersistenceReport(AnalysisResultDTO analysisResultDTO);
    Object getEjbJarReport(AnalysisResultDTO analysisResultDTO);
    Object getPomXmlReport(AnalysisResultDTO analysisResultDTO);
    Object getFacesConfigReport(AnalysisResultDTO analysisResultDTO);
}

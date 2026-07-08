package com.hybriddependcyanlysis.Service;

import java.io.IOException;

public interface DynamicAnalysisService {
    void jarPack(Integer sourceFolderId) throws IOException;

    void javaAgent(Integer sourceFolderId);
}

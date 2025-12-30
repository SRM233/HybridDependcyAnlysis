package com.hybriddependcyanlysis.Service;

import java.io.IOException;

public interface DynamicAnalysisService {
    void jarPack(Integer id) throws IOException;

    void javaAgent();
}

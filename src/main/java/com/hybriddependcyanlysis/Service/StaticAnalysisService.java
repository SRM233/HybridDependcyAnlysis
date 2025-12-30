package com.hybriddependcyanlysis.Service;

import java.util.HashMap;

public interface StaticAnalysisService {
    HashMap<String, Integer> dependencyCounting(Integer userId, Integer sourceFolderId);
}

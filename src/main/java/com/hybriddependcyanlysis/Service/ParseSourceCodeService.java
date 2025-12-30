package com.hybriddependcyanlysis.Service;

import java.io.IOException;

public interface ParseSourceCodeService {
    void staticParsing(Integer sourceFolderId) throws IOException;
}

package com.hybriddependcyanlysis.POJO.DAO.ClientSideSchema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ELexpressionDAO {
    private Integer id;
    private Integer pageId;
    private String expression;
    private String targetClass;
    private String targetMethod;
    private Integer lineNumber;

}

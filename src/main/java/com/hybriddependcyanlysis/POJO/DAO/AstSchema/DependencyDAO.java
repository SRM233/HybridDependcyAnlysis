package com.hybriddependcyanlysis.POJO.DAO.AstSchema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class DependencyDAO {
    private Integer id;
    private Integer outputId;
    private String sourceClass;
    private String sourceMethod;
    private String targetClass;
    private String targetMethod;
    private String dependencyType;
}

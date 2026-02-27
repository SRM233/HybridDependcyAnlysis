package com.hybriddependcyanlysis.POJO.DAO.AstSchema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ClassDAO {
    private Integer id;
    private Integer outputId;
    private String className;
    private boolean isInterface;
    private String modifier;
    private String packageName;
}

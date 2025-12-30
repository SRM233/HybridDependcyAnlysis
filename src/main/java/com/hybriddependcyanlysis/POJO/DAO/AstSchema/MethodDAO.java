package com.hybriddependcyanlysis.POJO.DAO.AstSchema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class MethodDAO {
    private Integer id;
    private Integer classId;
    private Integer outputId;
    private String methodName;
    private String returnType;
    private String modifier;

}

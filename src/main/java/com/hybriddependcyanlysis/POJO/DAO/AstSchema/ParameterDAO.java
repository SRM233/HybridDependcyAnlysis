package com.hybriddependcyanlysis.POJO.DAO.AstSchema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ParameterDAO {
    private Integer id;
    private Integer methodId;
    private Integer outputId;
    private String paramName;
    private String paramType;
    private Integer position;

}

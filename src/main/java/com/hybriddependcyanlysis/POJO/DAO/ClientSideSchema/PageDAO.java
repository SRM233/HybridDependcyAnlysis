package com.hybriddependcyanlysis.POJO.DAO.ClientSideSchema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PageDAO {
    private Integer id;
    private String pageName;
    private String path;
    private String type;
}

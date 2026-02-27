package com.hybriddependcyanlysis.POJO.DAO.ClientSideSchema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PageImportDAO {
    private Integer id;
    private Integer pageId;
    private String importClass;

}

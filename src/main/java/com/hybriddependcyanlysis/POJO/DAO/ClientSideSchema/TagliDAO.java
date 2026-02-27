package com.hybriddependcyanlysis.POJO.DAO.ClientSideSchema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TagliDAO {
    private Integer id;
    private Integer pageId;
    private String prefix;
    private String uri;
}

package com.hybriddependcyanlysis.POJO.DAO.ClientSideSchema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


enum SessionScope{
    PAGE, REQUEST, SESSION, APPLICATION
}

@AllArgsConstructor
@NoArgsConstructor
@Data
public class BeanUsageDAO {
    private Integer id;
    private Integer pageId;
    private String beanName;
    private SessionScope scope;
    private String className;

}

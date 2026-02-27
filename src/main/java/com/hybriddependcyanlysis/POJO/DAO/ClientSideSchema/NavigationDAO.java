package com.hybriddependcyanlysis.POJO.DAO.ClientSideSchema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class NavigationDAO {
    private Integer id;
    private String fromPage;
    private String toPage;
    private String activate;
}

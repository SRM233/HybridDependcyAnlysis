package com.hybriddependcyanlysis.POJO.DAO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserDAO {
    private Integer id;
    private String userName;
    private String passWord;
    private List<Integer> fileIds;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

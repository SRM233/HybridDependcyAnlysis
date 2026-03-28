package com.hybriddependcyanlysis.POJO.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserDTO {
    private Integer id;
    private String username;
    private String password;
    private Integer sourceFolderId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

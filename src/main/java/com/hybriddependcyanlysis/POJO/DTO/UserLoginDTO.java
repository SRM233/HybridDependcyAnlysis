package com.hybriddependcyanlysis.POJO.DTO;

import com.hybriddependcyanlysis.POJO.DAO.UserDAO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserLoginDTO {
    private UserDAO user;
    private String token;
    private List<String> permissions;

    public <T> UserLoginDTO(int i, String admin, String token, List<T> list) {
    }
}

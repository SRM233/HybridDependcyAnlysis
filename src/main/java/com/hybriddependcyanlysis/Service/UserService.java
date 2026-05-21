package com.hybriddependcyanlysis.Service;

import com.hybriddependcyanlysis.POJO.DAO.UserDAO;
import com.hybriddependcyanlysis.POJO.DTO.UserDTO;
import com.hybriddependcyanlysis.POJO.DTO.UserLoginDTO;

public interface UserService {
    void register(UserDTO userDTO);

    UserDAO login(String username, String password);

    UserDAO getUser(Integer userId);

    void logout();

    void deleteUser(Integer userId);

    UserDAO getUserByName(String username);
}

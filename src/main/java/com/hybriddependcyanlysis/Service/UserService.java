package com.hybriddependcyanlysis.Service;

import com.hybriddependcyanlysis.POJO.DAO.UserDAO;
import com.hybriddependcyanlysis.POJO.DTO.UserDTO;
import com.hybriddependcyanlysis.POJO.DTO.UserLoginDTO;

public interface UserService {
    void register(UserDTO userDTO);

    UserDAO login(String username, String password);

    void deleteSourdeFolders(Integer userDTO, Integer sourceFolderId);

    void deleteASTFiles(Integer userId, Integer outputId);

    void deleteJspAstFiles(Integer userId, Integer outputId);

    UserDAO getUser(Integer userId);

    void logout();

    void deleteUser(Integer userId);
}

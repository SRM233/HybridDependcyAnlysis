package com.hybriddependcyanlysis.Service.Impl;

import com.hybriddependcyanlysis.Mapper.IngestMapper;
import com.hybriddependcyanlysis.Mapper.ParseSourceCodeMapper;
import com.hybriddependcyanlysis.Mapper.UserMapper;
import com.hybriddependcyanlysis.POJO.DAO.UserDAO;
import com.hybriddependcyanlysis.POJO.DTO.UserDTO;
import com.hybriddependcyanlysis.Service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private IngestMapper ingestMapper;

    @Autowired
    private ParseSourceCodeMapper parseSourceCodeMapper;

    @Override
    public void register(UserDTO userDTO) {
        UserDAO userDAO = new UserDAO();
        BeanUtils.copyProperties(userDTO, userDAO);
        userDAO.setCreateTime(LocalDateTime.now());
        userDAO.setUpdateTime(LocalDateTime.now());

        userMapper.insertUser(userDAO);

    }

    @Override
    public UserDAO login(String userName, String password) {

        UserDAO userDAO = userMapper.getUser(userName, password);


        return userDAO;
    }

    @Override
    public void deleteSourdeFolders(Integer userId, Integer sourceFolderId) {
        ingestMapper.deleteById(userId, sourceFolderId);
    }

    @Override
    @Transactional
    public void deleteASTFiles(Integer userId, Integer outputId) {
        parseSourceCodeMapper.deleteASTErrorlog(userId, outputId);
        parseSourceCodeMapper.deleteASTOutput(userId, outputId);
    }

    @Override
    public void deleteJspAstFiles(Integer userId, Integer outputId) {
        parseSourceCodeMapper.deleteJspAstErrorlog(userId, outputId);
        parseSourceCodeMapper.deleteJspAstOutput(userId, outputId);
    }


}

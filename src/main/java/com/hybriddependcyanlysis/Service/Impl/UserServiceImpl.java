package com.hybriddependcyanlysis.Service.Impl;

import Common.JWT.JwtUtil;
import Common.Result;
import Common.UserContext.UserContextHolder;
import Common.Util.SecretConstant;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import com.hybriddependcyanlysis.Mapper.AnalysisReportMapper;
import com.hybriddependcyanlysis.Mapper.IngestMapper;
import com.hybriddependcyanlysis.Mapper.ParseSourceCodeMapper;
import com.hybriddependcyanlysis.Mapper.StaticAnalysisMapper;
import com.hybriddependcyanlysis.Mapper.UserMapper;
import com.hybriddependcyanlysis.POJO.DAO.UserDAO;
import com.hybriddependcyanlysis.POJO.DTO.UserDTO;
import com.hybriddependcyanlysis.POJO.DTO.UserLoginDTO;
import com.hybriddependcyanlysis.Service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private IngestMapper ingestMapper;

    @Autowired
    private ParseSourceCodeMapper parseSourceCodeMapper;

    @Autowired
    private AnalysisReportMapper analysisReportMapper;

    @Autowired
    private StaticAnalysisMapper staticAnalysisMapper;


    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void register(UserDTO userDTO) {
        UserDAO userDAO = new UserDAO();
        //使用BeanUtil复制属性给userDAO
        BeanUtils.copyProperties(userDTO, userDAO);

        //设置创建时间和更新时间为当前
        userDAO.setCreateTime(LocalDateTime.now());
        userDAO.setUpdateTime(LocalDateTime.now());

        //插入userDAO到user table
        userMapper.insertUser(userDAO);

    }

    @Override
    public UserDAO login(String username, String password) {

//        RSA rsa = new RSA(SecretConstant.PRIVATE_KEY, SecretConstant.PUBLIC_KEY);
//        String decryptUsername = rsa.decryptStr(username, KeyType.PrivateKey);
//        String decryptPassword = rsa.decryptStr(password, KeyType.PrivateKey);

        UserDAO userDAO = userMapper.getUserByName(username);




        if(userDAO.getPassword().equals(password))
        {
            return userDAO;
        }

        throw  new RuntimeException("Password is incorrect");
    }



    @Override
    public UserDAO getUser(Integer userId) {
        UserDAO userDAO = userMapper.getUserById(userId);

        if(userDAO == null)
        {
            throw new RuntimeException("User not found");
        }

        return userDAO;
    }

    @Override
    public void logout() {

        //释放Thread内存
        UserContextHolder.clear();
    }

    @Override
    @Transactional
    public void deleteUser(Integer userId) {
        UserDAO userDAO = userMapper.getUserById(userId);
        if (userDAO == null) {
            throw new RuntimeException("User not found");
        }

        UserContextHolder.clear();
        
        userMapper.deleteUser(userId);
    }

    @Override
    public UserDAO getUserByName(String username) {
        return userMapper.getUserByName(username);
    }


}

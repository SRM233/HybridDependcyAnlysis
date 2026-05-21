package com.hybriddependcyanlysis.Mapper;

import com.hybriddependcyanlysis.POJO.DAO.UserDAO;
import com.hybriddependcyanlysis.POJO.DTO.UserLoginDTO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    @Insert("insert into user (username, password, create_time, update_time) values (#{username}, #{password}, #{createTime}, #{updateTime})")
    void insertUser(UserDAO userDAO);


    @Select("select * from user where username = #{username} and password = #{password}")
    UserDAO getUser(UserLoginDTO userLoginDTO);


    @Select("select * from user where id = #{userId}")
    UserDAO getUserById(Integer userId);

    @Delete("delete from user where id = #{userId}")
    void deleteUser(Integer userId);

    @Select("select * from user where username = #{username}")
    UserDAO getUserByName(String username);
}

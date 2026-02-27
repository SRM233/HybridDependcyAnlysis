package com.hybriddependcyanlysis.Mapper;

import com.hybriddependcyanlysis.POJO.DAO.UserDAO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    @Insert("insert into hybridanalysis.user (user_name, password, create_time, update_time) values (#{userName}, #{password}, #{createTime}, #{updateTime})")
    void insertUser(UserDAO userDAO);


    @Select("select * from hybridanalysis.user where user_name = #{userName} and password = #{password}")
    UserDAO getUser(String userName, String password);
}

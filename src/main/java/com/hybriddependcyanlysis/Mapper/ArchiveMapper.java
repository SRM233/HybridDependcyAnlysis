package com.hybriddependcyanlysis.Mapper;

import com.hybriddependcyanlysis.POJO.DAO.SourceFolderDAO;
import com.hybriddependcyanlysis.POJO.DTO.UserDTO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ArchiveMapper {

    SourceFolderDAO getSouceFolder(UserDTO userDTO);
}

package com.hybriddependcyanlysis.POJO.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class SourceFolderQueryDTO {
    private Integer userId;
    private Integer pageSize;
    private Integer pageNumber;

}

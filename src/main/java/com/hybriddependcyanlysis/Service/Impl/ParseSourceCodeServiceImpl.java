package com.hybriddependcyanlysis.Service.Impl;
import com.hybriddependcyanlysis.Mapper.AstMapper;
import com.hybriddependcyanlysis.Mapper.FileMapper;
import com.hybriddependcyanlysis.Mapper.ParseSoruceCodeMapper;
import com.hybriddependcyanlysis.POJO.DAO.AstErrorDAO;
import com.hybriddependcyanlysis.POJO.DAO.AstOutPutDAO;
import com.hybriddependcyanlysis.POJO.DAO.FileDAO;
import com.hybriddependcyanlysis.POJO.DTO.UserDTO;
import com.hybriddependcyanlysis.Service.ParseSourceCodeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
public class ParseSourceCodeServiceImpl implements ParseSourceCodeService {

    @Autowired
    private ParseSoruceCodeMapper parseSoruceCodeMapper;

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private SpoonServicesImpl spoonServices;

    @Autowired
    private AstMapper astMapper;

    @Override
    @Transactional
    public void staticParsing(Integer sourceFolderId) throws IOException {


        ArrayList<FileDAO> files = fileMapper.getFileBySourceFolderId(sourceFolderId);

        File outPutFolder = new File(files.get(0).getUnpackPath() + "\\output");
        if (!outPutFolder.exists())
        {
            boolean mkdir = outPutFolder.mkdir();
        }
        String rootPath = files.get(0).getUnpackPath();
        File output = new File(rootPath + "\\output\\analysis_output.txt");
        File errorLog = new File(rootPath + "\\output\\errorLog.txt");


        parsing(output, errorLog, files,sourceFolderId);
    }

    @Transactional
    public void parsing(File output, File errorLog, ArrayList<FileDAO> files, Integer sourceFolderId) throws IOException {

        spoonServices.parsing(output, errorLog, files);



        AstOutPutDAO astOutPutDAO = astMapper.getOutPutBySourceFolderId(sourceFolderId);

        if(astOutPutDAO==null)
        {
            astOutPutDAO.setPath(output.getAbsolutePath());
            astOutPutDAO.setName("astAnalysisOutput");
            astOutPutDAO.setSourceFolderId(sourceFolderId);
            astOutPutDAO.setUserId(1);
            astOutPutDAO.setCreateTime(LocalDateTime.now());
            astOutPutDAO.setUpdateTime(LocalDateTime.now());
            parseSoruceCodeMapper.insertAstOutPut(astOutPutDAO);
        }

        AstErrorDAO astErrorDAO = astMapper.getErrorBySourceFolderId(sourceFolderId);

        if(astErrorDAO == null)
        {
            BeanUtils.copyProperties(astOutPutDAO, astErrorDAO);
            astErrorDAO.setOutputId(astOutPutDAO.getId());
            astErrorDAO.setName("astError");
            parseSoruceCodeMapper.insertAstError(astErrorDAO);
        }


        astOutPutDAO.setUpdateTime(LocalDateTime.now());
        astErrorDAO.setUpdateTime(LocalDateTime.now());

        astMapper.updateOutput(astOutPutDAO);
        astMapper.updateError(astOutPutDAO);




        System.out.println("Spoon-based analysis complete. Results in "
                    + output.getAbsolutePath() + ", errors in " + errorLog.getAbsolutePath());
    }


}

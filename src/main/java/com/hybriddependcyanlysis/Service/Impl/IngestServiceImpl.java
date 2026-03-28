package com.hybriddependcyanlysis.Service.Impl;

import Common.Result;
import Common.UserContext.UserContextHolder;
import com.hybriddependcyanlysis.Mapper.FileMapper;
import com.hybriddependcyanlysis.Mapper.IngestMapper;
import com.hybriddependcyanlysis.POJO.DAO.SourceFolderDAO;
import com.hybriddependcyanlysis.POJO.DTO.UserFolderDTO;
import com.hybriddependcyanlysis.Service.IngestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class IngestServiceImpl implements IngestService {

    @Autowired
    private IngestMapper ingestMapper;

    @Autowired
    private FileMapper fileMapper;

    @Transactional
    @Override
    public void sourceFolderIngest(UserFolderDTO userFolderDTO) {
        Integer userId = UserContextHolder.getUserId();

        userFolderDTO.setId(userId);

        try {
            SourceFolderDAO sourceFolderDAO = new SourceFolderDAO();

            // Create a unique folder for this upload
            String uniqueFolderName = userFolderDTO.getFile().getOriginalFilename()
                    .replaceAll("\\.zip$", "") + "_" + System.currentTimeMillis();

            Path storageDir = Paths.get("E:/FYP/ProgramStorage", uniqueFolderName);
            Files.createDirectories(storageDir);

            // Copy uploaded file into that folder
            Path storagePath = storageDir.resolve(userFolderDTO.getFile().getOriginalFilename());
            Files.copy(userFolderDTO.getFile().getInputStream(), storagePath, StandardCopyOption.REPLACE_EXISTING);
            sourceFolderDAO.setZipPath(storagePath.toString());
            sourceFolderDAO.setUserId(userId);
            sourceFolderDAO.setZipName(userFolderDTO.getFile().getOriginalFilename());
            sourceFolderDAO.setDirPath(storageDir.toString());

            sourceFolderDAO.setCreateTime(LocalDateTime.now());
            sourceFolderDAO.setUpdateTime(LocalDateTime.now());


            // Call unpack function
            unpackZip(storagePath.toFile(), storageDir, sourceFolderDAO);

            ingestMapper.insertSourceFolder(sourceFolderDAO);


        } catch (IOException e) {
            throw new RuntimeException("File preparation failed", e);
        }

    }

    private void unpackZip(File zipFile, Path targetDir, SourceFolderDAO sourceFolderDAO) {
//        ArrayList<FileDAO> files = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path newFile = targetDir.resolve(entry.getName());
                if(sourceFolderDAO.getUnpackPath() == null )
                {
                    sourceFolderDAO.setUnpackPath(newFile.toString());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(newFile);

                } else {
                    Files.createDirectories(newFile.getParent());
                    Files.copy(zis, newFile, StandardCopyOption.REPLACE_EXISTING);
                }
//                FileDAO fileDAO = new FileDAO();
//                fileDAO.setFileName(entry.getName());
//                fileDAO.setFilePath(newFile.toString());
//                fileDAO.setCreateTime(LocalDateTime.now());
//                fileDAO.setUpdateTime(LocalDateTime.now());
//                fileDAO.setSourceFolderId(sourceFolderDAO.getId());
//                fileDAO.setUnpackPath(targetDir.toString());
//                files.add(fileDAO);
            }


//            if (!files.isEmpty()) {
//                for(FileDAO file: files)
//                {
//                    fileMapper.insertFile(file);
//                }
//            }

//            System.out.println("Unpack Finish: " + targetDir);
        } catch (IOException e) {
            throw new RuntimeException("Unpack failed", e);
        }
    }
}

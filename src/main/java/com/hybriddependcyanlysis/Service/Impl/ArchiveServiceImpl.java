package com.hybriddependcyanlysis.Service.Impl;

import com.hybriddependcyanlysis.Mapper.IngestMapper;
import com.hybriddependcyanlysis.Mapper.ArchiveMapper;
import com.hybriddependcyanlysis.Mapper.FileMapper;
import com.hybriddependcyanlysis.POJO.DAO.FileDAO;
import com.hybriddependcyanlysis.POJO.DAO.SourceFolderDAO;
import com.hybriddependcyanlysis.POJO.DTO.UserDTO;
import com.hybriddependcyanlysis.Service.ArchiveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

@Service
public class ArchiveServiceImpl implements ArchiveService {

    @Autowired
    private ArchiveMapper archiveMapper;

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private IngestMapper ingestMapper;

    @Override
    @Transactional
    public void deleteArchive(UserDTO userDTO) {
        SourceFolderDAO sourceFolderDAO = archiveMapper.getSouceFolder(userDTO);



        if(sourceFolderDAO != null)
        {

            List<FileDAO> fileDAOS = fileMapper.getFileBySourceFolderId(sourceFolderDAO.getId());

            if(!fileDAOS.isEmpty())
            {


                for(FileDAO fileDAO : fileDAOS)
                {

                    String path = fileDAO.getFilePath();

                    deleteFiles(path);

                    fileMapper.deleteFile(fileDAO);
                }
            }

            String path = sourceFolderDAO.getZipPath();

            deleteFiles(path);
        }
    }

    @Override
    public void deleteAll(Integer sourceFolderId) {
        File drLocation = new File(ingestMapper.getPathById(sourceFolderId));

    }

    public void deleteFiles(String path)
    {
        Path filePath = Paths.get(path);
        try {
            if (Files.exists(filePath)) {
                if (Files.isDirectory(filePath)) {
                    // 递归删除文件夹内容
                    Files.walkFileTree(filePath, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            System.out.println("已删除文件: " + file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            Files.delete(dir);
                            System.out.println("已删除文件夹: " + dir);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } else {
                    boolean deleted = Files.deleteIfExists(filePath);
                    if (deleted) {
                        System.out.println("已删除文件: " + path);
                    } else {
                        System.out.println("文件不存在: " + path);
                    }
                }
            } else {
                System.out.println("路径不存在: " + path);
            }
        } catch (IOException e) {
            System.err.println("删除时出错: " + e.getMessage());
        }
    }
}

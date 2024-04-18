package com.cusob.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MinioService {

    /**
     * upload File
     * @param bucketName
     * @param file
     * @return
     */
    String uploadFile(String bucketName, MultipartFile file);

    /**
     * batch Remove files
     * @param urlList
     */
    void batchRemove(List<String> urlList);

    /**
     * upload Avatar
     * @param bucketName
     * @param file
     * @return
     */
    String uploadAvatar(String bucketName, MultipartFile file);
}

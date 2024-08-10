package com.hj.blogBatch.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import com.hj.blogBatch.entity.AtFile;

public interface CommonService {

    Long downloadImg(String urlStr) throws IOException;
    AtFile attachFile(String fileType, byte[] response, String fileNm) throws FileNotFoundException, IOException;
    void downloadVideo(String videoUrl, String outputFilePath) throws IOException;
    AtFile attachFileNotMove(String filePath);
    AtFile attachFileUploadS3(AtFile atFile);
}

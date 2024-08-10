package com.hj.blogBatch.service;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.hj.blogBatch.entity.AtFile;
import com.hj.blogBatch.repository.AtFileRepository;

import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommonServiceImp implements CommonService {

    @Value("${com.blogBatch.img.path}")
    String imgPath;
    @Value("${com.aws.s3.bucketName}")
    String s3BucketName;

    private final AtFileRepository atFileRepository;
    private final AmazonS3 amazonS3;

    @Override
    @Transactional
    public Long downloadImg(String urlStr) throws IOException {
        InputStream in = null;
        ByteArrayOutputStream out = null;

        if (urlStr.indexOf("http") == -1) {
            urlStr = "https://" + urlStr;
        }
        if (urlStr.indexOf("?") != -1) {
            urlStr = urlStr.split("\\?")[0];
        }
        String fileNm = urlStr.substring(urlStr.lastIndexOf("/") + 1);
        String fileType = fileNm.substring(fileNm.lastIndexOf(".") + 1);

        URL url;
        try {

            // Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1",
            // 8888));

            url = new URL(urlStr);

            InputStream openStream = url.openConnection(Proxy.NO_PROXY).getInputStream();

            // ReadableByteChannel readableByteChannel = Channels.newChannel(openStream)

            in = new BufferedInputStream(openStream);
            out = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int n = 0;
            while (-1 != (n = in.read(buf))) {
                out.write(buf, 0, n);
            }

            byte[] response = out.toByteArray();

            AtFile atFile = attachFile(fileType, response, fileNm);

            return atFile.getFileId();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {

            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }

        }

    }

    @Transactional
    @Override
    public AtFile attachFile(String fileType, byte[] response, String orgfileNm)
            throws FileNotFoundException, IOException {
        AtFile atFile = new AtFile();
        atFileRepository.saveAndFlush(atFile);
        String fileNm = atFile.getFileId() + "." + fileType;
        if (StringUtils.isBlank(fileNm)) {
            orgfileNm = fileNm;
        }
        atFile.setFileNm(fileNm);
        atFile.setOrgFileNm(orgfileNm);
        atFileRepository.save(atFile);

        // 파일 쓰기
        FileOutputStream fos = new FileOutputStream(imgPath + fileNm);
        fos.write(response);
        fos.close();
        return atFile;
    }

    @Override
    public void downloadVideo(String videoUrl, String outputFilePath) throws IOException {
        URL url = new URL(videoUrl);
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        try (InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                FileOutputStream fileOutputStream = new FileOutputStream(outputFilePath)) {

            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer, 0, 1024)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
            log.info("-------- finish download video videoUrl:" + videoUrl + " outputFilePath :" + outputFilePath
                    + "  ---------");
        }
    }

    @Transactional
    @Override
    public AtFile attachFileNotMove(String filePath) {

        if (filePath.startsWith(imgPath)) {
            filePath = filePath.substring(imgPath.length());
        }
        AtFile atFile = new AtFile();
        atFile.setFileNm(filePath);
        atFile.setOrgFileNm(filePath);
        atFile.setRegDt(LocalDateTime.now());
        atFileRepository.saveAndFlush(atFile);

        return atFile;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public AtFile attachFileUploadS3(AtFile atFile) {
        File file = new File(imgPath + atFile.getFileNm()); // 로컬 파일 경로
        
        try (FileInputStream input = new FileInputStream(file)) {
            String contentType = URLConnection.guessContentTypeFromStream(input);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.length());
            metadata.setContentType(contentType);

            amazonS3.putObject(s3BucketName, "media/"+ file.getName(), input, metadata);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
        log.info("------- S3 upload success --------");
        log.info(amazonS3.getUrl(s3BucketName, file.getName()).toString());

        AtFile uploadAtFile = new AtFile();
        uploadAtFile.setFileNm(atFile.getFileNm());
        uploadAtFile.setOrgFileNm(atFile.getOrgFileNm());
        uploadAtFile.setRegDt(LocalDateTime.now());
        uploadAtFile.setUploadYn("Y");
        atFileRepository.saveAndFlush(uploadAtFile);

        return uploadAtFile;
        // atFile.set
    }



}

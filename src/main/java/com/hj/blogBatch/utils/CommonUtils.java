package com.hj.blogBatch.utils;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;

@Slf4j
@RequiredArgsConstructor
@Component
public class CommonUtils {

    private final VideoUtils videoUtils;

    public static String[] splitByNthOccurrence(String str, String delimiter, int n) {
        int count = 0;
        int index = -1;

        // Loop through the string to find the nth occurrence of the delimiter
        for (int i = 0; i <= str.length() - delimiter.length(); i++) {
            if (str.substring(i, i + delimiter.length()).equals(delimiter)) {
                count++;
                if (count == n) {
                    index = i;
                    break;
                }
            }
        }

        // If the delimiter occurs n times, split the string
        if (index != -1) {
            String before = str.substring(0, index);
            String after = str.substring(index + delimiter.length());
            return new String[] { before, after };
        } else {
            return null; // Delimiter does not occur n times
        }
    }

    public static void deleteFile(String filePathStr) {
        // 삭제할 파일 경로를 지정합니다.
        Path filePath = Paths.get(filePathStr);

        try {
            Files.delete(filePath);
            System.out.println("파일이 성공적으로 삭제되었습니다.");
        } catch (IOException e) {
            System.err.println("파일 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    public String imageConvertWebp(String filePath) throws IOException {
        String fileExt = filePath.substring(filePath.lastIndexOf("."));
        File inputFile = new File(filePath); // 입력 이미지 파일
         // webp 변환 불가
        if(videoUtils.getMediaInfo(filePath).getStreams().get(0).height > 16000){
            imageResize(inputFile, 16000);
        }
        
        File outputFile = new File(filePath.substring(0, filePath.lastIndexOf("."))+".webp"); // 출력 WebP 파일
        String command = String.format("/opt/homebrew/bin/magick convert \"%s\" \"%s\"", inputFile.getAbsolutePath(), outputFile.getAbsolutePath());

        try {

            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("bash", "-c", command);
            log.info("-------- webp convert start --------");
            log.info("command : "+command);
            // 프로세스 시작
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("이미지가 WebP 형식으로 변환되었습니다.");
            } else {
                throw new RuntimeException("변환 중 오류 발생. Exit code: " + exitCode);
            }
            
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return outputFile.getAbsolutePath();
    }

    public static void imageResize(File file, Integer height){
        String command = String.format("/opt/homebrew/bin/magick convert \"%s\" -resize x%d \"%s\"",
                file.getAbsolutePath(), height, file.getAbsolutePath());
        log.info("-------- 이미지 크기 16000 넘어서 리사이징 --------");
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("bash", "-c", command);
            log.info("-------- image resize start --------");
            log.info("command : "+command);
            // 프로세스 시작
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("------ image resize complete -----");
            } else {
                throw new RuntimeException("resize 중 오류 발생. Exit code: " + exitCode);
            }
            
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

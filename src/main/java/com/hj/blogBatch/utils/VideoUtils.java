package com.hj.blogBatch.utils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.util.Strings;
import org.modelmapper.internal.util.Assert;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.threeten.bp.LocalDateTime;

import com.hj.blogBatch.dto.ShortsDto;
import com.hj.blogBatch.entity.Category;
import com.hj.blogBatch.entity.Item;
import com.hj.blogBatch.entity.Shorts;
import com.hj.blogBatch.repository.ShortsRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoUtils {

    @Value("${com.blogBatch.ffmpeg.path}")
    private String ffmpegPath;
    @Value("${com.blogBatch.ffprobe.path}")
    private String ffprobePath;
    @Value("${com.blogBatch.img.path}")
    String mediaPath;
    @Value("${com.blogBatch.video.intro}")
    String channelIntroPath;
    @Value("${com.blogBatch.bgm.free.path}")
    String freeBgmPath;
    @Value("${com.blogBatch.bgm.zzal.path}")
    String zzalBgmPath;
    @Value("${com.blogBatch.bgm.shorts.music.path}")
    String shortMusicBgmPath;

    @Value("${com.shortsBatch.gif.assets.path}")
    String assetsGifPath;
    @Value("${com.shortsBatch.gif.motion.path}")
    String motionGifPath;
    @Value("${com.shortsBatch.shorts.path}")
    String shortsPath;
    List<String> colorList = List.of("#5E11BC", "#8FC9A2", "#D27172", "#17dcbe", "#ff6a5e","#ffd700", "#ffb400", "#c51d26", "#003000");

    private final ShortsRepository shortsRepository;
    private FFmpeg ffmpeg;
    private FFprobe ffprobe;

    @PostConstruct
    public void init(){
        try {
            ffmpeg = new FFmpeg(ffmpegPath);
            Assert.isTrue(ffmpeg.isFFmpeg());

            ffprobe = new FFprobe(ffprobePath);
            Assert.isTrue(ffprobe.isFFprobe());

            log.debug("VideoFileUtils init complete.");
        } catch (Exception e) {
            log.error("VideoFileUtils init fail.", e);
        }
    }

    public FFmpegProbeResult getMediaInfo(String filePath) {
        FFmpegProbeResult probeResult;
        try {
            probeResult = ffprobe.probe(filePath);
        } catch (IOException e) {
            log.error("--------- ERROR FILE READ filePath : " + filePath + " ----------");
            throw new RuntimeException(e);
        }
        
        if (log.isDebugEnabled()) {
            log.info("========== VideoFileUtils.getMediaInfo() ==========");
            log.info("filename : {}", probeResult.getFormat().filename);
            log.info("format_name : {}", probeResult.getFormat().format_name);
            log.info("format_long_name : {}", probeResult.getFormat().format_long_name);
            log.info("tags : {}", probeResult.getFormat().tags.toString());
            log.info("duration : {} second", probeResult.getFormat().duration);
            log.info("size : {} byte", probeResult.getFormat().size);
            log.info("width : {} px", probeResult.getStreams().get(0).width);
            log.info("height : {} px", probeResult.getStreams().get(0).height);
            log.info("===================================================");
        }

        return probeResult;
    }

    public String makeDanawaVideo(Category category){
        String templateFilePath = "/Users/hojun/my/work/blogBatch/media/youtube/template.jpg";

        try {

            // 상품 별 영상 만들기
            category.getItemList().forEach(item -> {
                Double audioTimeSec = item.getScriptList().stream()
                        .mapToDouble(map -> {
                            double duration = getMediaInfo(map.get("audioFilePath").toString()).getFormat().duration;
                            map.put("duration", duration);
                            return duration;
                        })
                        .sum();

                Integer priceLength = item.getPrice().toString().length();
                String price = """
                    가격 \\: %s만원 대
                    """.formatted(priceLength>4?item.getPrice().toString().substring(0, priceLength-4):"").trim();

                // 앞뒤 여백 시간 3초씩 추가
                Double prefixTime = 3D;
                Double suffixTime = 3D;
                audioTimeSec += prefixTime + suffixTime;
                
                // 시간을 기준으로 영상 생성
                String cmpltPath = mediaPath+item.getCateId()+"/"+item.getItemRank()+"_cmplt.mp4";
                String targetPath = mediaPath+item.getCateId()+"/"+item.getItemRank()+"_target.mp4";
                String dirPath = mediaPath+item.getCateId()+"/";
                
                imgToMp4(templateFilePath, cmpltPath, (int) Math.round(audioTimeSec*1000),"1920:1080");
                
                // 이미지 파일 해상도 변경
                fileRatioChange(mediaPath + item.getItemFile().getFileNm(), 500, 500);
                fileRatioChange(mediaPath + item.getDetailFile().getFileNm(), 1100, 5000);

                // 이미지 오버레이
                String itemRankStr = item.getItemRank().toString();
                renameCmpltToTarget(itemRankStr, dirPath);
                videoInsertImage(targetPath, mediaPath + item.getItemFile().getFileNm(), cmpltPath, 100D, 140D);
                renameCmpltToTarget(itemRankStr, dirPath);
                Integer itemFileWidth = getMediaInfo(mediaPath + item.getDetailFile().getFileNm()).getStreams().get(0).width;
                /* videoInsertImage(targetPath, mediaPath + item.getDetailFile().getFileNm(), cmpltPath,
                        (double) (660 + (1260 - itemFileWidth)/2), 40D); */
                videoInsertImageWithUpAni(targetPath, mediaPath + item.getDetailFile().getFileNm(), cmpltPath,
                (double) (700 + (1220 - itemFileWidth) / 2), 90D, 3, 0);
                
                //cmplt 에서 target으로 파일명 변경
                renameCmpltToTarget(itemRankStr, dirPath);
                //자막 달기
                videoInsertLeftSubtitle(item.getScriptList(), targetPath, cmpltPath, 3D);
                //헤더 박기
                renameCmpltToTarget(itemRankStr, dirPath);
                videoInsertHeaderTitle("TOP" + itemRankStr + ". " + item.getItemNm(), targetPath, cmpltPath, category.getCateId());
                // 설명 박기
                renameCmpltToTarget(itemRankStr, dirPath);
                videoInsertExplain(price,"구매 링크는 설명 참조", targetPath, cmpltPath, category.getCateId());
                //cmplt 에서 target으로 파일명 변경
                renameCmpltToTarget(itemRankStr, dirPath);
                // 목소리 오디오 삽입
                videoInsertAudios(item.getScriptList(), targetPath, cmpltPath, item.getItemRank().toString(), dirPath);
                renameCmpltToTarget(itemRankStr, dirPath);
                fillSilenceAudio(targetPath, cmpltPath, suffixTime);
            });

            // 영상 합치기
            log.info("------- start concat items video -------");
            String catePath = mediaPath +category.getCateId()+"/";
            String outputCmpltPath = catePath + "output_cmplt.mp4";
            category.getItemList().stream().reduce(null, (a, b) -> {
                if(a != null){
                    if(a.getItemRank() == category.getItemList().size()){
                        concatVideos( catePath+a.getItemRank()+"_cmplt.mp4", catePath+ b.getItemRank()+"_cmplt.mp4", outputCmpltPath);
                    }else{
                        renameCmpltToTarget("output", catePath);
                        concatVideos( catePath + "output_target.mp4", catePath+ b.getItemRank()+"_cmplt.mp4", outputCmpltPath);
                    }
                }

                return b;
            });
            log.info("------- end concat items video -------");


            return outputCmpltPath;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        
    }

    public void fileRatioChange(String filePath, Integer targetWidth, Integer targetHeight){
        FFmpegProbeResult mediaInfo = getMediaInfo(filePath);

        Integer width = mediaInfo.getStreams().get(0).width;
        Integer height = mediaInfo.getStreams().get(0).height;

        String fileExt = filePath.substring(filePath.lastIndexOf("."));
        String tmpFilePath = filePath.substring(0, filePath.lastIndexOf(".")) + "_tmp" + fileExt;

        // 곱해야할 것이 더 작은 쪽에 맞춘다.
        Double divWidth = (double)targetWidth / width;
        Double divHeight = (double)targetHeight / height;
        Double multiple = NumberUtils.min(divWidth, divHeight);
        Integer changeWidth = (int) Math.floor(width * multiple);
        Integer changeHeight = (int )Math.floor(height * multiple);
        changeWidth = changeWidth % 2 == 0 ? changeWidth : changeWidth + 1;
        changeHeight = changeHeight % 2 == 0 ? changeHeight : changeHeight + 1;

        if(fileExt.equals(".webp") && (changeHeight > 16000 )){
            multiple = (double) 16000 / changeHeight;
            changeWidth = (int) Math.floor(width * multiple);
            changeHeight = (int )Math.floor(height * multiple);
            changeWidth = changeWidth % 2 == 0 ? changeWidth : changeWidth + 1;
            changeHeight = changeHeight % 2 == 0 ? changeHeight : changeHeight + 1;
        }

        String scale = String.valueOf(changeWidth) + ":" + String.valueOf(changeHeight);

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(filePath)
                .setVideoFilter("scale=" + scale)
                .addOutput(tmpFilePath)
                .done();

        FFmpegExecutor excutor = new FFmpegExecutor(ffmpeg, ffprobe);
        excutor.createJob(builder, p -> {
            if (p.isEnd()) {
                log.info("--- File 비율 수정 완료 tmpFilePath : " + tmpFilePath + " ------");
            }
        }).run();

        // 원래 파일 이름으로 변경
        Path file = Paths.get(tmpFilePath);
        Path newFile = Paths.get(filePath);
        try {
            Path newFilePath = Files.move(file, newFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("newFilePath : " + newFilePath.toString());
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
        log.info("--- finish fileRename filePath : " + filePath + " ------");
        
    }

    public void fileRatioChangeWithLetterBox(String filePath){
        FFmpegProbeResult mediaInfo = getMediaInfo(filePath);

        Integer width = mediaInfo.getStreams().get(0).width;
        Integer height = mediaInfo.getStreams().get(0).height;

        String fileExt = filePath.substring(filePath.lastIndexOf("."));
        String tmpFilePath = filePath.substring(0, filePath.lastIndexOf(".")) + "_tmp" + fileExt;

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(filePath)
                .setVideoFilter("pad=width=ih*16/9:height=ih:x=(ow-iw)/2:y=0:color=black")
                .addOutput(tmpFilePath)
                .done();

        FFmpegExecutor excutor = new FFmpegExecutor(ffmpeg, ffprobe);
        excutor.createJob(builder, p -> {
            if (p.isEnd()) {
                log.info("--- File 비율 수정 완료 tmpFilePath : " + tmpFilePath + " ------");
            }
        }).run();

        // 원래 파일 이름으로 변경
        Path file = Paths.get(tmpFilePath);
        Path newFile = Paths.get(filePath);
        try {
            Path newFilePath = Files.move(file, newFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("newFilePath : " + newFilePath.toString());
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
        log.info("--- finish fileRatioChangeWithLetterBox filePath : " + filePath + " ------");
        
    }

    public void imgToMp4(String filePath, String cmpltPath, Integer videoTimeMiliSecond,String scale){

        FFmpegBuilder builder = new FFmpegBuilder()
                .addExtraArgs("-loop", "1")
                .setInput(filePath)
                .addOutput(cmpltPath)
                .setVideoCodec("libx264")
                .setDuration(videoTimeMiliSecond, TimeUnit.MILLISECONDS) // 동영상 길이 설정 (초)
                .setVideoPixelFormat("yuv420p")
                .setVideoFilter("scale=" + scale)
                .done();

        FFmpegExecutor excutor = new FFmpegExecutor(ffmpeg, ffprobe);
        excutor.createJob(builder, p -> {
            if (p.isEnd()) {
                log.info("--- finish imgToMp4 filePath : " + filePath + ", targetPath : " + cmpltPath + " ------");
            }
        }).run();
    }

    public void videoInsertAudio(String vidPath, String audPath,String cmpltPath ,boolean isBaseAudioFlg, String delay, boolean shortestFlg){

        getMediaInfo(vidPath);

        FFmpegBuilder builder;

        delay = Strings.isBlank(delay) ? "0" : delay;

        // 기존 오디오가 없을 경우
        if(!isBaseAudioFlg){
            if(shortestFlg){
                builder = new FFmpegBuilder()
                .addExtraArgs("-stream_loop","10")
                .addInput(vidPath)
                .addInput(audPath)
                .setComplexFilter("""
                    [1:a]adelay=%s|%s[aud]
                        """.formatted(delay, delay))
                .addOutput(cmpltPath)
                .setVideoCodec("copy")
                .setAudioCodec("aac")
                .addExtraArgs("-shortest", "-y", "-map", "0:v", "-map", "[aud]")
                .done();
            }else{
                builder = new FFmpegBuilder()
                .addInput(vidPath)
                .addInput(audPath)
                .setComplexFilter("""
                    [1:a]adelay=%s|%s[aud]
                        """.formatted(delay, delay))
                .addOutput(cmpltPath)
                .setVideoCodec("copy")
                .setAudioCodec("aac")
                .addExtraArgs("-y", "-map", "0:v", "-map", "[aud]")
                .done();
            }
            
        } else {
            builder = new FFmpegBuilder()
                    .addInput(vidPath)
                    .addInput(audPath)
                    .setComplexFilter("""
                        [1]adelay=%s|%s,volume=1[aud];[0:a]volume=1[aud2];[aud][aud2]amix=inputs=2
                                """.formatted(delay, delay))
                    .addOutput(cmpltPath)
                    .setVideoCodec("copy")
                    .addExtraArgs("-y", shortestFlg ? "-shortest":"")
                    .done();
        }


        FFmpegExecutor excutor = new FFmpegExecutor(ffmpeg, ffprobe);
        excutor.createJob(builder, p -> {
            if (p.isEnd()) {
                log.info("--- finish videoInsertAudio videoPath : " + vidPath + ", cmpltPath : " + cmpltPath + " ------");
            }
        }).run();
    }

    /**
     * 비디오에 이미지 오버레이
     * @param vidPath
     * @param imgPath
     * @param x
     * @param y
     */
    public void videoInsertImage(String vidPath, String imgPath, String cmpltPath,Double x, Double y){
        
        FFmpegBuilder builder = new FFmpegBuilder()
        .addInput(vidPath)
        .addInput(imgPath)
        .setComplexFilter("""
            overlay=x=%.1f:y=%.1f
            """.formatted(x, y)
            )
        .addOutput(cmpltPath)
        .done();

        FFmpegExecutor excutor = new FFmpegExecutor(ffmpeg, ffprobe);
        excutor.createJob(builder, p -> {
            if (p.isEnd()) {
                log.info("--- finish videoInsertImage vidPath : " + vidPath + " ------");
            }
        }).run();
    }

    /**
     * 비디오에 이미지 오버레이 scale 지정
     */
    public void videoInsertImageWithScale(String vidPath, String imgPath, String cmpltPath,Double x, Double y, Integer scaleX, Integer scaleY){
        
        FFmpegBuilder builder = new FFmpegBuilder()
        .addInput(vidPath)
        .addInput(imgPath)
        .setComplexFilter("""
            [1:v]scale=%d:%d[overlay];[0:v][overlay]overlay=%.1f:%.1f
            """.formatted(scaleX, scaleY, x, y)
            )
        .addOutput(cmpltPath)
        .done();

        FFmpegExecutor excutor = new FFmpegExecutor(ffmpeg, ffprobe);
        excutor.createJob(builder, p -> {
            if (p.isEnd()) {
                log.info("--- finish videoInsertImage vidPath : " + vidPath + " ------");
            }
        }).run();
    }
    /**
     * 비디오에 비디오 오버레이 scale 지정
     */
    public void videoInsertVideo(String backPath, String frontPath, String cmpltPath){
        
        FFmpegBuilder builder = new FFmpegBuilder()
        .addInput(backPath)
        .addInput(frontPath)
        .setComplexFilter("overlay=x=(main_w-overlay_w)/2:y=(main_h-overlay_h)/2")
        .addOutput(cmpltPath)
        .setAudioCodec("copy")
        .done();

        FFmpegExecutor excutor = new FFmpegExecutor(ffmpeg, ffprobe);
        excutor.createJob(builder, p -> {
            if (p.isEnd()) {
                log.info("--- finish videoInsertVideoWithScale cmpltPath : " + cmpltPath + " ------");
            }
        }).run();
    }

    /**
     * 비디오에 이미지 오버레이 올라가는 애니메이션
     */
    public void videoInsertImageWithUpAni(String vidPath, String imgPath, String cmpltPath, Double x, Double firstY, Integer aniStartSec, Integer bottomGap){

        FFmpegStream vidStream = getMediaInfo(vidPath).getStreams().get(0);
        double vidDuration = vidStream.duration;
        double duration = vidDuration - aniStartSec;
        int vidHeight = vidStream.height;
        int imgHeight = getMediaInfo(imgPath).getStreams().get(0).height;
        int moveDistance = imgHeight - (vidHeight - (int) Math.round(firstY) - bottomGap);
        double speed = moveDistance / duration;

        String complexFilter = "";
        if(moveDistance > 0){
             complexFilter = """
                    [0][1]overlay=x=%.2f:y='if(gte(t, %d), if(lte(t, %.1f), %.1f - (%.2f * (t-%d)), %.1f), %.1f)'
                                """.formatted(x, aniStartSec, vidDuration, firstY, speed, aniStartSec,
                    firstY - (speed * duration), firstY);
        }else{
            complexFilter = """
                    [0][1]overlay=x=%.2f:y='(main_h-overlay_h)/2'
                    """.formatted(x);
        }
        FFmpegBuilder builder = new FFmpegBuilder()
        .addInput(vidPath)
        .addInput(imgPath)
        .setComplexFilter(complexFilter)
        .addOutput(cmpltPath)
        .setVideoPixelFormat("yuv420p")
        .done();

        FFmpegExecutor excutor = new FFmpegExecutor(ffmpeg, ffprobe);
        excutor.createJob(builder, p -> {
            if (p.isEnd()) {
                log.info("--- finish videoInsertImageWithUpAni cmpltPath : " + cmpltPath + " ------");
            }
        }).run();
    }

    public String renameCmpltToTarget(String onlyFileName, String dirPath) {
        Path file = Paths.get(dirPath + onlyFileName + "_cmplt.mp4");
        Path newFile = Paths.get(dirPath + onlyFileName + "_target.mp4");
        try {
            Path newFilePath = Files.move(file, newFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("newFilePath : " + newFilePath.toString());
            return newFilePath.toString();
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public String renameFile(String orgFile, String moveFile) {
        Path file = Paths.get(orgFile);
        Path newFile = Paths.get(moveFile);
        try {
            Path newFilePath = Files.move(file, newFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("newFilePath : " + newFilePath.toString());
            return newFilePath.toString();
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    

    public void videoInsertSubtitle(List<Map<String, Object>> scriptList, String filePath, String cmpltPath, double durationSec){
        String videoFilter = "";
        String fontFilePath = "/Users/hojun/Downloads/nanum-gothic/NanumGothic.ttf";
        int idx = 0;

        for (Map<String,Object> map : scriptList) {
            if(idx == 0){
                videoFilter += "[in]";
            }else{
                videoFilter += ", ";
            }
            
            double scriptDuration = NumberUtils.toDouble(map.get("duration").toString());
            
            videoFilter += """
                drawtext=fontsize=70:fontfile='%s':x=(w-text_w)/2:y=h-line_h-30:box=1:fontcolor=white:boxcolor=black@0.8:boxborderw=4
                :text='%s':enable='between(t,%.2f,%.2f)'
                """.formatted(fontFilePath, map.get("summary").toString(), durationSec, durationSec + scriptDuration);
            
            if (idx + 1 == scriptList.size()) {
                videoFilter += "[out]";
            }
            durationSec += scriptDuration;
            idx++;
        }

        FFmpegBuilder builder = new FFmpegBuilder()
        .addInput(filePath)
        .setVideoFilter(videoFilter)
        .addOutput(cmpltPath)
        .done();

        FFmpegExecutor excutor = new FFmpegExecutor(ffmpeg, ffprobe);
        excutor.createJob(builder, p -> {
            if (p.isEnd()) {
                log.info("--- finish videoInsertImage vidPath : " + cmpltPath + " ------");
            }
        }).run();
    }

    public void videoInsertLeftSubtitle(List<Map<String, Object>> scriptList, String filePath, String cmpltPath, double durationSec){
        String videoFilter = "";
        String fontFilePath = "/Users/hojun/Downloads/nanum-gothic/NanumGothic.ttf";
        int idx = 0;

        for (Map<String,Object> map : scriptList) {
            if(idx == 0){
                videoFilter += "[in]";
            }else{
                videoFilter += ", ";
            }
            double scriptDuration = NumberUtils.toDouble(map.get("duration").toString());
            String summary = map.get("summary").toString();
            List<String> summaryList = new ArrayList<>();
            // 10줄 씩 자르기
            for (int i = 0; i < summary.length(); i+=10) {
                if(summary.length()>i+10){
                    summaryList.add(summary.substring(i, i + 10));
                }else{
                    summaryList.add(summary.substring(i));
                }
            }
            summary = StringUtils.join(summaryList, "\n");
            
            videoFilter += """
                drawtext=fontsize=70:fontfile='%s':x=360-text_w/2:y=800:box=1:fontcolor=white:boxcolor=black@0.8:boxborderw=4
                :text='%s':enable='between(t,%.2f,%.2f)'
                """.formatted(fontFilePath, summary, durationSec, durationSec + scriptDuration);
            
            if (idx + 1 == scriptList.size()) {
                videoFilter += "[out]";
            }
            durationSec += scriptDuration;
            idx++;
        }

        FFmpegBuilder builder = new FFmpegBuilder()
        .addInput(filePath)
        .setVideoFilter(videoFilter)
        .addOutput(cmpltPath)
        .done();

        FFmpegExecutor excutor = new FFmpegExecutor(ffmpeg, ffprobe);
        excutor.createJob(builder, p -> {
            if (p.isEnd()) {
                log.info("--- finish videoInsertImage vidPath : " + cmpltPath + " ------");
            }
        }).run();
    }

    public void videoInsertHeaderTitle(String title, String filePath, String cmpltPath, Long cateId){
        String videoFilter = "";
        String fontFilePath = "/Users/hojun/Downloads/nanum-gothic/NanumGothic.ttf";

        String backgorundColor = colorList.get((int) (cateId % colorList.size()));
        videoFilter += """
            drawtext=text='%s':fontsize=50:fontfile='%s':x=10:y=10:box=1:fontcolor=white:boxcolor=%s@0.9:boxborderw=10
                """
                .formatted(title, fontFilePath, backgorundColor);

        FFmpegBuilder builder = new FFmpegBuilder()
        .addInput(filePath)
        .setVideoFilter(videoFilter)
        .addOutput(cmpltPath)
        .done();

        FFmpegExecutor excutor = new FFmpegExecutor(ffmpeg, ffprobe);
        excutor.createJob(builder, p -> {
            if (p.isEnd()) {
                log.info("--- finish videoInsertImage vidPath : " + cmpltPath + " ------");
            }
        }).run();
    }

    public void mediaInsertVideoFilter(String filePath, String cmpltPath, String videoFilter){

        FFmpegBuilder builder = new FFmpegBuilder()
        .addInput(filePath)
        .setVideoFilter(videoFilter)
        .addOutput(cmpltPath)
        .done();

        FFmpegExecutor excutor = new FFmpegExecutor(ffmpeg, ffprobe);
        excutor.createJob(builder, p -> {
            if (p.isEnd()) {
                log.info("--- finish mediaInsertVideoFilter vidPath : " + cmpltPath + " ------");
            }
        }).run();
    }

    public void videoInsertExplain(String topText, String botText, String filePath, String cmpltPath, Long cateId){
        String videoFilter = "";
        String fontFilePath = "/Users/hojun/Downloads/nanum-gothic/NanumGothic.ttf";
        double topX = 350D;
        double topY = 660D;
        double botX = 350D;
        double botY = 730D;

        String fontColor = colorList.get((int) (cateId % colorList.size()));
        videoFilter += """
            [in]
            drawtext=text='%s':fontsize=40:fontfile='%s':x=%.1f-text_w/2:y=%.1f:box=0:fontcolor=%s:borderw=1:bordercolor=black
            ,drawtext=text='%s':fontsize=30:fontfile='%s':x=%.1f-text_w/2:y=%.1f+10*sin(2*PI*t/2):box=0:fontcolor=%s:borderw=1:bordercolor=black
            [out]
                """
                .formatted(topText, fontFilePath, topX, topY, fontColor,botText, fontFilePath, botX, botY, fontColor);

        FFmpegBuilder builder = new FFmpegBuilder()
        .addInput(filePath)
        .setVideoFilter(videoFilter)
        .addOutput(cmpltPath)
        .done();

        FFmpegExecutor excutor = new FFmpegExecutor(ffmpeg, ffprobe);
        excutor.createJob(builder, p -> {
            if (p.isEnd()) {
                log.info("--- finish videoInsertImage vidPath : " + cmpltPath + " ------");
            }
        }).run();
    }



    public void videoInsertAudios(List<Map<String, Object>> scriptList, String filePath, String cmpltPath, String fileNm, String dirPath){
        
        // txt파일 만들어서 오디오 합치기
        List<String> fileTextList = new ArrayList<>();
        for (Map<String,Object> map : scriptList) {
            String txtRowString = "file '";
            txtRowString += map.get("audioFilePath").toString();
            txtRowString += "'";
            fileTextList.add(txtRowString);
        }

        String txtFilePath = dirPath + fileNm + "_concat.txt";
        String concatFilePath = dirPath + fileNm + "_concat.mp3";
        makeTxtFile(txtFilePath, fileTextList);
        concatMedia(txtFilePath, concatFilePath);

        videoInsertAudio(filePath, concatFilePath, cmpltPath, false, "3000", false);
        
        

        /* for (Map<String,Object> map : scriptList) {
            double scriptDuration = Double.parseDouble(map.get("duration").toString());
            Integer durationMili = 0;
            if(idx == 0){
                isBaseAudio = false;
            }else{
                renameCmpltToTarget(itemRank, dirPath);
                isBaseAudio = true;
                durationMili = (int) Math.round(duration*1000);
            }
            videoInsertAudio(filePath, map.get("audioFilePath").toString(), cmpltPath, isBaseAudio, Integer.toString(durationMili));
            // 오디오 삽입 후에 오디오가 1/2배 돼서 볼륨 2배 시켜주기
            // modMediaVolume(cmpltPath, 2D);

            duration += scriptDuration;
            idx++;
        } */

    }

    public void modMediaVolume(String filePath, Double volume){

        String fileExt = filePath.substring(filePath.lastIndexOf("."));
        String tmpFilePath = filePath.substring(0, filePath.lastIndexOf(".")) + "_tmp" + fileExt;

        FFmpegBuilder builder = new FFmpegBuilder()
                .addInput(filePath)
                .setAudioFilter("volume=" + volume.toString())
                .addOutput(tmpFilePath)
                .done();

        FFmpegExecutor excutor = new FFmpegExecutor(ffmpeg, ffprobe);
        excutor.createJob(builder, p -> {
            if (p.isEnd()) {
                log.info("--- finish modMediaVolume tmpFilePath : " + tmpFilePath + " ------");
            }
        }).run();


        // 원래 파일 이름으로 변경
        Path file = Paths.get(tmpFilePath);
        Path newFile = Paths.get(filePath);
        try {
            Path newFilePath = Files.move(file, newFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("newFilePath : " + newFilePath.toString());
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
        log.info("--- finish fileRename filePath : " + filePath + " ------");

    }

    public void makeTxtFile(String filePath, List<String> content){

        // Files.write 사용하여 파일 작성
        try {
            Files.write(Paths.get(filePath), content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("파일 작성이 완료되었습니다.");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void concatMedia(String txtFilePath, String cmpltFilePath){
        FFmpegBuilder builder = new FFmpegBuilder()
        .addExtraArgs("-f", "concat", "-safe", "0")
        .addInput(txtFilePath)
        .addOutput(cmpltFilePath)
        .addExtraArgs("-c", "copy")
        .done();

        FFmpegExecutor excutor = new FFmpegExecutor(ffmpeg, ffprobe);
        excutor.createJob(builder, p -> {
            if (p.isEnd()) {
                log.info("--- finish concatMedia vidPath : " + cmpltFilePath + " ------");
            }
        }).run();
    }

    public void concatMedia(String filePath1, String filePath2, String cmpltFilePath){
        FFmpegBuilder builder = new FFmpegBuilder()
                .addInput(filePath1)
                .addInput(filePath2)
                .setComplexFilter("[0:v][0:a][1:v][1:a]concat=n=2:v=1:a=1[outv][outa]")
                .addOutput(cmpltFilePath)
                .addExtraArgs("-map", "[outv]", "-map", "[outa]", "-preset", "veryfast")
                .done();

        FFmpegExecutor excutor = new FFmpegExecutor(ffmpeg, ffprobe);
        excutor.createJob(builder, p -> {
            if (p.isEnd()) {
                log.info("--- finish concatMedia cmpltFilePath : " + cmpltFilePath + " ------");
            }
        }).run();
    }

    public void concatMediaOnlyVideo(String filePath1, String filePath2, String cmpltFilePath){
        FFmpegBuilder builder = new FFmpegBuilder()
                .addInput(filePath1)
                .addInput(filePath2)
                .setComplexFilter("[0:v][1:v]concat=n=2:v=1[outv]")
                .addOutput(cmpltFilePath)
                .addExtraArgs("-map", "[outv]", "-preset", "veryfast")
                .done();

        FFmpegExecutor excutor = new FFmpegExecutor(ffmpeg, ffprobe);
        excutor.createJob(builder, p -> {
            if (p.isEnd()) {
                log.info("--- finish concatMediaOnlyVideo cmpltFilePath : " + cmpltFilePath + " ------");
            }
        }).run();
    }

    public void fillSilenceAudio(String filePath, String cmpltFilePath, Double durationSec) {
        FFmpegBuilder builder = new FFmpegBuilder()
                .addExtraArgs("-f", "lavfi", "-t", durationSec.toString(), "-i", "anullsrc=channel_layout=stereo:sample_rate=44100")
                .setInput(filePath)
                .setComplexFilter("[1:a][0:a]concat=n=2:v=0:a=1[a]")
                .addOutput(cmpltFilePath)
                .addExtraArgs("-map", "[a]", "-map", "1:v", "-y")
                .setVideoCodec("copy")
                .done();

        FFmpegExecutor excutor = new FFmpegExecutor(ffmpeg, ffprobe);
        excutor.createJob(builder, p -> {
            if (p.isEnd()) {
                log.info("--- finish fillSilenceAudio durationSec : " + durationSec.toString() + " ------");
            }
        }).run();
    }

    public void concatVideos(String filePath1,String filePath2, String cmpltFilePath){

        Double file1Duration = getMediaInfo(filePath1).getStreams().get(0).duration;
        Double duration = 2D;
        Double fadeOffset = file1Duration - duration;

        FFmpegBuilder builder = new FFmpegBuilder()
                .addInput(filePath1)
                .addInput(filePath2)
                .setComplexFilter(
                        """
                            [0:v][1:v]xfade=transition=slideleft:duration=%.1f:offset=%.1f[v];[0:a][1:a]acrossfade=d=0:curve1=nofade:curve2=nofade[a]
                                """.formatted(duration, fadeOffset))
                .addOutput(cmpltFilePath)
                .addExtraArgs("-map", "[v]", "-map", "[a]")
                .setVideoPixelFormat("yuv420p")
                .setVideoCodec("libx264")
                .setAudioCodec("aac")
                // .setConstantRateFactor(18D)
                // .addExtraArgs("-preset", "veryfast") todo test
                .done();
    
        FFmpegExecutor excutor = new FFmpegExecutor(ffmpeg, ffprobe);
        excutor.createJob(builder, p -> {
            if (p.isEnd()) {
                log.info("--- finish concatVideos cmpltFilePath : " + cmpltFilePath + " ------");
            }
        }).run();
    }

    public void cutMedia(String filePath, String cmpltFilePath, Integer startSec, Integer endSec) {

        String startTime = "";
        String endTime = "";

        startTime += String.format("%02d:", (int) (startSec / 3600));
        startTime += String.format("%02d:", (int) ((startSec % 3600) / 60));
        startTime += String.format("%02d", (int) (startSec % 60));
        endTime += String.format("%02d:", (int) (endSec / 3600));
        endTime += String.format("%02d:", (int) ((endSec % 3600) / 60));
        endTime += String.format("%02d", (int) (endSec % 60));

        FFmpegBuilder builder = new FFmpegBuilder()
                .addInput(filePath)
                .addOutput(cmpltFilePath)
                .addExtraArgs("-ss", startTime, "-to", endTime)
                .addExtraArgs("-c:v", "libx264")
                .done();

        FFmpegExecutor excutor = new FFmpegExecutor(ffmpeg, ffprobe);
        excutor.createJob(builder, p -> {
            if (p.isEnd()) {
                log.info("--- finish cutMedia cmpltFilePath : " + cmpltFilePath + " ------");
            }
        }).run();
    }

    public String makeThumbNail(Category category) {

        String targetPath = mediaPath + category.getCateId() + "/thumbnail_target.jpg";
        String cmpltPath = mediaPath + category.getCateId() + "/thumbnail_cmplt.jpg";
        String templateFilePath = "/Users/hojun/my/work/blogBatch/media/youtube/template.jpg";
        String fontFilePath = "/Users/hojun/Downloads/nanum-gothic/NanumGothic.ttf";
        String boldFontFilePath = "/Users/hojun/Downloads/nanum-gothic/NanumGothicBold.ttf";
        String fontColor = colorList.get((int) (category.getCateId() % colorList.size()));
        String cateNm = category.getCateNm().indexOf("/") != -1 ? category.getCateNm().split("/")[1] : category.getCateNm();

        double evenX = 40d;
        double oddX = 1280d;
        Integer scale = 600;
        double y = 50;
        for (Item item : category.getItemList()) {
            int index = category.getItemList().indexOf(item);
            double x = index % 2 == 0 ? evenX : oddX;

            if(index == 0){
                videoInsertImageWithScale(templateFilePath, mediaPath + item.getItemFile().getFileNm(), cmpltPath, x, y,
                        scale, scale);
            }else{
                renameFile(cmpltPath, targetPath);
                videoInsertImageWithScale(targetPath, mediaPath + item.getItemFile().getFileNm(), cmpltPath, x, y,
                        scale, scale);
            }

            if(index % 2 == 0){
                evenX += scale/3;
            }else{
                oddX -= scale/3;
                y += scale/3;
                scale = (int) Math.round(scale * 0.8);
            }
        }

        String topSubTitle = LocalDateTime.now().getYear() + "년 최고의 " + cateNm +" BEST "+category.getItemList().size();
        String midSubtitle = cateNm + " 구매가이드";
        String bottomSubTitle = "구매하기전 이거 보세요오!";
        
        int spacebarCnt = StringUtils.countMatches(midSubtitle, " ");

        String sufixSubtitle = "                             |";
        String prefixSubtitle = "|";
        for (int i = 0; i < 28-spacebarCnt - ((midSubtitle.length() - spacebarCnt)/2*3); i++) {
            prefixSubtitle += " ";
        }
        midSubtitle = prefixSubtitle + midSubtitle + sufixSubtitle;

        List<String> filterList = List.of(
            """
                drawtext=text='%s':x=50:y=500:fontfile='%s':fontsize=100:fontcolor=%s:borderw=7:bordercolor=black
                    """.formatted(topSubTitle,fontFilePath, fontColor),
            """
                drawtext=text='%s':x=-30:y=700:fontfile='%s':fontsize=120:fontcolor=white:borderw=7:bordercolor=black:box=1:boxcolor=%s:boxborderw=70       
                    """.formatted(midSubtitle, boldFontFilePath, fontColor),
            """
                drawtext=text='%s':x=1900-text_w:y=900:fontfile='%s':fontsize=80:fontcolor=white:borderw=5:bordercolor=%s
                    """.formatted(bottomSubTitle, fontFilePath, fontColor)
        );

        for (String filterStr : filterList) {
            renameFile(cmpltPath, targetPath);
            mediaInsertVideoFilter(targetPath, cmpltPath, filterStr);
        }

        return cmpltPath;
    }

    public void videoInsertBgm(String filePath, Category category, Integer videoDuration) {

        File path = new File(freeBgmPath);
        File[] fList = path.listFiles((FilenameFilter) (dir, name) -> name.endsWith(".mp3"));
        List<String> bgmPathList = new ArrayList<>();
        String txtFilePath = mediaPath + category.getCateId() + "/bgm_concat.txt";
        String concatFilePath = mediaPath + category.getCateId() + "/bgm_concat.mp3";

        int idx = (int) Math.ceil(category.getCateId() % fList.length);
        int sumDuration = 0;
        for (int i = 0; i < fList.length; i++) {
            String bgmPath = fList[idx % fList.length].getAbsolutePath();
            bgmPathList.add("""
                    file '%s'
                        """.formatted(bgmPath).trim());

            sumDuration += getMediaInfo(bgmPath).streams.get(0).duration;
            if(videoDuration < sumDuration){
                break;
            }
            idx++;
        }

        if(bgmPathList.size() > 1){
            makeTxtFile(txtFilePath, bgmPathList);
            concatMedia(txtFilePath, concatFilePath);    
        }else{
            concatFilePath = fList[idx % fList.length].getAbsolutePath();
        }
        
        videoInsertMusic(filePath, concatFilePath, 0.2D, 2.5D, true);

    }

    public void videoInsertMusic(String filePath, String bgmFilePath, Double bgmVolume, Double baseVolume, boolean isBaseAudioFlg) {
        String fileExt = filePath.substring(filePath.lastIndexOf("."));
        String tmpFilePath = filePath.substring(0, filePath.lastIndexOf(".")) + "_tmp" + fileExt;
        FFmpegBuilder builder;
        if(isBaseAudioFlg){
            builder = new FFmpegBuilder()
            .addInput(filePath)
            .addInput(bgmFilePath)
            .setComplexFilter("""
                    [1]volume=%.1f[aud];[0:a]volume=%.1f[aud2];[aud][aud2]amix=inputs=2
                    """.formatted(bgmVolume, baseVolume))
            .addOutput(tmpFilePath)
            .setVideoCodec("copy")
            .addExtraArgs("-y", "-shortest")
            .done();
        }else{
            builder = new FFmpegBuilder()
                .addInput(filePath)
                .addInput(bgmFilePath)
                .addOutput(tmpFilePath)
                .setVideoCodec("copy")
                .setAudioCodec("aac")
                .addExtraArgs("-shortest", "-y", "-map", "0:v", "-map", "1:a")
                .done();
        }        

        FFmpegExecutor excutor = new FFmpegExecutor(ffmpeg, ffprobe);
            excutor.createJob(builder, p -> {
                if (p.isEnd()) {
                    log.info("--- finish insertBgm tmpFilePath : " + tmpFilePath + " ------");
                }
            }).run();

        
        // 원래 파일 이름으로 변경
        Path file = Paths.get(tmpFilePath);
        Path newFile = Paths.get(filePath);
        try {
            Path newFilePath = Files.move(file, newFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("newFilePath : " + newFilePath.toString());
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }

        log.info("--- finish fileRename filePath : " + filePath + " ------");
    }

    public void videoInsertMusic(String targetFilePath, String cmpltFilePath, String bgmFilePath, Double bgmVolume,
            Double baseVolume, boolean isBaseAudioFlg) {
        // String fileExt = filePath.substring(filePath.lastIndexOf("."));
        // String tmpFilePath = filePath.substring(0, filePath.lastIndexOf(".")) +
        // "_tmp" + fileExt;

        isBaseAudioFlg = getMediaInfo(targetFilePath).getStreams().size() > 1;

        FFmpegBuilder builder;
        if (isBaseAudioFlg) {
            builder = new FFmpegBuilder()
                    .addInput(targetFilePath)
                    .addInput(bgmFilePath)
                    .setComplexFilter("""
                            [1]volume=%.1f[aud];[0:a]volume=%.1f[aud2];[aud][aud2]amix=inputs=2
                            """.formatted(bgmVolume, baseVolume))
                    .addOutput(cmpltFilePath)
                    .setVideoCodec("copy")
                    .addExtraArgs("-y", "-shortest")
                    .done();
        } else {
            builder = new FFmpegBuilder()
                    .addInput(targetFilePath)
                    .addInput(bgmFilePath)
                    .addOutput(cmpltFilePath)
                    .setVideoCodec("copy")
                    .setAudioCodec("aac")
                    .addExtraArgs("-shortest", "-y", "-map", "0:v", "-map", "1:a")
                    .done();
        }

        FFmpegExecutor excutor = new FFmpegExecutor(ffmpeg, ffprobe);
        excutor.createJob(builder, p -> {
            if (p.isEnd()) {
                log.info("--- finish insertBgm cmpltFilePath : " + cmpltFilePath + " ------");
            }
        }).run();
    }


    public void makeShortsVideo(ShortsDto shortsDto) {
        log.info("----- start makeShortsVideo  ------");
        String templateFilePath = "/Users/hojun/my/work/blogBatch/media/youtube/shorts_template.jpg";
        Double duration = 35D;
        boolean videoFlg = shortsDto.getTargetFilePath().endsWith(".mp4");
        Shorts shorts = new Shorts();
        shortsRepository.saveAndFlush(shorts);
        shortsDto.setShorts(shorts);
        
        String cmpltPath = shortsPath + shorts.getShortsId() + "_cmplt.mp4";
        String targetPath = shortsPath + shorts.getShortsId() + "_target.mp4";

        if (videoFlg) {
            duration = getMediaInfo(shortsDto.getTargetFilePath()).getStreams().get(0).duration;
        }
        // template mp4 만들기
        imgToMp4(templateFilePath, cmpltPath, (int) Math.round(duration*1000), "1080:1920");
        renameFile(cmpltPath, targetPath);

        // 모션 배경 넣기
        File path = new File(motionGifPath);
        File[] motionListFiles = path.listFiles((FilenameFilter) (dir, name) -> name.endsWith(".gif"));
        String motionFilePath = motionListFiles[(int) (shorts.getShortsId() % motionListFiles.length)].getAbsolutePath();
        FFmpegStream motionInfo = getMediaInfo(motionFilePath).getStreams().get(0);
        String motionScale = getOptimalScale(motionInfo.width, motionInfo.height, 1080, 1920, true);
        videoInsertMotionGif(targetPath, motionFilePath, cmpltPath, motionScale, 0D);


        // 소스 넣기
        renameFile(cmpltPath, targetPath);
        if(videoFlg){
            fileRatioChange(shortsDto.getTargetFilePath(), 1080, 1920);
            videoInsertVideo(targetPath, shortsDto.getTargetFilePath(), cmpltPath);
        }else{
            fileRatioChange(shortsDto.getTargetFilePath(), 900, 20000000);
            double x = 540D-getMediaInfo(shortsDto.getTargetFilePath()).getStreams().get(0).width/2;
            videoInsertImageWithUpAni(targetPath, shortsDto.getTargetFilePath(), cmpltPath, x, 300D, 0, 500);
        }

        // 상단 제목 넣기
        if(StringUtils.isBlank(shortsDto.getTitle())){
            shortsDto.setTitle(shorts.getShortsId().toString());
        }
        renameFile(cmpltPath, targetPath);
        shortsInsertHeaderTitle(shortsDto.getTitle(), targetPath, cmpltPath, 200);

        // 하단 asset gif 넣기
        renameFile(cmpltPath, targetPath);
        File[] assetListFiles = new File(assetsGifPath).listFiles((FilenameFilter) (dir, name) -> name.endsWith(".gif"));
        String assetFilePath = assetListFiles[(int) (LocalDateTime.now().getSecond() % assetListFiles.length)].getAbsolutePath();
        FFmpegStream assetInfo = getMediaInfo(assetFilePath).getStreams().get(0);
        String assetScale = getOptimalScale(assetInfo.width, assetInfo.height, 1080, 700, false);
        videoInsertMotionGif(targetPath, assetFilePath, cmpltPath, assetScale, 1220);

        renameFile(cmpltPath, targetPath);
        String zzalVideoPath = shortsPath + shorts.getShortsId() + "_zzal.mp4";
        String shortsMusicVideoPath = shortsPath + shorts.getShortsId() + "_sm.mp4";
        // 짤스튜디오 음원넣기
        File[] bgmListFiles = new File(zzalBgmPath).listFiles((FilenameFilter) (dir, name) -> name.endsWith(".mp3"));
        String bgmFilePath = bgmListFiles[(int) (LocalDateTime.now().getSecond() % bgmListFiles.length)].getAbsolutePath();
        videoInsertMusic(targetPath, zzalVideoPath, bgmFilePath, 0.8D, 2D, videoFlg);

        // 쇼츠뮤직 음원넣기
        bgmListFiles = new File(shortMusicBgmPath).listFiles((FilenameFilter) (dir, name) -> name.endsWith(".mp3"));
        bgmFilePath = bgmListFiles[(int) (LocalDateTime.now().getSecond() % bgmListFiles.length)].getAbsolutePath();
        videoInsertMusic(targetPath, shortsMusicVideoPath, bgmFilePath, 0.8D, 2D, videoFlg);

        shortsDto.setShortsFilePath(zzalVideoPath);
        shortsDto.setShortMusicFilePath(shortsMusicVideoPath);

        log.info("----- finish makeShortsVideo ------");
        
    }

    public void videoInsertMotionGif(String vidPath, String imgPath, String cmpltPath, String scale, double y){
        FFmpegBuilder builder = new FFmpegBuilder()
        .addExtraArgs("-ignore_loop", "0")
        .addInput(imgPath)
        .addInput(vidPath)
        .setComplexFilter("""
            [0:v]scale=%s[b];[1:v][b]overlay=(W-w)/2:%.1f:shortest=1
                        """.formatted(scale, y)
            )
        .addOutput(cmpltPath)
        .setVideoPixelFormat("yuv420p")
        .setAudioCodec("copy")
        .done();

        FFmpegExecutor excutor = new FFmpegExecutor(ffmpeg, ffprobe);
        excutor.createJob(builder, p -> {
            if (p.isEnd()) {
                log.info("--- finish videoInsertMotionGif cmpltPath : " + cmpltPath + " ------");
            }
        }).run();
    }


    private String getOptimalScale(Integer width, Integer height, Integer targetWidth, Integer targetHeight, boolean biggerFlg) {
        Double divWidth = (double) targetWidth / width;
        Double divHeight = (double) targetHeight / height;
        Double multiple = biggerFlg ? NumberUtils.max(divWidth, divHeight) : NumberUtils.min(divWidth, divHeight);
        Integer changeWidth = (int) Math.floor(width * multiple);
        Integer changeHeight = (int) Math.floor(height * multiple);
        String scale = String.valueOf(changeWidth) + ":" + String.valueOf(changeHeight);
        return scale;
    }

    public void shortsInsertHeaderTitle(String title, String filePath, String cmpltPath, Integer y) {
        String videoFilter = "";
        String fontFilePath = "/Users/hojun/Downloads/nanum-gothic/NanumGothicBold.ttf";
        String backgorundColor = colorList.get(LocalDateTime.now().getSecond() % colorList.size());
        if (title.length() > 16) {
            int spaceCnt = StringUtils.countMatches(title, " ");
            int middleIdx = (int) Math.ceil(spaceCnt / 2);
            String[] spaceSplit = CommonUtils.splitByNthOccurrence(title, " ", middleIdx);
            title = StringUtils.join(spaceSplit, "\n");
        }
        FFmpegStream mediaInfo = getMediaInfo(filePath).getStreams().get(0);

        videoFilter += """
                drawtext=text='%s':fontsize=80:fontfile='%s':x=%.1f-text_w/2:y=%d:borderw=2:bordercolor=white:fontcolor=%s
                    """
                .formatted(title, fontFilePath, (double) mediaInfo.width / 2, y, backgorundColor);

        FFmpegBuilder builder = new FFmpegBuilder()
                .addInput(filePath)
                .setVideoFilter(videoFilter)
                .addOutput(cmpltPath)
                .done();

        FFmpegExecutor excutor = new FFmpegExecutor(ffmpeg, ffprobe);
        excutor.createJob(builder, p -> {
            if (p.isEnd()) {
                log.info("--- finish videoInsertImage vidPath : " + cmpltPath + " ------");
            }
        }).run();
    }




}

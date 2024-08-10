package com.hj.blogBatch.processor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.threeten.bp.LocalDateTime;

import com.google.cloud.texttospeech.v1beta1.AudioConfig;
import com.google.cloud.texttospeech.v1beta1.AudioEncoding;
import com.google.cloud.texttospeech.v1beta1.SsmlVoiceGender;
import com.google.cloud.texttospeech.v1beta1.SynthesisInput;
import com.google.cloud.texttospeech.v1beta1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1beta1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1beta1.VoiceSelectionParams;
import com.google.protobuf.ByteString;
import com.hj.blogBatch.config.Selenium;
import com.hj.blogBatch.dto.VideoDto;
import com.hj.blogBatch.entity.Category;
import com.hj.blogBatch.entity.Item;
import com.hj.blogBatch.service.BlogService;
import com.hj.blogBatch.service.CommonService;
import com.hj.blogBatch.utils.CommonUtils;
import com.hj.blogBatch.utils.GptUtils;
import com.hj.blogBatch.utils.RealTimeKeyword;
import com.hj.blogBatch.utils.VideoUtils;
import com.hj.blogBatch.utils.YoutubeUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.probe.FFmpegStream;

@Slf4j
@RequiredArgsConstructor
@Component
public class VideoProcessor implements ItemProcessor<Category, VideoDto>{


    private final RealTimeKeyword realTimeKeyword;
    private final Selenium selenium;
    private final BlogService blogService;
    private final VideoUtils videoUtils;
    private final CommonService commonService;
    private final YoutubeUtil youtubeUtil;
    private final GptUtils gptUtils;
    
    private final String prefix = "다음 json을 읽고 존댓말의 구어체로 5줄로 요약해줘.";
    private final String channelIntro = "/Users/hojun/my/work/blogBatch/media/youtube/youtube_intro.mp4";

    @Value("${com.blogBatch.img.path}")
    String mediaPath;
    @Value("${com.blogBatch.coup.id}")
    String coupangId;
    @Value("${com.blogBatch.coup.pw}")
    String coupangPw;
    @Value("${com.blogBatch.business.email}")
    String businessEmail;
    
    String promptTextAreaSelector = "#chat-content > div > div > div > div > div > div > div > textarea";
    String twicePromptTextAreaSelector = "#rich-textarea";
    String proChangeBtn = "#chat-content > div > div > div > div > div > div > div > div > div > div > div > div";
    String modalSkipButton = "#web-modal > div > div > div > div > div > div";

    @Override
    public VideoDto process(Category category) throws Exception {
        
        VideoDto videoDto = new VideoDto();
        // 디렉토리 생성
        String dirPath = mediaPath + category.getCateId().toString() + "/" ;
        
        try { 
            Files.createDirectory(Paths.get(dirPath));
            log.info("complete creating directory : " + dirPath);
        } catch (FileAlreadyExistsException e) {
            log.warn("디렉토리가 이미 존재합니다");
        } catch (NoSuchFileException e) {
            throw new RuntimeException("There is no directory path");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 쿠팡 파트너스 링크 받기
        // makeCoupangSellLink(category);
        
        String cateNm = category.getCateNm().indexOf("/") != -1 ? category.getCateNm().split("/")[1] : category.getCateNm();
        String videoTitle = getVideoTitle("""
            %s년 %s BEST %d %s 제품들의 객관적인 순위 알려드립니다. %s 구매 하기전 최고의 선택. 이것 보세요오! %s 추천 | 가성비 %s
            """.formatted(LocalDateTime.now().getYear(), cateNm,
            category.getItemList().size(), cateNm, cateNm,
            cateNm, cateNm).trim());


        log.info("-------------- start make thumbnail ----------------");
        String thumbNailFilePath = videoUtils.makeThumbNail(category);
        log.info("-------------- end make thumbnail ----------------");

        log.info("-------------- start makeIntro ----------------");
        String introFilePath = makeIntro(category);
        log.info("-------------- end makeIntro ----------------");

        category.getItemList().sort((o1, o2) -> o2.getItemRank() - o1.getItemRank());

        String[] questionList = category.getItemList().stream().map(item -> prefix + "\n" + item.getDetailJson())
                .toArray(String[]::new);

        List<String> gptAnswer = gptUtils.getGptAnswer(questionList);
        for (int i = 0; i < category.getItemList().size() ; i++) {
            category.getItemList().get(i).setDetailSummary(gptAnswer.get(i));
        }

        log.info("-------------- start make sound ----------------");
        // google tts 사용해서 오디오 제작
        category.getItemList().forEach(item -> {
            try {
                String preSummary = "top" + item.getItemRank().toString() +" 제품은" + item.getItemNm() + "입니다. ";
                item.setDetailSummary(preSummary + item.getDetailSummary());
                String[] split = item.getDetailSummary().split("(다\\.( |\n)|요\\.( |\n))");
                for (int i = 0; i < split.length; i++) {
                    String summary = split[i];
                    if(!summary.endsWith(".") ){
                        if(summary.endsWith("니")){
                            summary += "다.";
                        } else if (summary.endsWith("에") || summary.endsWith("해") || summary.endsWith("어")
                                || summary.endsWith("세") || summary.endsWith("줘") || summary.endsWith("까")
                                || summary.endsWith("게")) {
                            summary += "요.";
                        }
                    }

                    if(summary.length() > 30){
                        splitScript(item.getScriptList(), summary);
                    }else{
                        Map<String, Object> scriptMap = new HashMap<String, Object>();
                        scriptMap.put("summary", summary);
                        scriptMap.put("ord", item.getScriptList().size());
                        item.getScriptList().add(scriptMap);
                    }
                }
                // tts 생성
                for (Map<String, Object> scriptMap : item.getScriptList()) {
                    synthesizeText(item, scriptMap);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        log.info("-------------- end make sound ----------------");

        
        log.info("-------------- start make video ----------------");
        String videoFilePath = videoUtils.makeDanawaVideo(category);
        String uploadVideoFilePath = dirPath + "upload_video.mp4";

        videoUtils.concatMedia(introFilePath, videoFilePath, uploadVideoFilePath);
        int duration = (int) Math.ceil(videoUtils.getMediaInfo(uploadVideoFilePath).streams.get(0).duration);
        videoUtils.videoInsertBgm(uploadVideoFilePath, category, duration);
        log.info("-------------- end make video ----------------");
        

        String coupangDesc = "";
        for (Item item : category.getItemList()) {
            coupangDesc += "\nTOP"+item.getItemRank()+". "+ item.getItemNm() + "\n" + (StringUtils.isNotBlank(item.getItemCoupangUrl())
                    ? item.getItemCoupangUrl()
                    : item.getItemSellUrl()) + "\n";
        }

        String videoDesc = """
                %d년 최고의 %s 제품들을 전적으로 객관적인 구매 및 검색 순위를 기반으로 추천해드립니다. 
                영상 시청은 구매 전 최고의 선택!!
                비즈니스 문의 : %s
                * 구독과 좋아요는 영상 제작에 큰 힘이 됩니다.
                * 이 포스팅은 쿠팡 파트너스 활동의 일환으로, 이에 따른 일정액의 수수료를 제공받습니다.
                %s
                """.formatted(LocalDateTime.now().getYear(), cateNm, businessEmail,coupangDesc).trim();

        videoDto.setCateId(category.getCateId());
        videoDto.setTitle(videoTitle);
        videoDto.setDescription(videoDesc);
        videoDto.setTagList(
            new ArrayList(List.of(cateNm + " 추천", "가성비 " + cateNm, cateNm + "BEST " + category.getItemList().size(),
                "제품 추천", cateNm + " 고르는 방법", cateNm + " 고르는 법", "좋은 " + cateNm + " 기준")));

        if (realTimeKeyword.getRealTimeKeyword() != null && realTimeKeyword.getRealTimeKeyword().size() > 0) {
            videoDto.getTagList().add(realTimeKeyword.getRealTimeKeyword().get(0));
            if (realTimeKeyword.getRealTimeKeyword().size() > 1) {
                realTimeKeyword.getRealTimeKeyword().remove(0);
            }
        }

        videoDto.setVideoFilePath(uploadVideoFilePath);
        videoDto.setThumbnailFilePath(thumbNailFilePath);
        
        
        return videoDto;
    }

    private String getVideoTitle(String text) {
        String prefix = "다음 글을 검색 엔진 최적화 시켜주고, 이모티콘도 바꾸고 100자 미만의 글로 만들어줘.";

        List<String> gptAnswer = gptUtils.getGptAnswer(prefix + "\n"+ text);

        return gptAnswer.get(0);

    }

    private void makeCoupangSellLink(Category category) {
        // todo API는 추후에
        try {
            String loginUrl = "https://login.coupang.com/login/login.pang?rtnUrl=https%3A%2F%2Fpartners.coupang.com%2Fapi%2Fv1%2Fpostlogin";

            WebDriver driver = selenium.getDriver(loginUrl);
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // 로그인버튼클릭
            driver.findElement(By.cssSelector("input[type='email']")).sendKeys(coupangId);
            driver.findElement(By.cssSelector("input[type='password']")).sendKeys(coupangPw);
            driver.findElement(By.cssSelector(".login__button")).click();
            threadSleep(2000L);

            driver.get("https://partners.coupang.com/#affiliate/ws/link-to-any-page");

            if(driver.findElements(By.cssSelector("input[type='password']")).size() > 0){
                driver.findElement(By.cssSelector("input[type='password']")).sendKeys(coupangPw);
                driver.findElement(By.cssSelector(".ant-modal-content button[type='submit']")).click();
                threadSleep(2000L);
            }
            threadSleep(1000L);
            for (Item item : category.getItemList()) {
                if(item.getCoupangYn().equals("Y")){
                    js.executeScript("document.querySelector('#url').value = ''");
                    threadSleep(500L);
                    driver.findElement(By.cssSelector("#url")).sendKeys(item.getItemSellUrl());
                    driver.findElement(By.cssSelector(
                            "div.workspace-container > div > div > div > div > div > div > div > div > div > div > div > div > div:nth-child(1) > div > form > div > div > div > span > span > span > button"))
                            .click();
                    threadSleep(2000L);
                    String coupangUrl = driver.findElement(By.cssSelector("div.tracking-url > div")).getText();
                    item.setItemCoupangUrl(coupangUrl);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally{
            selenium.closeDriver();
        }

    }

    private String translateOnPapago(String text) {
        try {
            String papagoUrl = "https://papago.naver.com/";

            WebDriver driver = selenium.getDriver(papagoUrl);
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // 로그인버튼클릭
            driver.findElement(By.cssSelector("#txtSource")).sendKeys(text);
            threadSleep(2000L);
            return driver.findElement(By.cssSelector("#txtTarget span:nth-child(1)")).getText();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally{
            selenium.closeDriver();
        }

    }

    private String makeIntro(Category category) {

        String cateNm = category.getCateNm().indexOf("/") != -1 ? category.getCateNm().split("/")[1] : category.getCateNm();
        String engCateNm = translateOnPapago(cateNm);
        String pixabayUrl = "https://pixabay.com/ko/videos/search/";
        String question = """
                %s을 구매하려고 할 때 고려해야 할 것 중에 가장 중요한 세가지를 알려줘. 형식은 영상에 쓰일 자막이니까 300자 이하의 존댓말의 구어체로 만들어줘.
                """.formatted(cateNm);

        String videoSelector = """
            #app > div:nth-child(1) > div > div > div > div > div > div:nth-child(%d) > div > div > a > img
            """;

        String videoQualitySelector = """
            #app > div:nth-child(1) > div > div > div > div > div > div:nth-child(%d) > div:nth-child(1) > div > div > div > div > strong
            """;

        String cmpltFilePath = mediaPath + category.getCateId()+"/intro_cmplt.mp4";
        String targetFilePath = mediaPath + category.getCateId()+"/intro_target.mp4";


        List<String> introList = new ArrayList<>();
        try {

            // pixabay 동영상 다운
            WebDriver driver = selenium.getDriver(pixabayUrl + engCateNm);

            if(driver.findElements(By.cssSelector(videoSelector.formatted(1))).size() == 0){
              driver = selenium.getDriver(pixabayUrl + cateNm);
            }
            if(driver.findElements(By.cssSelector(videoSelector.formatted(1))).size() == 0){
              driver = selenium.getDriver(pixabayUrl + "배경");
            }
            
            JavascriptExecutor js = (JavascriptExecutor) driver;
            for (int i = 1; i < 5; i++) {
                if(introList.size() == 2){
                    break;
                }
                String imgSrc = driver.findElement(By.cssSelector(videoSelector.formatted(i))).getAttribute("src");
                String videoQuality = driver.findElement(By.cssSelector(videoQualitySelector.formatted(i))).getText().toUpperCase();
                String targetQuality = videoQuality.equals("HD") ? "large" : videoQuality.equals("SD") ? "medium" : "small";
                String videoSrc = imgSrc.substring(0, imgSrc.lastIndexOf("tiny.")) + targetQuality +".mp4";
                String outputSuffix = """
                        /intro_%d.mp4
                        """.formatted(i).trim();
                String outputFilePath = mediaPath + category.getCateId() + outputSuffix;
                try{
                    commonService.downloadVideo(videoSrc, outputFilePath);
                    FFmpegStream fFmpegStream = videoUtils.getMediaInfo(outputFilePath).streams.get(0);
                    if(fFmpegStream.width != 1920 && fFmpegStream.height != 1080){
                        if((double) fFmpegStream.width/fFmpegStream.height == (double) 16/9){
                            videoUtils.fileRatioChange(outputFilePath, 1920, 1080);
                        }else if((double) fFmpegStream.width/fFmpegStream.height == (double) 4/3){
                            videoUtils.fileRatioChange(outputFilePath, 1440, 1080);
                            videoUtils.fileRatioChangeWithLetterBox(outputFilePath);
                        }else{
                            continue;
                        }
                        
                    }
                }catch (Exception e){
                    throw new RuntimeException(e);
                }
                introList.add(outputFilePath);
            }
            driver.close();

            String introScript = gptUtils.getGptAnswer(question).get(0);
            
            // tts 만들기
            String dirPath = mediaPath + category.getCateId() + "/";
            List<Map<String,Object>> scriptList = new ArrayList<>();
            try {
                LocalDateTime now = LocalDateTime.now();
                String preSummary = """
                        %s년 best%d %s 제품은 무엇인지 알아보겠습니다.
                        제품 소개에 앞서 %s 제품을 구입 시 반드시 알아둬야 할 사항부터 짚고 넘어가겠습니다.
                        """.formatted(now.getYear(), category.getItemList().size(), category.getCateNm(), category.getCateNm());

                introScript = preSummary + introScript;
                String[] split = introScript.split("(다\\.( |\n)|요\\.( |\n))");
                // String[] split = introScript.split("다.");
                for (int i = 0; i < split.length; i++) {
                    String summary = split[i];
                    if(!summary.endsWith(".") ){
                        if(summary.endsWith("니")){
                            summary += "다.";
                        } else if (summary.endsWith("에") || summary.endsWith("해") || summary.endsWith("어")
                                || summary.endsWith("세") || summary.endsWith("줘") || summary.endsWith("까")
                                || summary.endsWith("게")) {
                            summary += "요.";
                        }
                    }

                    if(summary.length() > 30){
                        splitScript(scriptList, summary);
                    }else{
                        Map<String, Object> scriptMap = new HashMap<String, Object>();
                        scriptMap.put("summary", summary);
                        scriptMap.put("ord", scriptList.size());
                        scriptList.add(scriptMap);
                    }
                }
                // tts 생성
                for (Map<String, Object> scriptMap : scriptList) {
                    synthesizeText(category, scriptMap);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            Double audioTimeSec = scriptList.stream()
                        .mapToDouble(map -> {
                            double duration = videoUtils.getMediaInfo(map.get("audioFilePath").toString()).getFormat().duration;
                            map.put("duration", duration);
                            return duration;
                        })
                        .sum();
            
            // 동영상 합치기
            if(introList.size() == 2){
                videoUtils.concatMediaOnlyVideo(introList.get(0), introList.get(1), cmpltFilePath);
            }
            videoUtils.renameCmpltToTarget("intro", dirPath);

            // tts 영상에 넣기
            // txt파일 만들어서 오디오 합치기
            List<String> fileTextList = new ArrayList<>();
            for (Map<String,Object> map : scriptList) {
                String txtRowString = "file '";
                txtRowString += map.get("audioFilePath").toString();
                txtRowString += "'";
                fileTextList.add(txtRowString);
            }

            String txtFilePath = dirPath + "intro" + "_concat.txt";
            String concatFilePath = dirPath + "intro" + "_concat.mp3";
            videoUtils.makeTxtFile(txtFilePath, fileTextList);
            videoUtils.concatMedia(txtFilePath, concatFilePath);

            // 오디오에 영상 길이 맞추기 오디오가 더 길면 영상 반복 처리 짧으면 영상 뒤에서 자르기
            double audioDuration = videoUtils.getMediaInfo(concatFilePath).streams.get(0).duration;
            double videoDuration = videoUtils.getMediaInfo(targetFilePath).streams.get(0).duration;

            boolean shortestFlg = true;
            // todo test
            /* if(audioDuration < (videoDuration+1)){
                shortestFlg = true;
            } */

            videoUtils.videoInsertAudio(targetFilePath, concatFilePath, cmpltFilePath, false, "1000", shortestFlg);

            // tts 영상에 넣기
            // videoUtils.renameCmpltToTarget(cmpltFilePath, targetFilePath);
            // videoUtils.videoInsertAudios(scriptList, targetFilePath, cmpltFilePath, "intro", mediaPath + category.getCateId() + "/");

            // 자막 넣기
            videoUtils.renameCmpltToTarget("intro", dirPath);
            videoUtils.videoInsertSubtitle(scriptList, targetFilePath, cmpltFilePath, 1D);
            

            videoUtils.renameCmpltToTarget("intro", dirPath);
            videoUtils.cutMedia(targetFilePath, cmpltFilePath, 0, (int) Math.ceil(audioDuration) + 1);

            videoUtils.renameCmpltToTarget("intro", dirPath);
            videoUtils.concatMedia(channelIntro, targetFilePath, cmpltFilePath);
            // videoUtils.renameCmpltToTarget("intro", dirPath);
            // videoUtils.fillSilenceAudio(targetFilePath, cmpltFilePath, 3D);

        }catch (Exception e){
            throw new RuntimeException(e);
        }

        return cmpltFilePath;

    }

    /**
     * 30자 이하가 될때 까지 재귀호출
     */
    private void splitScript(List<Map<String, Object>> scriptList, String summary) {
        int divCnt = (int) Math.ceil(summary.length() / 30);
        if(summary.length() > 30){
            String[] commaSplit = summary.split(", ");
            if(commaSplit.length == 1){ // 띄어쓰기 기준으로 나누기
                String[] spaceSplit = summary.split(" ");
                int middleIdx = (int) Math.ceil(spaceSplit.length / 2);
                spaceSplit = CommonUtils.splitByNthOccurrence(summary, " ", middleIdx);
                for (String splitSummary : spaceSplit) {
                    splitScript(scriptList, splitSummary);
                }
            }else{ // 콤마로 나누기
                int middleIdx = (int) Math.ceil(commaSplit.length / 2);
                commaSplit = CommonUtils.splitByNthOccurrence(summary, ", ", middleIdx);
                for (String splitSummary : commaSplit) {
                    splitScript(scriptList, splitSummary);
                }
            }

        }else{
            Map<String, Object> scriptMap = new HashMap<>();
            scriptMap.put("ord", scriptList.size());
            scriptMap.put("summary", summary);
            scriptList.add(scriptMap);
        }
        
    }

    private void threadSleep(long milSec){
        try {
            Thread.sleep(milSec);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void synthesizeText(Item item, Map<String, Object> scriptMap) throws Exception {

        // Instantiates a client
        try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {
            // Set the text input to be synthesized
            SynthesisInput input = SynthesisInput.newBuilder().setText(scriptMap.get("summary").toString()).build();

            // Build the voice request
            VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                    .setName("ko-KR-Wavenet-D")
                    .setLanguageCode("ko-KR") // languageCode = "en_us"
                    .setSsmlGender(SsmlVoiceGender.MALE) // ssmlVoiceGender = SsmlVoiceGender.FEMALE
                    .build();

            // Select the type of audio file you want returned
            AudioConfig audioConfig = AudioConfig.newBuilder()
                    .setAudioEncoding(AudioEncoding.MP3) // MP3 audio.
                    .setSpeakingRate(1D)
                    .build();

            // Perform the text-to-speech request
            SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);

            // Get the audio contents from the response
            ByteString audioContents = response.getAudioContent();

            // Write the response to the output file.
            String audioFilePath = mediaPath + item.getCateId() + "/" + item.getItemRank().toString()+"_"+ scriptMap.get("ord") +".mp3";
            scriptMap.put("audioFilePath", audioFilePath);

            try(FileOutputStream out = new FileOutputStream(audioFilePath)){
                out.write(audioContents.toByteArray());
            }
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    /**
     * 구글 tts
     */
    public void synthesizeText(Category category, Map<String, Object> scriptMap) throws Exception {

        // Instantiates a client
        try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {
            // Set the text input to be synthesized
            SynthesisInput input = SynthesisInput.newBuilder().setText(scriptMap.get("summary").toString()).build();

            // Build the voice request
            VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                    .setName("ko-KR-Wavenet-D")
                    .setLanguageCode("ko-KR") // languageCode = "en_us"
                    .setSsmlGender(SsmlVoiceGender.MALE) // ssmlVoiceGender = SsmlVoiceGender.FEMALE
                    .build();

            // Select the type of audio file you want returned
            AudioConfig audioConfig = AudioConfig.newBuilder()
                    .setAudioEncoding(AudioEncoding.MP3) // MP3 audio.
                    .setSpeakingRate(1.2D)
                    .build();

            // Perform the text-to-speech request
            SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);

            // Get the audio contents from the response
            ByteString audioContents = response.getAudioContent();

            // Write the response to the output file.
            String audioFilePath = mediaPath + category.getCateId() + "/intro_"+ scriptMap.get("ord") +".mp3";
            scriptMap.put("audioFilePath", audioFilePath);

            try(FileOutputStream out = new FileOutputStream(audioFilePath)){
                out.write(audioContents.toByteArray());
            }
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    

}


/* 
@Override
    public VideoDto process(Category category) throws Exception {
        VideoDto videoDto = new VideoDto();
        
        #chat-content > div > div > div > div > div > div > div > textarea
        // AI item 상세정보 요약
        WebDriver driver = selenium.getDriver(gptUrl);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        driver.findElement(By.cssSelector("#prompt-textarea")).sendKeys(firstConv);
        driver.findElement(By.cssSelector("#prompt-textarea")).sendKeys(Keys.ENTER);
        sendMessage(js, 3000L);

        category.getItemList().forEach(item -> {
            String question = prefix + "\n" + item.getDetailJson();
            driver.findElement(By.cssSelector("#prompt-textarea")).sendKeys(question);
            driver.findElement(By.cssSelector("#prompt-textarea")).sendKeys(Keys.ENTER);
            try {
                sendMessage(js, 20000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e.getMessage());
            }
            List<WebElement> answerEles = driver.findElements(By.cssSelector("div[data-message-author-role='assistant']"));
            String answer = answerEles.get(answerEles.size()-1).findElement(By.cssSelector("p")).getText();
            item.setDetailSummary(answer);
            log.info("-------------- gpt 답변 ----------------");
            log.info("제품명 : " + item.getItemNm());
            log.info(answer);
            log.info("-------------- gpt 답변 ----------------");
        });

        

        
        
        return null;
    }
 */
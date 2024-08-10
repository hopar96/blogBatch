package com.hj.blogBatch.job;

import static java.util.Map.entry;

import java.io.*;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;
import java.util.stream.*;

import org.modelmapper.internal.util.Strings;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.hj.blogBatch.config.Selenium;
import com.hj.blogBatch.dto.ShortsDto;
import com.hj.blogBatch.entity.AtFile;
import com.hj.blogBatch.entity.Blog;
import com.hj.blogBatch.entity.Shorts;
import com.hj.blogBatch.entity.Target;
import com.hj.blogBatch.processor.ShortsProcessor;
import com.hj.blogBatch.repository.AtFileRepository;
import com.hj.blogBatch.repository.BlogRepository;
import com.hj.blogBatch.repository.ShortsRepository;
import com.hj.blogBatch.repository.TargetRepository;
import com.hj.blogBatch.service.BlogService;
import com.hj.blogBatch.service.CommonService;
import com.hj.blogBatch.utils.CommonUtils;
import com.hj.blogBatch.utils.RealTimeKeyword;
import com.hj.blogBatch.utils.VideoUtils;
import com.hj.blogBatch.utils.YoutubeUtil;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;

@RequiredArgsConstructor
@Slf4j
@Configuration
public class ShortsJobConfig {

    private final PlatformTransactionManager transactionManager;
    private final Selenium selenium;
    private final ShortsProcessor shortsProcessor;
    private final BlogService blogService;
    private final CommonService commonService;
    private final AtFileRepository atFileRepository;
    private final ShortsRepository shortsRepository;
    private final TargetRepository targetRepository;
    private final BlogRepository blogRepository;
    private final RealTimeKeyword realTimeKeyword;
    private final YoutubeUtil youtubeUtil;
    private final VideoUtils videoUtils;
    private final CommonUtils commonUtils;

    @Value("${com.blogBatch.img.url}")
    private String imgUrl;
    @Value("${com.shortsBatch.target.path}")
    private String shortsTargetPath;
    @Value("${com.shortsBatch.kakao.id}")
    private String kakaoId;
    @Value("${com.shortsBatch.kakao.pw}")
    private String kakaoPw;
    @Value("${com.blogBatch.img.path}")
    String mediaPath;
    @Value("${com.shortsBatch.shortsYn}")
    String shortsYn;
    private FFmpegStream fFmpegStream;

    private Map<Integer, Long> channelMap = Map.ofEntries(
            entry(89, 1L), entry(87, 3L), entry(36, 4L), entry(31, 5L), entry(14, 6L), entry(32, 7L), entry(12, 8L),
            entry(7, 9L), entry(3, 10L), entry(58, 11L), entry(11, 12L));
    


    @Bean
    public Job shortsJob(JobRepository jobRepository, Flow uploadFlow, Flow makeFlow, Flow blogFlow,
            JobExecutionDecider decider) {
        return new JobBuilder("shortsJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(decider)
                .from(decider).on("upload").to(uploadFlow)
                .from(decider).on("make").to(makeFlow)
                .from(decider).on("blog").to(blogFlow)
                .end()
                .build();
    }
    
    @Bean
    public Flow uploadFlow(Step uploadInstaStep, Step uploadYoutueStep) {
        return new FlowBuilder<SimpleFlow>("uploadFlow")
                .start(uploadInstaStep) // 첫 번째 Step
                .next(uploadYoutueStep) // 두 번째 Step
                .build();
    }
    @Bean
    public Flow makeFlow(Step uploadInstaStep, Step uploadYoutueStep,Step makeShortsAndBlogStep,Step shortsCrawlingStep) {
        return new FlowBuilder<SimpleFlow>("makeFlow")
                .start(shortsCrawlingStep) // 첫 번째 Step
                .next(makeShortsAndBlogStep) // 두 번째 Step
                .next(uploadInstaStep)
                .next(uploadYoutueStep)
                .build();
    }
    @Bean
    public Flow blogFlow(Step makeOnlyBlogStep,Step shortsCrawlingStep) {
        return new FlowBuilder<SimpleFlow>("blogFlow")
                .start(shortsCrawlingStep) // 첫 번째 Step
                .next(makeOnlyBlogStep)
                .build();
    }

    @Bean
    public JobExecutionDecider decider() {
        return (JobExecution jobExecution, StepExecution stepExecution) -> {
            Integer count = shortsRepository.countByUploadYn("N");
        
            return shortsYn.equals("N") ? new FlowExecutionStatus("blog") : count > 5 ? new FlowExecutionStatus("upload"): new FlowExecutionStatus("make");
        };
                
    }

    @Bean
    public Step makeOnlyBlogStep(JobRepository jobRepository, Tasklet makeOnlyBlogTasklet) {
        return new StepBuilder("makeOnlyBlogStep", jobRepository)
                .tasklet(makeOnlyBlogTasklet, transactionManager)
                .build();
    }

    @Bean
    public Tasklet makeOnlyBlogTasklet() {
        return ((contribution, chunkContext) -> {
            Target target = targetRepository.findFirstByMakeYnOrderByTargetId("N");
            if (target == null) {
                return RepeatStatus.FINISHED;
            }
            AtFile atFile = atFileRepository.findById(target.getFileId()).orElseThrow();
            // file s3 업로드
            AtFile uploadAtFile = commonService.attachFileUploadS3(atFile);
            String blogContent = """
                    <div>
                        <img src="%s" alt="%s 이미지" />
                        <p>%s</p>
                    </div>
                                """.formatted(imgUrl + uploadAtFile.getFileNm(), target.getTitle(),
                    target.getContent().replaceAll("\n", "</p><p>")).trim();
            Blog blog = Blog.builder()
                    .foreignKeyId(target.getTargetId())
                    .foreignKeyTarget("target")
                    .title(target.getTitle())
                    .mainFileId(uploadAtFile.getFileId())
                    .keywords(target.getTitle().replaceAll(",", "").replaceAll(" ", ","))
                    .content(blogContent)
                    .views(0)
                    .regDt(LocalDateTime.now())
                    .authorNm("주인장")
                    .useYn("Y")
                    .blogCateId(target.getBlogCateId())
                    .build();

            blogRepository.saveAndFlush(blog);
            target.setMakeYn("Y");
            targetRepository.save(target);

            return RepeatStatus.CONTINUABLE;
        });
    }



    @Bean
    public Step makeShortsAndBlogStep(JobRepository jobRepository) {

        return new StepBuilder("makeShortsAndBlogStep", jobRepository)
                .<ShortsDto, ShortsDto>chunk(1, transactionManager)
                .reader(shortsItemReader())
                .processor(shortsProcessor)
                .writer(shortsItemWriter())
                .build();
    }

    @Bean
    public ItemReader<ShortsDto> shortsItemReader() {
        return new ItemReader<ShortsDto>() {
            @Override
            public ShortsDto read()
                    throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {

                File path = new File(shortsTargetPath);
                File[] listFiles = path.listFiles((FilenameFilter) (dir, name) -> name.endsWith(".mp4")
                        || name.endsWith(".png") || name.endsWith(".jpg"));

                ShortsDto shortsDto = new ShortsDto();
                if (listFiles.length == 0) {
                    Target target = targetRepository.findFirstByMakeYnOrderByTargetId("N");
                    if (target == null) {
                        return null;
                    }
                    AtFile atFile = atFileRepository.findById(target.getFileId()).orElseThrow();

                    shortsDto.setTargetId(target.getTargetId());
                    shortsDto.setTitle(target.getTitle());
                    shortsDto.setTargetFilePath(mediaPath + atFile.getFileNm());
                } else {
                    File targetFile = listFiles[0];
                    shortsDto.setTargetFilePath(targetFile.getAbsolutePath());

                    String onlyName = targetFile.getName().substring(0, targetFile.getName().lastIndexOf("."));
                    // 정규식을 사용하여 숫자가 포함되어 있는지 확인하는 패턴
                    Pattern pattern = Pattern.compile("^[0-9]*$");
                    Matcher matcher = pattern.matcher(onlyName);

                    // 숫자가 아닌 문자일 경우 제목으로 쓸겨
                    if (!matcher.find()) {
                        shortsDto.setTitle(onlyName);
                    }
                }
                log.info("shortsDto : " + shortsDto.toString());
                return shortsDto;
            }
        };
    }

    @Bean
    public ItemWriter<ShortsDto> shortsItemWriter() {
        return new ItemWriter<ShortsDto>() {

            @Override
            public void write(Chunk<? extends ShortsDto> chunk) throws Exception {
                log.info("---------------- Start shortsItemWriter ------------------");
                for (ShortsDto shortsDto : chunk) {
                    log.info("---------------- start write title : " + shortsDto.getTitle() + "------------------");
                    AtFile atFile = commonService.attachFileNotMove(shortsDto.getShortsFilePath());
                    AtFile shortsMusicAtFile = commonService.attachFileNotMove(shortsDto.getShortMusicFilePath());

                    Shorts shorts = shortsDto.getShorts();
                    shorts.setVideoFileId(atFile.getFileId());
                    shorts.setVideoTitle(shortsDto.getTitle());
                    shorts.setVideoDesc(shortsDto.getDescription());
                    shorts.setRegDt(LocalDateTime.now());
                    shorts.setUploadYn("N");
                    shorts.setSmUploadYn("N");
                    shorts.setSmVideoFileId(shortsMusicAtFile.getFileId());

                    if (shortsDto.getTargetId() != null) {
                        shorts.setForeignKeyTarget("target");
                        shorts.setForeignKeyId(shortsDto.getTargetId());

                        Target target = targetRepository.findById(shortsDto.getTargetId()).orElseThrow();
                        AtFile targetAtFile = atFileRepository.findById(target.getFileId()).orElseThrow();
                        target.setMakeYn("Y");
                        targetRepository.save(target);

                        // file s3 업로드
                        AtFile uploadAtFile = commonService.attachFileUploadS3(targetAtFile);
                        String blogContent = """
                                <div>
                                    <img src="%s" alt="%s 이미지" />
                                    <p>%s</p>
                                </div>
                                """.formatted(imgUrl+uploadAtFile.getFileNm(), target.getTitle(), target.getContent()).trim();
                        Blog blog = Blog.builder()
                                .foreignKeyId(shortsDto.getTargetId())
                                .foreignKeyTarget("target")
                                .title(shortsDto.getTitle())
                                .mainFileId(uploadAtFile.getFileId())
                                .keywords(shortsDto.getTitle().replaceAll(",", "").replaceAll(" ", ","))
                                .content(blogContent)
                                .views(0)
                                .regDt(LocalDateTime.now())
                                .authorNm("주인장")
                                .useYn("Y")
                                .blogCateId(target.getBlogCateId())
                                .build();

                        blogRepository.saveAndFlush(blog);
                    } else {
                        CommonUtils.deleteFile(shortsDto.getTargetFilePath());
                    }

                    shortsRepository.saveAndFlush(shorts);

                    log.info("---------------- end write title : " + shortsDto.getTitle() + "------------------");
                }

                log.info("---------------- End shortsItemWriter ------------------");
            }
        };
    }

    @Bean
    public Step uploadInstaStep(JobRepository jobRepository, Tasklet uploadInstaTasklet) {
        return new StepBuilder("uploadInstaStep", jobRepository)
                .tasklet(uploadInstaTasklet, transactionManager)
                .build();
    }

    @Bean
    public Tasklet uploadInstaTasklet() {
        return ((contribution, chunkContext) -> {
            String shellPath = "/Users/hojun/my/work/blogBatch/uploadInsta.sh";
            Pageable pageRequest = PageRequest.of(0, 14);
            List<Shorts> shortsList = shortsRepository.findBySmUploadYnAndSmVideoFileIdIsNotNull("N", pageRequest);
            String titleSuffix = String.join(" ", List.of("#웃긴짤", "#짤", "#썰", "#웃긴동영상", "#썰스타그램", "#짤스타그램"));
            Map<String, Object> map = new HashMap<>();
            List<Map<String, Object>> fileList = new ArrayList<>();
            for (Shorts shorts : shortsList) {
                AtFile atFile = atFileRepository.findById(shorts.getSmVideoFileId()).orElseThrow();
                Map<String, Object> tmpMap = new HashMap<>();
                tmpMap.put("path", mediaPath + atFile.getFileNm());
                tmpMap.put("title", shorts.getVideoTitle() + " " + titleSuffix);
                fileList.add(tmpMap);
                shorts.setSmUploadYn("Y");
            }
            map.put("fileList", fileList);
            ObjectMapper objectMapper = new ObjectMapper();

            try {
                // ProcessBuilder를 사용하여 셸 스크립트 실행
                ProcessBuilder processBuilder = new ProcessBuilder(shellPath, objectMapper.writeValueAsString(map));
                processBuilder.redirectErrorStream(true); // 표준 에러를 표준 출력으로 리디렉션

                // 프로세스 시작
                Process process = processBuilder.start();

                // 스크립트의 출력 결과를 읽어오기 위한 BufferedReader 설정
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info(line); // 출력 결과를 콘솔에 출력
                }

                // 프로세스가 종료될 때까지 대기
                int exitCode = process.waitFor();
                log.info("Exit Code: " + exitCode);

                /*
                 * Process process = Runtime.getRuntime().exec(shellPath + " " +
                 * objectMapper.writeValueAsString(map));
                 * 
                 * // Read output
                 * StringBuilder output = new StringBuilder();
                 * BufferedReader reader = new BufferedReader(
                 * new InputStreamReader(process.getInputStream()));
                 * 
                 * String line;
                 * while ((line = reader.readLine()) != null) {
                 * output.append(line);
                 * }
                 */
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            shortsRepository.saveAll(shortsList);

            return RepeatStatus.FINISHED;
        });
    }

    @Bean
    public Step uploadYoutueStep(JobRepository jobRepository, Tasklet uploadYoutueTasklet) {
        return new StepBuilder("uploadYoutueStep", jobRepository)
                .tasklet(uploadYoutueTasklet, transactionManager)
                .build();
    }

    @Bean
    public Tasklet uploadYoutueTasklet() {
        return ((contribution, chunkContext) -> {

            Shorts shorts = shortsRepository.findFirstByUploadYnAndSmVideoFileIdIsNotNullOrderByShortsId("N");
            if (shorts == null) {
                return RepeatStatus.FINISHED;
            }
            AtFile atFile = atFileRepository.findById(shorts.getVideoFileId()).orElseThrow();

            String coupangLink = "https://link.coupang.com/a/bJGqOP";

            String keyword = "";
            if (realTimeKeyword.getRealTimeKeyword() != null && realTimeKeyword.getRealTimeKeyword().size() > 0) {
                keyword = realTimeKeyword.getRealTimeKeyword().get(0);
                if (realTimeKeyword.getRealTimeKeyword().size() > 1) {
                    realTimeKeyword.getRealTimeKeyword().remove(0);
                }
            }
            String description = """
                    %s
                    항상 재밌는 썰이 듣고 보고 싶다면 구독 눌러주세요!
                    비즈니스 문의 : skaus458@gmail.com
                    * 구독과 좋아요는 영상 제작에 큰 힘이 됩니다.

                    * 이 포스팅은 쿠팡 파트너스 활동의 일환으로, 이에 따른 일정액의 수수료를 제공받습니다.
                    * 와우 회원이라면 매일 오전 7시 골드박스!
                    * %s
                    """.formatted(shorts.getVideoTitle(), coupangLink)
                    .trim();

            ShortsDto shortsDto = new ShortsDto();
            shortsDto.setDescription(description);

            shortsDto.setUploadTitle("""
                    %s #shorts #썰 #이야기 #웃긴썰 #웃긴영상 #유머 #이슈
                    """.formatted(shorts.getVideoTitle()).trim());

            for (String titleSplit : shorts.getVideoTitle().split(" ")) {
                shortsDto.getTagList().add(titleSplit);
            }
            shortsDto.getTagList().add(keyword);
            shortsDto.setShortsFilePath(mediaPath + atFile.getFileNm());

            youtubeUtil.uploadShorts(shortsDto);

            shorts.setUploadYn("Y");
            shorts.setYoutubeChannel(shortsDto.getYoutubeChannel());
            shortsRepository.save(shorts);

            return RepeatStatus.CONTINUABLE;
        });
    }

    @Bean
    public Step shortsCrawlingStep(JobRepository jobRepository, Tasklet shortsCrawlingTasklet) {
        return new StepBuilder("shortsCrawlingStep", jobRepository)
                .tasklet(shortsCrawlingTasklet, transactionManager)
                .build();
    }

    @Bean
    public Tasklet shortsCrawlingTasklet() {
        return ((contribution, chunkContext) -> {
            /*
             * File path = new File(shortsTargetPath);
             * File[] listFiles = path.listFiles((FilenameFilter) (dir, name) ->
             * name.endsWith(".mp4")
             * || name.endsWith(".png") || name.endsWith(".jpg"));
             * if (listFiles.length > 0) {
             * return RepeatStatus.FINISHED;
             * }
             */

            try {
                if(targetRepository.countByMakeYn("N") > 5){
                    return RepeatStatus.FINISHED;
                }

                // selenium call
                WebDriver driver = selenium.getDriver("https://accounts.kakao.com");
                WebElement idElement = driver.findElement(By.cssSelector("input[name='loginId']"));
                WebElement pwElement = driver.findElement(By.cssSelector("input[name='password']"));
                idElement.sendKeys(kakaoId);
                pwElement.sendKeys(kakaoPw);
                driver.findElement(By.cssSelector("button[type='submit']")).click();

                Thread.sleep(5000);

                driver.get("https://partners.newspic.kr/login");
                Thread.sleep(2000);
                JavascriptExecutor js = (JavascriptExecutor) driver;

                driver.findElement(By.cssSelector("button.btn-kakao")).click();
                Thread.sleep(2000);

                List<Integer> channelNoList = null;
                // 쇼츠 타겟일 경우 유머 탭만
                if(shortsYn.equals("Y")){
                    channelNoList = List.of(89);
                }else{
                    channelNoList = new ArrayList<>(channelMap.keySet());
                }

                for (Integer channelNo : channelNoList) {
                    driver.get("https://partners.newspic.kr/main");
                    Thread.sleep(5000);
                    js.executeScript(String.format("document.querySelector(\"a[data-channel-no='%s']\").click()", channelNo.toString()));
                    // driver.findElement(By.cssSelector(String.format("a[data-channel-no='%s']", channelNo.toString()))).click();
                    Thread.sleep(4000);

                    // driver.findElement(By.cssSelector("#channelList > li:nth-child(1) > div.info
                    // > a > span")).click();
                    // js.executeScript("window.scrollTo(0, 1000);");
                    // Thread.sleep(2000);

                    List<WebElement> itemList = driver.findElements(By.cssSelector("#channelList > li"));

                    List<String> titleList = new ArrayList<>();
                    for (WebElement item : itemList) {
                        titleList.add(item.findElement(By.cssSelector("div.info > a > span")).getText());
                    }
                    List<Target> dupTargetList = targetRepository.findByTitleIn(titleList);
                    itemList.removeIf(item -> {
                        for (Target target : dupTargetList) {
                            if (item.findElement(By.cssSelector("div.info > a > span")).getText()
                                    .equals(target.getTitle())) {
                                return true;
                            }
                        }

                        return false;
                    });

                    List<String> urlList = itemList.stream()
                            .map(item -> item.findElement(By.cssSelector("a")).getAttribute("href"))
                            .collect(Collectors.toList());

                    List<Target> targetList = new ArrayList<>();
                    for (String url : urlList) {
                        driver.get(url);
                        Thread.sleep(4000);
                        String title = driver.findElement(By.cssSelector("div.article_wrap > div.headline_view > h3"))
                                .getText();
                        // 비디오 있음
                        if (driver.findElements(By.cssSelector(".article_view video")).size() > 0) {
                            if(shortsYn.equals("N")){ //blog 크롤링은 video 없을 경우에만
                                continue; 
                            }

                            WebElement videoEle = driver.findElement(By.cssSelector(".article_view video"));
                            if (!videoEle.getAttribute("src").isBlank()) {
                                commonService.downloadVideo(
                                        driver.findElement(By.cssSelector(".article_view video")).getAttribute("src"),
                                        shortsTargetPath + "/" + title + ".mp4");
                            } else {
                                commonService.downloadVideo(
                                        videoEle.findElement(By.cssSelector("source")).getAttribute("src"),
                                        shortsTargetPath + "/" + title + ".mp4");
                            }

                        } else {
                            js.executeScript("document.querySelector('#naver-shoppingbox-div')?.remove();");
                            js.executeScript("document.querySelector('.bottom_bar_pc')?.remove();");
                            js.executeScript("document.querySelector('.source_partners')?.remove();");
                            js.executeScript("document.querySelectorAll('.link_photo')?.forEach(i => i.remove());");
                            js.executeScript("document.querySelector('.toast_msg_wrap_fixed')?.remove();");
                            js.executeScript("document.querySelector('.detail-related')?.remove();");
                            Thread.sleep(2000);

                            byte[] screenshot = driver.findElement(By.cssSelector(".article_view"))
                                    .getScreenshotAs(OutputType.BYTES);
                            AtFile screenshotFile = commonService.attachFile("png", screenshot, null);
                            String webpFilePath = commonUtils.imageConvertWebp(mediaPath + screenshotFile.getFileNm());
                            screenshotFile.setFileNm(webpFilePath.substring(webpFilePath.lastIndexOf("/") + 1));

                            List<WebElement> contentEeles = driver.findElements(By.cssSelector(".article_view p"));
                            List<String> contentList = new ArrayList<>();
                            for (WebElement pEle : contentEeles) {
                                if (!pEle.getText().isBlank()) {
                                    contentList.add(pEle.getText());
                                }
                            }

                            Target target = Target.builder()
                                    .title(title)
                                    .fileId(screenshotFile.getFileId())
                                    .content(String.join("\n", contentList))
                                    .makeYn("N")
                                    .blogCateId(channelMap.get(channelNo))
                                    .build();

                            targetList.add(target);
                        }

                        targetRepository.saveAllAndFlush(targetList);
                    }
                }
                

            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                selenium.closeDriver();
            }

            return RepeatStatus.FINISHED;
        });
    }

    /*
     * @Bean
     * public FlatFileItemReader<BlogCrawlDto> blogItemReader() {
     * return new FlatFileItemReaderBuilder<BlogCrawlDto>()
     * .name("blogItemReader")
     * .resource(new ClassPathResource("cate_title.csv"))
     * .delimited()
     * .names("title")
     * .targetType(BlogCrawlDto.class)
     * .build();
     * }
     */

    /*
     * @Bean
     * public BlogProcessor blogProcessor(){
     * return new BlogProcessor(selenium);
     * }
     */

}

package com.hj.blogBatch.job;

import java.text.ParseException;
import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import java.time.LocalDateTime;

import com.hj.blogBatch.config.Selenium;
import com.hj.blogBatch.dto.BlogCrawlDto;
import com.hj.blogBatch.dto.VideoDto;
import com.hj.blogBatch.entity.AtFile;
import com.hj.blogBatch.entity.Category;
import com.hj.blogBatch.entity.Video;
import com.hj.blogBatch.processor.CrawlProcessor;
import com.hj.blogBatch.processor.VideoProcessor;
import com.hj.blogBatch.repository.CategoryRepository;
import com.hj.blogBatch.repository.VideoRepository;
import com.hj.blogBatch.service.BlogService;
import com.hj.blogBatch.service.CommonService;
import com.hj.blogBatch.utils.GptUtils;
import com.hj.blogBatch.utils.YoutubeUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Configuration
public class DanawaJobConfig {

    private final PlatformTransactionManager transactionManager;
    private final CategoryRepository categoryRepository;
    private final VideoRepository videoRepository;
    private final Selenium selenium;
    private final CrawlProcessor crawlProcessor;
    private final VideoProcessor videoProcessor;
    private final BlogService blogService;
    private final CommonService commonService;
    private final YoutubeUtil youtubeUtil;
    private final GptUtils GptUtils;

    @Bean
    public Job danawaJob(JobRepository jobRepository, Step crawlStep, Step videoStep, Step uploadStep) {
        return new JobBuilder("danawaJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(crawlStep)
                .next(videoStep)
                .next(uploadStep)
                .build();
    }

    @Bean
    public Step crawlStep(JobRepository jobRepository) {

        return new StepBuilder("crawlStep", jobRepository)
                .<BlogCrawlDto, BlogCrawlDto>chunk(1, transactionManager)
                .reader(crawlItemReader())
                .processor(crawlProcessor)
                .writer(crawlItemWriter())
                .build();

    }

    @Bean
    public Step videoStep(JobRepository jobRepository) {
        return new StepBuilder("videoStep", jobRepository)
                .<Category, VideoDto>chunk(1, transactionManager)
                .reader(videoItemReader())
                .processor(videoProcessor)
                .writer(videoItemWriter())
                .build();
    }

    @Bean
    public Step uploadStep(JobRepository jobRepository) {
        return new StepBuilder("uploadStep", jobRepository)
                .<Video, Video>chunk(1, transactionManager)
                .reader(uploadItemReader())
                .processor(uploadItemProcessor())
                .writer(uploadItemWriter())
                .build();
    }

    @Bean
    public ItemWriter<Video> uploadItemWriter() {
        return new ItemWriter<Video>() {

            @Override
            public void write(Chunk<? extends Video> chunk) throws Exception {
                log.info("---------------- Start uploadItemWriter ------------------");
                for (Video video : chunk) {
                    log.info("---------------- start write videoId : " + video.getVideoId() + "------------------");
                    video.setUploadYn("Y");
                    video.setUploadDt(LocalDateTime.now());
                    videoRepository.saveAndFlush(video);
                    log.info("---------------- end write videoId : " + video.getVideoId() + "------------------");
                }

                log.info("---------------- End uploadItemWriter ------------------");
            }
        };
    }

    @Bean
    public ItemProcessor<Video, Video> uploadItemProcessor() {
        return new ItemProcessor<Video, Video>() {
            @Override
            public Video process(Video video) throws Exception {
                youtubeUtil.uploadVideo(video);
                return video;

            }

        };
    }

    @Bean
    public ItemReader<Video> uploadItemReader() {
        return new ItemReader<Video>() {
            @Override
            public Video read()
                    throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {

                Video video = videoRepository.findFirstByUploadYn("N");
                if (video == null) {
                    return null;
                }
                log.info("Video.getVideoId : " + video.getVideoId());
                return video;
            }
        };
    }

    @Bean
    public ItemWriter<BlogCrawlDto> crawlItemWriter() {
        return new ItemWriter<BlogCrawlDto>() {

            @Override
            public void write(Chunk<? extends BlogCrawlDto> chunk) throws Exception {
                log.info("---------------- Start crawlItemWriter ------------------");
                for (BlogCrawlDto blogCrawlDto : chunk) {
                    log.info(
                            "---------------- start write cateNm : " + blogCrawlDto.getCateNm() + "------------------");
                    blogService.regCateAndItem(blogCrawlDto);
                    log.info("---------------- end write cateNm : " + blogCrawlDto.getCateNm() + "------------------");
                }

                log.info("---------------- End crawlItemWriter ------------------");
            }
        };
    }

    @Bean
    public ItemReader<BlogCrawlDto> crawlItemReader() {
        return new ItemReader<BlogCrawlDto>() {
            @Override
            public BlogCrawlDto read()
                    throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {

                BlogCrawlDto blogCrawlDto = new BlogCrawlDto();
                Category category = categoryRepository.findFirstByCrawlYnOrderByCateIdAsc("N");
                if (category == null) {
                    return null;
                }

                blogCrawlDto.setCateId(category.getCateId());
                blogCrawlDto.setDanawaCateId(category.getDanawaCateId());
                blogCrawlDto.setMaxPrice(category.getMaxPrice());
                blogCrawlDto.setFilterStr(category.getFilterStr());
                blogCrawlDto.setCateNm(category.getCateNm());

                log.info("category : " + category.toString());

                return blogCrawlDto;
            }
        };
    }

    @Bean
    public ItemWriter<VideoDto> videoItemWriter() {
        return new ItemWriter<VideoDto>() {

            @Override
            public void write(Chunk<? extends VideoDto> chunk) throws Exception {
                log.info("---------------- Start videoItemWriter ------------------");
                for (VideoDto videoDto : chunk) {
                    Category category = categoryRepository.findById(videoDto.getCateId())
                            .orElseThrow(() -> new RuntimeException());
                    category.setVideoYn("Y");
                    categoryRepository.save(category);

                    AtFile videoAtFile = commonService.attachFileNotMove(videoDto.getVideoFilePath());
                    AtFile thumbnailAtFile = commonService.attachFileNotMove(videoDto.getThumbnailFilePath());

                    Video video = Video.builder()
                            .videoTitle(videoDto.getTitle())
                            .videoDesc(videoDto.getDescription())
                            .regDt(LocalDateTime.now())
                            .youtubeChannel(null)
                            .videoFileId(videoAtFile.getFileId())
                            .thumbnailFileId(thumbnailAtFile.getFileId())
                            .tags(videoDto.getTagList() != null && videoDto.getTagList().size() > 0
                                    ? String.join(",", videoDto.getTagList())
                                    : null)
                            .category(category)
                            .build();

                    videoRepository.saveAndFlush(video);

                    // blogService.regCateAndItem(blogCrawlDto);
                    // log.info("---------------- end write cateNm :
                    // "+blogCrawlDto.getCateNm()+"------------------");
                }

                log.info("---------------- End videoItemWriter ------------------");
            }
        };
    }

    @Bean
    public ItemReader<Category> videoItemReader() {
        return new ItemReader<Category>() {
            @Override
            public Category read()
                    throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
                log.info("---------------- Start videoItemReader ------------------");
                Pageable pageRequest = PageRequest.of(0, 1);
                List<Category> cateList = categoryRepository.findByVideoYnOrderByCateIdAsc("N", pageRequest);

                if (cateList.size() == 0) {
                    return null;
                }

                Category category = cateList.get(0);
                log.info("category : " + category.getCateNm());
                log.info("---------------- end videoItemReader ------------------");

                return category;
            }
        };
    }

    /*
     * @Bean
     * public Step uploadStep(JobRepository jobRepository, Tasklet uploadTasklet) {
     * return new StepBuilder("uploadStep", jobRepository)
     * .tasklet(uploadTasklet, transactionManager)
     * .build();
     * }
     * 
     * @Bean
     * public Tasklet uploadTasklet(){
     * 
     * }
     */

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

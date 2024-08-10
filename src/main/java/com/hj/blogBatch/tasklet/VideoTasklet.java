/* package com.hj.blogBatch.tasklet;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import com.hj.blogBatch.config.Selenium;
import com.hj.blogBatch.service.BlogService;

import lombok.Setter;

@Setter
public class VideoTasklet implements Tasklet, InitializingBean{

    private Selenium selenium;
    private BlogService blogService;
    
    private final String gptUrl = "https://chatgpt.com";
    private final String firstConv = "나는 영상제작자야.";
    private final String prefix = "다음 json을 읽고 존댓말의 구어체로 5줄로 요약해줘.";

    

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        blogService.getCate()
    

        WebDriver driver = selenium.getDriver(gptUrl);
        JavascriptExecutor js = (JavascriptExecutor) driver;

        js.executeScript("document.querySelector('#prompt-textarea').value = arguments[0]", )

        WebElement textareaEle = driver.findElement(By.cssSelector("#prompt-textarea"));
        

        
        return RepeatStatus.FINISHED;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        
        Assert.state(selenium != null, "selenium must set");
        Assert.state(blogService != null, "blogService must set");
        // Assert.state(directory != null, "Directory must be set");
        
    }



    
    
}
 */
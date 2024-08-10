package com.hj.blogBatch.utils;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.hj.blogBatch.config.Selenium;
import com.hj.blogBatch.entity.Item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class GptUtils {
    private final Selenium selenium;
    private final String gptUrl = "https://wrtn.ai/chat";
    private final String gptLoginUrl = "https://sso.wrtn.ai/";
    @Value("${com.blogBatch.gpt.id}")
    String gptId;
    @Value("${com.blogBatch.gpt.pw}")
    String gptPw;

    final String promptTextAreaSelector = "#chat-content > div > div > div > div > div > div > div > textarea";
    final String twicePromptTextAreaSelector = "#rich-textarea";
    final String answerSelector = "#chat-content > div > div > div > div > div > div > div > div > div > div > div > div > div > div > div > p";
    final String proChangeBtn = "#chat-content > div > div > div > div > div > div > div > div > div > div > div > div";
    final String modalSkipButton = "#web-modal > div > div > div > div > div > div";

    public List<String> getGptAnswer(String... questions) {

        List<String> answerList = new ArrayList<>();
        try {
            // AI item 상세정보 요약
            WebDriver driver = null;
            JavascriptExecutor js = null;

            driver = selenium.getDriver(gptLoginUrl);
            js = (JavascriptExecutor) driver;
            threadSleep(3000L);
            driver.findElement(By.cssSelector("#email")).sendKeys(gptId);
            driver.findElement(By.cssSelector("#password")).sendKeys(gptPw);
            driver.findElement(By.cssSelector("body > main > div:nth-child(1) > div > div > div > div > div > div > button")).click();
            // driver.get(gptUrl);
            threadSleep(5000L);
            js = (JavascriptExecutor) driver;
            if (driver.findElements(By.cssSelector(modalSkipButton)).size() > 0) {
                driver.findElement(By.cssSelector(modalSkipButton)).click();
                threadSleep(500L);
            }
            if(driver.findElements(By.cssSelector(proChangeBtn)).size() >0){
                driver.findElement(By.cssSelector(proChangeBtn)).click();
            }
            threadSleep(5000L);

            int idx = 0;
            for (String question : questions) {
                question = question.trim();
               
                /* if (idx % 3 == 0) {
                    if (driver != null) {
                        driver.close();
                    }
                    driver = selenium.getDriver(gptUrl);
                    js = (JavascriptExecutor) driver;
                    if (driver.findElements(By.cssSelector(modalSkipButton)).size() > 0) {
                        driver.findElement(By.cssSelector(modalSkipButton)).click();
                        threadSleep(500L);
                    }
                    driver.findElement(By.cssSelector(proChangeBtn)).click();
                } */
                String questionAreaSelector = idx == 0 ? promptTextAreaSelector : twicePromptTextAreaSelector;
                // driver.findElement(By.cssSelector(questionAreaSelector)).sendKeys(Keys.chord(Keys.SHIFT,
                // Keys.ENTER));
                driver.findElement(By.cssSelector(questionAreaSelector)).sendKeys(question);
                driver.findElement(By.cssSelector(questionAreaSelector)).sendKeys(Keys.ENTER);
                threadSleep(40000L);
                
                if (driver.findElements(By.cssSelector(answerSelector)).size() == 0) {
                    // answer element 없을 경우 한번더
                    if (idx % 3 == 0) {
                        idx++;
                        driver.findElement(By.cssSelector(twicePromptTextAreaSelector)).sendKeys(question);
                        driver.findElement(By.cssSelector(twicePromptTextAreaSelector)).sendKeys(Keys.ENTER);
                        threadSleep(30000L);
                    }else{
                        threadSleep(15000L);
                    }
                    
                }
                

                // String answerClassName = driver.findElement(By.cssSelector(answerSelector)).getAttribute("class");
                // List<WebElement> answerEles = driver.findElements(By.cssSelector("." + answerClassName));

                List<WebElement> answerSiblEles = driver.findElements(By.cssSelector(answerSelector));
                String answerClassName = "";
                for (WebElement answerEle : answerSiblEles) {
                    if(answerEle.getText().equals("답변")){
                        answerClassName = answerEle.getAttribute("class");
                        break;
                    }
                }
                if(answerClassName.isBlank()){
                    throw new RuntimeException("wrtn 답변 클래스 못찾았다 ㅅㅂ");
                }
                answerSiblEles = driver.findElements(By.cssSelector("." + answerClassName));
                WebElement answerSibl = answerSiblEles.get(answerSiblEles.size()-1); // 마지막 답변
                String answer = answerSibl.findElement(By.xpath("../../../div[3]")).getText();

                answerList.add(answer);

                log.info("-------------- gpt answer ----------------");
                log.info("question : " + question);
                log.info("answer : " + answer);
                log.info("---------------------------------------");
                idx++;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            selenium.closeDriver();
        }

        return answerList;

    }

    private void threadSleep(long milSec) {
        try {
            Thread.sleep(milSec);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}

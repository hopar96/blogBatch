package com.hj.blogBatch.utils;

import java.util.ArrayList;
import java.util.List;

import org.modelmapper.internal.util.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

import com.hj.blogBatch.config.Selenium;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
@Getter
@Setter
public class RealTimeKeyword {
    private final Selenium selenium;
    private List<String> realTimeKeyword = new ArrayList<>();

    @PostConstruct
    public void init() {
        try {
            WebDriver driver = selenium
                    .getDriver("https://trends.google.com/trends/trendingsearches/daily?geo=KR&hl=ko");
            // List<WebElement> elements = driver.findElements(By.cssSelector(".details-top .title span"));
            List<WebElement> elements = driver.findElements(By.cssSelector("#trend-table > div> table > tbody > tr> td:nth-child(2) > div:nth-child(1)"));

            elements.forEach(item -> {
                String text = item.getText();
                if (text.indexOf(" ") != -1) {
                    text = text.split(" ")[0];
                }
                realTimeKeyword.add(text);
            });

            log.debug("RealTimeKeyword init complete.");
        } catch (Exception e) {
            log.error("RealTimeKeyword init fail.", e);
        } finally {
            selenium.closeDriver();
        }
    }

}

package com.hj.blogBatch.config;

import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class Selenium {

    // private final ApplicationStartupEnvironment applicationStartupEnvironment;
    // private MessageSourceAccessor messageSourceAccessor;
    private WebDriver driver;
    @Value("${com.blogBatch.chrome.path}")
    private String chromePath;
    /* public Selenium(MessageSourceAccessor messageSourceAccessor) {
        this.messageSourceAccessor = messageSourceAccessor;
    } */
    @Value("${com.shortsBatch.shortsYn}")
    String shortsYn;


    public WebDriver getDriver(String url) throws InterruptedException {
        log.info("url : "+url);

        System.setProperty("webdriver.chrome.driver", chromePath);
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox"  );
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.114 Safari/537.36");
        // options.addArguments("Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko; compatible; Googlebot/2.1; +http://www.google.com/bot.html) Chrome/89.0.4389.114 Safari/537.36");
        if(shortsYn.equals("N")){
            options.addArguments("headless");
            options.addArguments("--window-size=1920,30000");
            options.addArguments("disable-gpu");
        }
        options.addArguments("disable-infobars");
        options.addArguments("--disable-extensions");
        options.addArguments("--renderer-process-limit=2");
        options.addArguments("--single-process");
        options.addArguments("--disable-blink-features=AutomationControlled");

        
/*         DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability(CapabilityType.UNHANDLED_PROMPT_BEHAVIOUR, UnexpectedAlertBehaviour.IGNORE);
        capabilities.setCapability(ChromeOptions.CAPABILITY, options);
 */
        options.setCapability(CapabilityType.UNHANDLED_PROMPT_BEHAVIOUR, UnexpectedAlertBehaviour.ACCEPT_AND_NOTIFY);

        this.driver = new ChromeDriver(options);
        this.driver.get(url);

        Thread.sleep(5000);

        return this.driver;
    }


    public void closeDriver(){
        this.driver.quit();
    }

}

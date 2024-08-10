package com.hj.blogBatch.processor;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hj.blogBatch.config.Selenium;
import com.hj.blogBatch.dto.BlogCrawlDto;
import com.hj.blogBatch.dto.BlogCrawlDto.ItemDto;
import com.hj.blogBatch.entity.AtFile;
import com.hj.blogBatch.entity.Category;
import com.hj.blogBatch.entity.Item;
import com.hj.blogBatch.service.CommonService;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class CrawlProcessor implements ItemProcessor<BlogCrawlDto, BlogCrawlDto>{

    private final Selenium selenium;
    private final CommonService commonService;
    @Value("${com.blogBatch.coup.id}")
    String coupangId;
    @Value("${com.blogBatch.coup.pw}")
    String coupangPw;

    @Override
    public BlogCrawlDto process(BlogCrawlDto blogCrawlDto) throws Exception {
        
        log.info("-----------BlogProcessor start-----------");

        // 카테 페이지에서 정보 가져오기
        getInCatePage(blogCrawlDto);
        
        Integer rank = 0;
        for (ItemDto itemDto : blogCrawlDto.getItemDtoList()) {
            rank++;
            itemDto.setRank(rank);
            getInItemPage(itemDto);
        }
        // 쿠팡 파트너스 링크 받기
        makeCoupangSellLink(blogCrawlDto);

        log.info("-----------BlogProcessor end-----------");
        return blogCrawlDto;
    }



    /**
     * 상품 정보 가져오기
     */
    private void getInItemPage(ItemDto itemDto) {
        log.info("--------- start getInItemPage danawaItemId = "+itemDto.getDanawaItemId()+" ----------");
        try {
            
            // selenium call
            WebDriver driver = selenium.getDriver("https://prod.danawa.com/info/?pcode="+ itemDto.getDanawaItemId());
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // 상품명
            WebElement titleElement = driver.findElement(By.cssSelector("span.title"));
            itemDto.setItemNm(titleElement.getText());

            // 상품이미지
            WebElement imgElement = driver.findElement(By.cssSelector("#baseImage"));
            String imgSrc = imgElement.getAttribute("src");
            // 이미지 다운로드
            Long itemImgId = commonService.downloadImg(imgSrc);
            itemDto.setItemFileId(itemImgId);

            // 제품 상세 정보 가져오기        
            js.executeScript("window.scrollTo(0, document.querySelector('.detail_cont').offsetTop);");
            js.executeScript("document.querySelector('.detail_cont').style.paddingTop = '3px';");
            Thread.sleep(1000);
            WebElement tblSpecEle = driver.findElement(By.cssSelector(".prod_spec > table.spec_tbl"));
            byte[] screenshot = tblSpecEle.getScreenshotAs(OutputType.BYTES);
            AtFile screenshotFile = commonService.attachFile("png", screenshot, null);
            itemDto.setDetailFileId(screenshotFile.getFileId());
            
            Map<String, Object> detailMap = new LinkedHashMap<>();
            List<WebElement> trElements = tblSpecEle.findElements(By.cssSelector("tr"));
            Map<String, Object> targetMap = null;
            for (WebElement trEle : trElements) {
                List<WebElement> thEles = trEle.findElements(By.cssSelector("th"));
                List<WebElement> tdEles = trEle.findElements(By.cssSelector("td"));
                // 일반
                if(tdEles.size() > 0){
                    if(targetMap == null){
                        for (int i = 0; i < thEles.size(); i++) {
                            if(StringUtils.isNotBlank(thEles.get(i).getText())){
                                detailMap.put(thEles.get(i).getText(), tdEles.get(i).getText());
                            }
                        }
                    }else{
                        for (int i = 0; i < thEles.size(); i++) {
                            if(StringUtils.isNotBlank(thEles.get(i).getText())){
                                targetMap.put(thEles.get(i).getText(), tdEles.get(i).getText());
                            }
                        }
                    }
                }else{ // 범주
                    String mapName = trEle.findElement(By.cssSelector("th")).getText();
                    if(mapName.equals("KC인증")){
                        break;
                    }
                    targetMap = new LinkedHashMap<>();
                    detailMap.put(mapName, targetMap);
                }                
            }

            ObjectMapper objectMapper = new ObjectMapper();
            String detailJson = objectMapper.writeValueAsString(detailMap);
            itemDto.setDetailJson(detailJson);

            // marketUrl 구하기
            WebElement coupangMarketEle = null;
            String moreBtnJs = null;
            String aTagSelector = null;
            boolean openMargetFlg = true;
            // 오픈 마켓일 경우
            if(driver.findElements(By.cssSelector("#OpenMarketMallListDiv")).size() > 0){
                openMargetFlg = true;
                moreBtnJs = "document.querySelector('#OpenMarketViewMoreDiv > a')?.click();";
            }else{
                openMargetFlg = false;
                moreBtnJs = "document.querySelectorAll('a.btn_open').forEach(item => { item.click(); });";
            }
            for (int i = 0; i < 3; i++) {
                // 쿠팡 로고로 확인
                List<WebElement> coupangMarketElements = driver.findElements( By.cssSelector((openMargetFlg
                ? "#OpenMarketMallListDiv"
                : "") + " .d_mall > a > img[src=\"//img.danawa.com/cmpny_info/images/TP40F_logo.gif\"]"));
                
                if(coupangMarketElements.size() == 0){ // 없으면 더보기 클릭
                    js.executeScript(moreBtnJs);
                    Thread.sleep(2000);
                }else {
                    coupangMarketEle = coupangMarketElements.get(0);
                    break;
                }
            }
            

            // 있을 경우
            String tmpLinkDanawaToMarketUrl = "";
            String price = "0";
            if(coupangMarketEle != null){
                itemDto.setCoupangYn("Y");
                tmpLinkDanawaToMarketUrl = coupangMarketEle.findElement(By.xpath("..")).getAttribute("href");
                price = coupangMarketEle.findElement(By.xpath("ancestor::*[3]")).findElement(By.cssSelector(".price em")).getText();
            }else{
                itemDto.setCoupangYn("N");
                List<WebElement> marketDiv = driver.findElements(By.cssSelector((openMargetFlg?"#OpenMarketMallListDiv":"") + " .d_mall > a"));

                for (int i = 0; i < marketDiv.size(); i++) {
                    if (marketDiv.get(i).findElements(By.cssSelector("img")).size() == 0 || !marketDiv.get(i)
                            .findElement(By.cssSelector("img")).getAttribute("src").contains("EE128_logo.gif")) {
                        tmpLinkDanawaToMarketUrl = marketDiv.get(i).getAttribute("href");
                        price = driver.findElements(By.cssSelector((openMargetFlg?"#OpenMarketMallListDiv":"")+ " .diff_item")).get(0).findElement(By.cssSelector(".price em")).getText();
                        break;
                    }
                }
                
            }
            itemDto.setPrice(Integer.parseInt(price.replaceAll(",", "")));
            if(tmpLinkDanawaToMarketUrl.isBlank()) {
                itemDto.setItemSellUrl("링크 정보가 없습니다.");
            } else {
                driver.get(tmpLinkDanawaToMarketUrl);
                Thread.sleep(5000);
                itemDto.setItemSellUrl(driver.getCurrentUrl());
            }
            

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            selenium.closeDriver();
        }
        log.info("--------- end getInItemPage danawaItemId = "+itemDto.getDanawaItemId()+" ----------");

        
    }

    private void makeCoupangSellLink(BlogCrawlDto blogCrawlDto) {
        // todo API는 추후에
        try {
            String loginUrl = "https://login.coupang.com/login/login.pang?rtnUrl=https%3A%2F%2Fpartners.coupang.com%2Fapi%2Fv1%2Fpostlogin";

            WebDriver driver = selenium.getDriver(loginUrl);
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // 로그인버튼클릭
            driver.findElement(By.cssSelector("input[type='email']")).sendKeys(coupangId);
            driver.findElement(By.cssSelector("input[type='password']")).sendKeys(coupangPw);
            driver.findElement(By.cssSelector(".login__button")).click();
            Thread.sleep(2000L);

            driver.get("https://partners.coupang.com/#affiliate/ws/link-to-any-page");

            if(driver.findElements(By.cssSelector("input[type='password']")).size() > 0){
                driver.findElement(By.cssSelector("input[type='password']")).sendKeys(coupangPw);
                driver.findElement(By.cssSelector(".ant-modal-content button[type='submit']")).click();
                Thread.sleep(2000L);
            }
            Thread.sleep(1000L);
            for (ItemDto itemDto : blogCrawlDto.getItemDtoList()) {
                if(itemDto.getCoupangYn().equals("Y")){
                    js.executeScript("document.querySelector('#url').value = ''");
                    Thread.sleep(500L);
                    driver.findElement(By.cssSelector("#url")).sendKeys(itemDto.getItemSellUrl());
                    driver.findElement(By.cssSelector(
                            "div.workspace-container > div > div > div > div > div > div > div > div > div > div > div > div > div:nth-child(1) > div > form > div > div > div > span > span > span > button"))
                            .click();

                    Thread.sleep(2000L);
                    String coupangUrl = driver.findElement(By.cssSelector("div.tracking-url > div")).getText();
                    itemDto.setItemCoupangUrl(coupangUrl);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally{
            selenium.closeDriver();
        }

    }


    /**
     * 카테 페이지에서 가져오기
     */
    private void getInCatePage(BlogCrawlDto blogCrawlDto) {
        try {
            String param = setParam(blogCrawlDto);

            WebDriver driver = selenium.getDriver("https://prod.danawa.com/list/?" + param);
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // 카테고리명 가져오기
            if(StringUtils.isBlank(blogCrawlDto.getCateNm())){
                WebElement titleElement = driver.findElement(By.cssSelector("title"));
                String cateNm = titleElement.getAttribute("text").split(":")[0].trim();
                if(cateNm.equals("전체보기")){
                    String description = driver.findElement(By.cssSelector("meta[name='Description']")).getAttribute("content");
                    description = description.split(">전체보기")[0];
                    cateNm = description.substring(description.lastIndexOf(">")+1);
                }
                
                blogCrawlDto.setCateNm(cateNm);    
            }

            // 가격 리미트가 있는경우 세팅
            if (blogCrawlDto.getMaxPrice() != null && blogCrawlDto.getMaxPrice() != 0) {
                // WebElement priceEle =
                // driver.findElement(By.cssSelector("input[name='priceRangeMaxPrice']"));
                WebElement searchBtnElement = driver.findElement(By.cssSelector("#priceRangeSearchButtonSimple"));

                js.executeScript("document.querySelector(\"input[name='priceRangeMaxPrice']\").value = "
                        + blogCrawlDto.getMaxPrice().toString());
                searchBtnElement.click();
                Thread.sleep(2000);
                
            }
            // 필터 적용
            if (StringUtils.isNotBlank(blogCrawlDto.getFilterStr())) {

                String[] filters = blogCrawlDto.getFilterStr().split(",");
                for (String filter : filters) {
                    WebElement searchBtnElement = driver.findElement(
                            By.cssSelector(
                                    "label[title='${title}'] input[type='checkbox']".replace("${title}", filter)));

                    if (searchBtnElement != null) {
                        searchBtnElement.click();
                        Thread.sleep(500);
                    }
                }
            }

            // 상품 리스트 가져오기
            WebElement danawaItemIdEle = driver.findElement(By.cssSelector("#productCodeListForContent"));
            String[] danawaItemIdArr = danawaItemIdEle.getAttribute("value").split(",");
            int rankLimit = 7; // 순위 제한
            rankLimit = danawaItemIdArr.length > rankLimit ? rankLimit : danawaItemIdArr.length;
            List<ItemDto> itemDtoList = new ArrayList<>();
            for (int i = 0; i < rankLimit; i++) {
                ItemDto itemDto = new ItemDto();
                itemDto.setDanawaItemId(danawaItemIdArr[i]);
                itemDtoList.add(itemDto);
            }

            blogCrawlDto.setItemDtoList(itemDtoList);

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            selenium.closeDriver();
        }

    }


    public String setParam(BlogCrawlDto blogCrawlDto){
        String param = "";

        if(!StringUtils.isEmpty(blogCrawlDto.getDanawaCateId())){
            param += "cate="+blogCrawlDto.getDanawaCateId();
        }

        return param;
    }
}
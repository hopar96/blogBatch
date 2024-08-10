package com.hj.blogBatch.processor;


import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.hj.blogBatch.dto.ShortsDto;
import com.hj.blogBatch.utils.RealTimeKeyword;
import com.hj.blogBatch.utils.VideoUtils;
import com.hj.blogBatch.utils.YoutubeUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class ShortsProcessor implements ItemProcessor<ShortsDto, ShortsDto> {

    private final VideoUtils videoUtils;
    private final YoutubeUtil youtubeUtil;
    private final RealTimeKeyword realTimeKeyword;

    @Override
    public ShortsDto process(ShortsDto shortsDto) throws Exception {

        log.info("-----------ShortsProcessor start-----------");
        // shorts video 만들기
        videoUtils.makeShortsVideo(shortsDto);

        // 이동 short upload로
      /*   String keyword = "";
        if (realTimeKeyword.getRealTimeKeyword() != null && realTimeKeyword.getRealTimeKeyword().size() > 0) {
            keyword = realTimeKeyword.getRealTimeKeyword().get(0);
            if (realTimeKeyword.getRealTimeKeyword().size() > 1) {
                realTimeKeyword.getRealTimeKeyword().remove(0);
            }
        }
        String coupangLink = "https://link.coupang.com/a/bJGqOP";

        String description = """
                %s %s
                항상 재밌는 썰이 듣고 보고 싶다면 구독 눌러주세요!
                비즈니스 문의 : skaus458@gmail.com
                * 구독과 좋아요는 영상 제작에 큰 힘이 됩니다.

                * 이 포스팅은 쿠팡 파트너스 활동의 일환으로, 이에 따른 일정액의 수수료를 제공받습니다.
                * 와우 회원이라면 매일 오전 7시 골드박스!
                * %s
                """.formatted(shortsDto.getTitle(),
                StringUtils.isBlank(shortsDto.getNewsPickLink()) ? "" : shortsDto.getNewsPickLink(), coupangLink)
                .trim();
        shortsDto.setDescription(description);

        shortsDto.setUploadTitle("""
                %s #shorts #썰 #이야기 #웃긴썰 #웃긴영상 #유머 #이슈
                """.formatted(shortsDto.getTitle()).trim());

        for (String titleSplit : shortsDto.getTitle().split(" ")) {
            shortsDto.getTagList().add(titleSplit);
        }
        shortsDto.getTagList().add(keyword);

        youtubeUtil.uploadShorts(shortsDto); */

        log.info("-----------ShortsProcessor end-----------");
        return shortsDto;
    }

    

}
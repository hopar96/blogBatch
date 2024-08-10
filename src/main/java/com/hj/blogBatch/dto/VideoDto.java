package com.hj.blogBatch.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Data
public class VideoDto {

    private String title;
    private String description;
    private Long cateId;
    private String content;
    private String cateNm;

    private LocalDateTime regDt;

    private String danawaCateId;
    private Integer maxPrice;
    private String filterStr;

    private String videoFilePath;
    private String thumbnailFilePath;
    private String channelTitle;
    private String youtubeVideoId;



    private List<VideoItemDto> itemDtoList = new ArrayList<>();
    private List<String> tagList = new ArrayList<>();


    @Data
    public static class VideoItemDto {
        private String itemNm; // varchar(50) NOT NULL COMMENT '상품명'
        private Long itemFileId; //bigint DEFAULT NULL COMMENT '상품 이미지 시퀀스';
        private String coupangYn; // char(1) NOT NULL DEFAULT 'N' COMMENT '쿠팡 판매 여부'
        private String itemSellUrl; // varchar(200) DEFAULT NULL COMMENT '아이템 판매 URL'
        private String itemCoupangUrl; // varchar(200) DEFAULT NULL COMMENT '아이템 쿠팡파트너스 URL'
        private String detailJson; // text COMMENT '상세정보 JSON'
        private Long detailFileId; // bigint DEFAULT NULL COMMENT '상세정보 이미지 시퀀스'
        private Long cateId; // bigint NOT NULL COMMENT '카테고리 시퀀스'
        private Integer rank; // bigint NOT NULL COMMENT '카테고리 시퀀스'
        
    }
    
}

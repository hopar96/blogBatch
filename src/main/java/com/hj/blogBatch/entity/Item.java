package com.hj.blogBatch.entity;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
@Entity
@Table(name = "item")
@Component
@Scope(value = "prototype")
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id", nullable = false)
    private Long itemId; // bigint NOT NULL AUTO_INCREMENT COMMENT '상품 시퀀스'
    @Column(name = "danawa_item_id", length = 30)
    private String danawaItemId; // varchar(30) DEFAULT NULL COMMENT '다나와 상품 번호'
    @Column(name = "item_nm", length = 50, nullable = false)
    private String itemNm; // varchar(50) NOT NULL COMMENT '상품명'
    @Column(name = "item_file_id")
    private Long itemFileId; //bigint DEFAULT NULL COMMENT '상품 이미지 시퀀스';
    @Column(name = "coupang_yn", length = 1, nullable = false)
    private String coupangYn; // char(1) NOT NULL DEFAULT 'N' COMMENT '쿠팡 판매 여부'
    @Column(name = "item_sell_url", length = 200)
    private String itemSellUrl; // varchar(200) DEFAULT NULL COMMENT '아이템 판매 URL'
    @Column(name = "item_coupang_url", length = 200)
    private String itemCoupangUrl; // varchar(200) DEFAULT NULL COMMENT '아이템 쿠팡파트너스 URL'
    @Column(name = "detail_json")
    private String detailJson; // text COMMENT '상세정보 JSON'
    @Column(name = "detail_file_id")
    private Long detailFileId; // bigint DEFAULT NULL COMMENT '상세정보 이미지 시퀀스'
    @Column(name = "item_rank", nullable = false)
    private Integer itemRank; // int NOT NULL default 0 COMMENT '상품 순위'
    @Column(name = "price", nullable = false)
    private Integer price; // int NOT NULL default 0 COMMENT '가격'
    @Column(name = "reg_dt")
    private LocalDateTime regDt; // datetime default NULL COMMENT '등록일'
    @Column(name = "cate_id", nullable = false, insertable = false, updatable = false)
    private Long cateId; // bigint NOT NULL COMMENT '카테고리 시퀀스'

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cate_id")
    private Category category;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_file_id", insertable = false, updatable = false)
    private AtFile itemFile;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "detail_file_id", insertable = false, updatable = false)
    private AtFile detailFile;



    // 비디오 제작 시 필요
    @Transient
    private String detailSummary;
    
    @Transient
    private List<Map<String, Object>> scriptList = new ArrayList<>();


    @PrePersist
    public void prePersist() {
        if (this.coupangYn == null) {
            setCoupangYn("N");
        }
        if(this.regDt == null){
            setRegDt(LocalDateTime.now());
        }
    }

    public Item() { }

    @Builder
    public Item(String danawaItemId, String itemNm, Long itemFileId, String coupangYn, String itemSellUrl,
            String itemCoupangUrl, String detailJson, Long detailFileId, Integer itemRank, LocalDateTime regDt,
            Category category, Integer price) {
        this.danawaItemId = danawaItemId;
        this.itemNm = itemNm;
        this.itemFileId = itemFileId;
        this.coupangYn = coupangYn;
        this.itemSellUrl = itemSellUrl;
        this.itemCoupangUrl = itemCoupangUrl;
        this.detailJson = detailJson;
        this.detailFileId = detailFileId;
        this.itemRank = itemRank;
        this.regDt = regDt;
        this.category = category;
        this.price = price;
    }


}
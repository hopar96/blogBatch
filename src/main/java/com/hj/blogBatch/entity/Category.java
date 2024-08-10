package com.hj.blogBatch.entity;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@Entity
@Table(name = "category")
@Component
@Scope(value = "prototype")
/* @NamedEntityGraph(name = "Category.findByVideoYnOrderByCateIdAsc", attributeNodes = {
            @NamedAttributeNode(value = "itemList", subgraph = "item-files")
        }, subgraphs = {
            @NamedSubgraph(name = "item-files", attributeNodes = {
                @NamedAttributeNode(value = "itemFile"),
                @NamedAttributeNode(value = "detailFile")
            })
        }) */
public class Category{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cate_id", nullable = false)
    private Long cateId; // bigint NOT NULL COMMENT '카테고리 시퀀스'
    @Column(name = "cate_nm", length = 100, nullable = false)
    private String cateNm; // varchar(100) NOT NULL COMMENT '카테고리 명'
    @Column(name = "crawl_yn", length = 1, nullable = false)
    private String crawlYn; // char(1) NOT NULL DEFAULT 'N' COMMENT '크롤링여부'
    @Column(name = "err_msg", length = 200)
    private String errMsg; // varchar(200) DEFAULT NULL COMMENT '에러메세지'
    @Column(name = "danawa_cate_id", length = 30)
    private String danawaCateId; // varchar(30) DEFAULT NULL COMMENT '다나와 카테고리 번호'
    @Column(name="filter_str", length = 100)
    private String filterStr; // VARCHAR(100)  NULL     COMMENT '카테고리에서 상세검색 필터 ( ,(콤마)로 구분)';
    @Column(name = "crawl_dt")
    private LocalDateTime crawlDt; // datetime default NULL COMMENT '크롤링 일자'
    @Column(name = "max_price")
    private Integer maxPrice;
    @Column(name = "video_yn", length = 1, nullable = false)
    private String videoYn; // char(1) NOT NULL DEFAULT 'N' COMMENT '비디오 생성 여부'
    @Column(name = "blog_yn", length = 1, nullable = false)
    private String blogYn; // char(1) NOT NULL DEFAULT 'N' COMMENT '블로그 생성 여부'


    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    List<Item> itemList = new ArrayList<Item>();
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    List<Video> video = new ArrayList<Video>();


    @PrePersist
    public void prePersist() {
        if (this.crawlYn == null) {
            setCrawlYn("N");
        }
        if (this.videoYn == null) {
            setVideoYn("N");
        }
        if (this.blogYn == null) {
            setBlogYn("N");
        }
        if(this.crawlDt == null){
            setCrawlDt(LocalDateTime.now());
        }
    }
}
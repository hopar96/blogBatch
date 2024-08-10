package com.hj.blogBatch.entity;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@ToString
@Entity
@Table(name = "blog")
@Component
@Scope(value = "prototype")
public class Blog implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "blog_id", nullable = false)
    private Long blogId; // bigint NOT NULL AUTO_INCREMENT COMMENT '블로그 시퀀스'
    @Column(name = "foreign_key_id")
    private Long foreignKeyId; // bigint DEFAULT NULL COMMENT '외래키 아이디'
    @Column(name = "foreign_key_target", length = 50)
    private String foreignKeyTarget; // varchar(50) DEFAULT NULL COMMENT '외래키 테이블명'
    @Column(name = "title", length = 200, nullable = false)
    private String title; // varchar(200) NOT NULL COMMENT '제목'
    @Column(name = "description", length = 200)
    private String description; // varchar(200) DEFAULT NULL COMMENT '설명'
    @Column(name = "main_file_id")
    private Long mainFileId; // bigint DEFAULT NULL COMMENT '메인 이미지 file id'
    @Column(name = "keywords")
    private String keywords; // text COMMENT '키워드 리스트
    @Column(name = "content", nullable = false)
    private String content; // text NOT NULL COMMENT '글'
    @Column(name = "views", nullable = false)
    private Integer views; // int NOT NULL DEFAULT '0' COMMENT '뷰 수'
    @Column(name = "reg_dt")
    private LocalDateTime regDt; // datetime DEFAULT NULL COMMENT '등록일'
    @Column(name = "upd_dt")
    private LocalDateTime updDt; // datetime DEFAULT NULL COMMENT '수정일'
    @Column(name = "author_nm", length = 50, nullable = false)
    private String authorNm; // varchar(50) NOT NULL COMMENT '작성자 이름'
    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn; // char(1) NOT NULL DEFAULT 'Y' COMMENT '사용여부'
    @Column(name = "blog_cate_id", nullable = false)
    private Long blogCateId; // bigint NOT NULL COMMENT '블로그 카테고리 시퀀스'

    @PrePersist
    public void prePersist() {
        if (this.views == null) {
            setViews(0);
        }
        if (this.useYn == null) {
            setUseYn("Y");
        }
        if (this.regDt == null) {
            setRegDt(LocalDateTime.now());
        }
    }

    @Builder
    public Blog(Long foreignKeyId, String foreignKeyTarget, String title, String description, Long mainFileId,
            String keywords, String content, Integer views, LocalDateTime regDt, LocalDateTime updDt, String authorNm,
            String useYn, Long blogCateId) {
        this.foreignKeyId = foreignKeyId;
        this.foreignKeyTarget = foreignKeyTarget;
        this.title = title;
        this.description = description;
        this.mainFileId = mainFileId;
        this.keywords = keywords;
        this.content = content;
        this.views = views;
        this.regDt = regDt;
        this.updDt = updDt;
        this.authorNm = authorNm;
        this.useYn = useYn;
        this.blogCateId = blogCateId;
    }

    public Blog() {
    }

}
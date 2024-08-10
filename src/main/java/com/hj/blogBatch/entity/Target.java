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
@Table(name = "target")
@Component
@Scope(value = "prototype")
public class Target implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "target_id", nullable = false)
    private Long targetId; // bigint NOT NULL AUTO_INCREMENT COMMENT '타겟 시퀀스'
    @Column(name = "reg_dt", nullable = false)
    private LocalDateTime regDt; // datetime NOT NULL COMMENT '등록일'
    @Column(name = "title", length = 200, nullable = false)
    private String title; // varchar(200) NOT NULL COMMENT '제목'
    @Column(name = "content", length = 200, nullable = false)
  private String content; // text NULL COMMENT '내용'
    @Column(name = "file_id")
    private Long fileId; // bigint DEFAULT NULL COMMENT '파일 시퀀스'
    @Column(name = "make_yn", length = 1, nullable = false)
    private String makeYn; // char(1) NOT NULL DEFAULT 'N' COMMENT '만듦 여부'
    @Column(name = "blog_cate_id")
    private Long blogCateId; // bigint  NULL COMMENT '블로그 카테고리 시퀀스'
    @PrePersist
    public void prePersist() {
        if (this.makeYn == null) {
            setMakeYn("N");
        }
        if (this.regDt == null) {
            setRegDt(LocalDateTime.now());
        }
    }

    public Target() {
    }

    @Builder
    public Target(LocalDateTime regDt, String title, Long fileId, String makeYn, String content, Long blogCateId) {
        this.regDt = regDt;
        this.title = title;
        this.fileId = fileId;
        this.makeYn = makeYn;
        this.content = content;
        this.blogCateId = blogCateId;
    }

}
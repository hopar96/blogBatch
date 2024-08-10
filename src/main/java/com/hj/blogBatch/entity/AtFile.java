package com.hj.blogBatch.entity;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
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
@Table(name = "at_file")
@Component
@Scope(value = "prototype")
public class AtFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id", nullable = false)
    private Long fileId; // bigint NOT NULL AUTO_INCREMENT COMMENT '파일 시퀀스'
    @Column(name = "org_file_nm", length = 100)
    private String orgFileNm; // varchar(100) DEFAULT NULL COMMENT '기존 파일명'
    @Column(name = "file_nm", length = 30)
    private String fileNm; // varchar(30) DEFAULT NULL COMMENT '저장된 파일명'
    @Column(name = "reg_dt")
    private LocalDateTime regDt; // datetime DEFAULT NULL COMMENT '등록일'
    @Column(name = "upload_yn")
    private String uploadYn;// upload_yn CHAR(1) NOT NULL DEFAULT 'N' COMMENT 's3 업로드 여부';

    @PrePersist
    public void prePersist() {
        if(this.regDt == null){
            setRegDt(LocalDateTime.now());
        }
        if(this.uploadYn == null){
            setUploadYn("N");
        }
    }
}
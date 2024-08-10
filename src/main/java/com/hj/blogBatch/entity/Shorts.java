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
@Table(name = "shorts")
@Component
@Scope(value = "prototype")
public class Shorts implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shorts_id", nullable = false)
    private Long shortsId; // bigint NOT NULL AUTO_INCREMENT COMMENT '쇼츠 시퀀스'
    @Column(name = "video_file_id")
    private Long videoFileId; // bigint DEFAULT NULL COMMENT 'video 파일 id'
    @Column(name = "video_title", length = 200)
    private String videoTitle; // varchar(200) DEFAULT NULL COMMENT '영상제목'
    @Column(name = "video_desc")
    private String videoDesc; // text COMMENT '영상설명'
    @Column(name = "reg_dt")
    private LocalDateTime regDt; // datetime DEFAULT NULL COMMENT '등록일'
    @Column(name = "youtube_channel", length = 100)
    private String youtubeChannel; // varchar(100) DEFAULT NULL COMMENT '유튜브 채널명'
    @Column(name = "upload_yn", length = 1, nullable = false)
    private String uploadYn; // char(1) NOT NULL DEFAULT 'N' COMMENT '영상 업로드 여부'
    @Column(name = "sm_video_file_id")
    private Long smVideoFileId; // bigint DEFAULT NULL COMMENT '쇼츠 뮤직 video 파일 id'
    @Column(name = "sm_upload_yn", length = 1, nullable = false)
    private String smUploadYn; // char(1) NOT NULL DEFAULT 'N' COMMENT '쇼츠뮤직 업로드 여부'
    @Column(name = "foreign_key_id")
    private Long foreignKeyId; // bigint DEFAULT NULL COMMENT '외래키 아이디'
    @Column(name = "foreign_key_target", length = 50)
    private String foreignKeyTarget; // varchar(50) DEFAULT NULL COMMENT '외래키 테이블명'
    @Column(name = "youtube_video_id", length = 50)
    private String youtubeVideoId; // varchar(50) DEFAULT NULL COMMENT 'youtube 비디오 id'

    @PrePersist
    public void prePersist() {
        if (this.uploadYn == null) {
            setUploadYn("N");
        }
        if (this.smUploadYn == null) {
            setSmUploadYn("N");
        }
    }

    public Shorts() {
    }

    @Builder
    public Shorts(Long videoFileId, String videoTitle, String videoDesc, LocalDateTime regDt, String youtubeChannel,
            String uploadYn) {
        this.videoFileId = videoFileId;
        this.videoTitle = videoTitle;
        this.videoDesc = videoDesc;
        this.regDt = regDt;
        this.youtubeChannel = youtubeChannel;
        this.uploadYn = uploadYn;
    }

}

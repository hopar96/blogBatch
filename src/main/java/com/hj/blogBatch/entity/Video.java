package com.hj.blogBatch.entity;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@Entity
@Table(name = "video")
@Component
@Scope(value = "prototype")
public class Video implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "video_id", nullable = false)
    private Long videoId; // bigint NOT NULL AUTO_INCREMENT COMMENT '비디오 시퀀스'
    @Column(name = "video_title", length = 200)
    private String videoTitle; // varchar(200) DEFAULT NULL COMMENT '영상제목'
    @Column(name = "video_desc")
    private String videoDesc; // text COMMENT '영상설명'
    @Column(name = "reg_dt")
    private LocalDateTime regDt; // datetime DEFAULT NULL COMMENT '등록일'
    @Column(name = "upload_dt")
    private LocalDateTime uploadDt; // datetime DEFAULT NULL COMMENT '업로드일자'
    @Column(name = "youtube_channel", length = 100)
    private String youtubeChannel; // varchar(100) DEFAULT NULL COMMENT '유튜브 채널명'
    @Column(name = "thumbnail_file_id")
    private Long thumbnailFileId; // bigint DEFAULT NULL COMMENT '썸네일 파일 id'
    @Column(name = "video_file_id")
    private Long videoFileId; // bigint DEFAULT NULL COMMENT 'video 파일 id'
    @Column(name = "tags", length = 300)
    private String tags;  // varchar(300) DEFAULT NULL COMMENT '태그 리스트 ,(콤마)로 구분'
    @Column(name = "upload_yn", length = 1)
    private String uploadYn; // char(1) DEFAULT 'N' COMMENT '업로드 여부'
    @Column(name = "youtube_video_id", length = 50)
    private String youtubeVideoId; // varchar(50) DEFAULT NULL COMMENT 'youtube 비디오 id'


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thumbnail_file_id", insertable = false, updatable = false)
    private AtFile thumbnailFile;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_file_id", insertable = false, updatable = false)
    private AtFile videoFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cate_id")
    private Category category;

    @PrePersist
    public void prePersist() {
        if(this.uploadYn == null){
            setUploadYn("N");
        }
    }

    public Video() {}

    @Builder
    public Video(String videoTitle, String videoDesc, LocalDateTime regDt, String youtubeChannel, Long thumbnailFileId,
            Long videoFileId, String tags, Category category) {
        this.videoTitle = videoTitle;
        this.videoDesc = videoDesc;
        this.regDt = regDt;
        this.youtubeChannel = youtubeChannel;
        this.thumbnailFileId = thumbnailFileId;
        this.videoFileId = videoFileId;
        this.tags = tags;
        this.category = category;
    }


    

    
}
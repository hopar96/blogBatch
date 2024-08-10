package com.hj.blogBatch.dto;

import java.util.ArrayList;
import java.util.List;

import com.hj.blogBatch.entity.Shorts;

import lombok.Data;

@Data
public class ShortsDto {

    private String targetFilePath;
    private String shortsFilePath;
    private String shortMusicFilePath;
    private String title;
    private String uploadTitle;
    private String description;
    private String newsPickLink;
    private String youtubeChannel;
    private String youtubeVideoId;
    private Long targetId;
    private List<String> tagList = new ArrayList<>();

    private Shorts shorts;
    
    
}

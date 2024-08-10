package com.hj.blogBatch.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hj.blogBatch.entity.Video;


public interface VideoRepository extends JpaRepository<Video, Long>{

    
    Video findFirstByUploadYn(String uploadYn);

}


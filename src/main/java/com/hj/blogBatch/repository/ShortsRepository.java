package com.hj.blogBatch.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.hj.blogBatch.entity.Shorts;
import java.util.*;
import java.util.List;

public interface ShortsRepository extends JpaRepository<Shorts, Long> {

    Shorts findFirstByUploadYnAndSmVideoFileIdIsNotNullOrderByShortsId(String smUploadYn);

    List<Shorts> findBySmUploadYnAndSmVideoFileIdIsNotNull(String uploadYn, Pageable pageable);

    List<Shorts> findByVideoTitleIn(List<String> videoTitles);

    Integer countByUploadYn(String uploadYn);

}

package com.hj.blogBatch.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hj.blogBatch.entity.Target;
import java.util.List;


public interface TargetRepository extends JpaRepository<Target, Long> {

  Target findFirstByMakeYnOrderByTargetId(String makeYn);
  Integer countByMakeYn(String makeYn);
  List<Target> findByTitleIn(List<String> titleList);
}

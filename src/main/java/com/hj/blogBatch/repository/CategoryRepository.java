package com.hj.blogBatch.repository;

import org.hibernate.annotations.BatchSize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;

import com.hj.blogBatch.entity.Category;
import java.util.List;


public interface CategoryRepository extends JpaRepository<Category, Long>{


    List<Category> findByCrawlYn(String crawlYn);

    Category findFirstByCrawlYnOrderByCateIdAsc(String crawlYn);

    
    // @EntityGraph(value = { "CategoryItemGraph" }, type = EntityGraphType.LOAD)
    @EntityGraph(attributePaths = {"itemList"}, type = EntityGraphType.LOAD)
    List<Category> findByVideoYnOrderByCateIdAsc(String videoYn, Pageable pageable);



    

}


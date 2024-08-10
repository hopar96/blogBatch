package com.hj.blogBatch.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hj.blogBatch.entity.Blog;

public interface BlogRepository extends JpaRepository<Blog, Long> {

}

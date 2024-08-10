package com.hj.blogBatch.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hj.blogBatch.entity.Item;

import java.util.List;


public interface ItemRepository extends JpaRepository<Item, Long>{



}


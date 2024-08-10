package com.hj.blogBatch.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hj.blogBatch.dto.BlogCrawlDto;
import com.hj.blogBatch.dto.BlogCrawlDto.ItemDto;
import com.hj.blogBatch.entity.Category;
import com.hj.blogBatch.entity.Item;
import com.hj.blogBatch.repository.CategoryRepository;
import com.hj.blogBatch.repository.ItemRepository;

import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BlogServiceImp implements BlogService {

    private final CategoryRepository categoryRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public void regCateAndItem(BlogCrawlDto blogCrawlDto) {

        Category category = categoryRepository.findById(blogCrawlDto.getCateId()).orElseThrow(() -> {
            throw new RuntimeException("there is no entity category");
        });

        category.setCateNm(blogCrawlDto.getCateNm());
        category.setCrawlDt(LocalDateTime.now());
        category.setCrawlYn("Y");
        categoryRepository.save(category);

        for (ItemDto itemDto : blogCrawlDto.getItemDtoList()) {

            Item item = Item.builder()
                    .danawaItemId(itemDto.getDanawaItemId())
                    .itemNm(itemDto.getItemNm())
                    .itemFileId(itemDto.getItemFileId())
                    .coupangYn(itemDto.getCoupangYn())
                    .itemSellUrl(itemDto.getItemSellUrl())
                    .itemCoupangUrl(itemDto.getItemCoupangUrl())
                    .detailJson(itemDto.getDetailJson())
                    .detailFileId(itemDto.getDetailFileId())
                    .itemRank(itemDto.getRank())
                    .regDt(LocalDateTime.now())
                    .category(category)
                    .price(itemDto.getPrice())
                    .build();

            itemRepository.save(item);
        }
        itemRepository.flush();


    }


}

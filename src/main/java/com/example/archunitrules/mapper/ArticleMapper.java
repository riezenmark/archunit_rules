package com.example.archunitrules.mapper;

import com.example.archunitrules.controller.request.CreateArticleRq;
import com.example.archunitrules.controller.response.ArticleResponse;
import com.example.archunitrules.entity.Article;
import com.example.archunitrules.enumeration.AgeRating;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ArticleMapper {
    public Article toEntity(CreateArticleRq request, AgeRating ageRating) {
        return Article.builder()
                .id(UUID.randomUUID())
                .title(request.title())
                .content(request.content())
                .ageRating(ageRating)
                .build();
    }

    public ArticleResponse toResponse(Article entity) {
        return ArticleResponse.builder()
                .id(entity.id())
                .title(entity.title())
                .content(entity.content())
                .build();
    }
}

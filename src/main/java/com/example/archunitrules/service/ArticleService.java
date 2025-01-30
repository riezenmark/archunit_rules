package com.example.archunitrules.service;

import com.example.archunitrules.controller.request.CreateArticleRq;
import com.example.archunitrules.controller.response.ArticleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ArticleService {
    UUID create(CreateArticleRq request);

    Page<ArticleResponse> getPage(Pageable pageable);

    ArticleResponse getOne(UUID id);
}

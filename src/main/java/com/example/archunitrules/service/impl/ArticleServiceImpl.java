package com.example.archunitrules.service.impl;

import com.example.archunitrules.controller.request.CreateArticleRq;
import com.example.archunitrules.controller.response.ArticleResponse;
import com.example.archunitrules.entity.Article;
import com.example.archunitrules.enumeration.AgeRating;
import com.example.archunitrules.exception.ArticleNotFoundException;
import com.example.archunitrules.mapper.ArticleMapper;
import com.example.archunitrules.repository.ArticleRepository;
import com.example.archunitrules.service.ArticleService;
import com.example.archunitrules.util.AgeRatingDeterminationUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {
    private final ArticleMapper articleMapper;
    private final ArticleRepository articleRepository;

    @Override
    @Transactional
    public UUID create(CreateArticleRq request) {
        AgeRating ageRating = AgeRatingDeterminationUtils.determineAgeRating(request.content());
        Article article = articleMapper.toEntity(request, ageRating);
        return articleRepository.save(article).id();
    }

    @Override
    public Page<ArticleResponse> getPage(Pageable pageable) {
        return articleRepository.findAll(pageable)
                .map(articleMapper::toResponse);
    }

    @Override
    public ArticleResponse getOne(UUID id) {
        return articleRepository.findById(id)
                .map(articleMapper::toResponse)
                .orElseThrow(() -> new ArticleNotFoundException(id));
    }
}

package com.example.archunitrules.controller;

import com.example.archunitrules.controller.request.CreateArticleRq;
import com.example.archunitrules.controller.response.ArticleResponse;
import com.example.archunitrules.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class ArticleController {
    public static final String PATH = "/v1/article";

    private final ArticleService articleService;

    @PostMapping("")
    public ResponseEntity<Void> create(@Valid @RequestBody CreateArticleRq request) {
        UUID createdArticleId = articleService.create(request);
        return ResponseEntity.created(URI.create(PATH + "/" + createdArticleId)).build();
    }

    @GetMapping("")
    public Page<ArticleResponse> getPage(@PageableDefault Pageable pageable) {
        return articleService.getPage(pageable);
    }

    @GetMapping("/{id}")
    public ArticleResponse getOne(@PathVariable UUID id) {
        return articleService.getOne(id);
    }
}

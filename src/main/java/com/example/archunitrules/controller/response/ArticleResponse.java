package com.example.archunitrules.controller.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record ArticleResponse(
        UUID id,
        String title,
        String content
) {
}

package com.example.archunitrules.controller.request;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record CreateArticleRq(
        @NotBlank
        String title,

        @NotBlank
        @Length(max = 500)
        String content
) {
}

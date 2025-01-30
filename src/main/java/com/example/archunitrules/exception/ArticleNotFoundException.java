package com.example.archunitrules.exception;

import com.example.archunitrules.common.exception.BaseParametrizedException;

public class ArticleNotFoundException extends BaseParametrizedException {
    public ArticleNotFoundException(Object parameter) {
        super("Article %s not found", parameter);
    }
}

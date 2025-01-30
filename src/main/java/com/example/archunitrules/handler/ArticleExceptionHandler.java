package com.example.archunitrules.handler;

import com.example.archunitrules.common.dto.ErrorDetails;
import com.example.archunitrules.exception.ArticleNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ArticleExceptionHandler {
    @ExceptionHandler(ArticleNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleArticleException(ArticleNotFoundException exception) {
        ErrorDetails errorDetails = new ErrorDetails(exception.getFormattedMessage());
        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
    }
}

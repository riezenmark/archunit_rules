package com.example.archunitrules.common.exception;

public class BaseParametrizedException extends RuntimeException {
    protected final transient Object parameter;

    protected BaseParametrizedException(String message, Object parameter) {
        super(message);
        this.parameter = parameter;
    }

    public String getFormattedMessage() {
        return this.getMessage().formatted(parameter);
    }
}

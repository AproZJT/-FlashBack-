package com.flashback.backend.exception;

public class BizException extends RuntimeException {
    public BizException(String message) {
        super(message);
    }
}

package com.GSU26SE22_SU26SE002.RealMateAI.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
/**
 * DuplicateResourceException — 409
 * Dùng khi user yêu thích 1 listing đã được yêu thích trước đó.
 */
@ResponseStatus(HttpStatus.CONFLICT)

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}

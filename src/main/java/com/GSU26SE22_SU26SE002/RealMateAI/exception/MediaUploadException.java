package com.GSU26SE22_SU26SE002.RealMateAI.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)

public class MediaUploadException extends RuntimeException {
    public MediaUploadException(String message) {
        super(message);
    }
    public MediaUploadException(String message, Throwable cause) {
        super(message, cause);
    }

}

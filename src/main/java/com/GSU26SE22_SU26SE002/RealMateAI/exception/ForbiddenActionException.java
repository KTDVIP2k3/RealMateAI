package com.GSU26SE22_SU26SE002.RealMateAI.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
/**
 * ForbiddenActionException — 403
 * Dùng khi Seller cố thao tác trên Listing không thuộc sở hữu của mình (IDOR).
 */
@ResponseStatus(HttpStatus.FORBIDDEN)

public class ForbiddenActionException extends RuntimeException {
    public ForbiddenActionException(String message) {
        super(message);
    }
}

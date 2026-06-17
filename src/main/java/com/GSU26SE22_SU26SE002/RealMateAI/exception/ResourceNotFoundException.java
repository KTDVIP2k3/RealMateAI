package com.GSU26SE22_SU26SE002.RealMateAI.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
/**
 * Dùng khi Listing/Property/FavoriteListing/Seller không tồn tại.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

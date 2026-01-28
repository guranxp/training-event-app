package com.guranxp.trainingeventapp.application.common.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateRequestException extends RuntimeException {
    
    public DuplicateRequestException(String message) {
        super(message);
    }
    
    public DuplicateRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}

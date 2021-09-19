package com.yhp.exception;

public class CacheNotExistException extends RuntimeException {
    public CacheNotExistException(String message) {
        super(message);
    }
}

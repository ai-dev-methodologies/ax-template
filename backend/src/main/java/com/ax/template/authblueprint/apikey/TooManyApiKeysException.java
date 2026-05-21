package com.ax.template.authblueprint.apikey;

/** Per-user quota exceeded — mapped to HTTP 400 with errorCode TOO_MANY_KEYS. */
public class TooManyApiKeysException extends RuntimeException {
    public TooManyApiKeysException(String detail) {
        super(detail);
    }
}

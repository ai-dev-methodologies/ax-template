package com.ax.template.authblueprint.reportexport;

/** Soft cap defense — mapped to HTTP 400 with errorCode TOO_MANY_ROWS. */
public class TooManyRowsException extends RuntimeException {
    public TooManyRowsException(String detail) {
        super(detail);
    }
}

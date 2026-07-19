package com.ax.template.authblueprint.consumerproof;

import java.util.Map;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// VIOLATING — controller_problemdetail_guard
// An @ExceptionHandler that returns a bare Map<String,String> instead of the
// RFC-9457 ProblemDetail wire shape. Typical AI shortcut.
@RestControllerAdvice
public class ErrorMappingAdvice {

    @ExceptionHandler(IllegalArgumentException.class)
    public Map<String, String> handleBadRequest(IllegalArgumentException ex) {
        return Map.of("error", ex.getMessage());
    }
}

package com.ax.template.authblueprint.filestorage;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Thrown when a file lookup fails for the caller — either because the row does
 * not exist OR because it belongs to a different user.
 * <p>
 * Trace: FILE-AUTHZ-002 — cross-user IDOR returns 404, not 403, to avoid
 * leaking the existence of another user's file.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class StoredFileNotFoundException extends RuntimeException {
    public StoredFileNotFoundException(UUID id) {
        super("Stored file not found: " + id);
    }
}

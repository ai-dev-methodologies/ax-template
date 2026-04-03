package com.ax.template.authblueprint.user;

import java.util.Optional;

public interface RefreshTokenSessionStore {
    Optional<RefreshTokenSession> findBySessionId(String sessionId);

    void save(RefreshTokenSession session);

    void revokeBySessionId(String sessionId);
}

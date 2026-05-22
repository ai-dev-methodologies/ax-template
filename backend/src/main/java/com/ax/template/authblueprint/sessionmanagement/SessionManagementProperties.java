package com.ax.template.authblueprint.sessionmanagement;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "session-management")
public class SessionManagementProperties {

    /** SESS-LIFECYCLE-005 — soft cap; new register over the limit auto-revokes the oldest ACTIVE session. */
    private int maxActiveSessionsPerUser = 25;

    public int getMaxActiveSessionsPerUser() { return maxActiveSessionsPerUser; }
    public void setMaxActiveSessionsPerUser(int v) { this.maxActiveSessionsPerUser = v; }
}

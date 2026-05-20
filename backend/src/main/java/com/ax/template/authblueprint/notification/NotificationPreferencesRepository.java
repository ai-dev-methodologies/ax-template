package com.ax.template.authblueprint.notification;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferencesRepository
        extends JpaRepository<NotificationPreferences, String> {
}

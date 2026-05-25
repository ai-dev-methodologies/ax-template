package com.ax.template.authblueprint.emailoutbox;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, String> {
}

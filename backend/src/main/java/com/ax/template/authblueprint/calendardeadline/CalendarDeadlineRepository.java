package com.ax.template.authblueprint.calendardeadline;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** NO delete method is declared — a computed deadline is immutable and kept, never removed. */
public interface CalendarDeadlineRepository extends JpaRepository<CalendarDeadline, UUID> {
}

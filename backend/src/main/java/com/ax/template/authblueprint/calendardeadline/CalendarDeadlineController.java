package com.ax.template.authblueprint.calendardeadline;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * business-day-deadline-arithmetic-l0 thin controller. The acting principal is ALWAYS the
 * authenticated caller (caller-authentication-only-no-userid-param). Delegates to
 * {@link CalendarDeadlineService}; carries no business logic.
 */
@RestController
public class CalendarDeadlineController {

    public record CreateCalendarReq(@NotBlank @Size(max = 100) String calendarName,
                                    @NotNull Set<LocalDate> holidays) {}
    public record EditCalendarReq(@NotNull Set<LocalDate> holidays) {}
    public record ComputeReq(@NotBlank @Size(max = 200) String obligationRef,
                             @NotNull LocalDate startDate,
                             @NotNull Integer periodCount,
                             @NotNull DeadlineMode mode,
                             @NotNull UUID holidayCalendarId,
                             @NotNull RollConvention rollConvention) {}

    public record CalendarDto(UUID id, String calendarName, List<LocalDate> holidays, long version) {
        static CalendarDto of(HolidayCalendar c) {
            return new CalendarDto(c.getId(), c.getCalendarName(),
                c.getHolidays().stream().sorted().toList(), c.getPublishedVersion());
        }
    }
    public record DeadlineDto(UUID id, String obligationRef, LocalDate startDate, int periodCount,
                              DeadlineMode mode, UUID holidayCalendarId, long holidayCalendarVersion,
                              LocalDate rawDeadline, RollConvention rollConvention,
                              LocalDate adjustedDeadline, boolean overdue) {
        static DeadlineDto of(CalendarDeadline d, boolean overdue) {
            return new DeadlineDto(d.getId(), d.getObligationRef(), d.getStartDate(), d.getPeriodCount(),
                d.getMode(), d.getHolidayCalendarId(), d.getHolidayCalendarVersion(), d.getRawDeadline(),
                d.getRollConvention(), d.getAdjustedDeadline(), overdue);
        }
    }

    private final CalendarDeadlineService service;

    public CalendarDeadlineController(CalendarDeadlineService service) {
        this.service = service;
    }

    @PostMapping("/api/calendar-deadline/calendars")
    public ResponseEntity<CalendarDto> createCalendar(@Valid @RequestBody CreateCalendarReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(CalendarDto.of(service.createCalendar(req.calendarName(), req.holidays())));
    }

    /** CALDLINE-CALVER-001 — editing the holiday set publishes a NEW version. */
    @PutMapping("/api/calendar-deadline/calendars/{id}")
    public CalendarDto editCalendar(@PathVariable UUID id, @Valid @RequestBody EditCalendarReq req) {
        return CalendarDto.of(service.editCalendar(id, req.holidays()));
    }

    @GetMapping("/api/calendar-deadline/calendars/{id}")
    public CalendarDto getCalendar(@PathVariable UUID id) {
        return CalendarDto.of(service.getCalendar(id));
    }

    /** CALDLINE-BASIS/BUSINESS/ROLL-001 — compute a deadline recording its full basis. */
    @PostMapping("/api/calendar-deadline/deadlines")
    public ResponseEntity<DeadlineDto> compute(@Valid @RequestBody ComputeReq req) {
        CalendarDeadline d = service.compute(req.obligationRef(), req.startDate(), req.periodCount(),
            req.mode(), req.holidayCalendarId(), req.rollConvention());
        return ResponseEntity.status(HttpStatus.CREATED).body(DeadlineDto.of(d, service.isOverdue(d)));
    }

    /** CALDLINE-OVERDUE-001 — overdue is recomputed on read. */
    @GetMapping("/api/calendar-deadline/deadlines/{id}")
    public DeadlineDto get(@PathVariable UUID id) {
        CalendarDeadline d = service.get(id);
        return DeadlineDto.of(d, service.isOverdue(d));
    }

    @ExceptionHandler(CalendarDeadlineException.class)
    public ResponseEntity<ProblemDetail> handle(CalendarDeadlineException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}

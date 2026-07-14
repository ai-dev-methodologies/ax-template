package com.ax.template.authblueprint.intervalexclusivity;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/** interval-exclusivity-l0 sole orchestrator for {@link BookingResource} registration. */
@Service
public class BookingResourceService {

    private final BookingResourceRepository resources;
    private final Clock clock;

    public BookingResourceService(BookingResourceRepository resources, Clock clock) {
        this.resources = resources;
        this.clock = clock;
    }

    @Transactional
    public BookingResource register(String resourceKey) {
        if (resources.existsByResourceKey(resourceKey)) {
            throw IntervalExclusivityException.duplicateResource();
        }
        try {
            return resources.saveAndFlush(new BookingResource(UUID.randomUUID(), resourceKey, Instant.now(clock)));
        } catch (DataIntegrityViolationException e) {                 // lost the uq(resource_key) race
            throw IntervalExclusivityException.duplicateResource();
        }
    }

    @Transactional(readOnly = true)
    public BookingResource get(String resourceKey) {
        return resources.findByResourceKey(resourceKey).orElseThrow(IntervalExclusivityException::resourceNotFound);
    }
}

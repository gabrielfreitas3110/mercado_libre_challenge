package com.quickcoders.infolabsproducts.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class MetricsService {
    
    private final MeterRegistry meterRegistry;
    
    // Reservation metrics
    private final Counter reservationsCreated;
    private final Counter reservationsCommitted;
    private final Counter reservationsReleased;
    private final Counter reservationsExpired;
    
    // Conflict metrics
    private final Counter versionConflicts;
    private final Counter optimisticLockingConflicts;
    
    // Operation timers
    private final Timer reservationTimer;
    private final Timer commitTimer;
    private final Timer releaseTimer;
    private final Timer adjustTimer;
    
    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize counters
        this.reservationsCreated = Counter.builder("inventory.reservations.created")
                .description("Number of reservations created")
                .register(meterRegistry);
                
        this.reservationsCommitted = Counter.builder("inventory.reservations.committed")
                .description("Number of reservations committed")
                .register(meterRegistry);
                
        this.reservationsReleased = Counter.builder("inventory.reservations.released")
                .description("Number of reservations released")
                .register(meterRegistry);
                
        this.reservationsExpired = Counter.builder("inventory.reservations.expired")
                .description("Number of reservations expired")
                .register(meterRegistry);
                
        this.versionConflicts = Counter.builder("inventory.conflicts.version")
                .description("Number of version conflicts (409 errors)")
                .register(meterRegistry);
                
        this.optimisticLockingConflicts = Counter.builder("inventory.conflicts.optimistic_locking")
                .description("Number of optimistic locking conflicts")
                .register(meterRegistry);
        
        // Initialize timers
        this.reservationTimer = Timer.builder("inventory.operations.reservation")
                .description("Time taken for reservation operations")
                .register(meterRegistry);
                
        this.commitTimer = Timer.builder("inventory.operations.commit")
                .description("Time taken for commit operations")
                .register(meterRegistry);
                
        this.releaseTimer = Timer.builder("inventory.operations.release")
                .description("Time taken for release operations")
                .register(meterRegistry);
                
        this.adjustTimer = Timer.builder("inventory.operations.adjust")
                .description("Time taken for adjust operations")
                .register(meterRegistry);
    }
    
    // Reservation metrics
    public void incrementReservationsCreated() {
        reservationsCreated.increment();
        log.debug("Incremented reservations created counter");
    }
    
    public void incrementReservationsCommitted() {
        reservationsCommitted.increment();
        log.debug("Incremented reservations committed counter");
    }
    
    public void incrementReservationsReleased() {
        reservationsReleased.increment();
        log.debug("Incremented reservations released counter");
    }
    
    public void incrementReservationsExpired() {
        reservationsExpired.increment();
        log.debug("Incremented reservations expired counter");
    }
    
    // Conflict metrics
    public void incrementVersionConflicts() {
        versionConflicts.increment();
        log.debug("Incremented version conflicts counter");
    }
    
    public void incrementOptimisticLockingConflicts() {
        optimisticLockingConflicts.increment();
        log.debug("Incremented optimistic locking conflicts counter");
    }
    
    // Operation timers
    public Timer.Sample startReservationTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordReservationTimer(Timer.Sample sample) {
        sample.stop(reservationTimer);
        log.debug("Recorded reservation operation timer");
    }
    
    public Timer.Sample startCommitTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordCommitTimer(Timer.Sample sample) {
        sample.stop(commitTimer);
        log.debug("Recorded commit operation timer");
    }
    
    public Timer.Sample startReleaseTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordReleaseTimer(Timer.Sample sample) {
        sample.stop(releaseTimer);
        log.debug("Recorded release operation timer");
    }
    
    public Timer.Sample startAdjustTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordAdjustTimer(Timer.Sample sample) {
        sample.stop(adjustTimer);
        log.debug("Recorded adjust operation timer");
    }
    
    // Custom metrics
    public void recordCustomMetric(String name, String description, double value, String... tags) {
        meterRegistry.gauge(name, value);
        log.debug("Recorded custom metric: {} = {}", name, value);
    }
    
    public void incrementCustomCounter(String name, String description, String... tags) {
        Counter.builder(name)
                .description(description)
                .tags(tags)
                .register(meterRegistry)
                .increment();
        log.debug("Incremented custom counter: {}", name);
    }
}

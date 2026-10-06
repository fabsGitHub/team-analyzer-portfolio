package com.teamanalyzer.teamanalyzer;

import com.teamanalyzer.teamanalyzer.port.AppClock;

import java.time.*;
import java.util.concurrent.atomic.AtomicReference;

public class MutableTestClock implements AppClock {
    private final AtomicReference<Instant> current = new AtomicReference<>();
    private final ZoneId zone = ZoneOffset.UTC;

    public MutableTestClock(Instant start) {
        this.current.set(start);
    }

    public void set(Instant t) {
        this.current.set(t);
    }

    public void plus(Duration d) {
        this.current.updateAndGet(i -> i.plus(d));
    }

    @Override
    public Instant now() {
        return current.get();
    }

    @Override
    public Clock asJavaClock() {
        return new Clock() {
            @Override
            public ZoneId getZone() {
                return zone;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return current.get();
            }
        };
    }
}

package com.rainbowteam.scheduler.model;

import java.time.LocalTime;
import java.util.Objects;

/** A time interval on a named day, for example Monday 08:00-10:00. */
public record TimeSlot(String id, String day, LocalTime start, LocalTime end) {
    public TimeSlot {
        id = required(id, "Time-slot id"); day = required(day, "Day");
        Objects.requireNonNull(start, "Start time cannot be null.");
        Objects.requireNonNull(end, "End time cannot be null.");
        if (!start.isBefore(end)) throw new IllegalArgumentException("Start time must be earlier than end time.");
    }
    public boolean overlaps(TimeSlot other) {
        return day.equalsIgnoreCase(other.day) && start.isBefore(other.end) && other.start.isBefore(end);
    }
    public String displayTime() { return day + " " + start + "-" + end; }
    private static String required(String value, String label) {
        Objects.requireNonNull(value, label + " cannot be null.");
        if (value.isBlank()) throw new IllegalArgumentException(label + " cannot be blank.");
        return value.trim();
    }
}

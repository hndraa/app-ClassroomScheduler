package com.rainbowteam.scheduler.model;

import java.util.Objects;

public record Room(String id, String name, int capacity) {
    public Room {
        id = required(id, "Room id"); name = required(name, "Room name");
        if (capacity < 1) throw new IllegalArgumentException("Room capacity must be at least 1.");
    }
    private static String required(String value, String label) {
        Objects.requireNonNull(value, label + " cannot be null.");
        if (value.isBlank()) throw new IllegalArgumentException(label + " cannot be blank.");
        return value.trim();
    }
}

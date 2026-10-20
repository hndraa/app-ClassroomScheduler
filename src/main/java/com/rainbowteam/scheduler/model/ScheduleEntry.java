package com.rainbowteam.scheduler.model;

import java.util.Objects;

public record ScheduleEntry(Activity activity, Room room, TimeSlot timeSlot) {
    public ScheduleEntry {
        Objects.requireNonNull(activity, "Activity cannot be null.");
        Objects.requireNonNull(room, "Room cannot be null.");
        Objects.requireNonNull(timeSlot, "Time slot cannot be null.");
    }
}

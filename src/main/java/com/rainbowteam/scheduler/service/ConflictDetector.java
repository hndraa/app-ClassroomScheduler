package com.rainbowteam.scheduler.service;

import com.rainbowteam.scheduler.model.ScheduleEntry;
import java.util.ArrayList;
import java.util.List;

public final class ConflictDetector {
    public List<String> findConflicts(List<ScheduleEntry> entries) {
        List<String> conflicts = new ArrayList<>();
        for (ScheduleEntry e : entries) if (e.room().capacity() < e.activity().requiredCapacity())
            conflicts.add("Room " + e.room().name() + " is too small for " + e.activity().courseName() + ".");
        for (int i = 0; i < entries.size(); i++) for (int j = i + 1; j < entries.size(); j++) {
            ScheduleEntry a = entries.get(i), b = entries.get(j);
            if (!a.timeSlot().overlaps(b.timeSlot())) continue;
            if (a.room().id().equalsIgnoreCase(b.room().id())
                    || a.room().name().equalsIgnoreCase(b.room().name()))
                conflicts.add("Room conflict: " + a.room().name() + " hosts overlapping activities.");
            if (a.activity().instructor().equalsIgnoreCase(b.activity().instructor())) conflicts.add("Instructor conflict: " + a.activity().instructor() + " has overlapping activities.");
            if (a.activity().studentGroup().equalsIgnoreCase(b.activity().studentGroup())) conflicts.add("Student-group conflict: " + a.activity().studentGroup() + " has overlapping activities.");
        }
        return List.copyOf(conflicts);
    }
    public boolean isValidPartialAssignment(List<ScheduleEntry> entries) { return findConflicts(entries).isEmpty(); }
}

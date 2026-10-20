package com.rainbowteam.scheduler.service;

import com.rainbowteam.scheduler.model.*;
import org.junit.jupiter.api.Test;
import java.time.LocalTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ConflictDetectorTest {
    private final ConflictDetector detector = new ConflictDetector();
    private TimeSlot slot(String id, int start, int end) { return new TimeSlot(id, "Monday", LocalTime.of(start, 0), LocalTime.of(end, 0)); }
    @Test void detectsRoomConflict() {
        Room room = new Room("R1", "Room 1", 40);
        var a = new ScheduleEntry(new Activity("A", "Course A", "Lecturer A", "Group A", 20), room, slot("S1", 8, 10));
        var b = new ScheduleEntry(new Activity("B", "Course B", "Lecturer B", "Group B", 20), room, slot("S2", 9, 11));
        assertTrue(detector.findConflicts(List.of(a, b)).stream().anyMatch(s -> s.contains("Room conflict")));
    }
    @Test void detectsInstructorConflict() {
        var a = new ScheduleEntry(new Activity("A", "Course A", "Same Lecturer", "Group A", 20), new Room("R1", "Room 1", 40), slot("S1", 8, 10));
        var b = new ScheduleEntry(new Activity("B", "Course B", "Same Lecturer", "Group B", 20), new Room("R2", "Room 2", 40), slot("S2", 8, 10));
        assertTrue(detector.findConflicts(List.of(a, b)).stream().anyMatch(s -> s.contains("Instructor conflict")));
    }
    @Test void detectsStudentGroupConflict() {
        var a = new ScheduleEntry(new Activity("A", "Course A", "Lecturer A", "Same Group", 20), new Room("R1", "Room 1", 40), slot("S1", 8, 10));
        var b = new ScheduleEntry(new Activity("B", "Course B", "Lecturer B", "Same Group", 20), new Room("R2", "Room 2", 40), slot("S2", 8, 10));
        assertTrue(detector.findConflicts(List.of(a, b)).stream().anyMatch(s -> s.contains("Student-group conflict")));
    }
    @Test void detectsInsufficientCapacity() {
        var entry = new ScheduleEntry(new Activity("A", "Course A", "Lecturer", "Group A", 45), new Room("R1", "Small room", 30), slot("S1", 8, 10));
        assertTrue(detector.findConflicts(List.of(entry)).stream().anyMatch(s -> s.contains("too small")));
    }
    @Test void adjacentIntervalsDoNotOverlap() { assertFalse(slot("S1", 8, 10).overlaps(slot("S2", 10, 12))); }
}

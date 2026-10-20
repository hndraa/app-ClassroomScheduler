package com.rainbowteam.scheduler.service;

import com.rainbowteam.scheduler.model.*;
import org.junit.jupiter.api.Test;
import java.time.LocalTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SchedulerServiceTest {
    @Test void createsValidSchedule() {
        List<Activity> activities = List.of(new Activity("A", "Algorithms", "Lecturer 1", "Group A", 20), new Activity("B", "Databases", "Lecturer 1", "Group B", 20), new Activity("C", "Networks", "Lecturer 2", "Group A", 20));
        List<Room> rooms = List.of(new Room("R1", "Room 1", 25), new Room("R2", "Room 2", 30));
        List<TimeSlot> slots = List.of(new TimeSlot("S1", "Monday", LocalTime.of(8, 0), LocalTime.of(10, 0)), new TimeSlot("S2", "Monday", LocalTime.of(10, 0), LocalTime.of(12, 0)));
        var result = new SchedulerService().createSchedule(activities, rooms, slots);
        assertTrue(result.isPresent()); assertEquals(activities.size(), result.orElseThrow().size());
        assertTrue(new ConflictDetector().findConflicts(result.orElseThrow()).isEmpty());
    }
    @Test void returnsEmptyWhenNoRoomFits() {
        var activities = List.of(new Activity("A", "Large lecture", "Lecturer", "Group A", 100));
        var rooms = List.of(new Room("R1", "Small room", 20));
        var slots = List.of(new TimeSlot("S1", "Monday", LocalTime.of(8, 0), LocalTime.of(10, 0)));
        assertTrue(new SchedulerService().createSchedule(activities, rooms, slots).isEmpty());
    }
    @Test void rejectsDuplicateActivityIdsIgnoringCase() {
        var activities = List.of(new Activity("A", "Course A", "Lecturer 1", "Group A", 20), new Activity("a", "Course B", "Lecturer 2", "Group B", 20));
        var rooms = List.of(new Room("R1", "Room 1", 30)); var slots = List.of(new TimeSlot("S1", "Monday", LocalTime.of(8, 0), LocalTime.of(10, 0)));
        assertThrows(IllegalArgumentException.class, () -> new SchedulerService().createSchedule(activities, rooms, slots));
    }
    @Test
    void newScheduleDoesNotConflictWithFixedExistingEntries() {
        Activity existingActivity = new Activity("OLD", "Existing Course", "Lecturer 1", "Group A", 20);
        Room room1 = new Room("R1", "Room 1", 30);
        TimeSlot slot1 = new TimeSlot("S1", "Monday", LocalTime.of(8, 0), LocalTime.of(10, 0));
        ScheduleEntry existing = new ScheduleEntry(existingActivity, room1, slot1);

        List<Activity> newActivities = List.of(
                new Activity("NEW", "New Course", "Lecturer 2", "Group B", 20));
        List<Room> rooms = List.of(room1, new Room("R2", "Room 2", 30));
        List<TimeSlot> slots = List.of(slot1,
                new TimeSlot("S2", "Monday", LocalTime.of(8, 0), LocalTime.of(10, 0)));

        Optional<List<ScheduleEntry>> result = scheduler.createSchedule(newActivities, rooms, slots, List.of(existing));

        assertTrue(result.isPresent());
        assertEquals(2, result.orElseThrow().size());
        assertTrue(new ConflictDetector().findConflicts(result.orElseThrow()).isEmpty());
    }

    @Test
    void doesNotAllowDuplicateActivityIdAlreadyInSavedSchedule() {
        Activity existingActivity = new Activity("A", "Existing Course", "Lecturer 1", "Group A", 20);
        ScheduleEntry existing = new ScheduleEntry(existingActivity,
                new Room("R1", "Room 1", 30),
                new TimeSlot("S1", "Monday", LocalTime.of(8, 0), LocalTime.of(10, 0)));
        List<Activity> newActivities = List.of(new Activity("a", "Repeated Course", "Lecturer 2", "Group B", 20));

        assertThrows(IllegalArgumentException.class, () -> scheduler.createSchedule(newActivities,
                List.of(new Room("R1", "Room 1", 30)),
                List.of(new TimeSlot("S1", "Monday", LocalTime.of(8, 0), LocalTime.of(10, 0))),
                List.of(existing)));
    }


    @Test
    void newActivityUsesOnlySlotsProvidedForCurrentDynamicInput() {
        Activity existingActivity = new Activity("OLD", "Existing course", "Lecturer Old", "Group Old", 10);
        Room existingRoom = new Room("R1", "Room 1", 30);
        TimeSlot monday = new TimeSlot("MON-1", "Senin",
                LocalTime.of(8, 0), LocalTime.of(10, 0));
        ScheduleEntry fixed = new ScheduleEntry(existingActivity, existingRoom, monday);

        Activity newActivity = new Activity("NEW", "Operating Systems", "Lecturer New", "Group New", 20);
        Room newRoom = new Room("R2", "Room 2", 30);
        TimeSlot wednesday = new TimeSlot("WED-1", "Rabu",
                LocalTime.of(7, 0), LocalTime.of(12, 0));

        Optional<List<ScheduleEntry>> result = scheduler.createSchedule(
                List.of(newActivity), List.of(existingRoom, newRoom),
                List.of(wednesday), List.of(fixed));

        assertTrue(result.isPresent());
        ScheduleEntry added = result.orElseThrow().stream()
                .filter(entry -> entry.activity().id().equals("NEW"))
                .findFirst().orElseThrow();
        assertEquals("Rabu", added.timeSlot().day());
        assertEquals(LocalTime.of(7, 0), added.timeSlot().start());
        assertEquals(LocalTime.of(12, 0), added.timeSlot().end());
        assertTrue(result.orElseThrow().contains(fixed));
    }

}

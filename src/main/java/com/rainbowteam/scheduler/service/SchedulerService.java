package com.rainbowteam.scheduler.service;

import com.rainbowteam.scheduler.graph.ConflictGraph;
import com.rainbowteam.scheduler.graph.GraphColoring;
import com.rainbowteam.scheduler.model.*;

import java.util.*;

/** Graph coloring proposes slots; backtracking assigns slots and rooms while enforcing hard constraints. */
public final class SchedulerService {
    private final ConflictDetector detector = new ConflictDetector();
    private final GraphColoring coloring = new GraphColoring();

    public Optional<List<ScheduleEntry>> createSchedule(List<Activity> activities, List<Room> rooms,
                                                         List<TimeSlot> slots) {
        return createSchedule(activities, rooms, slots, List.of());
    }

    /** Existing entries are fixed: new activities must fit around them without moving them. */
    public Optional<List<ScheduleEntry>> createSchedule(List<Activity> activities, List<Room> rooms,
                                                         List<TimeSlot> slots,
                                                         List<ScheduleEntry> fixedEntries) {
        validate(activities, rooms, slots, fixedEntries);
        if (!detector.findConflicts(fixedEntries).isEmpty()) {
            throw new IllegalArgumentException("Jadwal yang sudah tersimpan memiliki konflik. Periksa data/schedule.csv.");
        }
        Set<String> existingIds = new HashSet<>();
        for (ScheduleEntry entry : fixedEntries) existingIds.add(normalize(entry.activity().id()));
        for (Activity activity : activities) {
            if (existingIds.contains(normalize(activity.id()))) {
                throw new IllegalArgumentException("ID aktivitas " + activity.id()
                        + " sudah ada pada jadwal. Gunakan ID baru atau cek jadwal yang tersimpan.");
            }
        }

        ConflictGraph graph = new ConflictGraph(activities);
        Map<String, Integer> preferred = coloring.color(graph, slots);
        List<Activity> ordered = new ArrayList<>(activities);
        ordered.sort(Comparator.<Activity>comparingInt(a -> graph.degreeOf(a.id())).reversed()
                .thenComparingInt(a -> suitableRoomCount(a, rooms)).thenComparing(Activity::id));
        List<ScheduleEntry> assigned = new ArrayList<>(fixedEntries);
        if (!search(0, ordered, rooms, slots, preferred, assigned)) return Optional.empty();
        assigned.sort(Comparator.comparing((ScheduleEntry e) -> e.timeSlot().day())
                .thenComparing(e -> e.timeSlot().start()).thenComparing(e -> e.room().name()));
        return Optional.of(List.copyOf(assigned));
    }

    private boolean search(int index, List<Activity> activities, List<Room> rooms, List<TimeSlot> slots,
                           Map<String, Integer> preferred, List<ScheduleEntry> assigned) {
        if (index == activities.size()) return true;
        Activity activity = activities.get(index);
        List<Room> suitable = rooms.stream().filter(r -> r.capacity() >= activity.requiredCapacity())
                .sorted(Comparator.comparingInt(Room::capacity).thenComparing(Room::name)).toList();
        if (suitable.isEmpty()) return false;
        List<TimeSlot> orderedSlots = new ArrayList<>(slots);
        Integer preferredIndex = preferred.get(activity.id());
        if (preferredIndex != null && preferredIndex >= 0 && preferredIndex < slots.size()) {
            TimeSlot first = slots.get(preferredIndex);
            orderedSlots.remove(first);
            orderedSlots.add(0, first);
        }
        for (TimeSlot slot : orderedSlots) {
            for (Room room : suitable) {
                assigned.add(new ScheduleEntry(activity, room, slot));
                if (detector.isValidPartialAssignment(assigned)
                        && search(index + 1, activities, rooms, slots, preferred, assigned)) return true;
                assigned.remove(assigned.size() - 1);
            }
        }
        return false;
    }

    private int suitableRoomCount(Activity a, List<Room> rooms) {
        return (int) rooms.stream().filter(r -> r.capacity() >= a.requiredCapacity()).count();
    }

    private void validate(List<Activity> activities, List<Room> rooms, List<TimeSlot> slots,
                          List<ScheduleEntry> fixedEntries) {
        Objects.requireNonNull(activities, "Activities cannot be null.");
        Objects.requireNonNull(rooms, "Rooms cannot be null.");
        Objects.requireNonNull(slots, "Time slots cannot be null.");
        Objects.requireNonNull(fixedEntries, "Existing schedule cannot be null.");
        if (activities.isEmpty()) throw new IllegalArgumentException("At least one new activity is required.");
        if (rooms.isEmpty() || slots.isEmpty()) throw new IllegalArgumentException("Rooms and time slots must not be empty.");
        if (activities.stream().anyMatch(Objects::isNull) || rooms.stream().anyMatch(Objects::isNull)
                || slots.stream().anyMatch(Objects::isNull) || fixedEntries.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Input lists cannot contain null values.");
        }
        unique(activities.stream().map(Activity::id).toList(), "activity");
        unique(rooms.stream().map(Room::id).toList(), "room");
        unique(slots.stream().map(TimeSlot::id).toList(), "time-slot");
    }

    private void unique(List<String> ids, String label) {
        Set<String> seen = new HashSet<>();
        for (String id : ids) {
            if (!seen.add(normalize(id))) throw new IllegalArgumentException("Duplicate " + label + " id: " + id);
        }
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}

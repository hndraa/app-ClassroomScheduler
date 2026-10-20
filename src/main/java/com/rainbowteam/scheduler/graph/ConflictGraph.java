package com.rainbowteam.scheduler.graph;

import com.rainbowteam.scheduler.model.Activity;
import java.util.*;

/** Vertex = activity; edge = shared instructor or student group. */
public final class ConflictGraph {
    private final Map<String, Activity> activities = new LinkedHashMap<>();
    private final Map<String, Set<String>> neighbors = new LinkedHashMap<>();

    public ConflictGraph(List<Activity> input) {
        Objects.requireNonNull(input, "Activities cannot be null.");
        for (Activity activity : input) {
            Objects.requireNonNull(activity, "Activity list cannot contain null.");
            if (activities.putIfAbsent(activity.id(), activity) != null)
                throw new IllegalArgumentException("Duplicate activity id: " + activity.id());
            neighbors.put(activity.id(), new LinkedHashSet<>());
        }
        List<Activity> items = new ArrayList<>(activities.values());
        for (int i = 0; i < items.size(); i++) for (int j = i + 1; j < items.size(); j++) {
            Activity a = items.get(i), b = items.get(j);
            if (a.instructor().equalsIgnoreCase(b.instructor()) || a.studentGroup().equalsIgnoreCase(b.studentGroup())) {
                neighbors.get(a.id()).add(b.id()); neighbors.get(b.id()).add(a.id());
            }
        }
    }
    public Set<String> neighborsOf(String id) {
        Set<String> result = neighbors.get(id);
        if (result == null) throw new IllegalArgumentException("Unknown activity id: " + id);
        return Collections.unmodifiableSet(result);
    }
    public int degreeOf(String id) { return neighborsOf(id).size(); }
    public List<Activity> activities() { return List.copyOf(activities.values()); }
}

package com.rainbowteam.scheduler.graph;

import com.rainbowteam.scheduler.model.Activity;
import com.rainbowteam.scheduler.model.TimeSlot;
import java.util.*;

/** Greedy DSATUR-inspired heuristic; colors are indices of preferred time slots. */
public final class GraphColoring {
    public Map<String, Integer> color(ConflictGraph graph, List<TimeSlot> slots) {
        Objects.requireNonNull(graph); Objects.requireNonNull(slots);
        if (slots.isEmpty()) throw new IllegalArgumentException("At least one time slot is required.");
        Map<String, Integer> assigned = new LinkedHashMap<>();
        Set<String> uncolored = new LinkedHashSet<>();
        for (Activity a : graph.activities()) uncolored.add(a.id());
        while (!uncolored.isEmpty()) {
            String selected = selectNext(graph, uncolored, assigned);
            Set<Integer> used = new HashSet<>();
            for (String neighbor : graph.neighborsOf(selected)) if (assigned.containsKey(neighbor)) used.add(assigned.get(neighbor));
            for (int color = 0; color < slots.size(); color++) {
                if (!used.contains(color)) { assigned.put(selected, color); break; }
            }
            // If no color is available, leave it uncolored; backtracking will search all slots.
            uncolored.remove(selected);
        }
        return Collections.unmodifiableMap(assigned);
    }
    private String selectNext(ConflictGraph graph, Set<String> uncolored, Map<String, Integer> assigned) {
        String best = null; int bestSaturation = -1, bestDegree = -1;
        for (String candidate : uncolored) {
            Set<Integer> colors = new HashSet<>();
            for (String neighbor : graph.neighborsOf(candidate)) if (assigned.containsKey(neighbor)) colors.add(assigned.get(neighbor));
            int saturation = colors.size(), degree = graph.degreeOf(candidate);
            if (saturation > bestSaturation || (saturation == bestSaturation && degree > bestDegree)) {
                best = candidate; bestSaturation = saturation; bestDegree = degree;
            }
        }
        return best;
    }
}

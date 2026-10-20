package com.rainbowteam.scheduler.model;

import java.util.Objects;

/** A class or academic activity that needs one room and one time slot. */
public record Activity(String id, String courseName, String instructor, String studentGroup, int requiredCapacity) {
    public Activity {
        id = required(id, "Activity id");
        courseName = required(courseName, "Course name");
        instructor = required(instructor, "Instructor");
        studentGroup = required(studentGroup, "Student group");
        if (requiredCapacity < 1) throw new IllegalArgumentException("Required capacity must be at least 1.");
    }
    private static String required(String value, String label) {
        Objects.requireNonNull(value, label + " cannot be null.");
        if (value.isBlank()) throw new IllegalArgumentException(label + " cannot be blank.");
        return value.trim();
    }
}

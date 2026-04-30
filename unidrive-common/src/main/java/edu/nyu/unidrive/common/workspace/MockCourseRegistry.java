package edu.nyu.unidrive.common.workspace;

import java.util.List;

public final class MockCourseRegistry {

    public static final String CURRENT_TERM = "fall2026";

    private static final List<Course> COURSES = List.of(
        new Course("daa", "Design and Analysis of Algorithms"),
        new Course("java", "Introduction to Java"),
        new Course("bda", "Big Data Analytics")
    );

    public String currentTerm() {
        return CURRENT_TERM;
    }

    public List<Course> courses() {
        return COURSES;
    }

    public record Course(String slug, String displayName) {
    }
}

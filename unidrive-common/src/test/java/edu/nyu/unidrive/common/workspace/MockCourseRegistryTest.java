package edu.nyu.unidrive.common.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.nyu.unidrive.common.workspace.MockCourseRegistry.Course;
import java.util.List;
import org.junit.jupiter.api.Test;

class MockCourseRegistryTest {

    @Test
    void exposesCurrentTermAsFall2026() {
        assertEquals("fall2026", new MockCourseRegistry().currentTerm());
    }

    @Test
    void listsThreeMockCoursesWithSlugAndDisplayName() {
        List<Course> courses = new MockCourseRegistry().courses();
        assertEquals(3, courses.size());
        assertTrue(courses.contains(new Course("daa", "Design and Analysis of Algorithms")));
        assertTrue(courses.contains(new Course("java", "Introduction to Java")));
        assertTrue(courses.contains(new Course("bda", "Big Data Analytics")));
    }
}

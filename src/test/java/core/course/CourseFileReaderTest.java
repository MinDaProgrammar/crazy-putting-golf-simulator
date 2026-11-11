package core.course;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CourseFileReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void readsRequiredCourseParametersAndObstaclesFromFile() throws Exception {
        Path courseFile = tempDir.resolve("course.txt");
        Files.writeString(courseFile, """
                # Manual-style course input
                height_profile = 0.25 * sin((x + y) / 10) + 1
                friction_kinetic = 0.08
                friction_static = 0.2
                start_x = 7.0
                start_y = 8.0
                target_x = 14.0
                target_y = 1.0
                target_radius = 0.1
                vmax = 5.0
                tree = 3.0, 4.0, 0.5
                sandpit = 5.0, 6.0, 1.2, 0.4, 0.55
                """);

        Course course = CourseFileReader.readCourseFile(courseFile.toString());

        assertEquals("0.25 * sin((x + y) / 10) + 1", course.getHeightProfile());
        assertEquals(0.08, course.getFrictionKinetic(), 1e-12);
        assertEquals(0.2, course.getFrictionStatic(), 1e-12);
        assertEquals(7.0, course.getStartX(), 1e-12);
        assertEquals(8.0, course.getStartY(), 1e-12);
        assertEquals(14.0, course.getTargetX(), 1e-12);
        assertEquals(1.0, course.getTargetY(), 1e-12);
        assertEquals(0.1, course.getTargetRadius(), 1e-12);
        assertEquals(5.0, course.getVMax(), 1e-12);

        assertEquals(1, course.getTrees().size());
        assertEquals(3.0, course.getTrees().get(0).x, 1e-12);
        assertEquals(4.0, course.getTrees().get(0).y, 1e-12);
        assertEquals(0.5, course.getTrees().get(0).radius, 1e-12);

        assertEquals(1, course.getSandpits().size());
        assertEquals(5.0, course.getSandpits().get(0).x, 1e-12);
        assertEquals(6.0, course.getSandpits().get(0).y, 1e-12);
        assertEquals(1.2, course.getSandpits().get(0).radius, 1e-12);
        assertEquals(0.4, course.getSandpits().get(0).kineticFriction, 1e-12);
        assertEquals(0.55, course.getSandpits().get(0).staticFriction, 1e-12);
    }
}

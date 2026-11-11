package experiments;

import core.course.Course;
import core.course.CourseFileReader;
import core.course.HeightFunction;

public class HeightFunctionTest {
    public static void main(String[] args) {
        try {
            // Test 1: Simple manual check
            System.out.println("--- Test 1: Manual Expression ---");
            String simpleExpr = "sin(x) + y";
            HeightFunction hSimple = HeightFunction.compileExpression(simpleExpr);
            
            // At x=0, y=1: sin(0) + 1 should be 1.0
            double result1 = hSimple.evaluate(0, 1);
            System.out.println("Expression: " + simpleExpr);
            System.out.println("At (0, 1), result is: " + result1 + " (Expected: 1.0)");

            // Test 2: Check the actual course file
            System.out.println("\n--- Test 2: Course File Expression ---");
            Course course = CourseFileReader.readCourseFile("crazy_putting/src/main/resources/courses/test_course.txt");
            String profile = course.getHeightProfile();
            
            HeightFunction hCourse = HeightFunction.compileExpression(profile);
            
            // Pick a point to test, e.g., (0, 0)
            double x = 0;
            double y = 0;
            double result2 = hCourse.evaluate(x, y);
            System.out.println("Profile from file: " + profile);
            System.out.println("At ("+x+","+y+"), height is: " + result2);

        } catch (Exception e) {
            System.err.println("Error during testing:");
            e.printStackTrace();
        }
    }
}
package experiments;

import core.bots.Bot;
import core.bots.MLBot;
import core.bots.MazeMLBot;
import core.course.Course;
import core.course.CourseFileReader;

/**
 * Entry point for the bot comparison experiment.
 * Loads courses from file and runs either the cell size sweep or a bot vs bot
 * comparison. This class is run manually to collect results, not at runtime.
 * Comment out whichever block you don't need.
 */
public class BotComparisonMain {

    private static final String COURSE_PATH = "crazy_putting/target/classes/courses/";

    private static final String[] COURSES = {
            "demo_course.txt",
    //        "demo_maze1.txt",
     //       "demo_maze2.txt",
     //       "demo_maze3.txt",
     //      "demo_maze4.txt",
     //       "demo_mazebot1.txt",
     //       "demo_mazebot2.txt",
      //      "demo_mazebot3.txt",
            "demo_mazebot4.txt",
      //      "test_course.txt",
      //      "test_course2.txt",
       //     "test_course3.txt",
            "test_course4.txt",
            "test_course5.txt",
            "test_course6.txt",
            "test_maze1.txt",
    };

    // grid cell sizes to test in the sweep
    private static final double[] CELL_SIZES = { 0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.6, 0.75, 1.0, 2.0 };

    // number of trials per run
    private static final int TRIALS = 6;

    public static void main(String[] args) throws Exception {
        for (String filename : COURSES) {
            Course course = CourseFileReader.readCourseFile(COURSE_PATH + filename);
            System.out.println(filename);

            // --- cell size sweep ---
            BotComparison.cellSizeSweep(course, CELL_SIZES, TRIALS);

            // --- bot vs bot comparison ---
             //Bot mlBot = new MLBot();
             //Bot mazeBot = new MazeMLBot();
             //BotComparison.run(mlBot, mazeBot, course, TRIALS);

            System.out.println();
        }
    }

}

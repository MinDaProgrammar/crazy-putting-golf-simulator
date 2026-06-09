package gui.golf;

import core.course.Course;
import core.course.HeightFunction;

public class GolfGameState {

    private Course currentCourse;
    private HeightFunction currentHeightFunction;
    private double ballX;
    private double ballY;
    private int shotCount;
    private int mlShotCounter;

    public void loadCourse(Course course, HeightFunction heightFunction) {
        this.currentCourse = course;
        this.currentHeightFunction = heightFunction;
        resetBallToStart();
        resetShotCount();
        resetMlShotCounter();
    }


    // -------
    // helpers
    // -------
    public boolean hasCourseLoaded() {
        return currentCourse != null && currentHeightFunction != null;
    }

    public void resetBallToStart() {
        ballX = currentCourse.getStartX();
        ballY = currentCourse.getStartY();
    }

    public void setBallPosition(double x, double y) {
        ballX = x;
        ballY = y;
    }

    public void incrementShotCount() { shotCount++; }
    public void resetShotCount() { shotCount = 0; }
    public void resetMlShotCounter() { mlShotCounter = 0; }

    // -------
    // getters
    // -------
    public Course getCurrentCourse() { return currentCourse; }
    public HeightFunction getCurrentHeightFunction() { return currentHeightFunction; }
    public double getBallX() { return ballX; }
    public double getBallY() { return ballY; }
    public int getShotCount() { return shotCount; }
    public int getMlShotCounter() { return mlShotCounter; }
}

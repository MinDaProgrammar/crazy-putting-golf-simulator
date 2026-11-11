package core.bots;

import core.course.Course;

public interface Bot {
    /** Returns [vx, vy] given the course and current ball position. */
    double[] computeShot(Course course, double x, double y);
}
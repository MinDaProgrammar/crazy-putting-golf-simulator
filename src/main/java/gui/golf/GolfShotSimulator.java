package gui.golf;

import core.course.Course;
import core.course.HeightFunction;
import gui.SimulationEngine;
import gui.SimulationResult;
import solvers.RungeKutta4;
import systems.BallMotionSystem;

public class GolfShotSimulator {

    private static final double STEP_SIZE = 0.01;
    private static final double MAX_TIME = 15.0;

    public SimulationResult simulateShot(
            Course course,
            HeightFunction heightFunction,
            double ballX,
            double ballY,
            double vx,
            double vy
    ) {
        BallMotionSystem physics = new BallMotionSystem(
                heightFunction,
                course.getFrictionKinetic(),
                course.getFrictionStatic(),
                course.getSandpits()
        );

        double[] initialState = {ballX, ballY, vx, vy};
        
        return SimulationEngine.run(physics, new RungeKutta4(STEP_SIZE), initialState, STEP_SIZE, MAX_TIME);
    }
}

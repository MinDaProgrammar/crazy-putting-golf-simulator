package experiments;

import core.*;
import core.bots.Bot;
import core.bots.MLBot;
import core.bots.MazeMLBot;
import core.course.Course;
import core.course.HeightFunction;
import gui.SimulationEngine;
import gui.SimulationResult;
import core.course.HeightParser;
import systems.BallMotionSystem;
import java.util.List;
import java.util.Random;

/**
 * Runs two bots on a course and prints a comparison of their performance.
 * Both bots must implement the Bot interface.
 * Course and HeightFunction are built and passed in from outside (main).
 *
 * Bot ruleBot = new RuleBasedBot(...);
 * Bot mlBot = new MLBot(...);
 * BotComparison.run(ruleBot, mlBot, course, hf, 20);
 */
public class BotComparison {

    private static final double STEP_SIZE = 0.01;
    private static final double MAX_SIM_T = 30.0;
    private static final double STOP_SPEED = 1e-4;
    private static final int MAX_SHOTS = 5;

    // Mirror MazeMLBot's own defaults, held fixed while only cellSize varies.
    private static final double MAZE_MIN_AIM = MazeMLBot.DEFAULT_MIN_AIM_DISTANCE;
    private static final double MAZE_MAX_AIM = MazeMLBot.DEFAULT_MAX_AIM_DISTANCE;
    private static final double MAZE_DIRECT_AIM = MazeMLBot.DEFAULT_DIRECT_AIM_DISTANCE;
    private static final double MAZE_WATER_MARGIN = MazeMLBot.DEFAULT_WATER_SAFETY_MARGIN;
    private static final double MAZE_OBSTACLE_PADDING = MazeMLBot.DEFAULT_OBSTACLE_PADDING;
    private static final boolean MAZE_FOUR_CONNECTED = MazeMLBot.DEFAULT_FOUR_CONNECTED;

    /**
     * Runs both bots on the same course for the given number of trials
     * and prints their results side by side. Both bots receive identical
     * randomized start positions per trial for a fair comparison.
     *
     * @param botA   the first bot (e.g. rule-based)
     * @param botB   the second bot (e.g. ML)
     * @param course the course to play on (target, friction, start position)
     * @param trials how many times each bot plays the course
     */
    public static void run(Bot botA, Bot botB, Course course, int trials) {
        System.out.println(botA.getClass().getSimpleName() + " vs " + botB.getClass().getSimpleName());

        HeightFunction hf = HeightParser.parse(course.getHeightProfile());
        double[][] starts = generateStarts(course, hf, trials, 0.5);

        double[] a = evaluate(botA, course, trials, starts);
        double[] b = evaluate(botB, course, trials, starts);

        // a/b = [successRate, avgFinalDist, avgShots, avgComputeMs, avgTotalDist]
        System.out.println("Success rate  :  " + (a[0] * 100) + "%   " + (b[0] * 100) + "%");
        System.out.println("Dist to target :  " + a[1] + " m   " + b[1] + " m");
        System.out.println("Total distance:  " + a[4] + " m   " + b[4] + " m");
        System.out.println("Total shots     :  " + a[2] + "   " + b[2]);
        System.out.println("Compute time  :  " + a[3] + " ms   " + b[3] + " ms");
    }

    /**
     * Runs one bot on the course for N trials and returns averaged performance
     * metrics. If starts is provided, each trial uses the corresponding
     * pre-generated position. If starts is null, every trial uses the
     * course's original start position (fixed start).
     *
     * @param bot    the bot to evaluate
     * @param course the course to play on
     * @param trials number of times the bot plays the course
     * @param starts pre-generated start positions [trial][x,y], or null for
     *               fixed start
     * @return double[] { successRate, avgFinalDistance, avgShots, avgComputeMs,
     *         avgTotalDist }
     */
    public static double[] evaluate(Bot bot, Course course, int trials, double[][] starts) {
        // set up physics system and solver
        HeightFunction hf = HeightParser.parse(course.getHeightProfile());
        BallMotionSystem sys = new BallMotionSystem(hf, course.getFrictionKinetic(), course.getFrictionStatic(),
                course.getSandpits());
        ODESolver solver = new solvers.RungeKutta4(STEP_SIZE);

        double sumSuccess = 0, sumDist = 0, sumShots = 0, sumMs = 0, sumTotalDist = 0;

        for (int t = 0; t < trials; t++) {
            // use shared starts if provided, otherwise fixed course start
            double x, y;
            if (starts != null) {
                x = starts[t][0];
                y = starts[t][1];
            } else {
                x = course.getStartX();
                y = course.getStartY();
            }

            boolean success = false;
            int shots = 0;
            double totalDist = 0;

            // each shot: ask bot for velocity, simulate, check result
            for (int shot = 0; shot < MAX_SHOTS; shot++) {
                shots++;

                // time the bot's computation
                long t0 = System.nanoTime();
                double[] vel = bot.computeShot(course, x, y);
                sumMs += (System.nanoTime() - t0) / 1_000_000.0;

                // simulate the shot with RK4
                SimulationResult sim = SimulationEngine.run(
                        sys, solver, new double[] { x, y, vel[0], vel[1] }, STEP_SIZE, MAX_SIM_T);

                // find where the ball stopped moving
                int stop = findStop(sim);

                // accumulate total distance travelled during this shot
                List<Double> xs = sim.dataMatrix.get(0);
                List<Double> ys = sim.dataMatrix.get(1);
                for (int step = 1; step <= stop; step++) {
                    totalDist += Math.hypot(
                            xs.get(step) - xs.get(step - 1),
                            ys.get(step) - ys.get(step - 1));
                }

                // walk every step to catch obstacles and target hits mid-roll
                for (int step = 1; step <= stop; step++) {
                    x = xs.get(step);
                    y = ys.get(step);

                    // obstacle check — reset to start if ball is in water or a tree
                    if (isInObstacle(x, y, hf, course)) {
                        x = course.getStartX();
                        y = course.getStartY();
                        break;
                    }

                    // target check — ball is within target radius
                    if (Math.hypot(x - course.getTargetX(), y - course.getTargetY()) <= course.getTargetRadius()) {
                        success = true;
                        break;
                    }
                }

                if (success) {
                    break;
                }

            }

            // record trial results
            if (success)
                sumSuccess++;
            sumDist += Math.hypot(x - course.getTargetX(), y - course.getTargetY());
            sumShots += shots;
            sumTotalDist += totalDist;
        }

        // return averaged metrics across all trials
        return new double[] {
                sumSuccess / trials, // success rate
                sumDist / trials, // avg final distance from target
                sumShots / trials, // avg shots taken
                sumMs / trials, // avg bot compute time (ms)
                sumTotalDist / trials // avg total distance travelled
        };
    }

    /**
     * Tests how the MazeMLBot's cell size affects its performance on a course.
     * Builds a fresh MazeMLBot for each cell size, keeping every other parameter
     * at its default, then evaluates it and prints one result row per cell size.
     * All cell sizes share the same randomized start positions for a fair
     * comparison. Letting only cellSize vary isolates its effect on the metrics.
     *
     * @param course    the course to play on
     * @param cellSizes the grid cell sizes to test
     * @param trials    number of trials per cell size
     */
    public static void cellSizeSweep(Course course, double[] cellSizes, int trials) {
        System.out.println("MazeMLBot cell size sweep");

        HeightFunction hf = HeightParser.parse(course.getHeightProfile());
        double[][] starts = generateStarts(course, hf, trials, 0.5);

        for (double cellSize : cellSizes) {
            // build a MazeMLBot with this cell size, all other params at default
            MazeMLBot bot = new MazeMLBot(
                    new MLBot(),
                    cellSize,
                    MAZE_MIN_AIM,
                    MAZE_MAX_AIM,
                    MAZE_DIRECT_AIM,
                    MAZE_WATER_MARGIN,
                    MAZE_OBSTACLE_PADDING,
                    MAZE_FOUR_CONNECTED);

            // evaluate this configuration and print its metrics
            double[] r = evaluate(bot, course, trials, starts);

            System.out.println("cellSize = " + cellSize);
            System.out.println("  Success rate   :  " + (r[0] * 100) + "%");
            System.out.println("  Dist to target :  " + r[1] + " m");
            System.out.println("  Total distance :  " + r[4] + " m");
            System.out.println("  Total shots    :  " + r[2]);
            System.out.println("  Compute time   :  " + r[3] + " ms");
        }
    }

    /**
     * Pre-generates randomized start positions for all trials. Each position
     * is within the given radius of the course start and avoids obstacles.
     * Uses a fixed seed (42) so results are reproducible across runs.
     *
     * @param course the course (for start position and trees)
     * @param hf     the height function (for water checks)
     * @param trials number of start positions to generate
     * @param radius the max distance from the original start
     * @return double[trials][2] array of {x, y} start positions
     */
    private static double[][] generateStarts(Course course, HeightFunction hf, int trials, double radius) {
        Random rng = new Random(42);
        double[][] starts = new double[trials][2];
        for (int t = 0; t < trials; t++) {
            double[] s = randomStart(course, hf, rng, radius);
            starts[t][0] = s[0];
            starts[t][1] = s[1];
        }
        return starts;
    }

    /**
     * Checks whether a position is inside an obstacle (water or tree).
     *
     * @param x      the x coordinate to check
     * @param y      the y coordinate to check
     * @param hf     the height function (negative = water)
     * @param course the course (for tree positions)
     * @return true if the position is in water or inside a tree
     */
    private static boolean isInObstacle(double x, double y, HeightFunction hf, Course course) {
        if (hf.evaluate(x, y) < 0) {
            return true;
        }

        if (course.getTrees() != null) {
            for (Course.Tree tree : course.getTrees()) {
                if (Math.hypot(x - tree.x, y - tree.y) <= tree.radius) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Generates a random start position within a given radius of the course
     * start, ensuring the point is not inside an obstacle. Falls back to the
     * original start position if no valid point is found after 100 attempts.
     *
     * @param course the course (for start position and trees)
     * @param hf     the height function (for water checks)
     * @param rng    the random number generator
     * @param radius the max distance from the original start
     * @return double[] { x, y } of the randomized start position
     */
    private static double[] randomStart(Course course, HeightFunction hf, Random rng, double radius) {
        double cx = course.getStartX();
        double cy = course.getStartY();

        for (int attempt = 0; attempt < 100; attempt++) {
            double angle = rng.nextDouble() * 2 * Math.PI;
            double dist = rng.nextDouble() * radius;
            double x = cx + dist * Math.cos(angle);
            double y = cy + dist * Math.sin(angle);

            if (isInObstacle(x, y, hf, course))
                continue;

            return new double[] { x, y };
        }

        return new double[] { cx, cy };
    }

    /**
     * Finds the time index in the simulation where the ball stopped moving.
     * Walks through the recorded vx and vy values and returns the first index
     * where the ball's speed drops below STOP_SPEED.
     * The first 10 steps are skipped to avoid a false stop at the very start.
     *
     * @param sim the simulation result containing the full trajectory
     * @return the index at which the ball stopped, or the last index if it never
     *         stopped
     */
    private static int findStop(SimulationResult sim) {
        List<Double> vx = sim.dataMatrix.get(2); // vx from BallMotionSystem { "x", "y", "vx", "vy" }
        List<Double> vy = sim.dataMatrix.get(3); // vy from BallMotionSystem { "x", "y", "vx", "vy" }
        for (int i = 10; i < vx.size(); i++) {
            if (Math.hypot(vx.get(i), vy.get(i)) < STOP_SPEED)
                return i;
        }
        return vx.size() - 1;
    }

}
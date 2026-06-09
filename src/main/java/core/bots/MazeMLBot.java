package core.bots;

import core.Vector2D;
import core.course.Course;
import core.course.HeightFunction;
import core.pathfinding.AStarPathfinder;
import core.pathfinding.GridMap;
import core.pathfinding.PathSimplifier;
import core.pathfinding.WaypointSelector;
import solvers.RungeKutta4;
import systems.BallMotionSystem;

import java.util.List;

/**
 * Phase 3 bot for maze-like courses. Plans with A* on a grid, then delegates
 * the actual shot optimization to the existing bot by temporarily
 * retargeting the course to the chosen waypoint.
 */
public class MazeMLBot implements Bot {

    public static final double DEFAULT_CELL_SIZE = 0.5;
    public static final double DEFAULT_MIN_AIM_DISTANCE = 5.0;
    public static final double DEFAULT_MAX_AIM_DISTANCE = 35.0;
    public static final double DEFAULT_DIRECT_AIM_DISTANCE = 6.0;
    public static final double DEFAULT_WATER_SAFETY_MARGIN = 0.08;
    public static final double DEFAULT_OBSTACLE_PADDING = 0.7;
    public static final boolean DEFAULT_FOUR_CONNECTED = true;

    /** Minimum terrain height for shot segment checks — blocks actual water (h≤0) only. */
    private static final double MIN_TERRAIN_HEIGHT = 0.01;
    private static final double STEP_SIZE = 0.01;
    private static final double MAX_SIM_TIME = 30.0;
    private static final double STOP_SPEED = 1e-2;
    private static final double MIN_PROGRESS = 0.25;
    private static final double ZERO_VELOCITY = 1e-6;
    private static final double SAME_POINT_EPSILON = 1e-6;
    private static final double[] FALLBACK_CELL_SIZES = {0.75, 1.0, 0.2, 0.3, 0.4, 0.6};

    private final MLBot mlBot;
    private final double cellSize;
    private final double minAimDistance;
    private final double maxAimDistance;
    private final double directAimDistance;
    private final double waterSafetyMargin;

    private final double obstaclePadding;
    private final boolean fourConnected;

    public MazeMLBot() {
        this(new MLBot());
    }
    public MazeMLBot(MLBot mlBot) {
        this(
                mlBot,
                DEFAULT_CELL_SIZE,
                DEFAULT_MIN_AIM_DISTANCE,
                DEFAULT_MAX_AIM_DISTANCE,
                DEFAULT_DIRECT_AIM_DISTANCE,
                DEFAULT_WATER_SAFETY_MARGIN,
                DEFAULT_OBSTACLE_PADDING,
                DEFAULT_FOUR_CONNECTED);
    }

    public MazeMLBot(
            MLBot mlBot,
            double cellSize,
            double minAimDistance,
            double maxAimDistance,
            double directAimDistance,
            double waterSafetyMargin,
            double obstaclePadding,
            boolean fourConnected
    ) {
        this.mlBot = mlBot;
        this.cellSize = cellSize;
        this.minAimDistance = minAimDistance;
        this.maxAimDistance = maxAimDistance;
        this.directAimDistance = directAimDistance;
        this.waterSafetyMargin = waterSafetyMargin;
        this.obstaclePadding = obstaclePadding;
        this.fourConnected = fourConnected;
    }

    /**
     * Computes a shot by planning an aim point first, then letting MLBot solve
     * the actual velocity needed for that aim point. If the default grid gives
     * a shot that collides or does not improve the distance enough, the bot
     * retries with several fallback grid sizes and keeps the best candidate.
     *
     * @param course the current course, including target, terrain and obstacles
     * @param x      current ball x position
     * @param y      current ball y position
     * @return velocity as {vx, vy}; may still be imperfect on very dense mazes
     */
    @Override
    public double[] computeShot(Course course, double x, double y) {
        double targetX = course.getTargetX();
        double targetY = course.getTargetY();
        double startDistance = Math.hypot(x - targetX, y - targetY);

        ShotCandidate best = computeCandidate(course, x, y, targetX, targetY, cellSize);
        if (best.plainFallback) {
            return best.velocity;
        }
        if (isNearlyZero(best.velocity)) {
            return mlBot.computeShot(course, x, y);
        }
        if (best.isUsable(startDistance)) {
            return best.velocity;
        }

        for (double fallbackCellSize : FALLBACK_CELL_SIZES) {
            if (Math.abs(fallbackCellSize - cellSize) < 1e-9) {
                continue;
            }
            ShotCandidate candidate = computeCandidate(course, x, y, targetX, targetY, fallbackCellSize);
            if (candidate.plainFallback) {
                return candidate.velocity;
            }
            if (isNearlyZero(candidate.velocity)) {
                return mlBot.computeShot(course, x, y);
            }
            if (candidate.score() < best.score()) {
                best = candidate;
            }
            if (candidate.isUsable(startDistance)) {
                return candidate.velocity;
            }
        }

        return isNearlyZero(best.velocity) ? mlBot.computeShot(course, x, y) : best.velocity;
    }

    /**
     * Uses the bot's normal cell size to choose the next point to aim at.
     */
    private Vector2D chooseAimPoint(Course course, double x, double y, double targetX, double targetY) {
        return chooseAimPoint(course, x, y, targetX, targetY, cellSize);
    }

    /**
     * Builds the planning grids, runs A*, simplifies the resulting path and
     * selects the next reachable waypoint. The final target is used directly
     * only when the direct segment is short enough and clear.
     */
    private Vector2D chooseAimPoint(
            Course course,
            double x,
            double y,
            double targetX,
            double targetY,
            double gridCellSize
    ) {
        HeightFunction heightFunction = HeightFunction.compileExpression(course.getHeightProfile());
        // Planning grid: padded + water margin — used for A* and PathSimplifier
        GridMap planGrid = new GridMap(course, heightFunction, gridCellSize, waterSafetyMargin, obstaclePadding);
        // Aim grid: no water margin (allows shots over shallow shores), same tree
        // padding as planGrid so segment checks are consistent with A* routing.
        GridMap aimGrid = new GridMap(course, heightFunction, gridCellSize, 0.0, obstaclePadding);
        if (Math.hypot(x - targetX, y - targetY) <= directAimDistance
                && aimGrid.isSegmentFree(x, y, targetX, targetY, heightFunction, MIN_TERRAIN_HEIGHT)) {
            return new Vector2D(targetX, targetY);
        }
        List<Vector2D> path = AStarPathfinder.findPath(planGrid, x, y, targetX, targetY, fourConnected, heightFunction);
        if (path == null || path.isEmpty()) {
            return null;
        }
        List<Vector2D> simplified = PathSimplifier.simplify(planGrid, heightFunction, path);
        return WaypointSelector.select(
                aimGrid, heightFunction, simplified, path, x, y, targetX, targetY, minAimDistance, maxAimDistance);
    }

    /**
     * Builds one complete candidate for a given grid size: choose waypoint,
     * compute the physical ML shot toward it, then simulate that shot so the
     * fallback logic can compare candidates.
     */
    private ShotCandidate computeCandidate(
            Course course,
            double x,
            double y,
            double targetX,
            double targetY,
            double gridCellSize
    ) {
        Vector2D aim = chooseAimPoint(course, x, y, targetX, targetY, gridCellSize);
        // Defensive fallback for extreme mazes where no executable waypoint is found.
        if (aim == null || Math.hypot(aim.x() - x, aim.y() - y) < SAME_POINT_EPSILON) {
            return new ShotCandidate(mlBot.computeShot(course, x, y), null, true);
        }
        double[] velocity = computeMlShotAt(course, x, y, aim);
        ShotEvaluation evaluation = evaluateShot(course, x, y, velocity);
        return new ShotCandidate(velocity, evaluation, false);
    }

    /**
     * Reuses MLBot for an intermediate waypoint by temporarily replacing the
     * course target. The original target is restored in the finally block so
     * callers do not observe this internal retargeting trick.
     */
    private double[] computeMlShotAt(Course course, double x, double y, Vector2D target) {
        double savedTargetX = course.getTargetX();
        double savedTargetY = course.getTargetY();
        try {
            course.setTargetX(target.x());
            course.setTargetY(target.y());
            return mlBot.computeShot(course, x, y);
        } finally {
            course.setTargetX(savedTargetX);
            course.setTargetY(savedTargetY);
        }
    }

    /**
     * Simulates the candidate shot with the current physics and records whether
     * it hit water/tree, crossed the target radius, and how far it ended from
     * the real target. This is a planning check, not the full GUI game loop.
     */
    private ShotEvaluation evaluateShot(Course course, double x0, double y0, double[] velocity) {
        HeightFunction heightFunction = HeightFunction.compileExpression(course.getHeightProfile());
        BallMotionSystem system = new BallMotionSystem(
                heightFunction, course.getFrictionKinetic(), course.getFrictionStatic(), course.getSandpits());
        RungeKutta4 solver = new RungeKutta4(STEP_SIZE);

        double[] state = {x0, y0, velocity[0], velocity[1]};
        double t = 0.0;
        int maxSteps = (int) (MAX_SIM_TIME / STEP_SIZE);
        boolean hitObstacle = false;
        boolean reachedTarget = false;

        for (int i = 0; i < maxSteps; i++) {
            double x = state[0];
            double y = state[1];

            if (heightFunction.evaluate(x, y) < 0.0 || isInTree(course, x, y)) {
                hitObstacle = true;
                break;
            }
            if (Math.hypot(x - course.getTargetX(), y - course.getTargetY()) <= course.getTargetRadius()) {
                reachedTarget = true;
                break;
            }
            if (Math.hypot(state[2], state[3]) < STOP_SPEED) {
                break;
            }

            state = solver.nextStep(t, state, system);
            t += STEP_SIZE;
        }

        double finalDistance = Math.hypot(state[0] - course.getTargetX(), state[1] - course.getTargetY());
        return new ShotEvaluation(finalDistance, hitObstacle, reachedTarget);
    }

    private boolean isInTree(Course course, double x, double y) {
        if (course.getTrees() == null) {
            return false;
        }
        for (Course.Tree tree : course.getTrees()) {
            if (Math.hypot(x - tree.x, y - tree.y) <= tree.radius) {
                return true;
            }
        }
        return false;
    }

    private boolean isNearlyZero(double[] velocity) {
        return Math.hypot(velocity[0], velocity[1]) < ZERO_VELOCITY;
    }

    private record ShotCandidate(double[] velocity, ShotEvaluation evaluation, boolean plainFallback) {
        private boolean isUsable(double startDistance) {
            return evaluation.reachedTarget
                    || (!evaluation.hitObstacle && evaluation.finalDistance < startDistance - MIN_PROGRESS);
        }

        private double score() {
            return evaluation.finalDistance + (evaluation.hitObstacle ? 100.0 : 0.0);
        }
    }

    private record ShotEvaluation(double finalDistance, boolean hitObstacle, boolean reachedTarget) {
    }

}

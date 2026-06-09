package core.bots;

import java.util.ArrayList;
import java.util.List;

import core.Vector2D;
import core.course.Course;
import core.course.HeightFunction;
import core.course.PartialDerivative;
import solvers.RungeKutta4;
import systems.BallMotionSystem;

/**
 * Bot that searches over possible initial velocities using the course physics
 * as its scoring function. MazeMLBot reuses this class for intermediate
 * waypoints, so this bot is the local shot optimizer even when A* does the
 * global planning.
 */
public class MLBot implements Bot {

    private final int maxIterations;
    private final double initialStep;
    private final double minStep;
    private final double tolerance;
    public MLBot() {
        this(200, 0.5, 0.01, 0.02);
    }

    public MLBot(int maxIterations, double initialStep, double minStep, double tolerance) {
        this.maxIterations = maxIterations;
        this.initialStep = initialStep;
        this.minStep = minStep;
        this.tolerance = tolerance;
    }

    /**
     * Chooses a velocity by trying several rule-based starting guesses and then
     * doing a simple local search around the best one. The returned velocity is
     * clamped to the course maximum speed.
     */
    @Override
    public double[] computeShot(Course course, double x, double y) {
        HeightFunction h = HeightFunction.compileExpression(course.getHeightProfile());
        Vector2D target = new Vector2D(course.getTargetX(), course.getTargetY());
        Vector2D ball = new Vector2D(x, y);

        double kineticFriction = course.getFrictionKinetic();
        double vmax = course.getVMax();

        double w = 1.0;
        double k = 1.0;

        List<Vector2D> starts = new ArrayList<>();

        Vector2D ruleBased = computeInitialVelocityRuleBased(h, target, ball, kineticFriction, w, k);
        starts.add(clampToMaxSpeed(ruleBased, vmax));

        Vector2D toTarget = target.subtract(ball);
        Vector2D dir = toTarget.norm() < 1e-9 ? new Vector2D(0, 0) : toTarget.normalize();

        starts.add(clampToMaxSpeed(dir.multiply(0.25 * vmax), vmax));
        starts.add(clampToMaxSpeed(dir.multiply(0.50 * vmax), vmax));
        starts.add(clampToMaxSpeed(dir.multiply(0.75 * vmax), vmax));
        starts.add(clampToMaxSpeed(dir.multiply(1.00 * vmax), vmax));

        starts.add(clampToMaxSpeed(dir.rotate(Math.toRadians(15)).multiply(0.50 * vmax), vmax));
        starts.add(clampToMaxSpeed(dir.rotate(Math.toRadians(-15)).multiply(0.50 * vmax), vmax));
        starts.add(clampToMaxSpeed(dir.rotate(Math.toRadians(15)).multiply(0.75 * vmax), vmax));
        starts.add(clampToMaxSpeed(dir.rotate(Math.toRadians(-15)).multiply(0.75 * vmax), vmax));
        starts.add(clampToMaxSpeed(dir.rotate(Math.toRadians(30)).multiply(0.60 * vmax), vmax));
        starts.add(clampToMaxSpeed(dir.rotate(Math.toRadians(-30)).multiply(0.60 * vmax), vmax));

        double bestVx = 0.0;
        double bestVy = 0.0;
        double bestCost = Double.POSITIVE_INFINITY;

        for (Vector2D start : starts) {
            double cost = evaluateShot(h, course, x, y, start.x(), start.y());
            if (cost < bestCost) {
                bestCost = cost;
                bestVx = start.x();
                bestVy = start.y();
            }
        }

        double step = initialStep;
        double[][] directions = {{ 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 }, { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 }};

        for (int iter = 0; iter < maxIterations && step > minStep && bestCost > tolerance; iter++) {
            boolean improved = false;
            for (double[] d : directions) {
                double nvx = bestVx + d[0] * step;
                double nvy = bestVy + d[1] * step;
                double nspeed = Math.hypot(nvx, nvy);
                if (nspeed > vmax) {
                    nvx = (nvx / nspeed) * vmax;
                    nvy = (nvy / nspeed) * vmax;
                }
                double cost = evaluateShot(h, course, x, y, nvx, nvy);
                if (cost < bestCost) {
                    bestCost = cost;
                    bestVx = nvx;
                    bestVy = nvy;
                    improved = true;
                }
            }
            if (!improved) {
                step *= 0.5;}
        }

        return new double[]{bestVx, bestVy};
    }

    private Vector2D clampToMaxSpeed(Vector2D v, double vmax) {
        double speed = v.norm();
        if (speed <= vmax || speed < 1e-12) {
            return v;}
        return v.normalize().multiply(vmax);}

    /**
     * Simulates one candidate velocity and turns the trajectory into a cost.
     * Reaching the target radius is best; water and trees add penalties so the
     * search prefers safer shots when several end near the target.
     */
    private double evaluateShot(HeightFunction h, Course course, double x0, double y0, double vx0, double vy0) {
        BallMotionSystem system = new BallMotionSystem(h,course.getFrictionKinetic(),course.getFrictionStatic(),course.getSandpits());

        RungeKutta4 solver = new RungeKutta4(0.01);
        double[] state = new double[]{x0, y0, vx0, vy0};
        double t = 0.0;
        double maxSimTime = 10.0;
        int maxSteps = (int) (maxSimTime / solver.getStepSize());
        double speedThreshold = 1e-3;
        boolean inHole = false;
        boolean inWater = false;
        boolean inTree = false;

        for (int i = 0; i < maxSteps; i++) {
            double x = state[0];
            double y = state[1];
            double vx = state[2];
            double vy = state[3];

            double speed = Math.hypot(vx, vy);
            if (speed < speedThreshold) {
                break;
            }

            double height = h.evaluate(x, y);
            if (height < 0.0) {
                inWater = true;
                break;
            }

            if (course.getTrees() != null) {
                for (Course.Tree tree : course.getTrees()) {
                    double distToTree = Math.hypot(x - tree.x, y - tree.y);
                    if (distToTree <= tree.radius + 0.05) {
                        inTree = true;
                        break;
                    }}}

            if (inTree) {
                break;
            }

            double distToTarget = Math.hypot(x - course.getTargetX(), y - course.getTargetY());
            if (distToTarget <= course.getTargetRadius()) {
                inHole = true;
                break;}
        
            state = solver.nextStep(t, state, system);
            t += solver.getStepSize();
        }

        double finalX = state[0];
        double finalY = state[1];
        double dist = Math.hypot(finalX - course.getTargetX(), finalY - course.getTargetY());

        if (inHole) {
            return 0.0;
        }

        double cost = dist;
        if (inWater) {
            cost += 10.0;
        }
        if (inTree) {
            cost += 10.0;
        }
        return cost;
    }

    private Vector2D distance(Vector2D vect1, Vector2D vect2) {
        return vect1.subtract(vect2);}
    private Vector2D golfBreak(HeightFunction h, Vector2D vect1, Vector2D vect2, double x, double y) {
        double gx = PartialDerivative.partialX(h, x, y);
        double gy = PartialDerivative.partialY(h, x, y);
        Vector2D grad = new Vector2D(gx, gy);
        Vector2D dist = distance(vect1, vect2).normalize();
        double slope = grad.dot(dist);
        return new Vector2D(
                grad.x() - dist.x() * slope,
                grad.y() - dist.y() * slope);}
    private Vector2D aim(HeightFunction h, Vector2D target, Vector2D ball, double k) {
        Vector2D brk = golfBreak(h, target, ball, ball.x(), ball.y());
        Vector2D dist = distance(target, ball);
        return dist.subtract(brk.multiply(k)).normalize();
    }
    private double powerInput(Vector2D target, Vector2D ball) {
        Vector2D dist = distance(target, ball);
        return 0.8 * dist.norm() * dist.norm();
    }

    private double slopeCorrection(HeightFunction h, Vector2D target, Vector2D ball) {
        Vector2D dir = distance(target, ball).normalize();
        double gx = PartialDerivative.partialX(h, ball.x(), ball.y());
        double gy = PartialDerivative.partialY(h, ball.x(), ball.y());
        Vector2D grad = new Vector2D(gx, gy);
        return grad.dot(dir);
    }
    private double adjustedPower(HeightFunction h, Vector2D target, Vector2D ball, double w) {
        double slope = slopeCorrection(h, target, ball);
        double pow = powerInput(target, ball) + w * slope;
        if (slope < 0) {
            pow *= 0.9;
        } return pow;}

    private Vector2D computeInitialVelocityRuleBased(HeightFunction h, Vector2D target, Vector2D ball, double kineticFriction, double w, double k) {
        if (ball.subtract(target).norm() < 1e-9) {
            return new Vector2D(0, 0);
        }
        double rawPower = adjustedPower(h, target, ball, w);
        double hitPower = Math.max(0.2, Math.min(5.0, k * rawPower));
        Vector2D adjustedVelocity = aim(h, target, ball, k).multiply(hitPower * 1.5 * kineticFriction);

        if (adjustedVelocity.norm() > 5.0) {
            adjustedVelocity = adjustedVelocity.normalize().multiply(5.0);
        }
        return adjustedVelocity;
    }
}

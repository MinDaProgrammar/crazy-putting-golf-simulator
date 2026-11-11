package core.bots;


import systems.BallMotionSystem;
import core.Vector2D;
import core.course.Course;
import core.course.HeightFunction;
import core.course.PartialDerivative;
import solvers.RungeKutta4;

public class MLBotPrevious implements Bot {

    private final int maxIterations;
    private final double initialStep;
    private final double minStep;
    private final double tolerance;

    public MLBotPrevious() {
        this(200, 0.5, 0.01, 0.02);
    }

    public MLBotPrevious(int maxIterations, double initialStep, double minStep, double tolerance) {
        this.maxIterations = maxIterations;
        this.initialStep = initialStep;
        this.minStep = minStep;
        this.tolerance = tolerance;
    }

    @Override
    public double[] computeShot(Course course, double x, double y) {
        HeightFunction h = HeightFunction.compileExpression(course.getHeightProfile());

        Vector2D target = new Vector2D(course.getTargetX(), course.getTargetY());
        double kineticFriction = course.getFrictionKinetic();
        double vmax = course.getVMax();

        Vector2D ball = new Vector2D(x, y);

        double w = 1.0;
        double k = 1.0;
        Vector2D v0 = computeInitialVelocityRuleBased(
                h, target, ball, kineticFriction, w, k);
        double vx = v0.x();
        double vy = v0.y();
        double speed = Math.sqrt(vx * vx + vy * vy);
        if (speed > vmax) {
            vx *= vmax / speed;
            vy *= vmax / speed;
        }

        double step = initialStep;
        double bestCost = evaluateShot(h, course, x, y, vx, vy);

        for (int iter = 0; iter < maxIterations && step > minStep && bestCost > tolerance; iter++) {
            boolean improved = false;
            double[][] directions = {
                    { 1, 0 }, {-1, 0 },
                    { 0, 1 }, { 0,-1 }, { 1, 1 }, { 1,-1 },
                    {-1, 1 }, {-1,-1 }
            };

            for (double[] d : directions) {
                double nvx = vx + d[0] * step;
                double nvy = vy + d[1] * step;
                double nspeed = Math.sqrt(nvx * nvx + nvy * nvy);
                if (nspeed > vmax) {
                    nvx *= vmax/  nspeed;
                    nvy *= vmax /nspeed;}

                double cost = evaluateShot(h, course, x, y, nvx, nvy);

                if (cost < bestCost) {
                    bestCost = cost;
                    vx = nvx;
                    vy = nvy;
                    improved = true;
                }
            }




            if (!improved) {
                step *= 0.5;
            }
        }

        return new double[] { vx, vy };
    }

    private double evaluateShot(HeightFunction h, Course course, double x0, double y0, double vx0, double vy0) {
        BallMotionSystem system = new BallMotionSystem(h, course.getFrictionKinetic(), course.getFrictionStatic(), course.getSandpits());
        RungeKutta4 solver = new RungeKutta4(0.01);
        double[] state = new double[] { x0, y0, vx0, vy0 };
        double t = 0.0;
        double maxSimTime = 10.0;
        int maxSteps = (int)(maxSimTime / solver.getStepSize());
        double speedThreshold = 1e-3;

        boolean inHole = false;
        boolean inWater = false;

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
            } double dxTarget = x - course.getTargetX();
            double dyTarget = y - course.getTargetY();
            double distToTarget = Math.hypot(dxTarget, dyTarget);
            if (distToTarget <= course.getTargetRadius()) {
                inHole = true;
                break;
            }

            state = solver.nextStep(t, state, system);
            t += solver.getStepSize();
        }

        double finalDx = state[0] - course.getTargetX();
        double finalDy = state[1] - course.getTargetY();
        double dist = Math.hypot(finalDx, finalDy);

        if (inHole) {
            return 0.0;
        }

        double cost = dist;
        if (inWater) {
            cost += 10.0;
        }
        return cost;
    }

    private Vector2D normalizedVector(Vector2D vect) {
        return vect.normalize();
    }

    private Vector2D distance(Vector2D vect1, Vector2D vect2) {
        return vect1.subtract(vect2);
    }

    private Vector2D golfBreak(HeightFunction h, Vector2D vect1, Vector2D vect2,
                               double x, double y) {
        double gx = PartialDerivative.partialX(h, x, y);
        double gy = PartialDerivative.partialY(h, x, y);
        Vector2D grad = new Vector2D(gx, gy);
        Vector2D dist = distance(vect1, vect2).normalize();
        double slope = grad.dot(dist);
        return new Vector2D(
                grad.x() - dist.x() * slope,
                grad.y() - dist.y() * slope
        );
    }

    private Vector2D aim(HeightFunction h, Vector2D target, Vector2D ball, double k) {
        Vector2D brk = golfBreak(h, target, ball, ball.x(), ball.y());
        Vector2D dist = distance(target, ball);
        return dist.subtract(brk.multiply(k)).normalize();
    }

    private double powerInput(Vector2D target, Vector2D ball) {
        Vector2D dist = distance(target, ball);
        return 0.8 * dist.norm() + dist.norm();
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
        }return pow;
    }
    private Vector2D computeInitialVelocityRuleBased(HeightFunction h, Vector2D target, Vector2D ball, double kineticFriction, double w, double k) {
        if (ball.subtract(target).norm() < 1e-9) {
            return new Vector2D(0, 0);}
        double rawPower = adjustedPower(h, target, ball, w);
        double hitPower = Math.max(0.2, Math.min(5.0 / k, rawPower));

        Vector2D adjustedVelocity =
                aim(h, target, ball, k).multiply(hitPower + 1.5 * kineticFriction);

        if (adjustedVelocity.norm() > 5.0) {
            adjustedVelocity = adjustedVelocity.normalize().multiply(5.0);
        }

        return adjustedVelocity;
    }
}
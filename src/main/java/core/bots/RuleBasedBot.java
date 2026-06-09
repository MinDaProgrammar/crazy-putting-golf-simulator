package core.bots;


import core.Vector2D;
import core.course.Course;
import core.course.HeightFunction;
import core.course.HeightParser;
import core.course.PartialDerivative;
import systems.BallMotionSystem;


/**
 * Represents a rule‑based bot that computes an initial velocity vector for a golf ball
 * so that it reaches the target (hole‑in‑one) based on:
 * <ul>
 *     <li>the geometry of the terrain (height function)</li>
 *     <li>the ball's current position</li>
 *     <li>the target position</li>
 *     <li>slope effects and friction</li>
 * </ul>
 *
 * <p>The bot uses gradient‑based break estimation and simple physical heuristics
 * to adjust both direction and power.</p>
 */
public class RuleBasedBot implements Bot {

    @Override
    public double[] computeShot(Course course, double x, double y) {
        HeightFunction hf = HeightParser.parse(course.getHeightProfile());
        Vector2D vel = computeInitialVelocity(hf,
            new Vector2D(course.getTargetX(), course.getTargetY()),
            new Vector2D(x, y),
            course.getFrictionKinetic(), 1.0);
        return new double[]{ vel.x(), vel.y() };
    }

     /**
     * Computes the vector difference {@code vect1 - vect2}.
     *
     * @param vect1 the first vector
     * @param vect2 the second vector
     * @return the displacement from {@code vect2} to {@code vect1}
     */
    public static Vector2D distance(Vector2D vect1, Vector2D vect2) {
        return vect1.subtract(vect2);
    }

    /**
     * Computes the lateral break caused by terrain slope.
     *
     * <p>The break is computed by projecting the gradient onto the direction of travel
     * and removing the component parallel to the shot direction, leaving only the
     * sideways influence.</p>
     *
     * @param h     the height function of the terrain
     * @param target the target position
     * @param ball the ball position
     * @return a vector representing the sideways break effect
     */
    public static Vector2D golfBreak(HeightFunction h, Vector2D target, Vector2D ball) {

        double x = ball.x();
        double y = ball.y();
        Vector2D grad = PartialDerivative.gradient(h, x, y).multiply(-1);
        Vector2D dist = distance(target, ball).normalize();
        //this gradient at the ball position minus its projection onto the direction ball--->target
        //that is the sideway component of the acceleration due to the slope
        return grad.subtract(grad.projectionTo(dist));

    }

    /**
     * Computes the aiming direction by compensating for terrain break.
     *
     * @param h      the height function
     * @param target the target position
     * @param ball   the ball position
     * @param k      break compensation factor
     * @return a normalized direction vector toward the compensated aim point
     */
    public static Vector2D aim(HeightFunction h, Vector2D target, Vector2D ball, double k) {
        Vector2D brk = golfBreak(h, target, ball);
        Vector2D dist = distance(target, ball);
        return dist.subtract(brk.multiply(k)).normalize();

    }

    /**
     * Computes a base power estimate based purely on distance.
     *
     * @param target the target position
     * @param ball   the ball position
     * @return the raw power estimate
     */
    public static double powerInput(HeightFunction h, Vector2D target, Vector2D ball, double k_friction) {
        Vector2D dist = distance(target, ball);
        double gravity= 9.81;
        double grad = PartialDerivative.gradient(h, ball.x(), ball.y()).norm();
        return Math.sqrt(2.0 * gravity * (k_friction + grad)*dist.norm());
    }

    /**
     * Computes the slope correction term by projecting the terrain gradient
     * onto the direction of the shot.
     *
     * @param h      the height function
     * @param target the target position
     * @param ball   the ball position
     * @return positive for uphill, negative for downhill
     */
    public static double slopeCorrection(HeightFunction h, Vector2D target, Vector2D ball) {
        Vector2D dir = distance(target, ball).normalize();
        Vector2D grad = PartialDerivative.gradient(h, ball.x(), ball.y(), 0.2);
        return grad.dot(dir);
    }

    /**
     * Computes the final adjusted power, including slope effects.
     *
     * <p>Downhill shots receive a 10% reduction.</p>
     *
     * @param h      the height function
     * @param target the target position
     * @param ball   the ball position
     * @param w      slope weight factor
     * @return the adjusted power value
     */
    public static double adjustedPower(HeightFunction h, Vector2D target, Vector2D ball, double w, double k_friction) {
        double slope = slopeCorrection(h, target, ball);

        double pow = powerInput(h, target, ball,  k_friction) ;
        return pow + (w * slope);
    }

    /**
     * Computes the initial velocity vector for the shot.
     *
     * <p>This method:</p>
     * <ol>
     *     <li>computes slope‑adjusted power</li>
     *     <li>clamps the power to allowed limits</li>
     *     <li>computes the aim direction with break compensation</li>
     *     <li>adds friction compensation</li>
     *     <li>clamps the final velocity to the maximum allowed speed</li>
     * </ol>
     *
     * @param h                the height function
     * @param target           the target position
     * @param ball             the ball position
     * @param kinetic_friction friction coefficient
     * @param w                slope weight factor
     * @return the initial velocity vector to apply
     */
    public static Vector2D computeInitialVelocity(HeightFunction h, Vector2D target, Vector2D ball,
                                                  double kinetic_friction, double w) {

        double dist = target.subtract(ball).norm();

        // If ball is already at target
        if (dist < 1e-9) {
            return new Vector2D(0, 0);
        }

        // Break compensation factor

        double c = 3.7776; // tuning constant works for most terrains
        double k = dist * dist  / c;
        // Compute slope-adjusted power
        double hitPower = adjustedPower(h, target, ball, w, kinetic_friction);

        // Compute aim direction with break compensation
        Vector2D aiming = aim(h, target, ball, k);
        // Final velocity
        Vector2D adjustedVelocity = aiming.multiply(hitPower);
        // Clamp max speed
        if (adjustedVelocity.norm() > 5.0) {
            adjustedVelocity = adjustedVelocity.normalize().multiply(5.0);
        }
        
        return adjustedVelocity;
    }

}
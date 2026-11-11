package systems;

import core.ODESystem;
import core.course.HeightFunction;
import core.course.PartialDerivative;
import core.course.Course.Sandpit;

import java.util.List;       

/**
 * Models the physics of a golf ball moving across a sloped terrain.
 * Implements the ODE system for the state vector [x, y, vx, vy] using
 * gravity and friction forces derived from the terrain height function.
 *
 * When the ball is moving, kinetic friction opposes the velocity direction.
 * When the ball is stationary, static friction prevents motion unless the
 * slope force exceeds the static friction threshold.
 */


public class BallMotionSystem implements ODESystem {

    private static final double GRAVITY = 9.81;
    private static final double SPEED_EPSILON = 1e-3;  // 1 cm/s — below this the ball is treated as at rest and static friction is applied
    private static final double SLOPE_EPSILON = 1e-8;

    private final HeightFunction heightFunction;
    private double muK; // Acts as the default/grass friction
    private double muS;
    private List<Sandpit> sandpits; // for each sandpit it might have different friction

    public BallMotionSystem(HeightFunction heightFunction, double muK, double muS, List<Sandpit> sandpits) {
        if (heightFunction == null) {
            throw new IllegalArgumentException("Height function cannot be null.");
        }
        if (muK < 0.0) {
            throw new IllegalArgumentException("Kinetic friction coefficient must be non-negative.");
        }
        if (muS < muK) {
            throw new IllegalArgumentException("Static friction coefficient must be >= kinetic friction coefficient.");
        }

        this.heightFunction = heightFunction;
        this.muK = muK;
        this.muS = muS;
        this.sandpits = sandpits; 
    }

    // Based on the ball's location, decided whether to use standard or sand pit friction
    private double getKineticFrictionAt(double x, double y) {
        if (sandpits != null) {
            for (Sandpit pit : sandpits) {
                // If the ball is inside the sandpit radius, return the sand friction
                if (Math.hypot(x - pit.x, y - pit.y) <= pit.radius) {
                    return pit.kineticFriction;
                }
            }
        }
        return muK; // Otherwise, return default course friction
    }

    private double getStaticFrictionAt(double x, double y) {
    if (sandpits != null) {
        for (Sandpit pit : sandpits) {
            if (Math.hypot(x - pit.x, y - pit.y) <= pit.radius) {
                return pit.staticFriction;
            }
        }
    }
    return muS;
}

    @Override
    public double[] getDerivatives(double t, double[] state) {
        validateState(state);

        double x  = state[0];
        double y  = state[1];
        double vx = state[2];
        double vy = state[3];

        double hx = PartialDerivative.partialX(heightFunction, x, y);
        double hy = PartialDerivative.partialY(heightFunction, x, y);

        double[] acceleration = computeAcceleration(x, y, hx, hy, vx, vy);
        
        return new double[] { vx, vy, acceleration[0], acceleration[1] };
    }

    private double[] computeAcceleration(double x, double y, double hx, double hy, double vx, double vy) {

        // --- Downhill acceleration (gravity projected onto surface) ---
        double downhillAx = -GRAVITY * hx;
        double downhillAy = -GRAVITY * hy;
        double speed = Math.hypot(vx, vy); 

        double slopeMagnitude = Math.hypot(hx, hy);

        double currentMuK = getKineticFrictionAt(x, y);
        double currentMuS = getStaticFrictionAt(x, y);

        // Ball is moving: kinetic friction opposes velocity
        if(speed > SPEED_EPSILON) {
            double frictionAx = -currentMuK * GRAVITY * (vx / speed);
            double frictionAy = -currentMuK * GRAVITY * (vy / speed);

            return new double[] {
                    downhillAx + frictionAx,
                    downhillAy + frictionAy
            };
        }

        // ===============================================================
        // CASE 2 — BALL IS AT REST → static friction may hold it
        // ===============================================================

        // perfectly flat → no motion
        if (slopeMagnitude < SLOPE_EPSILON) {
            return new double[] {0.0, 0.0};
        }

        // Static friction is strong enough to hold the ball in place
        if (slopeMagnitude <= currentMuS) {
        return new double[] {0.0, 0.0};
        }

        double frictionAx = currentMuK * GRAVITY * (hx / slopeMagnitude);
        double frictionAy = currentMuK * GRAVITY * (hy / slopeMagnitude);

        return new double[] {
                downhillAx + frictionAx,
                downhillAy + frictionAy
        };
    }

    @Override
    public String[] getVariableNames() {
        return new String[] { "x", "y", "vx", "vy" };
    }

    @Override
    public String[] getParameterNames() {
        return new String[] { "muK", "muS" };
    }

    @Override
    public void setParameter(String name, double value) {
        switch (name) {
            case "muK":
                if (value < 0.0) {
                    throw new IllegalArgumentException("Kinetic friction coefficient must be non-negative.");
                }
                if (value > muS) {
                    throw new IllegalArgumentException("Kinetic friction cannot exceed static friction.");
                }
                this.muK = value;
                break;

            case "muS":
                if (value < 0.0) {
                    throw new IllegalArgumentException("Static friction coefficient must be non-negative.");
                }
                if (value < muK) {
                    throw new IllegalArgumentException("Static friction must be >= kinetic friction.");
                }
                this.muS = value;
                break;

            default:
                throw new IllegalArgumentException("Unknown parameter: " + name);
        }
    }



      /**
     * Validates that the state vector is non-null and has exactly 4 elements.
     */
    private void validateState(double[] state) {
        if (state == null) {
            throw new IllegalArgumentException("State cannot be null.");
        }
        if (state.length != 4) {
            throw new IllegalArgumentException("BallMotionSystem needs a 4D state vector: [x, y, vx, vy].");
        }
    }
}
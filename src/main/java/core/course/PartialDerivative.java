package core.course;

import core.Vector2D;

public class PartialDerivative {

    /*
    This value is small enough for smooth course functions, but not
    too small that. floating-point starts dominating immediately
    */
    private static final double DEFAULT_STEP = 1e-5;

    private PartialDerivative() {}

    public static double partialX(HeightFunction heightFunction, double x, double y) {
        return partialX(heightFunction, x, y, DEFAULT_STEP);
    }

    public static double partialY(HeightFunction heightFunction, double x, double y) {
        return partialY(heightFunction, x, y, DEFAULT_STEP);
    }

    public static double partialX(HeightFunction heightFunction, double x, double y, double step) {
        validateInputs(heightFunction, step);

        double forward = heightFunction.evaluate(x + step, y);
        double backward = heightFunction.evaluate(x - step, y);

        return (forward - backward) / (2.0 * step);
    }

    public static double partialY(HeightFunction heightFunction, double x, double y, double step) {
        validateInputs(heightFunction, step);

        double forward = heightFunction.evaluate(x, y + step);
        double backward = heightFunction.evaluate(x, y - step);

        return (forward - backward) / (2.0 * step);
    }

    private static void validateInputs(HeightFunction heightFunction, double step) {
        if(heightFunction == null) {
            throw new IllegalArgumentException("Height function cannot be null.");
        }

        if(step <= 0.0) {
            throw new IllegalArgumentException("Derivative step should be positive.");
        }
    }

    public static Vector2D gradient(HeightFunction heightFunction, double x, double y, double step) {
        return new Vector2D(partialX(heightFunction, x, y, step), partialY(heightFunction, x, y, step));
    }

    public static Vector2D gradient(HeightFunction heightFunction, double x, double y) {
        return new Vector2D(partialX(heightFunction, x, y), partialY(heightFunction, x, y));
    }
}
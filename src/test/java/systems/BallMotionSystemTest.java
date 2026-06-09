package systems;

import core.course.Course;
import core.course.HeightFunction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BallMotionSystemTest {

    private static final double G = 9.81;

    @Test
    void flatTerrainWithoutFrictionKeepsVelocityConstant() {
        BallMotionSystem system = new BallMotionSystem((x, y) -> 0.0, 0.0, 0.0, List.of());

        double[] derivatives = system.getDerivatives(0.0, new double[] {0.0, 0.0, 2.0, -1.0});

        assertArrayEquals(new double[] {2.0, -1.0, 0.0, 0.0}, derivatives, 1e-10);
    }

    @Test
    void kineticFrictionOpposesMotionOnFlatTerrain() {
        double muK = 0.1;
        BallMotionSystem system = new BallMotionSystem((x, y) -> 0.0, muK, 0.2, List.of());

        double[] derivatives = system.getDerivatives(0.0, new double[] {0.0, 0.0, 3.0, 4.0});

        assertEquals(3.0, derivatives[0], 1e-12);
        assertEquals(4.0, derivatives[1], 1e-12);
        assertEquals(-muK * G * 3.0 / 5.0, derivatives[2], 1e-10);
        assertEquals(-muK * G * 4.0 / 5.0, derivatives[3], 1e-10);
    }

    @Test
    void staticFrictionHoldsRestingBallOnGentleSlope() {
        HeightFunction gentleSlope = (x, y) -> 0.05 * x;
        BallMotionSystem system = new BallMotionSystem(gentleSlope, 0.05, 0.2, List.of());

        double[] derivatives = system.getDerivatives(0.0, new double[] {0.0, 0.0, 0.0, 0.0});

        assertArrayEquals(new double[] {0.0, 0.0, 0.0, 0.0}, derivatives, 1e-8);
    }

    @Test
    void restingBallStartsSlidingWhenSlopeExceedsStaticFriction() {
        HeightFunction steepSlope = (x, y) -> 0.3 * x;
        BallMotionSystem system = new BallMotionSystem(steepSlope, 0.05, 0.1, List.of());

        double[] derivatives = system.getDerivatives(0.0, new double[] {0.0, 0.0, 0.0, 0.0});

        assertEquals(0.0, derivatives[0], 1e-12);
        assertEquals(0.0, derivatives[1], 1e-12);
        assertTrue(derivatives[2] < -2.0, "Ball should accelerate downhill in the negative x direction");
        assertEquals(0.0, derivatives[3], 1e-8);
    }

    @Test
    void sandpitFrictionIsUsedInsideSandpit() {
        Course.Sandpit sandpit = new Course.Sandpit(0.0, 0.0, 2.0, 0.4, 0.5);
        BallMotionSystem system = new BallMotionSystem((x, y) -> 0.0, 0.05, 0.2, List.of(sandpit));

        double[] derivatives = system.getDerivatives(0.0, new double[] {0.0, 0.0, 1.0, 0.0});

        assertEquals(-0.4 * G, derivatives[2], 1e-10);
        assertEquals(0.0, derivatives[3], 1e-10);
    }
}

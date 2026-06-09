package solvers;

import core.ODESolver;
import core.ODESystem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ODESolverTest {

    @Test
    void eulerAdvancesArbitraryDimensionalState() {
        ODESystem system = new ODESystem() {
            @Override
            public double[] getDerivatives(double t, double[] state) {
                return new double[] {1.0, -2.0, 0.5};
            }

            @Override
            public String[] getParameterNames() {
                return new String[0];
            }

            @Override
            public String[] getVariableNames() {
                return new String[] {"x", "y", "z"};
            }

            @Override
            public void setParameter(String name, double value) {
                throw new IllegalArgumentException("No parameters");
            }
        };

        ODESolver solver = new EulerSolver(0.25);
        double[] next = solver.nextStep(0.0, new double[] {2.0, 4.0, -1.0}, system);

        assertArrayEquals(new double[] {2.25, 3.5, -0.875}, next, 1e-12);
    }

    @Test
    void rk4IsMoreAccurateThanEulerForExponentialGrowth() {
        ODESystem exponential = new ODESystem() {
            @Override
            public double[] getDerivatives(double t, double[] state) {
                return new double[] {state[0]};
            }

            @Override
            public String[] getParameterNames() {
                return new String[0];
            }

            @Override
            public String[] getVariableNames() {
                return new String[] {"y"};
            }

            @Override
            public void setParameter(String name, double value) {
                throw new IllegalArgumentException("No parameters");
            }
        };

        double eulerValue = integrate(new EulerSolver(0.1), exponential);
        double rk4Value = integrate(new RungeKutta4(0.1), exponential);
        double exact = Math.exp(1.0);

        double eulerError = Math.abs(eulerValue - exact);
        double rk4Error = Math.abs(rk4Value - exact);

        assertTrue(rk4Error < 1e-5, "RK4 should closely match the analytical solution");
        assertTrue(rk4Error < eulerError / 1000.0, "RK4 should be much more accurate than Euler");
    }

    private static double integrate(ODESolver solver, ODESystem system) {
        double t = 0.0;
        double[] state = {1.0};
        for (int i = 0; i < 10; i++) {
            state = solver.nextStep(t, state, system);
            t += solver.getStepSize();
        }
        return state[0];
    }
}

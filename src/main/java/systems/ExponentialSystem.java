package systems;

import core.ODESystem;

/**
 * System representing y' = y, used for accuracy benchmarks.
 * Analytical solution: y(t) = y(0) * e^t
 */
public class ExponentialSystem implements ODESystem {

    @Override
    public double[] getDerivatives(double t, double[] state) {
        return new double[] { state[0] };
    }

    @Override
    public String[] getVariableNames() {
        return new String[] { "y" };
    }

    @Override
    public String[] getParameterNames() {
        return new String[0]; // No parameters needed for this test system
    }

    @Override
    public void setParameter(String name, double value) {
        // Not needed for the benchmark, but must be implemented
    }
}
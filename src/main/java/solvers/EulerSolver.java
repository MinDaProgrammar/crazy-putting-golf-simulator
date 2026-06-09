package solvers;

import core.ODESolver;
import core.ODESystem;

/**
 * Implements the Euler method: y(t+h) = y(t) + h * f(t, y(t))
 * Simple but accumulates error over time, use small step sizes for accuracy.
 */

public class EulerSolver extends ODESolver {

    
    public EulerSolver(double stepSize) {
        super(stepSize);
        if (stepSize <= 0) { throw new IllegalArgumentException("Step size must be positive."); }
    }

    /**
     * Computes one Euler step from the current state.
     * @param t            the current time in the simulation
     * @param currentState the state vector holding all variable values at time t
     * @param system       the ODE system that provides the derivative function f(t, y)
     * @return             the new state vector at time t + h
     */

    @Override
    public double[] nextStep(double t, double[] currentState, ODESystem system) {
        if (currentState == null) { throw new IllegalArgumentException("State cannot be null."); }
        if (system == null) { throw new IllegalArgumentException("ODESystem cannot be null."); }

        double[] derivative = system.getDerivatives(t, currentState);

        if (derivative == null || derivative.length != currentState.length) { throw new IllegalArgumentException("Derivative must match the state dimension."); }

        double[] nextState = new double[currentState.length];

        for (int i = 0; i < currentState.length; i++) {
            // Euler update: nextState[i] = state[i] + h * derivative[i]
            nextState[i] = currentState[i] + stepSize * derivative[i];
        }

        return nextState;
    }




    /**
     * Runs the full integration over a given number of steps.
     * @param t0           the starting time
     * @param initialState the state vector at time t0
     * @param steps        the number of steps to perform
     * @param system       the ODE system to integrate
     * @return             2D array [steps+1][stateDimension], index 0 is the initial state
     */
    public double[][] solve(double t0, double[] initialState, int steps, ODESystem system) {
        if (initialState == null) { throw new IllegalArgumentException("Initial state cannot be null."); }
        if (steps < 0) { throw new IllegalArgumentException("Number of steps cannot be negative."); }
        if (system == null) { throw new IllegalArgumentException("ODESystem cannot be null."); }

        double[][] states = new double[steps + 1][initialState.length];

        double[] currentState = new double[initialState.length];
        for (int i = 0; i < initialState.length; i++) {
            currentState[i] = initialState[i];
            states[0][i] = initialState[i];
        }

        double t = t0;

        for (int s = 1; s <= steps; s++) {
            double[] nextState = nextStep(t, currentState, system);
            states[s] = nextState;
            currentState = nextState;
            t += stepSize;
        }

        return states;
    }
}
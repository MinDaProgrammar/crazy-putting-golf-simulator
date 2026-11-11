package solvers;

import core.ODESolver;
import core.ODESystem;

/**
 * Implements the fourth-order Runge-Kutta method (RK4) for solving ODE systems.
 * More accurate than Euler by computing four slope estimates per step and combining them:
 * y(t+h) = y(t) + (h/6) * (k1 + 2*k2 + 2*k3 + k4)
 */

public class RungeKutta4 extends ODESolver {

    public RungeKutta4(double stepSize) {
        
        super(stepSize);

        if (stepSize <= 0) { throw new IllegalArgumentException("Step size must be positive."); }
    }

     /**
     * Computes one RK4 step from the current state.
     * @param t            the current time in the simulation
     * @param currentState the state vector holding all variable values at time t
     * @param system       the ODE system that provides the derivative function f(t, y)
     * @return             the new state vector at time t + h
     */

    @Override
    public double[] nextStep(double t, double[] currentState, ODESystem system) {

        if (currentState == null) { throw new IllegalArgumentException("State cannot be null."); }
        if (system == null) { throw new IllegalArgumentException("ODESystem cannot be null."); }

        int n = currentState.length;

        double h = this.stepSize;
        double halfH = h / 2.0;  //I compute it in the constructor, since it remains unchanged

        // Arrays to hold intermediate states and result
        double[] tempState = new double[n];
        double[] nextState = new double[n];

        // Calculating k1 (slope)

        double[] k1 = system.getDerivatives(t, currentState);
        //Calculate k2 (slope at the midpoint)
        for (int i = 0; i < n; i++) {
            tempState[i] = currentState[i] + halfH * k1[i];
        }
        double[] k2 = system.getDerivatives(t + halfH, tempState);

        // Calculate k3 
        for (int i = 0; i < n; i++) {
            tempState[i] = currentState[i] + halfH * k2[i];

        }
        double[] k3 = system.getDerivatives(t + halfH, tempState);

        //Calculating k4 (slope at the end)
        for (int i = 0; i < n; i++) {
            tempState[i] = currentState[i] + h * k3[i];
        }
        double[] k4 = system.getDerivatives(t + h, tempState);

        // Combine k1, k2, k3, and k4 to calculate the next state
        for (int i = 0; i < n; i++) {
        nextState[i] = currentState[i] + (h / 6.0) * (k1[i] + 2 * k2[i] + 2 * k3[i] + k4[i]);
        }
        return nextState;
    }
}

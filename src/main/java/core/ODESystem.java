package core;

/**
 * Represents a system of Ordinary Differential Equations (ODEs).
 * 
 * <p>This interface defines the structure required by numerical ODE solvers
 * (Euler, Runge–Kutta, etc.) to evaluate the derivatives of a dynamical system.
 * Implementations provide the specific equations, parameter handling, and
 * variable naming used for visualization or user interaction.</p>
 *
 * <p>Example: for a system y' = f(t, y), the solver repeatedly calls
 * {@link #getDerivatives(double, double[])} to compute f(t, y).</p>
 */
public interface ODESystem {

    /**
     * Computes the derivatives of the system at a given time and state.
     *
     * <p>For a system of n variables, the returned array must also have length n.
     * The solver uses this method to advance the solution in time.</p>
     *
     * @param t     the current time
     * @param state the current state vector y(t)
     * @return an array containing the derivatives dy/dt evaluated at (t, state)
     */
    double[] getDerivatives(double t, double[] state);

    /**
     * Returns the names of the system parameters.
     *
     * <p>Used for GUI or visualization components that allow users to modify
     * system parameters dynamically.</p>
     *
     * @return an array of parameter names
     */
    String[] getParameterNames();

    /**
     * Returns the names of the system variables.
     *
     * <p>Used for labeling plots, tables, or UI elements that display the
     * evolution of the system variables.</p>
     *
     * @return an array of variable names
     */
    String[] getVariableNames();

    /**
     * Sets the value of a system parameter.
     *
     * <p>Implementations should match the parameter name to the appropriate
     * internal variable and update its value.</p>
     *
     * @param name  the name of the parameter to modify
     * @param value the new value of the parameter
     */
    void setParameter(String name, double value);
}

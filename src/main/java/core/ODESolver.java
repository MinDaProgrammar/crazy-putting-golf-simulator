package core;

/**
 * Base abstract class for ODE Solvers (Euler's, Ruge Kutta ,etc)
 * Provides setStepsize, getStepSize and nextStep methods
 * Subclasses must implement nextStep method 
 */
public abstract class ODESolver {
    protected double stepSize;

    public ODESolver(double stepSize) {
        this.stepSize = stepSize;
    }

    
    /**
     * Calculates the next state vector after one time step.
     * @param t the time step 
     * @param currentState an array of state variable 
     * @param system the ODE system to be solved
     * 
     * @return the state vector after advancing one iteration
     */
    public abstract double[] nextStep(double t, double[] currentState, ODESystem system);

    // Getter and setter for stepSize
    public void setStepSize(double h) { this.stepSize = h; }
    public double getStepSize() { return stepSize; }
}
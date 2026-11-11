package systems;

import core.ODESystem;

/**
 * ODE system representing the Lotka-Volterra predator-prey model.
 * Models the population dynamics between a prey species x and a predator species y.
 *
 * dx/dt = alpha*x - beta*x*y
 * dy/dt = delta*x*y - gamma*y
 *
 * All parameters must be positive.
 */

public class LotkaVolterra implements ODESystem {

    private double alpha;  // prey natural growth rate
    private double beta;   // prey death rate due to predation
    private double delta;  // predator growth rate from eating prey
    private double gamma;  // predator natural death rate

    public LotkaVolterra(double alpha, double beta, double gamma, double delta) {
        this.alpha = alpha;
        this.beta  = beta;
        this.gamma = gamma;
        this.delta = delta;
    }

    @Override
    public double[] getDerivatives(double t, double[] state) {
        double x = state[0]; // prey
        double y = state[1]; // predators
        
        double dxdt = alpha * x - beta * x * y;
        double dydt = delta * x * y - gamma * y;

        return new double[] { dxdt, dydt };
    }
 
    @Override
    public String[] getVariableNames() {
        return new String[] { "prey","predators" };
    }

    @Override
    public void setParameter(String name, double value) {
        switch (name) {
            case "alpha":
                alpha = value;
                break;
            case "beta":
                beta = value;
                break;
            case "gamma":
                gamma = value;
                break;
            case "delta":
                delta = value;
                break;
            default: throw new IllegalArgumentException("Unknown parameter: " + name);
        }
    }

    @Override
    public String[] getParameterNames() {
        return new String[] { "alpha", "beta", "gamma", "delta" };
    }
}

package systems;

import core.ODESystem;

/**
 * ODE system representing the FitzHugh-Nagumo model for a spiking neuron.
 * Models excitable dynamics with a fast voltage variable and a slow recovery variable.
 *
 * dV/dt = V - (V^3 / 3) - W + I
 * dW/dt = epsilon * (V + alpha - beta * W)
 *
 */

public class FitzHughNagumo implements ODESystem {

    private  double alpha; // Baseline shift of recovery dynamics
    private  double beta; // Strength of recovery feedback
    private  double epsilon; // Timescale separation
    private  double I; // External applied voltage

    public FitzHughNagumo(double alpha, double beta, double epsilon, double I) {
        this.alpha = alpha;
        this.beta = beta;
        this.epsilon = epsilon;
        this.I = I;
    }

    
       /**
     * Computes the derivatives [dV/dt, dW/dt] from the current state.
     * @param t     the current time
     * @param state the current state vector [voltage, recovery]
     * @return      the derivative vector [dV/dt, dW/dt]
     */
    @Override
    public double[] getDerivatives(double t, double[] state) {
        double voltage = state[0];
        double recovery = state[1];

        double dVdt = voltage - ((voltage * voltage * voltage)/3) - recovery + I;
        double dWdt = epsilon * (voltage + alpha - beta * recovery);
        
        return new double[] {dVdt, dWdt};
    }

    @Override
    public String[] getVariableNames() {
        return new String[] {"Voltage (x)", "RecoveryPotential(y)"};
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
            case "epsilon":
                epsilon = value;
                break;
            case "I":
                I = value;
                break;
            default: throw new IllegalArgumentException("Unknown parameter: " + name);
        }
    }

    @Override
    public String[] getParameterNames() {
        return new String[] { "alpha", "beta", "epsilon", "I" };
    }
}

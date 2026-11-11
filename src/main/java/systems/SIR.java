package systems;

import core.ODESystem;

/**
 * ODE system representing the SIR model for infectious disease spread.
 * Tracks three population fractions: susceptible S, infected I, and recovered R.
 *
 * dS/dt = -k*S*I + mu*(1 - S)
 * dI/dt =  k*S*I - (gamma + mu)*I
 * dR/dt =  gamma*I - mu*R
 *
 */

public class SIR implements ODESystem {

    private double k;
    private double mu;
    private double gamma;

    public SIR(double k, double mu, double gamma) {
        this.k = k; 
        this.mu = mu;
        this.gamma = gamma;
    }

    @Override
    public double[] getDerivatives(double t, double[] state) {
        double S = state[0];
        double I = state[1];
        double R = state[2];

        double dS = -k * S * I + mu*(1 - S);
        double dI =  k * S * I - (gamma + mu) * I;
        double dR =  gamma * I - mu * R;

        return new double[] { dS, dI, dR };
    }

    @Override
    public String[] getVariableNames() {
        return new String[] { "S", "I", "R" };
    }

    @Override
    public void setParameter(String name, double value) {
        switch (name) {
            case "k":
                k = value;
                break;
            case "mu":
                mu = value;
                break;
            case "gamma":
                gamma = value;
                break;
            default: throw new IllegalArgumentException("Unknown parameter: " + name);
        }
    }

    @Override
    public String[] getParameterNames() {
        return new String[] { "k", "mu", "gamma" };
    }
}

package experiments;

import core.ODESystem;
import core.ODESolver;
import solvers.EulerSolver; // Use Euler as specified in the image

public class UnitTest {
    public static void main(String[] args) {
        double h = 0.001;           // Time step h
        double t0 = 0.0;            // Initial time
        double tEnd = 10.0;         // Target time t 
        
        // Initial conditions x(0) = -0.1, y(0) = 0.5, z(0) = 0.2
        double[] initialState = {-0.1, 0.5, 0.2}; 

        ODESystem quizSystem = new ODESystem() {
            // Parameters: σ = 10, ρ = 28, β = 2.7
            private final double sigma = 10.0;
            private final double rho = 28.0;
            private final double beta = 2.7;

            @Override
            public double[] getDerivatives(double t, double[] state) {
                double x = state[0];
                double y = state[1];
                double z = state[2];
                
                // dx/dt = σ(y - x)
                double dxdt = sigma * (y - x);
                // dy/dt = x(ρ - z) - y
                double dydt = x * (rho - z) - y;
                // dz/dt = xy - βz
                double dzdt = (x * y) - (beta * z);

                return new double[] { dxdt, dydt, dzdt };
            }

            @Override
            public String[] getVariableNames() { return new String[]{"x", "y", "z"}; }
            @Override
            public String[] getParameterNames() { return new String[]{"sigma", "rho", "beta"}; }
            @Override
            public void setParameter(String name, double value) {}
        };

        ODESolver solver = new EulerSolver(h);

        double[] currentState = initialState;
        double t = t0;

        while (t < tEnd - (h / 2.0)) { // Small epsilon to avoid floating point overshoot
            currentState = solver.nextStep(t, currentState, quizSystem);
            t += h;
        }

        System.out.printf("Final Results at t = %.2f:%n", t);
        System.out.printf("Value of x(t): %.5f%n", currentState[0]);
        System.out.printf("Value of y(t): %.5f%n", currentState[1]);
        System.out.printf("Value of z(t): %.5f%n", currentState[2]);
    }
}
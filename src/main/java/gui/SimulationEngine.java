package gui;
import core.*;
import java.util.*;

/**
 * Runs an ODE simulation and collects the results at each time step.
 */
public class SimulationEngine {

      /**
     * Integrates the given ODE system forward in time from t=0 to maxT.
     * @param sys   the ODE system defining the derivatives
     * @param sol   the solver to use for each time step
     * @param state the initial state vector
     * @param h     the step size
     * @param maxT  the maximum simulation time
     * @return      a SimulationResult containing all time steps and state values
     */
    private static final double STOP_SPEED = 1e-2;

    public static SimulationResult run(ODESystem sys, ODESolver sol, double[] state, double h, double maxT) {
        SimulationResult res = new SimulationResult();
        res.variableNames = sys.getVariableNames();
        for (int i = 0; i < state.length; i++) res.dataMatrix.add(new ArrayList<>());

        double t = 0;
        while (t <= maxT) {
            res.timeSteps.add(t);
            for (int i = 0; i < state.length; i++) res.dataMatrix.get(i).add(state[i]);

            // Stop early if the ball has come to rest (vx and vy both negligible).
            // Only applies to ball-motion systems whose state is [x, y, vx, vy].
            if (state.length >= 4 && Math.hypot(state[2], state[3]) < STOP_SPEED) break;

            state = sol.nextStep(t, state, sys);
            t += h;
        }
        return res;
    }
}
package experiments;

import core.ODESolver;
import core.ODESystem;
import solvers.EulerSolver;
import solvers.RungeKutta4;


public class SolverAccuracy {

    private static final int WARMUP_REPETITIONS = 50;
    private static final int TIMED_REPETITIONS = 2000; // were the most optimal values

    public final String solverName;
    public final double stepSize;
    public final double runtimeMS;
    public final double finalValue;
    public final double exactValue;
    public final double error;

    public SolverAccuracy(String solverName, double stepSize, double runtimeMs, double finalValue, double exactValue, double error) {
        this.solverName = solverName;
        this.stepSize = stepSize;
        this.runtimeMS = runtimeMs;
        this.finalValue = finalValue;
        this.exactValue = exactValue;
        this.error = error;
    }


    // exp_system: y' = y, y(0) = 1, and the solution y(t) = e^t
    private static final ODESystem exp_system = new ODESystem() {
        @Override
        public double[] getDerivatives(double t, double[] state) {
            return new double[] { state[0] };
        }

        @Override
        public String[] getParameterNames() {
            return new String[0];
        }

        @Override
        public void setParameter(String name, double value) {
            throw new UnsupportedOperationException("You cant run the benchmark without parameters.");
        }

        @Override
        public String[] getVariableNames() {
            return new String[] { "y" };
        }

    };

    public static SolverAccuracy oneExperiment(ODESolver solver, String solvername, double h, double t0, double tEnd) {
        double[] state = { 1.0 };
        double t = t0; // initial t

        while(t < tEnd) {
            state = solver.nextStep(t, state, exp_system);
            t += h;
        }

        double exactValue = Math.exp(tEnd);
        double finalValue = state[0];
        double error = Math.abs(finalValue - exactValue);

        for (int w = 0; w < WARMUP_REPETITIONS; w++) {
            double[] warmState = { 1.0 };
            double warmT = t0;
            while (warmT < tEnd) {
                warmState = solver.nextStep(warmT, warmState, exp_system);
                warmT += h;
            }
        }

        long startTimer = System.nanoTime();
        for (int rep = 0; rep < TIMED_REPETITIONS; rep++) {
            double[] timedState = { 1.0 };
            double timedT = t0;

            while (timedT < tEnd) {
                timedState = solver.nextStep(timedT, timedState, exp_system);
                timedT += h;
            }
        }
        long endTimer = System.nanoTime();
        double runtimeMS = ((endTimer - startTimer) / (double) TIMED_REPETITIONS) / 1_000_000.0;

        return new SolverAccuracy(solvername, h, runtimeMS, finalValue, exactValue, error);
    }

    public static SolverAccuracy[] allExperiments() {
        // optimal enough values of step sizes h for running the experiment
        double[] hSizes = { 0.5, 0.25, 0.125, 0.0625, 0.03125, 0.015625 };
        double t0 = 0.0;
        double tEnd = 1.0;

        SolverAccuracy[] results = new SolverAccuracy[hSizes.length*2];
        
        int i = 0;
        for(double h : hSizes) {
            results[i++] = oneExperiment(new RungeKutta4(h), "RK4", h, t0, tEnd);
            results[i++] = oneExperiment(new EulerSolver(h), "Euler", h, t0, tEnd);
        }

        return results;
    }

    // creates an array with the results of the benchmark for a specific solver
    public static SolverAccuracy[] filterBySolver(SolverAccuracy[] results, String solverName) {
        int filteredSize = 0;
        for(SolverAccuracy r : results) {
            if(r.solverName.equals(solverName)) filteredSize++;
        }

        SolverAccuracy[] filtered = new SolverAccuracy[filteredSize];
        int i = 0;
        for(SolverAccuracy r : results) {
            if(r.solverName.equals(solverName)) filtered[i++] = r;
        }
        return filtered;
    }

    /*
        Getters for placing the data on a log-log plot
    */
    public static double[] getLogStepSize(SolverAccuracy[] results) {
        double[] values = new double[results.length];
        for(int i = 0; i < results.length; i++) {
            values[i] = Math.log10(results[i].stepSize);
        }
        return values;
    }

    public static double[] getLogError(SolverAccuracy[] results) {
        double[] values = new double[results.length];
        for(int i = 0; i < results.length; i++) {
            values[i] = Math.log10(results[i].error);
        }
        return values;
    }

    public static double[] getLogRuntimes(SolverAccuracy[] results) {
        double[] values = new double[results.length];
        for (int i = 0; i < results.length; i++) {
            values[i] = Math.log10(Math.max(results[i].runtimeMS, 1e-9));
        }
        return values;
    }

    // Printing method to check the values
    public static void printResults(SolverAccuracy[] results) {
        System.out.println("--------------------------------------------------------------------------------------------------");
        System.out.printf("%-15s | %-12s | %-15s | %-15s | %-15s%n", "Solver", "Step Size", "Runtime (ms)", "Final Value", "Error");
        System.out.println("--------------------------------------------------------------------------------------------------");
        int n=0;
        for (SolverAccuracy r : results) {
            System.out.printf("%-15s | %-12.6f | %-15.6f | %-15.6f | %-15.6e%n",
                r.solverName, r.stepSize, r.runtimeMS, r.finalValue, r.error);
            if(n%2==1){
                System.out.println("--------------------------------------------------------------------------------------------------");
            }
            n++;
        }
        System.out.println("--------------------------------------------------------------------------------------------------");
    }

    public static void main(String[] args) {
        SolverAccuracy[] results = allExperiments();
        printResults(results);
    }
    
}

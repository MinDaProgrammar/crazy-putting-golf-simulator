package gui;

import java.util.*;

/**
 * Stores the output of a simulation run.
 * variableNames maps to rows in the dataMatrix, so variableNames[i]
 * corresponds to dataMatrix.get(i), which holds that variable's value at each time step.
 */
public class SimulationResult {

    public String[] variableNames;

    public List<Double> timeSteps = new ArrayList<>();
    
    public List<List<Double>> dataMatrix = new ArrayList<>(); // dataMatrix.get(varIndex).get(timeIndex)


}
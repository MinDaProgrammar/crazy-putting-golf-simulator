package gui;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.HBox;

/**
 * A GUI panel containing two side-by-side charts for visualising simulation results.
 * The left chart shows each variable plotted over time, the right chart shows
 * the trajectory in phase space for a chosen pair of variables.
 */


public class ChartPanel extends HBox {
    private LineChart<Number, Number> timeChart, phaseChart;

    public ChartPanel() {
        setSpacing(15);
        setPadding(new javafx.geometry.Insets(10));
        timeChart = createChart("Time Series", "Time (t)", "Value");
        phaseChart = createChart("Phase Space", "X", "Y");
        getChildren().addAll(timeChart, phaseChart);
    }


      /**
     * Updates both charts with the latest simulation result.
     * @param res  the simulation result containing time steps, variable names and state data
     * @param xIdx index of the variable to plot on the phase space x axis
     * @param yIdx index of the variable to plot on the phase space y axis
     */
    public void update(SimulationResult res, int xIdx, int yIdx) {
        timeChart.getData().clear();
        phaseChart.getData().clear();

        // Time Plot
        for (int i = 0; i < res.variableNames.length; i++) {
            XYChart.Series<Number, Number> s = new XYChart.Series<>();
            s.setName(res.variableNames[i]);
            for (int j = 0; j < res.timeSteps.size(); j++) {
                s.getData().add(new XYChart.Data<>(res.timeSteps.get(j), res.dataMatrix.get(i).get(j)));
            }
            timeChart.getData().add(s);
        }

        // Phase Plot
        XYChart.Series<Number, Number> p = new XYChart.Series<>();
        p.setName("Trajectory");
        for (int j = 0; j < res.timeSteps.size(); j++) {
            p.getData().add(new XYChart.Data<>(res.dataMatrix.get(xIdx).get(j), res.dataMatrix.get(yIdx).get(j)));
        }
        phaseChart.getData().add(p);
        
        phaseChart.getXAxis().setLabel(res.variableNames[xIdx]);
        phaseChart.getYAxis().setLabel(res.variableNames[yIdx]);
    }

    public void resetAxes() {
        timeChart.getXAxis().setAutoRanging(true);
        timeChart.getYAxis().setAutoRanging(true);
        phaseChart.getXAxis().setAutoRanging(true);
        phaseChart.getYAxis().setAutoRanging(true);
    }



    /**
     * Creates a styled LineChart with the given title and axis labels.
     * Symbols and animations are disabled for performance with large datasets.
     * @param t the chart title
     * @param x the x axis label
     * @param y the y axis label
     * @return  the configured LineChart
     */
    private LineChart<Number, Number> createChart(String t, String x, String y) {
        NumberAxis ax = new NumberAxis(); ax.setLabel(x);
        NumberAxis ay = new NumberAxis(); ay.setLabel(y);
        LineChart<Number, Number> c = new LineChart<>(ax, ay);
        c.setTitle(t); c.setCreateSymbols(false); c.setAnimated(false);
        return c;
    }
}
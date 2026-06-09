package gui;

import core.ODESolver;
import core.ODESystem;
import solvers.EulerSolver;
import solvers.RungeKutta4;
import systems.LotkaVolterra;
import systems.SIR;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The sidebar panel containing all simulation controls.
 * Lets the user configure the step size, max time, ODE system, solver,
 * initial state, system parameters, and phase space axes before running.
 * Dynamically rebuilds its input fields whenever the selected system changes.
 */

public class SidebarPanel extends VBox {

    private final TextField hField  = new TextField("0.1");
    private final TextField tField  = new TextField("50");
    public  final ComboBox<String> sysSelect = new ComboBox<>();
    public  final ComboBox<String> solSelect = new ComboBox<>();
    private final ComboBox<String> phaseX    = new ComboBox<>();
    private final ComboBox<String> phaseY    = new ComboBox<>();

    // Dynamic sections — rebuilt when the system changes
    private final GridPane paramGrid  = new GridPane();
    private final GridPane stateGrid  = new GridPane();
    private final List<TextField> paramFields = new ArrayList<>();
    private final List<TextField> stateFields = new ArrayList<>();
    private String[] currentParamNames;

    private ODESystem activeSystem;

    public SidebarPanel(Runnable onRun, Runnable onSysChange) {
        setPadding(new Insets(10));
        setSpacing(6);
        setMinWidth(270);
        setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 0 0 0 1;");

        // ── system + solver on the same row ──────────────────────────────
        sysSelect.getItems().addAll("Lotka-Volterra", "SIR Model", "FitzHugh-Nagumo");
        sysSelect.setValue("Lotka-Volterra");
        sysSelect.setMaxWidth(Double.MAX_VALUE);
        sysSelect.setOnAction(e -> { updateActiveSystem(); onSysChange.run(); });

        solSelect.getItems().addAll("Euler", "Runge-Kutta 4");
        solSelect.setValue("Euler");
        solSelect.setMaxWidth(Double.MAX_VALUE);

        GridPane topGrid = twoColGrid();
        topGrid.add(boldLabel("System:"), 0, 0);
        topGrid.add(boldLabel("Solver:"), 1, 0);
        topGrid.add(sysSelect,            0, 1);
        topGrid.add(solSelect,            1, 1);

        // ── step size + max time on the same row ─────────────────────────
        hField.setMaxWidth(Double.MAX_VALUE);
        tField.setMaxWidth(Double.MAX_VALUE);

        GridPane htGrid = twoColGrid();
        htGrid.add(boldLabel("Step size (h):"), 0, 0);
        htGrid.add(boldLabel("Max time:"),      1, 0);
        htGrid.add(hField,                      0, 1);
        htGrid.add(tField,                      1, 1);

        // ── phase-space axis selectors on the same row ───────────────────
        phaseX.setMaxWidth(Double.MAX_VALUE);
        phaseY.setMaxWidth(Double.MAX_VALUE);

        GridPane phaseGrid = twoColGrid();
        phaseGrid.add(boldLabel("Phase X:"), 0, 0);
        phaseGrid.add(boldLabel("Phase Y:"), 1, 0);
        phaseGrid.add(phaseX,               0, 1);
        phaseGrid.add(phaseY,               1, 1);

        // ── dynamic param / state grids ──────────────────────────────────
        paramGrid.setHgap(6);
        paramGrid.setVgap(3);
        paramGrid.getColumnConstraints().addAll(labelCol(), fieldCol());

        stateGrid.setHgap(6);
        stateGrid.setVgap(3);
        stateGrid.getColumnConstraints().addAll(labelCol(), fieldCol());

        // ── run button ───────────────────────────────────────────────────
        Button runBtn = new Button("Run Simulation");
        runBtn.setMaxWidth(Double.MAX_VALUE);
        runBtn.setStyle("-fx-background-color: #2778ae; -fx-text-fill: white; -fx-font-weight: bold;");
        runBtn.setOnAction(e -> onRun.run());

        // ── assemble sidebar ─────────────────────────────────────────────
        getChildren().addAll(
            topGrid,
            htGrid,
            new Separator(),
            boldLabel("── Parameters ──"),  paramGrid,
            boldLabel("── Initial State ──"), stateGrid,
            new Separator(),
            boldLabel("── Phase Space ──"), phaseGrid,
            new Separator(),
            runBtn
        );

        // Populate with correct defaults for the initial system
        updateActiveSystem();
    }

    // ── helpers ──────────────────────────────────────────────────────────

    /** Two equal-width columns, 8 px gap. */
    private GridPane twoColGrid() {
        GridPane g = new GridPane();
        g.setHgap(8);
        g.setVgap(3);
        ColumnConstraints cc = new ColumnConstraints();
        cc.setPercentWidth(50);
        cc.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(cc, cc);
        return g;
    }

    /** Fixed-width label column for param/state grids. */
    private ColumnConstraints labelCol() {
        ColumnConstraints c = new ColumnConstraints(90);
        c.setHgrow(Priority.NEVER);
        return c;
    }

    /** Stretchy field column for param/state grids. */
    private ColumnConstraints fieldCol() {
        ColumnConstraints c = new ColumnConstraints();
        c.setHgrow(Priority.ALWAYS);
        c.setFillWidth(true);
        return c;
    }

    private Label boldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        return l;
    }

    // ── system switching ─────────────────────────────────────────────────

    private void updateActiveSystem() {
        String[] paramDefaults;
        String[] stateDefaults;

        switch (sysSelect.getValue()) {
            case "SIR Model" -> {
                activeSystem  = new SIR(3.0, 0.001, 1.0);
                paramDefaults = new String[]{ "3.0", "0.001", "1.0" };  // k, mu, gamma
                stateDefaults = new String[]{ "0.99", "0.01", "0.0" };  // S, I, R
            }
            case "FitzHugh-Nagumo" -> {
                activeSystem  = new systems.FitzHughNagumo(1.0, 1.0, 0.05, 0.5);
                paramDefaults = new String[]{ "1.0", "1.0", "0.05", "0.5" }; // alpha, beta, epsilon, I
                stateDefaults = new String[]{ "-1.0", "-0.5" };               // Voltage, Recovery
            }
            default -> {  // Lotka-Volterra
                activeSystem  = new LotkaVolterra(1.1, 0.4, 0.4, 0.1);
                paramDefaults = new String[]{ "1.1", "0.4", "0.4", "0.1" }; // alpha, beta, gamma, delta
                stateDefaults = new String[]{ "10.0", "5.0" };               // prey, predators
            }
        }

        updateDynamicInputs(
            activeSystem.getVariableNames(),
            activeSystem.getParameterNames(),
            paramDefaults,
            stateDefaults
        );
    }

    /**
     * Rebuilds parameter and initial-state rows (label | field on one line each).
     * Called whenever the selected system changes.
     */
    public void updateDynamicInputs(String[] variableNames, String[] paramNames,
                                    String[] paramDefaults, String[] stateDefaults) {
        // Parameters
        paramGrid.getChildren().clear();
        paramFields.clear();
        currentParamNames = paramNames;
        for (int i = 0; i < paramNames.length; i++) {
            TextField f = new TextField(i < paramDefaults.length ? paramDefaults[i] : "0.1");
            f.setMaxWidth(Double.MAX_VALUE);
            paramFields.add(f);
            paramGrid.add(new Label(paramNames[i] + ":"), 0, i);
            paramGrid.add(f,                              1, i);
        }

        // Initial state
        stateGrid.getChildren().clear();
        stateFields.clear();
        for (int i = 0; i < variableNames.length; i++) {
            TextField f = new TextField(i < stateDefaults.length ? stateDefaults[i] : "0.0");
            f.setMaxWidth(Double.MAX_VALUE);
            stateFields.add(f);
            stateGrid.add(new Label(variableNames[i] + ":"), 0, i);
            stateGrid.add(f,                                  1, i);
        }

        // Phase-space axis dropdowns
        phaseX.getItems().setAll(variableNames);
        phaseY.getItems().setAll(variableNames);
        if (variableNames.length >= 2) {
            phaseX.setValue(variableNames[0]);
            phaseY.setValue(variableNames[1]);
        }
    }

    // ── public API ───────────────────────────────────────────────────────

    public void applyParameters(ODESystem sys) {
        for (int i = 0; i < currentParamNames.length; i++) {
            double val = Double.parseDouble(paramFields.get(i).getText());
            sys.setParameter(currentParamNames[i], val);
        }
    }

    public double    getStepSize()     { return Double.parseDouble(hField.getText()); }
    public double    getMaxTime()      { return Double.parseDouble(tField.getText()); }
    public int       getPhaseXIndex()  { return phaseX.getSelectionModel().getSelectedIndex(); }
    public int       getPhaseYIndex()  { return phaseY.getSelectionModel().getSelectedIndex(); }
    public ODESystem getSelectedSystem() { return activeSystem; }

    public double[] getInitialState() {
        return stateFields.stream().mapToDouble(f -> Double.parseDouble(f.getText())).toArray();
    }

    public ODESolver getSelectedSolver(double h) {
        return solSelect.getValue().equals("Euler") ? new EulerSolver(h) : new RungeKutta4(h);
    }

    public void showError(String title, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setContentText(content);
        a.show();
    }
}

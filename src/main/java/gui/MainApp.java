package gui;

import core.*;
import core.bots.Bot;
import core.bots.MLBot;
import core.bots.MazeMLBot;
import core.bots.RuleBasedBot;
import core.course.Course;
import core.course.CourseFileReader;
import core.course.HeightFunction;
import experiments.SolverAccuracy;
import gui.golf.GolfGameState;
import gui.golf.GolfShotAnimator;
import gui.golf.GolfShotSimulator;
import gui.rendering.GolfCourseRenderer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class MainApp extends Application {
    // State management for dynamic course loading
    private final GolfGameState golfState = new GolfGameState();
    private final GolfShotSimulator golfShotSimulator = new GolfShotSimulator();
    private javafx.animation.Timeline activeTimeline;

    private SidebarPanel sidebar;
    private ChartPanel charts;

    private final double windowHeight = 600;
    private final double windowWidth = 800;
    private final double COURSE_VIEW_MARGIN = 4.0;
    private final double AIM_CLICK_RADIUS = 16.0;
    private final double AIM_PIXELS_PER_VELOCITY = 45.0;
    private Label shotCountLabel;
    private GolfShotAnimator golfShotAnimator;
    
    private boolean aiming;
    private Canvas golfCanvas;
    private GolfCourseRenderer golfRenderer;
    ComboBox<String> courseSelector = new ComboBox<>();




    /**
     * Builds the main window and its three tabs, then shows the stage.
     * @param stage the primary JavaFX stage provided by the runtime
     */
    @Override
    public void start(Stage stage) {
        stage.setTitle("Crazy Putting Visualization");

        TabPane mainTabs = new TabPane();

        // Phase 1 Tab
        Tab odeTab = new Tab("ODE Solver Tests");
        BorderPane odeLayout = new BorderPane();
        charts = new ChartPanel();
        sidebar = new SidebarPanel(this::runSimulation, this::handleSystemChange);
        odeLayout.setCenter(charts);
        odeLayout.setRight(sidebar);
        odeTab.setContent(odeLayout);
        odeTab.setClosable(false);

        // Accuracy Tab
        Tab accuracyTab = new Tab("Solver Accuracy");
        accuracyTab.setContent(createAccuracyView());
        accuracyTab.setClosable(false);

        // Phase 2 Tab
        Tab golfTab = new Tab("Crazy Putting");
        golfTab.setContent(createGolfView());
        golfTab.setClosable(false);

        mainTabs.getTabs().addAll(odeTab, accuracyTab, golfTab);
        stage.setScene(new Scene(mainTabs, 1200, 850));
        stage.show();
    }

    //This is just the same code from loglog.java but moved as a method instead for easier visualization
    private HBox createAccuracyView() {
        SolverAccuracy[] results = SolverAccuracy.allExperiments();

        //Setup Error Chart
        NumberAxis xAxisE = new NumberAxis();
        xAxisE.setLabel("log10(h)");
        NumberAxis yAxisE = new NumberAxis();
        yAxisE.setLabel("log10(error)");
        LineChart<Number, Number> errorChart = new LineChart<>(xAxisE, yAxisE);
        errorChart.setTitle("Log-Log Error Plot");

        //Setup Runtime Chart
        NumberAxis xAxisR = new NumberAxis();
        xAxisR.setLabel("log10(h)");
        NumberAxis yAxisR = new NumberAxis();
        yAxisR.setLabel("log10(runtime in ms)");
        LineChart<Number, Number> runtimeChart = new LineChart<>(xAxisR, yAxisR);
        runtimeChart.setTitle("Log-Log Runtime Plot");

        //Create Series for both charts
        XYChart.Series<Number, Number> eulerErr = new XYChart.Series<>();
        eulerErr.setName("Euler Error");
        XYChart.Series<Number, Number> rk4Err = new XYChart.Series<>();
        rk4Err.setName("RK4 Error");

        XYChart.Series<Number, Number> eulerTime = new XYChart.Series<>();
        eulerTime.setName("Euler Runtime");
        XYChart.Series<Number, Number> rk4Time = new XYChart.Series<>();
        rk4Time.setName("RK4 Runtime");

        //Fill data
        for (SolverAccuracy r : results) {
            double logH = Math.log10(r.stepSize);
            double logError = Math.log10(r.error);
            double logRuntime = Math.log10(Math.max(r.runtimeMS, 1e-9)); // Prevent log(0)

            if (r.solverName.equals("Euler")) {
                eulerErr.getData().add(new XYChart.Data<>(logH, logError));
                eulerTime.getData().add(new XYChart.Data<>(logH, logRuntime));
            } else {
                rk4Err.getData().add(new XYChart.Data<>(logH, logError));
                rk4Time.getData().add(new XYChart.Data<>(logH, logRuntime));
            }
        }

        // Assemble
        errorChart.getData().addAll(eulerErr, rk4Err);
        runtimeChart.getData().addAll(eulerTime, rk4Time);

        HBox container = new HBox(15, errorChart, runtimeChart); // Add both charts
        HBox.setHgrow(errorChart, Priority.ALWAYS);
        HBox.setHgrow(runtimeChart, Priority.ALWAYS);
        return container;
    }   

     /**
     * Builds the golf simulation tab with the canvas, course selector, and shot input controls.
     * @return a BorderPane with the canvas on the left and controls on the right
     */
    
    private BorderPane createGolfView() {
        BorderPane layout = new BorderPane();
        golfCanvas = new Canvas(windowWidth, windowHeight);
        golfRenderer = new GolfCourseRenderer(golfCanvas, COURSE_VIEW_MARGIN);
        golfShotAnimator = new GolfShotAnimator(golfState, golfRenderer, sidebar);

        courseSelector = new ComboBox<>();
        courseSelector.getItems().addAll(
                // Phase 2 — open terrain courses
                "test_course.txt",
                "test_course2.txt",
                "test_course3.txt",
                "test_course4.txt",
                "test_course5.txt",
                "test_course6.txt",
                "test_course7.txt",
                // Demo / showcase
                "demo_course.txt",
                // S-maze water course
                "test_maze1.txt",
                // Phase 3a — demo-based maze (obstacles on MLBot path)
                "demo_maze1.txt",
                "demo_maze2.txt",
                "demo_maze3.txt",
                "demo_maze4.txt",
                // Phase 3b — demo-based maze (obstacles on MazeMLBot A* path)
                "demo_mazebot1.txt",
                "demo_mazebot2.txt",
                "demo_mazebot3.txt",
                "demo_mazebot4.txt",
                "yuri.txt"
        );
        courseSelector.setValue("test_course.txt"); // Set a default

        VBox controls = new VBox(15);
        controls.setPadding(new Insets(20));
        controls.setPrefWidth(250);
        controls.setStyle("-fx-background-color: #f4f4f4;");

        TextField vxInput = new TextField("0.0");
        TextField vyInput = new TextField("0.0");
        Button puttBtn = new Button("Putt!");
        Button mlPuttBtn = new Button("ML Putt");
        Button mazePuttBtn = new Button("Maze ML Putt");
        Button ruleBotBtn = new Button("Shoot (Rule Bot)");
        Button resetPositionBtn = new Button("Reset Position");
        Label posLabel = new Label("Pos: (0,0)");
        shotCountLabel = new Label("Shots: 0");
        configureAimControls(vxInput, vyInput, posLabel);

        // Dynamic Course Loading Listener
        courseSelector.setOnAction(e -> {
            try {
                if (activeTimeline != null) activeTimeline.stop(); // Stop ball if course changes mid-shot

                String selected = courseSelector.getValue();
                Course course = CourseFileReader.readCourseFile("src/main/resources/courses/" + selected);
                HeightFunction heightFunction = HeightFunction.compileExpression(course.getHeightProfile());

                golfState.loadCourse(course, heightFunction);

                golfRenderer.invalidateTerrainCache();
                golfRenderer.render(
                        golfState.getCurrentCourse(),
                        golfState.getCurrentHeightFunction(),
                        golfState.getBallX(),
                        golfState.getBallY()
                );

                posLabel.setText(String.format(
                        "Pos: (%.2f, %.2f)",
                        golfState.getBallX(),
                        golfState.getBallY()
                ));

                updateShotCount(true);

            } catch (Exception ex) {
                sidebar.showError("Loading Error", "Could not load course: " + ex.getMessage());
            }
        });

        // Trigger initial load for the default selection
        courseSelector.getOnAction().handle(null);

        puttBtn.setOnAction(e -> {
            try {
                double vx = Double.parseDouble(vxInput.getText());
                double vy = Double.parseDouble(vyInput.getText());
                startGolfShot(vx, vy, posLabel);
            } catch (NumberFormatException ex) {
                sidebar.showError("Input Error", "Please enter valid numbers for velocity.");
            }
        });

        ruleBotBtn.setOnAction(e -> runRuleBot(vxInput, vyInput, posLabel));

        resetPositionBtn.setOnAction(e -> {
            if (activeTimeline != null) {
                activeTimeline.stop();
            }
            if (!golfState.hasCourseLoaded()) {
                sidebar.showError("Reset Error", "No course is loaded.");
                return;
            }

            updateShotCount(true);
            golfState.resetBallToStart();
            golfRenderer.render(
                    golfState.getCurrentCourse(),
                    golfState.getCurrentHeightFunction(),
                    golfState.getBallX(),
                    golfState.getBallY()
            );
            posLabel.setText(String.format(
                    "Pos: (%.2f, %.2f)",
                    golfState.getBallX(),
                    golfState.getBallY()
            ));
            
        });

        mlPuttBtn.setOnAction(e -> runMlBot(vxInput, vyInput, posLabel, new MLBot(), "ML bot failed: "));
        mazePuttBtn.setOnAction(e -> runMlBot(vxInput, vyInput, posLabel, new MazeMLBot(), "Maze ML bot failed: "));

        controls.getChildren().addAll(
            new Label("Select Course:"), courseSelector,
            new Label("Shot Input"), new Label("Vx:"), vxInput, new Label("Vy:"), vyInput, 
            puttBtn, ruleBotBtn, mlPuttBtn, mazePuttBtn, resetPositionBtn, new Separator(), new Label("Stats"), posLabel, shotCountLabel
        );

        layout.setCenter(golfCanvas);
        layout.setRight(controls);
        return layout;
    }

    private void runMlBot(TextField vxInput, TextField vyInput, Label posLabel, Bot bot, String errorPrefix) {
        try {
            if (!golfState.hasCourseLoaded()) {
                sidebar.showError("Bot Error", "Please load a course first!");
                return;}
            double[] v = bot.computeShot(
                    golfState.getCurrentCourse(),
                    golfState.getBallX(),
                    golfState.getBallY()
            );

            vxInput.setText(String.format("%.3f", v[0]));
            vyInput.setText(String.format("%.3f", v[1]));
            startGolfShot(v[0], v[1], posLabel);
        } catch (Exception ex) {
            ex.printStackTrace();
            sidebar.showError("Bot Error", errorPrefix + ex.getMessage());
        }
    }

    private void runRuleBot(TextField vxInput, TextField vyInput, Label posLabel) {
        if (!golfState.hasCourseLoaded()) {
            sidebar.showError("Error", "Please load a course first!");
            return;
        }

        try {
            // Set up vectors for the bot
            Vector2D targetPos = new Vector2D(
                    golfState.getCurrentCourse().getTargetX(),
                    golfState.getCurrentCourse().getTargetY()
            );
            Vector2D ballPos = new Vector2D(golfState.getBallX(), golfState.getBallY());

            // Tunable weights for the bot calculation
            double w = 0.6; // slope correction weight
            // Compute the optimal velocity vector
            Vector2D botShot = RuleBasedBot.computeInitialVelocity(
                    golfState.getCurrentHeightFunction(),
                    targetPos,
                    ballPos,
                    golfState.getCurrentCourse().getFrictionKinetic(),
                    w
            );

            // Vector2D is a record, so we use .x() and .y()
            double vx = botShot.x();
            double vy = botShot.y();

            // Update the UI fields so the user sees what the bot chose
            vxInput.setText(String.format("%.2f", vx));
            vyInput.setText(String.format("%.2f", vy));

            // Run the simulation using the existing shot mechanism
            startGolfShot(vx, vy, posLabel);

        } catch (Exception ex) {
            sidebar.showError("Bot Error", "The Rule Bot encountered an error: " + ex.getMessage());
        }
    }

    private void configureAimControls(TextField vxInput, TextField vyInput, Label posLabel) {
        golfCanvas.setOnMousePressed(event -> {
            if (!golfState.hasCourseLoaded()) {
                return;
            }

            double ballScreenX = golfRenderer.worldToScreenX(golfState.getBallX());
            double ballScreenY = golfRenderer.worldToScreenY(golfState.getBallY());
            double distanceFromBall = Math.hypot(event.getX() - ballScreenX, event.getY() - ballScreenY);

            if (distanceFromBall <= AIM_CLICK_RADIUS) {
                if (activeTimeline != null) {
                    activeTimeline.stop();
                }
                aiming = true;
                updateAimFromMouse(event.getX(), event.getY(), vxInput, vyInput, posLabel);
            }
        });

        golfCanvas.setOnMouseDragged(event -> {
            if (aiming) {
                updateAimFromMouse(event.getX(), event.getY(), vxInput, vyInput, posLabel);
            }
        });

        golfCanvas.setOnMouseReleased(event -> {
            if (!aiming) {
                return;
            }

            aiming = false;
            golfRenderer.clearAimPreview();
            golfRenderer.render(
                    golfState.getCurrentCourse(),
                    golfState.getCurrentHeightFunction(),
                    golfState.getBallX(),
                    golfState.getBallY()
            );

            try {
                double vx = Double.parseDouble(vxInput.getText());
                double vy = Double.parseDouble(vyInput.getText());
                startGolfShot(vx, vy, posLabel);
            } catch (NumberFormatException ex) {
                sidebar.showError("Input Error", "Aim did not produce valid velocity values.");
            }
        });
    }

    private void startGolfShot(double vx, double vy, Label posLabel) {
        if (!golfState.hasCourseLoaded()) {
            sidebar.showError("Shot Error", "No course is loaded.");
            return;
        }

        updateShotCount(false);

        if (activeTimeline != null) {
            activeTimeline.stop();
        }

        SimulationResult result = golfShotSimulator.simulateShot(
            golfState.getCurrentCourse(),
            golfState.getCurrentHeightFunction(),
            golfState.getBallX(),
            golfState.getBallY(),
            vx,
            vy
        );

        activeTimeline = golfShotAnimator.animateBallPath(result, posLabel);
        activeTimeline.play();
    }

    private void updateAimFromMouse(double mouseX, double mouseY, TextField vxInput, TextField vyInput, Label posLabel) {
        double ballScreenX = golfRenderer.worldToScreenX(golfState.getBallX());
        double ballScreenY = golfRenderer.worldToScreenY(golfState.getBallY());

        // Pulling away from the ball works like billiards: the shot goes opposite the drag.
        double vx = (ballScreenX - mouseX) / AIM_PIXELS_PER_VELOCITY;
        double vy = (mouseY - ballScreenY) / AIM_PIXELS_PER_VELOCITY;
        double speed = Math.hypot(vx, vy);
        double maxSpeed = golfState.getCurrentCourse().getVMax();

        if (speed > maxSpeed) {
            double scale = maxSpeed / speed;
            vx *= scale;
            vy *= scale;
        }

        vxInput.setText(String.format("%.2f", vx));
        vyInput.setText(String.format("%.2f", vy));
        posLabel.setText(String.format(
                "Pos: (%.2f, %.2f) | Aim: (%.2f, %.2f)",
                golfState.getBallX(),
                golfState.getBallY(),
                vx,
                vy
        ));

        golfRenderer.setAimPreview(
                golfState.getBallX(),
                golfState.getBallY(),
                golfState.getBallX() + vx,
                golfState.getBallY() + vy
        );
        golfRenderer.render(
                golfState.getCurrentCourse(),
                golfState.getCurrentHeightFunction(),
                golfState.getBallX(),
                golfState.getBallY()
        );
    }

    private void handleSystemChange() {
        // updateActiveSystem() in SidebarPanel already rebuilt all fields with correct defaults.
        // We only need to clear the charts here.
        charts.resetAxes();
    }

    private void runSimulation() {
        try {
            double h = sidebar.getStepSize();
            double maxT = sidebar.getMaxTime();
            ODESystem system = sidebar.getSelectedSystem();
            ODESolver solver = sidebar.getSelectedSolver(h);
            double[] state = sidebar.getInitialState();
            sidebar.applyParameters(system);
            SimulationResult result = SimulationEngine.run(system, solver, state, h, maxT);
            charts.update(result, sidebar.getPhaseXIndex(), sidebar.getPhaseYIndex());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Count how many shots have been taken
    private void updateShotCount(boolean reset) {
        if (reset) {
            golfState.resetShotCount();
        } else {
            golfState.incrementShotCount();
        }
        shotCountLabel.setText("Shots: " + golfState.getShotCount());
    }
    public static void main(String[] args) { launch(args); }
}

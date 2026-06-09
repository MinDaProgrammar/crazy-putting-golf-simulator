package gui.golf;

import core.course.Course;
import core.course.HeightFunction;
import gui.SidebarPanel;
import gui.SimulationResult;
import gui.rendering.GolfCourseRenderer;

import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.util.Duration;

public class GolfShotAnimator {
    private final GolfGameState state;
    private final GolfCourseRenderer renderer;
    private final SidebarPanel sidebar;

    public GolfShotAnimator(GolfGameState state, GolfCourseRenderer renderer, SidebarPanel sidebar) {
        this.state = state;
        this.renderer = renderer;
        this.sidebar = sidebar;
    }

    public Timeline animateBallPath(SimulationResult result, Label posLabel) {
        Timeline timeline = new Timeline();

        Course course = state.getCurrentCourse();
        HeightFunction hFunc = state.getCurrentHeightFunction();

        // From the simulated result, get x and y value
        List<Double> xCoords = result.dataMatrix.get(0);
        List<Double> yCoords = result.dataMatrix.get(1);

        for (int i = 0; i < xCoords.size(); i++) {
            final int index = i;
            KeyFrame frame = new KeyFrame(
                    Duration.millis(index * 10), // Adjust speed of animation here
                    e -> {
                        // Update the current ball position
                        state.setBallPosition(xCoords.get(index), yCoords.get(index));

                        // Redraw the scene using the optimized cache
                        renderer.render(
                                course,
                                hFunc,
                                state.getBallX(),
                                state.getBallY(),
                                xCoords.subList(0, index + 1),
                                yCoords.subList(0, index + 1)
                        );

                        posLabel.setText(String.format(
                                "Pos: (%.2f, %.2f)",
                                state.getBallX(),
                                state.getBallY()
                        ));

                        // Check Tree Collisions
                        if (course.getTrees() != null) {
                            for (Course.Tree tree : course.getTrees()) {
                                double distToTree = Math.hypot(
                                        state.getBallX() - tree.x,
                                        state.getBallY() - tree.y
                                );
                                // Assuming ball radius is roughly 0.05 meters in world scale
                                if (distToTree <= tree.radius + 0.05) {
                                    timeline.stop();
                                    sidebar.showError("Ouch!", "You hit a tree. Try again!");
                                    // Stop when it hits a treee
                                    return;
                                }
                            }
                        }

                        double distToHole = Math.hypot(
                                state.getBallX() - course.getTargetX(),
                                state.getBallY() - course.getTargetY()
                        );

                        // Check if the ball reached the hole (Target Radius check)
                        if (distToHole <= course.getTargetRadius()) {
                            timeline.stop();
                            showGoalReachedAlert(course);
                            return;
                        }

                        // Check if the ball fell into water (Height < 0)
                        if (hFunc.evaluate(state.getBallX(), state.getBallY()) < 0) {
                            timeline.stop();
                            state.resetBallToStart();
                            state.resetMlShotCounter();
                            renderer.render(course, hFunc, state.getBallX(), state.getBallY());
                        }
                    }
            );

            timeline.getKeyFrames().add(frame);
        }

        return timeline;
    }

    private void showGoalReachedAlert(Course course) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Goal Reached");
        alert.setHeaderText("Goal reached!");
        alert.setContentText(String.format(
                "Ball position: (%.2f, %.2f)%nTarget: (%.2f, %.2f)",
                state.getBallX(),
                state.getBallY(),
                course.getTargetX(),
                course.getTargetY()
        ));
        alert.show();
    }
}

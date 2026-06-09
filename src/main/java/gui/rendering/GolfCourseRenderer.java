package gui.rendering;

import core.course.Course;
import core.course.HeightFunction;

import java.util.List;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class GolfCourseRenderer {
    private final Canvas canvas;
    private final GraphicsContext gc;
    private final double worldMargin;

    private WritableImage cachedTerrain;
    private boolean showingAimPreview;
    private double aimStartX;
    private double aimStartY;
    private double aimEndX;
    private double aimEndY;
    private double scale;
    private double worldMinX;
    private double worldMaxY;
    private double screenOffsetX;
    private double screenOffsetY;

    public GolfCourseRenderer(Canvas canvas, double worldMargin) {
        this.canvas = canvas;
        this.gc = canvas.getGraphicsContext2D();
        this.worldMargin = worldMargin;
    }

    public void invalidateTerrainCache() {
        cachedTerrain = null;
    }

    public void setAimPreview(double startX, double startY, double endX, double endY) {
        showingAimPreview = true;
        aimStartX = startX;
        aimStartY = startY;
        aimEndX = endX;
        aimEndY = endY;
    }

    public void clearAimPreview() {
        showingAimPreview = false;
    }

    public void render(Course course, HeightFunction heightFunction, double ballX, double ballY) {
        render(course, heightFunction, ballX, ballY, null, null);
    }

    public void render(
            Course course,
            HeightFunction heightFunction,
            double ballX,
            double ballY,
            List<Double> trailX,
            List<Double> trailY
    ) {
        if (course == null || heightFunction == null) {
            clear();
            return;
        }

        if (updateViewport(course)) {
            invalidateTerrainCache();
        }

        if (cachedTerrain == null) {
            cachedTerrain = createTerrainImage(heightFunction);
        }

        gc.drawImage(cachedTerrain, 0, 0);
        drawTrail(trailX, trailY);
        drawStart(course);
        drawTarget(course);
        drawAimPreview();
        drawBall(ballX, ballY);
        drawSandpits(course);
        drawTrees(course);
    }

    /*  
        Recomputes the visible world area for the loaded course.
        This makes each course fit inside the canvas based on all
        of its objects around.
    */

    private boolean updateViewport(Course course) {
        // World origin (0,0) is always the bottom-left corner.
        // Only the maximum extents are derived from course content.
        double maxX = Math.max(course.getStartX(), course.getTargetX() + course.getTargetRadius());
        double maxY = Math.max(course.getStartY(), course.getTargetY() + course.getTargetRadius());

        if (course.getTrees() != null) {
            for (Course.Tree tree : course.getTrees()) {
                maxX = Math.max(maxX, tree.x + tree.radius);
                maxY = Math.max(maxY, tree.y + tree.radius);
            }
        }

        if (course.getSandpits() != null) {
            for (Course.Sandpit sandpit : course.getSandpits()) {
                maxX = Math.max(maxX, sandpit.x + sandpit.radius);
                maxY = Math.max(maxY, sandpit.y + sandpit.radius);
            }
        }

        maxX += worldMargin;
        maxY += worldMargin;

        double canvasWidth  = canvas.getWidth();
        double canvasHeight = canvas.getHeight();
        double newScale = Math.min(canvasWidth / Math.max(maxX, 1.0), canvasHeight / Math.max(maxY, 1.0));

        // Pin (0,0) to the bottom-left corner: x=0 → screenX=0, y=0 → screenY=canvasHeight
        double newScreenOffsetX = 0.0;
        double newScreenOffsetY = canvasHeight - maxY * newScale;

        boolean changed =
                Math.abs(newScale - scale) > 1e-9 ||
                Math.abs(maxY - worldMaxY) > 1e-9 ||
                Math.abs(newScreenOffsetY - screenOffsetY) > 1e-9;

        scale         = newScale;
        worldMinX     = 0.0;
        worldMaxY     = maxY;
        screenOffsetX = newScreenOffsetX;
        screenOffsetY = newScreenOffsetY;

        return changed;
    }

    private WritableImage createTerrainImage(HeightFunction heightFunction) {
        int width = (int) canvas.getWidth();
        int height = (int) canvas.getHeight();
        double[] heights = new double[width * height];
        double minLandHeight = Double.POSITIVE_INFINITY;
        double maxLandHeight = Double.NEGATIVE_INFINITY;

        for (int screenX = 0; screenX < width; screenX++) {
            for (int screenY = 0; screenY < height; screenY++) {
                double worldX = screenToWorldX(screenX);
                double worldY = screenToWorldY(screenY);
                double h = heightFunction.evaluate(worldX, worldY);
                heights[screenY * width + screenX] = h;

                if (h >= 0.0) {
                    minLandHeight = Math.min(minLandHeight, h);
                    maxLandHeight = Math.max(maxLandHeight, h);
                }
            }
        }

        if (!Double.isFinite(minLandHeight) || !Double.isFinite(maxLandHeight)) {
            minLandHeight = 0.0;
            maxLandHeight = 1.0;
        }

        WritableImage terrain = new WritableImage(width, height);
        PixelWriter pixelWriter = terrain.getPixelWriter();

        for (int screenX = 0; screenX < width; screenX++) {
            for (int screenY = 0; screenY < height; screenY++) {
                double h = heights[screenY * width + screenX];
                pixelWriter.setColor(screenX, screenY, colorForHeight(h, minLandHeight, maxLandHeight));
            }
        }

        return terrain;
    }

    private Color colorForHeight(double height, double minLandHeight, double maxLandHeight) {
        if (height < 0.0) {
            double depth = clamp(-height / 0.35, 0.0, 1.0);
            return Color.rgb(58, 132, 190).interpolate(Color.rgb(20, 63, 125), depth);
        }

        // Normalize per loaded terrain so low-height courses still show ridges and valleys.
        double heightRange = Math.max(maxLandHeight - minLandHeight, 1e-9);
        double normalized = clamp((height - minLandHeight) / heightRange, 0.0, 1.0);
        Color low = Color.rgb(42, 112, 58);
        Color mid = Color.rgb(92, 168, 78);
        Color high = Color.rgb(214, 226, 132);
        Color color = normalized < 0.55
                ? low.interpolate(mid, normalized / 0.55)
                : mid.interpolate(high, (normalized - 0.55) / 0.45);

        double contour = Math.abs(height * 12.0 - Math.rint(height * 12.0));
        if (contour < 0.025) {
            color = color.interpolate(Color.rgb(28, 82, 42), 0.35);
        }

        return color;
    }

    private void drawStart(Course course) {
        double x = worldToScreenX(course.getStartX());
        double y = worldToScreenY(course.getStartY());

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2.0);
        gc.strokeOval(x - 7.0, y - 7.0, 14.0, 14.0);
    }

    private void drawTarget(Course course) {
        double x = worldToScreenX(course.getTargetX());
        double y = worldToScreenY(course.getTargetY());
        double radius = course.getTargetRadius() * scale;

        gc.setFill(Color.BLACK);
        gc.fillOval(x - radius, y - radius, radius * 2.0, radius * 2.0);

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.5);
        gc.strokeOval(x - radius, y - radius, radius * 2.0, radius * 2.0);
    }

    private void drawBall(double ballX, double ballY) {
        double x = worldToScreenX(ballX);
        double y = worldToScreenY(ballY);

        gc.setFill(Color.WHITE);
        gc.fillOval(x - 5.0, y - 5.0, 10.0, 10.0);

        gc.setStroke(Color.rgb(40, 40, 40));
        gc.setLineWidth(1.0);
        gc.strokeOval(x - 5.0, y - 5.0, 10.0, 10.0);
    }

    private void drawTrail(List<Double> trailX, List<Double> trailY) {
        if (trailX == null || trailY == null || trailX.size() < 2 || trailX.size() != trailY.size()) {
            return;
        }

        gc.setStroke(Color.rgb(255, 245, 180, 0.75));
        gc.setLineWidth(2.0);

        for (int i = 1; i < trailX.size(); i++) {
            gc.strokeLine(
                    worldToScreenX(trailX.get(i - 1)),
                    worldToScreenY(trailY.get(i - 1)),
                    worldToScreenX(trailX.get(i)),
                    worldToScreenY(trailY.get(i))
            );
        }
    }

    private void drawAimPreview() {
        if (!showingAimPreview) {
            return;
        }

        double startScreenX = worldToScreenX(aimStartX);
        double startScreenY = worldToScreenY(aimStartY);
        double endScreenX = worldToScreenX(aimEndX);
        double endScreenY = worldToScreenY(aimEndY);

        gc.setStroke(Color.rgb(255, 228, 92));
        gc.setLineWidth(3.0);
        gc.strokeLine(startScreenX, startScreenY, endScreenX, endScreenY);

        // Compute the arrow direction angle from its screen-space x/y movement.
        double angle = Math.atan2(endScreenY - startScreenY, endScreenX - startScreenX);
        double arrowLength = 12.0;
        double arrowAngle = Math.PI / 7.0;
        gc.strokeLine(
                endScreenX,
                endScreenY,
                endScreenX - arrowLength * Math.cos(angle - arrowAngle),
                endScreenY - arrowLength * Math.sin(angle - arrowAngle)
        );
        gc.strokeLine(
                endScreenX,
                endScreenY,
                endScreenX - arrowLength * Math.cos(angle + arrowAngle),
                endScreenY - arrowLength * Math.sin(angle + arrowAngle)
        );
    }

    private void drawSandpits(Course course) {
        if (course.getSandpits() == null) return;
        
        gc.setFill(Color.rgb(238, 214, 175, 0.8)); 
        
        for (Course.Sandpit sandpit : course.getSandpits()) {
            double screenX = worldToScreenX(sandpit.x);
            double screenY = worldToScreenY(sandpit.y);
            double screenRadius = sandpit.radius * scale;
            
            gc.fillOval(screenX - screenRadius, screenY - screenRadius, screenRadius * 2, screenRadius * 2);
            
            gc.setStroke(Color.rgb(200, 180, 140));
            gc.setLineWidth(1.5);
            gc.strokeOval(screenX - screenRadius, screenY - screenRadius, screenRadius * 2, screenRadius * 2);
        }
    }

    private void drawTrees(Course course) {
        if (course.getTrees() == null) return;
        
        for (Course.Tree tree : course.getTrees()) {
            double screenX = worldToScreenX(tree.x);
            double screenY = worldToScreenY(tree.y);
            double screenRadius = tree.radius * scale;

            // Draw shadow
            gc.setFill(Color.rgb(0, 0, 0, 0.3));
            gc.fillOval(screenX - screenRadius*0.8, screenY + screenRadius*0.2, screenRadius * 1.6, screenRadius);

            // Draw trunk
            gc.setFill(Color.rgb(101, 67, 33)); // Brown
            gc.fillRect(screenX - 3, screenY, 6, screenRadius);

            // Draw leaves (canopy)
            gc.setFill(Color.rgb(34, 139, 34)); // Forest Green
            gc.fillOval(screenX - screenRadius, screenY - screenRadius, screenRadius * 2, screenRadius * 2);
            
            // Highlight for 3D effect
            gc.setFill(Color.rgb(50, 180, 50)); 
            gc.fillOval(screenX - screenRadius*0.5, screenY - screenRadius*0.8, screenRadius, screenRadius);
        }
    }

    private void clear() {
        gc.clearRect(0.0, 0.0, canvas.getWidth(), canvas.getHeight());
    }

    public double worldToScreenX(double worldX) {
        return screenOffsetX + (worldX - worldMinX) * scale;
    }

    public double worldToScreenY(double worldY) {
        return screenOffsetY + (worldMaxY - worldY) * scale;
    }

    public double screenToWorldX(double screenX) {
        return worldMinX + (screenX - screenOffsetX) / scale;
    }

    public double screenToWorldY(double screenY) {
        return worldMaxY - (screenY - screenOffsetY) / scale;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

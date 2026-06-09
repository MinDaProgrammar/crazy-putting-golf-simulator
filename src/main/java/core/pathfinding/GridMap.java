package core.pathfinding;

import core.course.Course;
import core.course.HeightFunction;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Discretizes a course into a 2D grid of free/blocked cells for path planning.
 */
public class GridMap {

    private final double minX;
    private final double minY;
    private final double cellSize;
    private final double waterSafetyMargin;
    private final double obstaclePadding;
    private final int cols;
    private final int rows;
    private final boolean[][] free;

    public GridMap(Course course, HeightFunction heightFunction, double cellSize) {
        this(course, heightFunction, cellSize, 0.08, 0.35);
    }

    /**
     * Creates the planning grid used by A*. A cell is marked blocked if one of
     * its sample points is below the water margin or too close to a tree after
     * applying obstacle padding.
     */
    public GridMap(
            Course course,
            HeightFunction heightFunction,
            double cellSize,
            double waterSafetyMargin,
            double obstaclePadding
    ) {
        this.cellSize = cellSize;
        this.waterSafetyMargin = waterSafetyMargin;
        this.obstaclePadding = obstaclePadding;

        double[] bounds = computeBounds(course);
        this.minX = bounds[0];
        this.minY = bounds[1];
        double maxX = bounds[2];
        double maxY = bounds[3];
        this.cols = Math.max(1, (int) Math.ceil((maxX - minX) / cellSize));
        this.rows = Math.max(1, (int) Math.ceil((maxY - minY) / cellSize));
        this.free = new boolean[rows][cols];

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                free[row][col] = !isBlockedAtCenter(course, heightFunction, col, row);
            }
        }
    }

    public double getCellSize() {
        return cellSize;
    }

    public int getCols() {
        return cols;
    }

    public int getRows() {
        return  rows;
    }

    public boolean inBounds(int col, int row) {
        return col >= 0 && col < cols && row >= 0 && row < rows;
    }

    public boolean  isFree(int col, int row) {
        return inBounds(col, row) && free[row][col];
    }

    public double[] gridToWorld(int col, int row) {
        double x = minX +  (col + 0.5) * cellSize;
        double y = minY + (row + 0.5) * cellSize;
        return new double[]{x, y};
    }

    /**
     * Converts a world position to a grid cell and clamps it to the known map
     * bounds. This keeps pathfinding stable when the ball or target lies just
     * outside the computed course rectangle.
     */
    public int[] worldToGrid(double x, double y) {
        int col = (int) Math.floor((x - minX) / cellSize);
        int row = (int) Math.floor((y - minY) / cellSize);
        col = Math.max(0, Math.min(cols - 1, col));
        row = Math.max(0,  Math.min(rows - 1, row));
        return new int[]{col, row};
    }

    /**
     * Finds the nearest free cell by breadth-first search. This lets A* start
     * or finish near an obstacle without immediately failing because the exact
     * mapped cell was blocked.
     */
    public int[] nearestFreeCell(int col, int row) {
        if (isFree(col, row)) {
            return new int[]{col, row};
        }
        boolean[][] visited = new boolean[rows][cols];
        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{col, row});
        visited[row][col] = true;

        int[][] directions = {{1, 0},  {-1, 0}, {0, 1}, {0, -1}};

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int c = current[0];
            int r = current[1];

            if (isFree(c, r)) {
                return new int[]{c, r};
            }

            for (int[] d : directions) {
                int nc = c + d[0];
                int nr = r + d[1];
                if (inBounds(nc, nr) && !visited[nr][nc]) {
                    visited[nr][nc] = true;
                    queue.add(new int[]{nc, nr});
                }
            }
        }

        return new int[]{col, row};
    }

    /** Returns true if every grid cell touched by the segment is free. */
    public boolean isSegmentFree(double x0, double y0, double x1, double y1) {
        return isSegmentFree(x0, y0, x1, y1, null, 0.0);
    }

    /**
     * Samples along a world-space segment and checks both the touched grid
     * cells and, when supplied, the actual terrain height at the sampled points.
     * This is used for line-of-sight simplification and waypoint reachability.
     */
    public boolean isSegmentFree(
            double x0,double y0,
            double x1, double y1,
            HeightFunction heightFunction,
            double minHeight
    ) {
        double length = Math.hypot(x1 - x0, y1 - y0);
        if (length < 1e-9) {
            int[] cell = worldToGrid(x0, y0);
            return isFree(cell[0], cell[1]);
        }

        int steps = Math.max(2, (int) Math.ceil(length / (cellSize * 0.25)));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double x = x0 + t * (x1 - x0);
            double y = y0 + t * (y1 - y0);
            int[] cell = worldToGrid(x, y);
            if (!isFree(cell[0], cell[1])) {
                return false;
            }
            if (heightFunction != null && heightFunction.evaluate(x, y) < minHeight) {
                return false;
            }
        }
        return true;
    }

    private double[] computeBounds(Course course) {
        double minX = Math.min(course.getStartX(), course.getTargetX());
        double minY = Math.min(course.getStartY(), course.getTargetY());
        double maxX = Math.max(course.getStartX(), course.getTargetX());
        double maxY = Math.max(course.getStartY(), course.getTargetY());

        if (course.getTrees() != null) {
            for (Course.Tree tree : course.getTrees()) {
                minX = Math.min(minX, tree.x - tree.radius);
                minY = Math.min(minY, tree.y - tree.radius);
                maxX = Math.max(maxX, tree.x + tree.radius);
                maxY = Math.max(maxY, tree.y + tree.radius);
            }
        }

        double padding = 2.0;
        return new double[]{minX - padding,minY - padding,maxX + padding,maxY + padding};
    }

    /**
     * Tests the center and four nearby points of a cell. The extra samples make
     * narrow tree/water contacts less likely to slip through the grid.
     */
    private boolean isBlockedAtCenter(Course course, HeightFunction heightFunction, int col, int row) {
        double half = cellSize * 0.45;
        double[][] sampleOffsets = {
                {0.0, 0.0},{half, 0.0}, {-half, 0.0}, {0.0, half}, {0.0, -half}
        };
        double[] center = gridToWorld(col, row);
        for (double[] offset : sampleOffsets) {
            double x = center[0] + offset[0];
            double y = center[1] + offset[1];

            if (heightFunction.evaluate(x, y) < waterSafetyMargin) {
                return true;
            }

            if (course.getTrees() != null) {
                for (Course.Tree tree : course.getTrees()) {
                    double dist = Math.hypot(x - tree.x, y - tree.y);
                    if (dist <= tree.radius + obstaclePadding) {
                        return true;
                    }
                }}}
        return false;
    }
}

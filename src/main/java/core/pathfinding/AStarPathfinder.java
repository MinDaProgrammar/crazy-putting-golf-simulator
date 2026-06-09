package core.pathfinding;

import core.Vector2D;
import core.course.HeightFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Finds a shortest path  using the A* algorithm.
 */
public class AStarPathfinder {

    private static final int[][] CARDINAL_NEIGHBORS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}
    };

    private static final int[][] ALL_NEIGHBORS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    private AStarPathfinder() {
    }

    public static List<Vector2D> findPath(GridMap grid,double startX,double startY, double goalX,double goalY) {
        return findPath(grid, startX, startY, goalX, goalY, true);
    }

    public static List<Vector2D> findPath(GridMap grid,double startX, double startY,double goalX,double goalY,boolean fourConnected) {
        return findPath(grid, startX, startY, goalX, goalY, fourConnected, null);
    }

    /**
     * Finds a path through the grid and returns it as world-space points. The
     * start and goal are snapped to the nearest free cells; the last path point
     * is replaced with the exact goal position so later bot logic still aims at
     * the real hole instead of only the center of its grid cell.
     *
     * @param fourConnected true for cardinal movement only, false to allow diagonals
     * @param heightFunction optional terrain cost penalty for low ground
     * @return path from start to goal, or an empty list when no grid route exists
     */
    public static List<Vector2D> findPath(
            GridMap grid,
            double startX,
            double startY,
            double goalX,
            double goalY,
            boolean fourConnected,
            HeightFunction heightFunction
    ) {
        int[][] neighbors = fourConnected ? CARDINAL_NEIGHBORS : ALL_NEIGHBORS;
        int[] start = grid.worldToGrid(startX, startY);
        int[] goal =grid.worldToGrid(goalX, goalY);

        start= grid.nearestFreeCell(start[0], start[1]);
        goal = grid.nearestFreeCell(goal[0], goal[1]);

        if (start[0] == goal[0] && start[1] == goal[1]) {
            return List.of(new Vector2D(goalX, goalY));
        }

        String startKey = cellKey(start[0], start[1]);
        String goalKey = cellKey(goal[0], goal[1]);

        PriorityQueue<Node> open = new PriorityQueue<>();

        Map<String,Double> gScore =new HashMap<>();
        Map<String,String> cameFrom = new HashMap<>();
        Set<String> closed = new HashSet<>();

        gScore.put(startKey, 0.0);
        open.add(new Node(start[0], start[1], heuristic(grid, start[0], start[1], goal[0], goal[1])));

        while (!open.isEmpty()) {
            Node current = open.poll();
            String currentKey = cellKey(current.col, current.row);

            if (closed.contains(currentKey)) {continue;}
            closed.add(currentKey);

            if (currentKey.equals(goalKey)) {
                return reconstructPath(grid, cameFrom, current.col, current.row, goalX, goalY);
            }

            for (int[] offset : neighbors) {
                int nextCol = current.col + offset[0];
                int nextRow = current.row + offset[1];
                if (!grid.isFree(nextCol, nextRow)) {
                    continue;
                }

                double stepCost = grid.getCellSize();
                if (!fourConnected && offset[0] != 0 && offset[1] != 0) {
                    stepCost *= Math.sqrt(2.0);
                }
                if (heightFunction != null) {
                    double[] world = grid.gridToWorld(nextCol, nextRow);
                    double height = heightFunction.evaluate(world[0], world[1]);
                    stepCost += Math.max(0.0, 0.15 - height) * 3.0;
                }

                String nextKey = cellKey(nextCol, nextRow);
                double tentativeG = gScore.getOrDefault(currentKey, Double.POSITIVE_INFINITY) + stepCost;

                if (tentativeG >= gScore.getOrDefault(nextKey, Double.POSITIVE_INFINITY)) {
                    continue;
                }

                cameFrom.put(nextKey, currentKey);
                gScore.put(nextKey, tentativeG);
                double f = tentativeG + heuristic(grid, nextCol, nextRow, goal[0], goal[1]);
                open.add(new Node(nextCol, nextRow, f));
            }
        }

        return Collections.emptyList();
    }

    private static List<Vector2D> reconstructPath(
            GridMap grid,
            Map<String, String> cameFrom,
            int goalCol,
            int goalRow,
            double goalX,
            double goalY
    ) {
        List<Vector2D> path = new ArrayList<>();
        String currentKey = cellKey(goalCol, goalRow);

        while (currentKey != null) {
            int col = keyCol(currentKey);
            int row = keyRow(currentKey);
            double[] world = grid.gridToWorld(col, row);
            path.add(new Vector2D(world[0], world[1]));
            currentKey = cameFrom.get(currentKey);
        }

        Collections.reverse(path);
        path.set(path.size() - 1, new Vector2D(goalX, goalY));
        return path;
    }

    private static double heuristic(GridMap grid, int col, int row, int goalCol, int goalRow) {
        double[] current = grid.gridToWorld(col, row);
        double[] goal = grid.gridToWorld(goalCol, goalRow);
        return Math.hypot(current[0] - goal[0], current[1] - goal[1]);
    }

    private static String cellKey(int col, int row) {
        return col + "," + row;
    }

    private static int keyCol(String key) {
        return Integer.parseInt(key.split(",")[0]);
    }

    private static int keyRow(String key) {
        return Integer.parseInt(key.split(",")[1]);
    }

    private static final class Node implements Comparable<Node> {
        private final int col;
        private final int row;
        private final double fScore;

        private Node(int col, int row, double fScore) {
            this.col = col;
            this.row = row;
            this.fScore = fScore;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.fScore, other.fScore);
        }
    }
}

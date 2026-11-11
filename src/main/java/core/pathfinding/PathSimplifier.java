package core.pathfinding;

import core.Vector2D;
import core.course.HeightFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * Reduces a grid path to the minimal set of waypoints using line-of-sight
 * simplification: keeps only points where the direct segment from the last
 * kept point is no longer grid-free, collapsing diagonal staircases into
 * single straight legs.
 */
public class PathSimplifier {

    private PathSimplifier() {
    }

    /**
     * Collapses a raw A* path by keeping an anchor point until the next direct
     * segment becomes blocked. This reduces grid stair-steps into a smaller set
     * of aim points while preserving the grid's obstacle and height checks.
     */
    public static List<Vector2D> simplify(GridMap grid, HeightFunction heightFunction, List<Vector2D> path) {
        if (path == null || path.size() <= 2) {
            return path;
        }

        List<Vector2D> result = new ArrayList<>();
        result.add(path.get(0));

        int anchor = 0;
        for (int i = 2; i < path.size(); i++) {
            Vector2D from = path.get(anchor);
            Vector2D to   = path.get(i);
            if (!grid.isSegmentFree(from.x(), from.y(), to.x(), to.y(), heightFunction, 0.08)) {
                // The direct segment to i is blocked — keep i-1 as a waypoint
                result.add(path.get(i - 1));
                anchor = i - 1;
            }
        }

        result.add(path.get(path.size() - 1));
        return result;
    }
}

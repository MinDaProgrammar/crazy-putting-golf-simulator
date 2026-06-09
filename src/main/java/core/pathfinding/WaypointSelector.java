package core.pathfinding;

import core.Vector2D;
import core.course.HeightFunction;

import java.util.List;

/**
 * Picks the next waypoint from the simplified A* path.
 *
 * Strategy (in order of preference for each candidate corner):
 *   1. Direct shot to the corner — simplest and safest if the segment is free.
 *   2. Axis-aligned (L-shaped) intermediate — only if direct is blocked but one
 *      of the two axis-aligned legs is clear.
 *
 * This order prevents L-shaped legs from skimming along y=const or x=const
 * lines that run parallel to obstacle edges.
 */
public class WaypointSelector {

    private static final double MIN_TERRAIN_HEIGHT = 0.01;
    /** How far past a waypoint to check for obstacles in the overshoot zone. */
    private static final double OVERSHOOT_BUFFER = 1.0;
    private static final double FALLBACK_MIN_DISTANCE = 2.0;

    private WaypointSelector() {
    }

    /**
     * Chooses the next aim point from the simplified path first, then from the
     * raw A* path if needed. The final target is returned only when the direct
     * segment to it is free; otherwise the selector tries reachable path points
     * and finally falls back to the current ball position.
     */
    public static Vector2D select(
            GridMap grid,
            HeightFunction heightFunction,
            List<Vector2D> path,
            List<Vector2D> fallbackPath,
            double ballX,
            double ballY,
            double targetX,
            double targetY,
            double minAimDistance,
            double maxAimDistance
    ) {
        boolean directTargetClear = grid.isSegmentFree(
                ballX, ballY, targetX, targetY, heightFunction, MIN_TERRAIN_HEIGHT);

        // If we can reach the target directly, do it.
        double distToTarget = Math.hypot(ballX - targetX, ballY - targetY);
        if (distToTarget <= maxAimDistance && directTargetClear) {
            return new Vector2D(targetX, targetY);
        }

        if (path == null || path.isEmpty()) {
            return directTargetClear
                    ? new Vector2D(targetX, targetY)
                    : new Vector2D(ballX, ballY);
        }

        double ballDistToTarget = Math.hypot(ballX - targetX, ballY - targetY);

        for (Vector2D corner : path) {
            if (isTarget(corner, targetX, targetY) && !directTargetClear) {
                continue;
            }
            double distance = Math.hypot(corner.x() - ballX, corner.y() - ballY);
            if (distance < minAimDistance) {
                continue;
            }
            // Skip corners that are farther from the target than we are — going backwards.
            double cornerDistToTarget = Math.hypot(corner.x() - targetX, corner.y() - targetY);
            if (cornerDistToTarget >= ballDistToTarget) {
                continue;
            }

            // 1. Try direct shot to this corner (also verify overshoot zone is clear)
            if (distance <= maxAimDistance
                    && grid.isSegmentFree(
                            ballX, ballY, corner.x(), corner.y(), heightFunction, MIN_TERRAIN_HEIGHT)
                    && isOvershootClear(grid, heightFunction, ballX, ballY, corner.x(), corner.y())) {
                return corner;
            }

            // 2. Try L-shaped aim only if direct is blocked
            if (distance <= maxAimDistance) {
                Vector2D aim = axisAlignedAim(ballX, ballY, corner);
                double aimDistance = Math.hypot(aim.x() - ballX, aim.y() - ballY);
                if (aimDistance >= minAimDistance
                        && aimDistance <= maxAimDistance
                        && grid.isSegmentFree(
                                ballX, ballY, aim.x(), aim.y(), heightFunction, MIN_TERRAIN_HEIGHT)
                        && isOvershootClear(grid, heightFunction, ballX, ballY, aim.x(), aim.y())) {
                    return aim;
                }

                // Try the other axis-aligned leg
                Vector2D aimAlt = axisAlignedAimAlt(ballX, ballY, corner);
                double aimAltDistance = Math.hypot(aimAlt.x() - ballX, aimAlt.y() - ballY);
                if (aimAltDistance >= minAimDistance
                        && aimAltDistance <= maxAimDistance
                        && grid.isSegmentFree(
                                ballX, ballY, aimAlt.x(), aimAlt.y(), heightFunction, MIN_TERRAIN_HEIGHT)
                        && isOvershootClear(grid, heightFunction, ballX, ballY, aimAlt.x(), aimAlt.y())) {
                    return aimAlt;
                }
            }
        }

        Vector2D fallback = selectPathFallback(
                grid, heightFunction, fallbackPath, ballX, ballY, targetX, targetY,
                directTargetClear, maxAimDistance);
        if (fallback != null) {
            return fallback;
        }

        fallback = selectPathFallback(
                grid, heightFunction, path, ballX, ballY, targetX, targetY,
                directTargetClear, maxAimDistance);
        if (fallback != null) {
            return fallback;
        }

        return directTargetClear
                ? new Vector2D(targetX, targetY)
                : new Vector2D(ballX, ballY);
    }

    /**
     * Last-resort waypoint choice. This walks path points in order and returns
     * the first reachable point that is far enough to be worth aiming at, while
     * avoiding the blocked final target fallback that made MazeMLBot behave like
     * plain MLBot on maze courses.
     */
    private static Vector2D selectPathFallback(
            GridMap grid,
            HeightFunction heightFunction,
            List<Vector2D> candidates,
            double ballX,
            double ballY,
            double targetX,
            double targetY,
            boolean directTargetClear,
            double maxAimDistance
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        for (Vector2D candidate : candidates) {
            if (isTarget(candidate, targetX, targetY) && !directTargetClear) {
                continue;
            }
            double distance = Math.hypot(candidate.x() - ballX, candidate.y() - ballY);
            if (distance < FALLBACK_MIN_DISTANCE || distance > maxAimDistance) {
                continue;
            }
            if (grid.isSegmentFree(
                    ballX, ballY, candidate.x(), candidate.y(), heightFunction, MIN_TERRAIN_HEIGHT)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isTarget(Vector2D point, double targetX, double targetY) {
        return Math.hypot(point.x() - targetX, point.y() - targetY) < 1e-6;
    }

    /**
     * Checks that the OVERSHOOT_BUFFER meters past the aim point (in the same direction
     * of travel) is free of obstacles. This prevents the ball from rolling into a tree
     * that sits directly behind the waypoint.
     */
    private static boolean isOvershootClear(
            GridMap grid, HeightFunction heightFunction,
            double ballX, double ballY, double aimX, double aimY) {
        double dx = aimX - ballX;
        double dy = aimY - ballY;
        double dist = Math.hypot(dx, dy);
        if (dist < 1e-9) return true;
        double ux = dx / dist;
        double uy = dy / dist;
        double extX = aimX + ux * OVERSHOOT_BUFFER;
        double extY = aimY + uy * OVERSHOOT_BUFFER;
        return grid.isSegmentFree(aimX, aimY, extX, extY, heightFunction, MIN_TERRAIN_HEIGHT);
    }

    /** Horizontal-first intermediate: (corner.x, ballY) */
    private static Vector2D axisAlignedAim(double ballX, double ballY, Vector2D corner) {
        double dx = Math.abs(corner.x() - ballX);
        double dy = Math.abs(corner.y() - ballY);
        if (dy >= dx) {
            // vertical-first: move to ball's x, corner's y
            return new Vector2D(ballX, corner.y());
        }
        // horizontal-first: move to corner's x, ball's y
        return new Vector2D(corner.x(), ballY);
    }

    /** The alternative axis leg (opposite of axisAlignedAim) */
    private static Vector2D axisAlignedAimAlt(double ballX, double ballY, Vector2D corner) {
        double dx = Math.abs(corner.x() - ballX);
        double dy = Math.abs(corner.y() - ballY);
        if (dy >= dx) {
            // return horizontal-first instead
            return new Vector2D(corner.x(), ballY);
        }
        // return vertical-first instead
        return new Vector2D(ballX, corner.y());
    }
}

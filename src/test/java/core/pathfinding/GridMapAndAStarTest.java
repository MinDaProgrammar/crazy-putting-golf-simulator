package core.pathfinding;

import core.Vector2D;
import core.course.Course;
import core.course.HeightFunction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GridMapAndAStarTest {

    @Test
    void gridBlocksWaterButAStarFindsDetourAroundIt() {
        Course course = baseCourse(0.0, 0.0, 4.0, 0.0);
        HeightFunction waterWall = (x, y) -> Math.abs(x - 2.0) < 0.35 && y < 1.5 ? -1.0 : 1.0;
        GridMap grid = new GridMap(course, waterWall, 0.5, 0.0, 0.0);

        assertFalse(grid.isSegmentFree(0.0, 0.0, 4.0, 0.0, waterWall, 0.0));

        List<Vector2D> path = AStarPathfinder.findPath(grid, 0.0, 0.0, 4.0, 0.0, true, waterWall);

        assertFalse(path.isEmpty());
        assertTrue(path.stream().anyMatch(point -> point.y() > 1.5), "Path should route above the water wall");
    }

    @Test
    void gridBlocksTreeOnDirectSegment() {
        Course course = baseCourse(0.0, 0.0, 4.0, 0.0);
        course.addTree(new Course.Tree(2.0, 0.0, 0.6));
        HeightFunction flat = (x, y) -> 1.0;
        GridMap grid = new GridMap(course, flat, 0.5, 0.0, 0.0);

        assertFalse(grid.isSegmentFree(0.0, 0.0, 4.0, 0.0, flat, 0.0));
    }

    @Test
    void waypointSelectorFallsBackToPathInsteadOfBlockedTarget() {
        Course course = baseCourse(0.0, 0.0, 4.0, 0.0);
        course.addTree(new Course.Tree(2.0, 0.0, 0.6));
        HeightFunction flat = (x, y) -> 1.0;
        GridMap grid = new GridMap(course, flat, 0.5, 0.0, 0.0);
        List<Vector2D> path = List.of(
                new Vector2D(0.0, 0.0),
                new Vector2D(0.0, 2.0),
                new Vector2D(4.0, 2.0),
                new Vector2D(4.0, 0.0));

        Vector2D selected = WaypointSelector.select(
                grid, flat, path, path, 0.0, 0.0, 4.0, 0.0, 5.0, 10.0);

        assertFalse(
                Math.hypot(selected.x() - 4.0, selected.y()) < 1e-6,
                "Blocked target must not be used as the fallback aim point");
        assertTrue(
                grid.isSegmentFree(0.0, 0.0, selected.x(), selected.y(), flat, 0.01),
                "Fallback waypoint should be reachable from the current ball position");
    }

    private static Course baseCourse(double startX, double startY, double targetX, double targetY) {
        Course course = new Course();
        course.setHeightProfile("1");
        course.setFrictionKinetic(0.08);
        course.setFrictionStatic(0.2);
        course.setStartX(startX);
        course.setStartY(startY);
        course.setTargetX(targetX);
        course.setTargetY(targetY);
        course.setTargetRadius(0.1);
        course.setVMax(5.0);
        return course;
    }
}

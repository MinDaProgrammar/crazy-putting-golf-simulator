package core.course;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeightParserTest {

    @Test
    void evaluatesManualTestCourseExpression() {
        HeightFunction h = HeightFunction.compileExpression("0.25 * sin((x + y) / 10) + 1");

        double value = h.evaluate(7.0, 8.0);

        assertEquals(0.25 * Math.sin(1.5) + 1.0, value, 1e-12);
    }

    @Test
    void evaluatesNestedGaussianCourseExpression() {
        String expression = "0.3 + 0.12 * sin(x / 7.0) + 0.10 * cos(y / 7.0)"
                + " - 0.55 * exp(-((x - 9.0)^2 + (y - 9.0)^2) / 18.0)";
        HeightFunction h = HeightFunction.compileExpression(expression);

        double value = h.evaluate(9.0, 9.0);

        double expected = 0.3 + 0.12 * Math.sin(9.0 / 7.0) + 0.10 * Math.cos(9.0 / 7.0) - 0.55;
        assertEquals(expected, value, 1e-12);
    }
}

package core.course;

import java.util.Map;

public final class HeightParser {
    private HeightParser() {} // prevent instantiation
    public static HeightFunction parse(String expression) {
        var tokens = HeightTokenizer.tokenize(expression);
        var rpn = HeightTokenizer.shunting(tokens);
        return (x, y) -> HeightTokenizer.evaluator(rpn, Map.of("x", x, "y", y));
    }
}
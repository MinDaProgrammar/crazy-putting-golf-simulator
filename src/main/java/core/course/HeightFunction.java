package core.course;

@FunctionalInterface
public interface HeightFunction {
    double evaluate(double x, double y);

    static HeightFunction compileExpression(String expression){
        return HeightParser.parse(expression);
    }

}

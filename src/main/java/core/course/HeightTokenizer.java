package core.course;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import static java.util.Map.of;

enum TokenType {
    NUMBER,
    VARIABLE,
    MATH_FUNCTION,
    OPERATOR,
    UNARY_OPERATOR,
    LPAREN,
    RPAREN
}

/**
 * <p>
 *     
 *     <p/>
 *
 */
class HeightTokenizer {

    public record Token(TokenType type, String value){}
    private static final Set<String> MATHFUNCTIONS = Set.of(
            "sin", "cos", "tan", "sqrt", "log", "exp",
            "pow", "acos", "asin", "atan", "cbrt",
            "cosh", "log10", "sinh", "tanh");

    private static final Map<String, Function<Double, Double>> MathFunctionMap = of(
            "sin", Math::sin,
            "cos", Math::cos,
            "tan", Math::tan,
            "sqrt", Math::sqrt,
            "log", Math::log,
            "exp", Math::exp,
            "acos",Math::acos,
            "asin",Math::asin,
            "cbrt",Math::cbrt,
            "log10",Math::log10);

    private static final Map<String, BinaryOperator<Double>> OperatorMap = of(
            "+", Double::sum,
            "-", (a, b) -> a - b,
            "*", (a, b) -> a * b,
            "/", (a, b) -> a / b,
            "^", Math::pow
    );



    private static final Set<Character> OPERATORS = Set.of(
            '+', '-', '*', '/', '^');

    private static final Map<String, Integer> PRECEDENCE = of(
            "+", 1,
            "-", 1,
            "*", 2,
            "/", 2,
            "^", 3,
            "u-", 4);

    private static final Map<String, Boolean> RIGHT_ASSOC = of(
            "^", true, "u-", true);

    static List<Token> tokenize(String expression){
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        while(i < expression.length()){
            char c = expression.charAt(i);
            // Skip whitespace
            if (Character.isWhitespace(c)){
                i++;
                continue;
            }
            if(c == '-'){
                boolean isUnaryOperator =
                        //case the unary op is at the start of expression
                        tokens.isEmpty() ||
                        // case unary op follows another op
                        tokens.getLast().type == TokenType.OPERATOR||
                        // case it follows a left parentheses
                        tokens.getLast().type == TokenType.LPAREN||
                         //case it follows a function name
                       tokens.getLast().type == TokenType.MATH_FUNCTION;
                if(isUnaryOperator){
                    tokens.add(new Token(TokenType.UNARY_OPERATOR, "u-"));
                }else{
                    tokens.add(new Token(TokenType.OPERATOR, "-"));
                }
                i++;
                continue;
            }
            // Number (integer or decimal)
            if(Character.isDigit(c) || c == '.'){
                int start = i;
                while(i < expression.length() &&
                        (Character.isDigit(expression.charAt(i)) || expression.charAt(i) == '.')){
                    i++;
                }
                tokens.add(new Token(TokenType.NUMBER, expression.substring(start, i)));
                continue;
            }
            // Variable or function (letters)
            if(Character.isLetter(c)){
                int start = i;
                while(i < expression.length() && Character.isLetter(expression.charAt(i))){
                    i++;
                }
                String word = expression.substring(start, i);
                if(MATHFUNCTIONS.contains(word)){
                    tokens.add(new Token(TokenType.MATH_FUNCTION, word));
                }else{
                    tokens.add(new Token(TokenType.VARIABLE, word));
                }
                continue;
            }
            // Operators
            if(OPERATORS.contains(c)){
                tokens.add(new Token(TokenType.OPERATOR, Character.toString(c)));
                i++;
                continue;
            }
            // Parentheses
            if(c == '('){
                tokens.add(new Token(TokenType.LPAREN, "("));
                i++;
                continue;
            }
            if(c == ')'){
                tokens.add(new Token(TokenType.RPAREN, ")"));
                i++;
                continue;
            }
            throw new RuntimeException("Unexpected character: " + c);
        }
        return tokens;
    }

    static Queue<Token> shunting(List<Token> tokens){
        Stack<Token> stack = new Stack<>();
        Queue<Token> queue = new LinkedList<>();

        for(Token token : tokens){
            switch (token.type) {

                case NUMBER:
                case VARIABLE:
                    queue.add(token);
                    break;
                case MATH_FUNCTION:
                case LPAREN:
                    stack.push(token);
                    break;
                case OPERATOR:
                case UNARY_OPERATOR:
                    while(!stack.isEmpty()){
                        Token top = stack.peek();
                        // Only compare with operators (binary or unary)
                        if (top.type != TokenType.OPERATOR &&
                                top.type != TokenType.UNARY_OPERATOR)
                            break;
                        int p1 = PRECEDENCE.get(token.value);
                        int p2 = PRECEDENCE.get(top.value);
                        boolean leftAssoc =
                                !RIGHT_ASSOC.getOrDefault(token.value, false);
                        if ((leftAssoc && p1 <= p2) ||
                                (!leftAssoc && p1 < p2)) {
                            queue.add(stack.pop());
                        }else{
                            break;
                        }
                    }
                    stack.push(token);
                    break;
                case RPAREN:
                    while(!stack.isEmpty() &&
                            stack.peek().type != TokenType.LPAREN){
                        queue.add(stack.pop());
                    }
                    stack.pop(); // remove '('
                    // If a function is on top, pop it too
                    if(!stack.isEmpty() &&
                            stack.peek().type == TokenType.MATH_FUNCTION){
                        queue.add(stack.pop());
                    }
                    break;
            }
        }

        while(!stack.isEmpty()){
            queue.add(stack.pop());
        }
        return queue;
    }

    private static Map<String, Double> getValues(double x, double y){
        Map<String, Double> values = new HashMap<>();
        values.put("x", x);
        values.put("y", y);
        return values;
    }

    static double evaluator(Queue<Token> tokens, Map<String, Double> x_y_values) {
        Stack<Double> stack = new Stack<>();
        for(Token token : tokens){
            if(token.type == TokenType.NUMBER){
                stack.push(Double.parseDouble(token.value));
            }
            else if(token.type == TokenType.VARIABLE){
               stack.push(x_y_values.get(token.value));
            }
            else if(token.type == TokenType.OPERATOR){
                double v =  stack.pop();
                if(stack.isEmpty()){
                    if(Objects.equals(token.value, "-")){
                        stack.push(-v);
                    }
                    else if(Objects.equals(token.value, "+")){
                        stack.push(v);
                    }
                }else{
                    double v2 =  stack.pop();
                    double result = OperatorMap.get(token.value).apply(v2,v);
                    stack.push(result);
                }
            }
            else if(token.type == TokenType.MATH_FUNCTION){
                double v =  stack.pop();
                double result = MathFunctionMap.get(token.value).apply(v);
                stack.push(result);
            }
            else if(token.type == TokenType.UNARY_OPERATOR){
                double v =  stack.pop();
                stack.push(-v);
            }
        }
        return stack.pop();
    }

    public static double evaluate(String expression, double x, double y) {
        return
                evaluator(shunting(tokenize(expression)), getValues(x, y));
    }
}

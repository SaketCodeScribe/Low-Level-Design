package CompositeDesign.ArithmeticExpression;

public class Expression implements ArithmeticExpressionIntf{
    Operation expr;
    ArithmeticExpressionIntf leftExpression, rightExpression;

    public Expression(Operation expr, ArithmeticExpressionIntf leftExpression, ArithmeticExpressionIntf rightExpression) {
        this.expr = expr;
        this.leftExpression = leftExpression;
        this.rightExpression = rightExpression;
    }

    @Override
    public long evaluate() throws Exception {
        switch (expr){
            case ADD:
                return leftExpression.evaluate()+rightExpression.evaluate();
            case SUBTRACT:
                return leftExpression.evaluate()-rightExpression.evaluate();
            case MULTIPLY:
                return leftExpression.evaluate()*rightExpression.evaluate();
            default:
                throw new Exception("wrong expression");
        }
    }
}

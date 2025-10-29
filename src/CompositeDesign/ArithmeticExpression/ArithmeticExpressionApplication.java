package CompositeDesign.ArithmeticExpression;

public class ArithmeticExpressionApplication {
    public static void main(String[] args) throws Exception {
        ArithmeticExpressionIntf two = new Number(2);

        ArithmeticExpressionIntf one = new Number(1);
        ArithmeticExpressionIntf seven = new Number(7);


        ArithmeticExpressionIntf addExpression = new Expression(Operation.ADD, one,seven);

        ArithmeticExpressionIntf parentExpression = new Expression(Operation.MULTIPLY, two,addExpression);

        System.out.println(parentExpression.evaluate());
    }
}

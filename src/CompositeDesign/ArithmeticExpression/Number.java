package CompositeDesign.ArithmeticExpression;

public class Number implements ArithmeticExpressionIntf{
    long data;

    public Number(long data) {
        this.data = data;
    }

    @Override
    public long evaluate() {
        return data;
    }
}

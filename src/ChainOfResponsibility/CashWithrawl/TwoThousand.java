package ChainOfResponsibility.CashWithrawl;

public class TwoThousand implements Cash{
    private int value = Denomination.TWO_THOUNSAND.val;
    private int count;

    public TwoThousand(int count) {
        this.count = count;
    }

    @Override
    public int money(int amount) {
        int cnt = Math.min(amount/value, count);
        count -= Math.min(amount/value, count);
        return cnt;
    }
    @Override
    public Denomination denomination(){
        return Denomination.TWO_THOUNSAND;
    }
}

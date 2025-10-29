package ChainOfResponsibility.CashWithrawl;

public class One  implements Cash{
    private int value = Denomination.ONE.val;
    private int count;

    public One(int count) {
        this.count = count;
    }

    @Override
    public int money(int amount) {
        int cnt = Math.min(amount/value, count);
        count -= Math.min(amount/value, count);
        return cnt;
    }@Override
    public Denomination denomination(){
        return Denomination.ONE;
    }
}

package ChainOfResponsibility.CashWithrawl;

public class Hundred  implements Cash{
    private int value = Denomination.HUNDRED.val;
    private int count;

    public Hundred(int count) {
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
        return Denomination.HUNDRED;
    }
}

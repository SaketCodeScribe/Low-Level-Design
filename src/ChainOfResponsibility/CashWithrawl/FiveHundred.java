package ChainOfResponsibility.CashWithrawl;

public class FiveHundred  implements Cash{
    private int value = Denomination.Five_HUNDRED.val;
    private int count;

    public FiveHundred(int count) {
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
        return Denomination.Five_HUNDRED;
    }
}

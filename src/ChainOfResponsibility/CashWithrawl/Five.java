package ChainOfResponsibility.CashWithrawl;

public class Five  implements Cash{
    private int value = Denomination.FIVE.val;
    private int count;

    public Five(int count) {
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
        return Denomination.FIVE;
    }
}

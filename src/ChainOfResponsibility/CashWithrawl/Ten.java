package ChainOfResponsibility.CashWithrawl;

public class Ten  implements Cash {
    private int value = Denomination.TEN.val;
    private int count;

    public Ten(int count) {
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
        return Denomination.TEN;
    }
}
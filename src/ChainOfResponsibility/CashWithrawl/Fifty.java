package ChainOfResponsibility.CashWithrawl;

public class Fifty implements Cash{
    private int value = Denomination.FIFTY.val;
    private int count;

    public Fifty(int count) {
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
        return Denomination.FIFTY;
    }
}

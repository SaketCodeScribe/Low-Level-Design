package ChainOfResponsibility.CashWithrawl;

public interface Cash {
    public int money(int amount);

    public Denomination denomination();
}

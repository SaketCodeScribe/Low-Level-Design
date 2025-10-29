package ChainOfResponsibility.CashWithrawl;

public enum Denomination {
    TWO_THOUNSAND(2000),
    Five_HUNDRED(500),
    HUNDRED(100),
    FIFTY(50),
    TEN(10),
    FIVE(5),
    ONE(1);

    public int val;

    Denomination(int val) {
        this.val = val;
    }
}

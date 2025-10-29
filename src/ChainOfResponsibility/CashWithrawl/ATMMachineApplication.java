package ChainOfResponsibility.CashWithrawl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ATMMachineApplication {
    public static void main(String[] args) throws Exception {
        Cash one = new One(136);
        Cash five = new Five(24);
        Cash Ten = new Ten(100);
        Cash fifty = new Fifty(21);
        Cash hundred = new Hundred(30);
        Cash fiveHundred = new FiveHundred(4);
        Cash twoThousand = new TwoThousand(2);
        List<Cash> cashes = Arrays.asList(twoThousand, fiveHundred, hundred, five, one);
        Collections.sort(cashes, (a,b) -> Integer.compare(b.denomination().val, a.denomination().val));

        ATM atm = new ATM(cashes);

        atm.getMoney(3523);
        atm.getMoney(3751);
        atm.getMoney(516);
        atm.getMoney(0);
    }
}

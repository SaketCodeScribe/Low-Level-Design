package ChainOfResponsibility.CashWithrawl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ATM {
    List<Cash> cashes;

    public ATM(List<Cash> cash) {
        this.cashes = cash;
    }
    public List<List<Integer>> getMoney(int amount) throws Exception {
        if (amount <= 0)
            throw new Exception("Amount should be positive");
        List<List<Integer>> ans = new ArrayList<>();
        for(Cash cash:cashes){
            if (amount == 0)
                break;
            int count = cash.money(amount);
            int denomination = cash.denomination().val;
            ans.add(Arrays.asList(count, denomination));
            amount -= count*denomination;
//            System.out.println(amount+" "+ans);

        }
        if (amount > 0)
            throw new Exception("Amount could not be served");
        System.out.println(ans);
        return ans;
    }
}

package Problems.ManagementSystems.ATM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public class ATM {
    private AtmState state;
    private Transaction transaction;
    private final BankService bankService;
    private final CashDispenser cashDispenser;

    public ATM(BankService bankService, CashDispenser cashDispenser) {
        this.state = Idle.getInstance();
        this.bankService = bankService;
        this.cashDispenser = cashDispenser;
    }

    public void insertCard(Card card) {
        transaction = new Transaction(card);
        state = state.insertCard();
    }

    public void authenticate(String pin) {
        state = state.authenticate(transaction, bankService, pin);
    }

    public void selectTransactionType(TransactionType transactionType) {
        state = state.selectTransactionType(transaction, transactionType);
    }

    public void cashWithdrawal(double amt) {
        state = state.cashWithdrawal(transaction, amt, bankService, cashDispenser);
    }

    public void cashDeposit(List<Map.Entry<Denomination, Integer>> cash) {
        state = state.cashDeposit(transaction, cash, bankService, cashDispenser);
    }

    public void balanceInquiry() {
        state = state.balanceInquiry(transaction, bankService);
    }

    public void cancel(){
        state = Idle.getInstance();
    }
}

class Card{
    private final String cardNo;
    private final String pin;
    private final String userId;

    public Card(String cardNo, String pin, String userId) {
        this.cardNo = cardNo;
        this.pin = pin;
        this.userId = userId;
    }

    public String getCardNo() {
        return cardNo;
    }

    public String getPin() {
        return pin;
    }

    public String getUserId() {
        return userId;
    }
}

class Account{
    private final User user;
    private final Card card;
    private final String accountNo;
    private AtomicReference<Double> amount;

    public Account(User user, Card card, String accountNo) {
        this.user = user;
        this.card = card;
        this.accountNo = accountNo;
        this.amount = new AtomicReference<Double>(0d);
    }

    public User getUser() {
        return user;
    }

    public Card getCard() {
        return card;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public double getAmount() {
        return amount.get();
    }

    public boolean setAmount(double amount) {
        while(true){
            Double oldValue = this.getAmount();
            Double newAmt = amount + oldValue;
            if (newAmt < 0) return false;
            if (this.amount.compareAndSet(oldValue, newAmt)){
                return true;
            }
        }
    }
}

class User{
    private final String userName;
    private final String userId;

    public User(String userName, String userId) {
        this.userName = userName;
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserId() {
        return userId;
    }
}

enum TransactionType{
    WITHDRAW,
    DEPOSIT,
    BALANCE_INQUIRY;
}

enum Denomination{
    THOUSAND(1000),
    FIVE_HUNDRED(500),
    HUNDRED(100);

    private final int value;

    Denomination(int value){
        this.value = value;
    }

    public static int getValue(Denomination denomination){
        return denomination.value;
    }
}
class Transaction{
    private TransactionType transactionType;
    private final Card card;

    public Transaction(Card card) {
        this.card = card;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public Card getCard() {
        return card;
    }
}

class BankService{
    ConcurrentMap<String, Account> accountDetails;

    public Account getAccDetails(String userId){
        return accountDetails.get(userId);
    }

    public void withdraw(String userId, double amt){
        assert amt >= 0;
        Account acc = accountDetails.get(userId);
        if (acc != null){
            updateBalance(acc, -amt);
        }
    }


    public void deposit(String userId, double amt){
        assert amt >= 0;
        Account acc = accountDetails.get(userId);
        if (acc != null){
            updateBalance(acc, amt);
        }
    }
    public void updateBalance(Account acc, double amt){
        acc.setAmount(amt);
    }

    public double inquire(String userId){
        Account acc = accountDetails.get(userId);
        assert acc != null;
        return acc.getAmount();
    }

    public boolean authenticate(String userId, String ping){
        Account acc = accountDetails.get(userId);
        return ping.equals(acc.getCard().getPin());
    }
}

class CashDispenser {
    private final Map<Denomination, Integer> denominations = new HashMap<>();

    private static final Denomination[] values = Denomination.values();
    public CashDispenser(){
        for(Denomination d:Denomination.values()){
            denominations.put(d, 0);
        }
    }

    public void deposit(Transaction transaction, BankService bankService, Map<Denomination, Integer> cash){
        double totalCash = 0;
        for(Map.Entry<Denomination, Integer> c:cash.entrySet()){
            denominations.compute(c.getKey(), (k, v) -> v + c.getValue());
            totalCash += Denomination.getValue(c.getKey())* c.getValue();
        }
        if (!updateAmount(transaction, bankService, totalCash)){
            System.out.println("Unable to connect to BankServer");
            rollback(cash);
        }
    }

    public Map<Denomination, Integer> withdraw(Transaction transaction, BankService bankService, double amt){
        List<Denomination> l = new ArrayList<>();

        if (amt <= 0) throw new AtmException("Amount > 0");

        if (amt > bankService.inquire(transaction.getCard().getUserId())) throw new AtmException("Insufficient fund");

        double remaining = amt;
        Map<Denomination, Integer> toDispense = new HashMap<>();

        for (Denomination d : values) {
            int available = denominations.getOrDefault(d, 0);
            if (available == 0) continue;

            int needed = (int) (remaining / Denomination.getValue(d));
            int dispenseCount = Math.min(available, needed);
            if (dispenseCount > 0) {
                toDispense.put(d, dispenseCount);
                remaining -= dispenseCount * Denomination.getValue(d);
                l.add(d);
            }
        }

        if (remaining > 0) {
            throw new AtmException("Cannot dispense exact amount with available denominations. Please input multiple of denomination: "+ l);
        }

        toDispense.forEach((denomination, count) -> {
            denominations.compute(denomination, (k, v) -> v - count);
        });if (updateAmount(transaction, bankService, -amt)) {
            updateCash(toDispense);
        }
        else {
            System.out.println("Unable to connect to BankServer");
            rollback(toDispense);
        }
        return toDispense;
    }

    private void rollback(Map<Denomination, Integer> l) {
        l.forEach((key, value) -> denominations.compute(key, (k, v) -> v + value));
    }

    private void updateCash(Map<Denomination, Integer> l) {
        l.forEach((key, value) -> denominations.compute(key, (k,v) -> v + value));
    }

    private boolean updateAmount(Transaction transaction, BankService bankService, double amt) {
        Account account = bankService.getAccDetails(transaction.getCard().getUserId());
        return account.setAmount(amt);
    }

    static class Entry{
        private final Denomination denomination;
        private final Integer value;

        public Entry(Denomination denomination, Integer value) {
            this.denomination = denomination;
            this.value = value;
        }

        public Denomination getDenomination() {
            return denomination;
        }

        public Integer getValue() {
            return value;
        }
    }
}

interface AtmState {
    public AtmState insertCard();

    public AtmState authenticate(Transaction transaction, BankService bankService, String pin);

    AtmState selectTransactionType(Transaction transaction, TransactionType transactionType);

    public AtmState cashWithdrawal(Transaction transaction, double amt, BankService bankService, CashDispenser cashDispenser);

    AtmState cashDeposit(Transaction transaction, Map<Denomination, Integer> cash, BankService bankService, CashDispenser cashDispenser);

    public AtmState balanceInquiry(Transaction transaction, BankService bankService);

    public AtmState ejectCard();

}

class Idle implements AtmState{
    private static volatile AtmState instance = null;

    public static AtmState getInstance(){
        if (instance == null){
            synchronized (AtmState.class){
                if (instance == null){
                    instance = new Idle();
                }
            }
        }
        return instance;
    }

    @Override
    public AtmState insertCard() {
        return CardInserted.getInstance();
    }

    @Override
    public AtmState authenticate(Transaction transaction, BankService bankService, String pin) {
        throw new AtmException("not supported");
    }

    @Override
    public AtmState selectTransactionType(Transaction transaction, TransactionType transactionType) {
        throw new AtmException("not supported");
    }

    @Override
    public AtmState cashWithdrawal(Transaction transaction, double amt, BankService bankService, CashDispenser cashDispenser) {
        throw new AtmException("not supported");
    }

    @Override
    public AtmState cashDeposit(Transaction transaction, Map<Denomination, Integer> cash, BankService bankService, CashDispenser cashDispenser) {
        throw new AtmException("not supported");
    }

    @Override
    public AtmState balanceInquiry(Transaction transaction, BankService bankService) {
        throw new AtmException("not supported");
    }

    @Override
    public AtmState ejectCard() {
        return getInstance();
    }
}
class CardInserted implements AtmState{
    private final int MAX_ATTEMPT = 3;
    private static volatile AtmState instance = null;

    public static AtmState getInstance(){
        if (instance == null){
            synchronized (AtmState.class){
                if (instance == null){
                    instance = new CardInserted();
                }
            }
        }
        return instance;
    }
    @Override
    public AtmState insertCard() {
        throw new AtmException("not supported");
    }

    @Override
    public AtmState authenticate(Transaction transaction, BankService bankService, String pin) {
        int r = 0;
        while(r < MAX_ATTEMPT) {
            if (bankService.authenticate(transaction.getCard().getUserId(), pin)){
                return Authenticated.getInstance();
            }
            r++;
        }
        return ejectCard();
    }

    @Override
    public AtmState selectTransactionType(Transaction transaction, TransactionType transactionType) {
        throw new AtmException("not supported");
    }

    @Override
    public AtmState cashWithdrawal(Transaction transaction, double amt, BankService bankService, CashDispenser cashDispenser) {
        throw new AtmException("not supported");
    }

    @Override
    public AtmState cashDeposit(Transaction transaction, Map<Denomination, Integer> cash, BankService bankService, CashDispenser cashDispenser) {
        throw new AtmException("not supported");
    }

    @Override
    public AtmState balanceInquiry(Transaction transaction, BankService bankService) {
        throw new AtmException("not supported");
    }

    @Override
    public AtmState ejectCard() {
        return Idle.getInstance();
    }
}
class Authenticated implements AtmState{

    private static volatile AtmState instance = null;

    public static AtmState getInstance(){
        if (instance == null){
            synchronized (AtmState.class){
                if (instance == null){
                    instance = new Authenticated();
                }
            }
        }
        return instance;
    }

    @Override
    public AtmState insertCard() {
        throw new AtmException("not supported");
    }

    @Override
    public AtmState authenticate(Transaction transaction, BankService bankService, String pin) {
        throw new AtmException("not supported");
    }

    @Override
    public AtmState selectTransactionType(Transaction transaction, TransactionType transactionType){
        transaction.setTransactionType(transactionType);
        switch (transactionType){
            case WITHDRAW -> {
                return CashWithdrawal.getInstance();
            }
            case DEPOSIT -> {
                return CashDeposit.getInstance();
            }
            case BALANCE_INQUIRY -> {
                return BalanceInquiry.getInstance();
            }
            default -> {
                return ejectCard();
            }
        }
    }
    @Override
    public AtmState cashWithdrawal(Transaction transaction, double amt, BankService bankService, CashDispenser cashDispenser) {
        throw new AtmException("not supported");
    }

    @Override
    public AtmState cashDeposit(Transaction transaction, Map<Denomination, Integer> cash, BankService bankService, CashDispenser cashDispenser) {
        throw new AtmException("not supported");
    }

    @Override
    public AtmState balanceInquiry(Transaction transaction, BankService bankService) {
        throw new AtmException("not supported");
    }

    @Override
    public AtmState ejectCard() {
        return Idle.getInstance();
    }
}

class CashWithdrawal implements AtmState{

    private static volatile AtmState instance = null;

    public static AtmState getInstance(){
        if (instance == null){
            synchronized (AtmState.class){
                if (instance == null){
                    instance = new CashWithdrawal();
                }
            }
        }
        return instance;
    }

    @Override
    public AtmState insertCard() {
        throw new AtmException("not supported");
    }

    @Override
    public AtmState authenticate(Transaction transaction, BankService bankService, String pin) {
        throw new AtmException("not supported");
    }

    @Override
    public AtmState selectTransactionType(Transaction transaction, TransactionType transactionType) {
        throw new AtmException("not supported");
    }

    @Override
    public AtmState cashWithdrawal(Transaction transaction, double amt, BankService bankService, CashDispenser cashDispenser) {
        Map<Denomination, Integer> cash = cashDispenser.withdraw(transaction, bankService, amt);
        System.out.println(cash);
        return ejectCard();
    }

    @Override
    public AtmState cashDeposit(Transaction transaction, Map<Denomination, Integer> cash, BankService bankService, CashDispenser cashDispenser) {
        throw new AtmException("not supported");
    }

    @Override
    public AtmState balanceInquiry(Transaction transaction, BankService bankService) {
        throw new AtmException("not supported");
    }

    @Override
    public AtmState ejectCard() {
        return Idle.getInstance();
    }
}

class CashDeposit implements AtmState{

    private static volatile AtmState instance = null;

    public static AtmState getInstance(){
        if (instance == null){
            synchronized (AtmState.class){
                if (instance == null){
                    instance = new CashWithdrawal();
                }
            }
        }
        return instance;
    }

    @Override
    public AtmState insertCard() {
        throw new AtmException("not supported");
    }

    @Override
    public AtmState authenticate(Transaction transaction, BankService bankService, String pin) {
        throw new AtmException("not supported");
    }

    @Override
    public AtmState selectTransactionType(Transaction transaction, TransactionType transactionType) {
        throw new AtmException("not supported");
    }

    @Override
    public AtmState cashWithdrawal(Transaction transaction, double amt, BankService bankService, CashDispenser cashDispenser) {
        throw new AtmException("not supported");

    }

    @Override
    public AtmState cashDeposit(Transaction transaction, Map<Denomination, Integer> cash, BankService bankService, CashDispenser cashDispenser) {
        cashDispenser.deposit(transaction,bankService, cash);
        return ejectCard();
    }

    @Override
    public AtmState balanceInquiry(Transaction transaction, BankService bankService) {
        throw new AtmException("not supported");
    }

    @Override
    public AtmState ejectCard() {
        return Idle.getInstance();
    }
}



class BalanceInquiry implements AtmState{

    private static volatile AtmState instance = null;

    public static AtmState getInstance(){
        if (instance == null){
            synchronized (AtmState.class){
                if (instance == null){
                    instance = new BalanceInquiry();
                }
            }
        }
        return instance;
    }

    @Override
    public AtmState insertCard() {
        throw new AtmException("not supported");
    }

    @Override
    public AtmState authenticate(Transaction transaction, BankService bankService, String pin) {
        throw new AtmException("not supported");
    }

    @Override
    public AtmState selectTransactionType(Transaction transaction, TransactionType transactionType) {
        throw new AtmException("not supported");
    }

    @Override
    public AtmState cashWithdrawal(Transaction transaction, double amt, BankService bankService, CashDispenser cashDispenser) {
        throw new AtmException("not supported");
    }

    @Override
    public AtmState cashDeposit(Transaction transaction, Map<Denomination, Integer> cash, BankService bankService, CashDispenser cashDispenser) {
        throw new AtmException("not supported");
    }


    @Override
    public AtmState balanceInquiry(Transaction transaction, BankService bankService) {
        System.out.println(bankService.inquire(transaction.getCard().getUserId()));
        return ejectCard();
    }

    @Override
    public AtmState ejectCard() {
        return Idle.getInstance();
    }
}

class AtmException extends RuntimeException{
    public AtmException(String mssg){
        super(mssg);
    }
}
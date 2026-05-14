package Problems.ManagementSystems.ATM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class ATM {
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

    private boolean sufficientFund(Account acc, double amount){
        return acc != null && Double.compare(acc.getAmount(), amount) == 0;
    }
    public void withdraw(String userId, double amt){
        assert amt >= 0;
        Account acc = accountDetails.get(userId);
        if (sufficientFund(acc, amt)){
            updateBalance(acc, -amt);
        }
    }


    public void deposit(String userId, double amt){
        assert amt >= 0;
        Account acc = accountDetails.get(userId);
        if (sufficientFund(acc, amt)){
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
}

class CashDispenser {
    private final Map<Denomination, Integer> denominations = new HashMap<>();

    private static final Denomination[] values = Denomination.values();
    public CashDispenser(){
        for(Denomination d:Denomination.values()){
            denominations.put(d, 0);
        }
    }

    public void deposit(List<Map.Entry<Denomination, Integer>> cash){
        for(Map.Entry<Denomination, Integer> c:cash){
            denominations.compute(c.getKey(), (k, v) -> v + c.getValue());
        }
    }

    public void withdraw(Transaction transaction, BankService bankService, double amt){
        List<Entry> l = new ArrayList<>();

        for(Denomination d:values){
            int cnt = denominations.get(d);
            if (cnt == 0) continue;
            int withDraw = (int)(amt/Denomination.getValue(d));
            int min = Math.min(cnt, withDraw);
            denominations.put(d, cnt - min);
            if (min > 0){
                l.add(new Entry(d, -min));
            }
            amt -= Denomination.getValue(d) * Math.min(cnt, withDraw);
        }
        if (amt > 0) throw new AtmException("Please entry in multiples of denomination of: "+ l.stream().map(Entry::getKey).collect(Collectors.toList())toString());
        if (updateAmount(transaction, bankService, amt)) {
            updateCash(l);
        }
        else {
            throw new AtmException("Unable to connect to BankServer");
        }
    }

    private void updateCash(List<Entry> l) {
        l.forEach(entry -> denominations.compute(entry.denomination, (k,v) -> v + entry.getValue()));
    }

    private boolean updateAmount(Transaction transaction, BankService bankService, double amt) {
        Account account = bankService.getAccDetails(transaction.getCard().getUserId());
        return account.setAmount(-amt);
    }

    public void deposit(Map<Denomination, Integer> map){
        List<Entry> l = new ArrayList<>();
        updateCash(map.entrySet().stream().map(entry -> new Entry(entry.getKey(), entry.getValue())).collect(Collectors.toList()));
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
    public void insertCard(Card card);

    public boolean authenticate(BankService bankService);

    public boolean cashWithdrawal(double amt, BankService bankService, CashDispenser cashDispenser);

    public boolean cashDeposit(double amt, BankService bankService, CashDispenser cashDispenser);

    public double balanceInquiry(BankService bankService);

    public void ejectCard();

}

class Idle implements AtmState{
    @Override
    public void insertCard(Card card) {

    }

    @Override
    public boolean authenticate(BankService bankService) {
        return false;
    }

    @Override
    public boolean cashWithdrawal(double amt, BankService bankService, CashDispenser cashDispenser) {
        return false;
    }

    @Override
    public boolean cashDeposit(double amt, BankService bankService, CashDispenser cashDispenser) {
        return false;
    }

    @Override
    public double balanceInquiry(BankService bankService) {
        return 0;
    }

    @Override
    public void ejectCard() {

    }
}
class CardInserted implements AtmState{
    @Override
    public void insertCard(Card card) {

    }

    @Override
    public boolean authenticate(BankService bankService) {
        return false;
    }

    @Override
    public boolean cashWithdrawal(double amt, BankService bankService, CashDispenser cashDispenser) {
        return false;
    }

    @Override
    public boolean cashDeposit(double amt, BankService bankService, CashDispenser cashDispenser) {
        return false;
    }

    @Override
    public double balanceInquiry(BankService bankService) {
        return 0;
    }

    @Override
    public void ejectCard() {

    }
}
class Authenticated implements AtmState{
    @Override
    public void insertCard(Card card) {

    }

    @Override
    public boolean authenticate(BankService bankService) {
        return false;
    }

    @Override
    public boolean cashWithdrawal(double amt, BankService bankService, CashDispenser cashDispenser) {
        return false;
    }

    @Override
    public boolean cashDeposit(double amt, BankService bankService, CashDispenser cashDispenser) {
        return false;
    }

    @Override
    public double balanceInquiry(BankService bankService) {
        return 0;
    }

    @Override
    public void ejectCard() {

    }
}

class AtmException extends RuntimeException{
    public AtmException(String mssg){
        super(mssg);
    }
}
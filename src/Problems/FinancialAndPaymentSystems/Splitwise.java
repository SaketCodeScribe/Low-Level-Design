package Problems.FinancialAndPaymentSystems;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * FRs:
 * 1. Expense can be of type - group and non-group
 * 2. User can create and delete a group
 * 3. Deleting a group will move non-settled expense to non-group expense
 * 4. Users can only leave a group if he doesn't have any dues
 * 5. Users can settle amount.
 * 6. Notification of adding/settling expense should be sent to user
 * <p>
 * NFRs:
 * 1. Modular - Class should follow OOD principle for clear separation of concern & better testability
 * 2. Extensible - Classes should be extensible for future features
 * 3. Scalability - System should be scalable to handle growing no of users
 * 4. Concurrency - System should handle concurrent reads & writes
 * 5. Consistency - Data should be consistent
 */
public class Splitwise {
    enum ExpenseType {
        Group,
        Non_Group;
    }


    enum Transaction {
        Settled,
        Due;
    }

    enum UserAction {
        Added,
        Leave,
        Settle,
        Due;
    }

    interface Observable {
        void notifyAllObservers(User user, String mssg, UserAction action);
    }

    interface Observer {
        void updateStateChange(User user, String message, UserAction action);
    }

    interface SplittingStrategy {
        Map<User, Map<User, Double>> split(Map<User, Map<User, Double>> owe, Record record);
    }

    record User(String name) {
    }

    record Record(String description, User user, Map<User, Double> amount, Transaction transaction,
                  ZonedDateTime creationTime) {
        public long recordId(){
            return description.hashCode()+creationTime.toEpochSecond();
        }
    }

    static class ExpenseSplitter implements SplittingStrategy {
        public Map<User, Map<User, Double>> split(Map<User, Map<User, Double>> owe, Record record) {
            updateOwe(owe, record);
            List<Map.Entry<User, Double>> amount = traverseOwe(owe);
            amount.sort(Comparator.comparingDouble(Map.Entry::getValue));
            return split(amount);
        }

        private Map<User, Map<User, Double>> split(List<Map.Entry<User, Double>> amount) {
            int i = 0, j = amount.size() - 1;
            Map<User, Map<User, Double>> owe = new HashMap<>();

            while (i < j) {
                Map.Entry<User, Double> entry1 = amount.get(i);
                Map.Entry<User, Double> entry2 = amount.get(j);
                if (entry1.getValue() == 0) {
                    i++;
                    continue;
                }
                if (entry2.getValue() == 0) {
                    j--;
                    continue;
                }
                if (entry1.getValue() > 0 || entry2.getValue() < 0) break;
                User usrA = entry1.getKey();
                double amtA = Math.abs(entry1.getValue());
                User usrB = entry2.getKey();
                double amtB = entry2.getValue();
                if (amtA <= amtB) {
                    owe.compute(usrA, (key, value) -> {
                        if (value == null) {
                            value = new HashMap<>();
                        }
                        value.compute(usrB, (k, v) -> {
                            if (v == null) throw new RuntimeException("Data is inconsistent");
                            return v - amtA;
                        });
                        return value;
                    });
                    entry2.setValue(entry2.getValue() - amtA);
                    i++;
                    if (amtA == amtB) j--;
                } else if (amtA > amtB) {
                    owe.compute(usrA, (key, value) -> {
                        if (value == null) {
                            value = new HashMap<>();
                        }
                        value.compute(usrB, (k, v) -> v == null ? amtB : v + amtA);
                        return value;
                    });
                    entry1.setValue(entry1.getValue() - amtB);
                    j--;
                }
            }
            return owe;
        }

        private List<Map.Entry<User, Double>> traverseOwe(Map<User, Map<User, Double>> owe) {
            Map<User, Double> amount = new HashMap<>();
            for (Map.Entry<User, Map<User, Double>> entry : owe.entrySet()) {
                User payer = entry.getKey();
                double amt = 0;
                for (Map.Entry<User, Double> mapEntry : entry.getValue().entrySet()) {
                    amt -= amount.compute(mapEntry.getKey(), (k, v) -> {
                        if (v == null) v = 0d;
                        return v + mapEntry.getValue();
                    });
                }
                amount.put(payer, amount.getOrDefault(payer, 0d) + amt);
            }
            return amount.entrySet().stream().toList();
        }

        private void updateOwe(Map<User, Map<User, Double>> graph, Record record) {
            User user = record.user();
            var amt = record.amount();

            switch (record.transaction()) {
                case Settled -> settle(user, graph, amt);
                case Due -> dues(user, graph, amt);
                default -> throw new IllegalArgumentException(record.transaction() + " not supported");
            }
        }

        private void settle(User settler, Map<User, Map<User, Double>> graph, Map<User, Double> amounts) {
            graph.compute(settler, (key, value) -> {
                if (value == null) throw new RuntimeException("Data is inconsistent");
                for (Map.Entry<User, Double> entry : amounts.entrySet()) {
                    User payee = entry.getKey();
                    Double money = entry.getValue();
                    value.compute(payee, (k, v) -> v == null ? -money : v - money);
                }
                return value;
            });
        }

        private void dues(User lender, Map<User, Map<User, Double>> graph, Map<User, Double> amounts) {
            for (Map.Entry<User, Double> entry : amounts.entrySet()) {
                User payee = entry.getKey();
                Double money = entry.getValue();
                graph.compute(payee, (key, value) -> {
                    if (value == null) value = new HashMap<>();
                    value.compute(lender, (k, v) -> v == null ? money : v + money);
                    return value;
                });
            }
        }
    }

    static class Expense {
        String name;
        Set<User> users;
        Map<Long, Record> records;
        ExpenseGraph expenseGraph;

        public Expense(String name, ExpenseSplitter splitter) {
            this.name = name;
            this.users = ConcurrentHashMap.newKeySet();
            this.records = new LinkedHashMap<>();
            this.expenseGraph = new ExpenseGraph(splitter);
        }

        public void add(User user) {
            users.add(user);
        }

        public boolean checkIfUserExists(User user) {
            return users.contains(user);
        }

        public boolean remove(User user) {
            return users.remove(user);
        }

        public void append(Record record) {
            records.putIfAbsent(record.recordId(), record);
        }

        public List<Record> records() {
            return new ArrayList<>(records.values());
        }

        public ExpenseGraph getExpenseGraph() {
            return expenseGraph;
        }

        public void updateSplit(Record record) {
            this.expenseGraph.split(record);
        }

        public Set<User> getUsers() {
            return users;
        }

        public String name() {
            return name;
        }

        @Override
        public String toString() {
            return "Expense{" +
                    "name='" + name + '\'' +
                    ", users=" + users +
                    ", records=" + records +
                    '}';
        }

        public void remove(Record record) {
            records.remove(record.recordId());
        }

        public Record getRecord(Record record) {
            return records.get(record.recordId());
        }
    }

    static class ExpenseGraph {
        private final ExpenseSplitter splitter;
        private Map<User, Map<User, Double>> graph;

        public ExpenseGraph(ExpenseSplitter splitter) {
            this.graph = new HashMap<>();
            this.splitter = splitter;
        }

        public boolean userHasDebt(User user) {
            return graph.get(user) != null;
        }

        public void split(Record record) {
            this.graph = splitter.split(graph, record);
        }

        public Map<User, Double> getUsersDebt(User user) {
            return this.graph.getOrDefault(user, Map.of());
        }

        public Map<User, Map<User, Double>> getExpenseDebt() {
            return this.graph;
        }


    }

    static class ExpenseManagementService implements Observable {
        private final Map<String, Expense> expenses;
        private final Map<String, ReentrantReadWriteLock> resourceMonitor;
        private final Set<Observer> observers;
        private final ExpenseSplitter splitter;

        public ExpenseManagementService(ExpenseSplitter splitter) {
            this.expenses = new HashMap<>();
            this.observers = ConcurrentHashMap.newKeySet();
            this.splitter = splitter;
            this.resourceMonitor = new HashMap<>();
        }

        private static String createMessage(Record record) {
            return null;
        }

        private static String getNonExpenseGroupName(String creator, Optional<User> payer) {
            if (payer.isEmpty()) throw new IllegalStateException("payer can't be null!");
            String p = payer.get().name();
            return creator.compareTo(p) <= 0 ? creator + "_" + p + "_Non-Expense" : p + "_" + creator + "_Non-Expense";
        }

        public void add(Observer ob) {
            this.observers.add(ob);
        }

        public void remove(Observer ob) {
            this.observers.remove(ob);
        }

        public void create(String creator, Set<User> users, String expenseName, ExpenseType expenseType) {
            resourceMonitor.compute(expenseName, (key, value) -> {
                if (value == null) value = new ReentrantReadWriteLock(true);
                value.writeLock().lock();
                try {
                    expenses.putIfAbsent(expenseName, new Expense(expenseName, splitter));
                } finally {
                    value.writeLock().unlock();
                }
                for (User user : users) add(user, expenseName);
                return value;
            });
        }

        public void add(User user, String expenseName) {
            expenses.computeIfPresent(expenseName, (key, value) -> {
                value.add(user);
                notifyAllObservers(user, String.format("%s added to %s", user.name(), expenseName), UserAction.Added);
                return value;
            });
        }

        public void remove(User user, String expenseName) {
            resourceMonitor.computeIfPresent(expenseName, (key, value) -> {
                value.writeLock().lock();
                try {
                    Expense expense = expenses.get(key);
                    if (!expense.checkIfUserExists(user))
                        throw new IllegalStateException(user.name() + " doesn't exist in the group " + expenseName);
                    if (!expense.remove(user))
                        throw new IllegalStateException(user.name() + " has debt. Clear them to leave the group " + expenseName);
                    for (User usr : expense.getUsers())
                        notifyAllObservers(usr, String.format("%s left %s", user.name(), expenseName), UserAction.Leave);
                } finally {
                    value.writeLock().unlock();
                }
                return value;
            });
        }

        public void newTransaction(String expenseName, Record record) {
            resourceMonitor.computeIfPresent(expenseName, (key, value) -> {
                boolean transaction = false;
                Expense expense;
                value.writeLock().lock();
                expense = expenses.get(expenseName);
                try {
                    expense.append(record);
                    expense.updateSplit(record);
                    transaction = true;
                    notifyTransaction(expense, record);

                } finally {
                    if (!transaction) expense.remove(record);
                    value.writeLock().unlock();
                }
                return value;
            });
        }

        public void delete(String expenseName, Record record){
            resourceMonitor.computeIfPresent(expenseName, (key, value) -> {
                boolean transaction = false;
                Expense expense;
                value.writeLock().lock();
                expense = expenses.get(expenseName);
                try {
                    expense.remove(record);
                    expense.updateSplit(record);
                    transaction = true;
                } finally {
                    if (!transaction) expense.append(record);
                    value.writeLock().unlock();
                }
                return value;
            });
        }

        public void delete(String expenseName) {
            resourceMonitor.computeIfPresent(expenseName, (key, value) -> {
                boolean isRemoved = false;
                value.writeLock().lock();
                try {
                    expenses.remove(expenseName);
                    isRemoved = true;
                } finally {
                    if (isRemoved) value = null;
                    else value.writeLock().unlock();
                }
                return value;
            });
        }



        public void edit(String expenseName, Record record) {
            resourceMonitor.computeIfPresent(expenseName, (key, value) -> {
                boolean transaction = false;
                Expense expense;
                Record rec;
                value.writeLock().lock();
                expense = expenses.get(expenseName);
                rec = expense.getRecord(record);
                try {
                    expense.remove(record);
                    expense.append(record);
                    expense.updateSplit(record);
                    transaction = true;
                    notifyTransaction(expense, record);

                } finally {
                    if (!transaction) {
                        expense.remove(record);
                        expense.append(rec);
                    }
                    value.writeLock().unlock();
                }
                return value;
            });
        }

        private void notifyTransaction(Expense expense, Record record) {
            String expenseName = expense.name();
            switch (record.transaction()) {
                case Due -> {
                    notifyDue(expense.getExpenseGraph().getExpenseDebt(), expenseName);
                }
                case Settled -> {
                    notifySettle(record.amount(), record.user().name(), expenseName);
                }
                default -> throw new IllegalArgumentException(record.transaction() + " not supported");
            }
        }

        private void notifySettle(Map<User, Double> amount, String user, String expenseName) {
            for (Map.Entry<User, Double> entry : amount.entrySet()) {
                notifyAllObservers(entry.getKey(), String.format("%s settled %s: %f", user, entry.getKey().name(), entry.getValue()), UserAction.Settle);
            }
        }

        private void notifyDue(Map<User, Map<User, Double>> expense, String expenseName) {
            for (Map.Entry<User, Map<User, Double>> entry : expense.entrySet()) {
                User payer = entry.getKey();
                for (Map.Entry<User, Double> payee : entry.getValue().entrySet()) {
                    notifyAllObservers(payer, String.format("%s owe %s: %f", payer.name(), payee.getKey().name(), payee.getValue()), UserAction.Due);
                }
            }
        }

        @Override
        public void notifyAllObservers(User user, String mssg, UserAction action) {
            for (Observer ob : observers) {
                ob.updateStateChange(user, mssg, action);
            }
        }
    }

    static class Notification implements Observer {

        @Override
        public void updateStateChange(User user, String message, UserAction action) {
            System.out.println(message);
        }
    }

    static class SplitwiseFacade {
        private static volatile SplitwiseFacade INSTANCE;
        private final ExpenseManagementService expenseManagementService;

        private SplitwiseFacade(ExpenseManagementService expenseManagementService) {
            this.expenseManagementService = expenseManagementService;
        }

        public static SplitwiseFacade getInstance(ExpenseManagementService expenseManagementService) {
            if (INSTANCE == null) {
                synchronized (SplitwiseFacade.class) {
                    if (INSTANCE == null) {
                        INSTANCE = new SplitwiseFacade(expenseManagementService);
                    }
                }
            }
            return INSTANCE;
        }

        // --- Group lifecycle ---

        public void createGroup(String creator, Set<User> users, String groupName) {
            expenseManagementService.create(creator, users, groupName, ExpenseType.Group);
        }

        public void deleteGroup(String groupName) {
            expenseManagementService.delete(groupName);
        }

        public void addUserToGroup(User user, String groupName) {
            expenseManagementService.add(user, groupName);
        }

        public void removeUserFromGroup(User user, String groupName) {
            expenseManagementService.remove(user, groupName);
        }

        // --- Non-group (1:1) expense ---

        private void createNonGroupExpense(String creator, User other, String expenseName) {
            expenseManagementService.create(creator, Set.of(new User(creator), other), expenseName, ExpenseType.Non_Group);
        }

        // --- Transactions ---

        public void addExpense(String expenseName, String description, User paidBy, Map<User, Double> splits) {
            if ((expenseName == null || expenseName.isEmpty()) && splits.size() == 1) {
                User other = splits.keySet().stream().findFirst().get();
                expenseName = getNonGroupExpenseName(paidBy.name(), other);
                createNonGroupExpense(paidBy.name(), other, expenseName);
                expenseManagementService.create(paidBy.name(), splits.keySet(), expenseName, ExpenseType.Non_Group);
            }
            Record record = new Record(description, paidBy, splits, Transaction.Due, ZonedDateTime.now());
            expenseManagementService.newTransaction(expenseName, record);
        }

        public void settleUp(String expenseName, User settler, Map<User, Double> amounts) {
            Record record = new Record("Settlement", settler, amounts, Transaction.Settled, ZonedDateTime.now());
            expenseManagementService.newTransaction(expenseName, record);
        }

        public void delete(String expenseName, Record record) {
            expenseManagementService.delete(expenseName, record);
        }

        public void edit(String expenseName, Record record) {
            expenseManagementService.edit(expenseName, record);
        }

        // --- Observers ---

        public void registerObserver(Observer observer) {
            expenseManagementService.add(observer);
        }

        public void removeObserver(Observer observer) {
            expenseManagementService.remove(observer);
        }

        // --- Helpers ---

        private String getNonGroupExpenseName(String creator, User other) {
            String p = other.name();
            return creator.compareTo(p) <= 0 ? creator + "_" + p + "_Non-Expense" : p + "_" + creator + "_Non-Expense";
        }
    }

}

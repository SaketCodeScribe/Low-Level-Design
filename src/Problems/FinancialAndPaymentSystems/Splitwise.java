package Problems.FinancialAndPaymentSystems;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    enum Transaction {
        Settled,
        Due;
    }

    interface Observable {
        void notifyAllObservers(User user, String message);
    }

    interface Observer {
        void updateStateChange(User user, String message);
    }

    interface SplittingStrategy {
        Map<User, Map<User, Double>> split(Map<User, Map<User, Double>> owe, Record record);
    }

    static class ExpenseSplitter implements SplittingStrategy {
        public synchronized Map<User, Map<User, Double>> split(Map<User, Map<User, Double>> owe, Record record) {
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
                User usrA = entry1.getKey();
                double amtA = -entry1.getValue();
                User usrB = entry2.getKey();
                double amtB = entry2.getValue();
                if (amtA <= amtB) {
                    owe.compute(usrA, (key, value) -> {
                        if (value == null) {
                            value = new HashMap<>();
                        }
                        value.compute(usrB, (k, v) -> v == null ? amtA : v + amtA);
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

        private void updateOwe(Map<User, Map<User, Double>> owe, Record record) {
            User user = record.user();
            double amt = record.amount();
            for (User usr : record.splitBetween()) {
                owe.compute(usr, (key, value) -> {
                    if (value == null) {
                        value = new HashMap<>();
                    }
                    value.compute(user, (k, v) -> {
                        if (v == null) v = 0d;
                        v += amt;
                        return v;
                    });
                    return value;
                });
            }
        }
    }


    record User(String name) {
    }

    record Record(String description, Double amount, User user, Set<User> splitBetween, Transaction transaction,
                  ZonedDateTime creationTime) {
    }

    static class Notification implements Observer {
        @Override
        public void updateStateChange(User user, String message) {
            System.out.printf(message, user.name());
        }
    }

    static class Expense {
        String name;
        Set<User> users;
        Set<Record> records;

        public Expense(String name) {
            this.name = name;
            this.users = ConcurrentHashMap.newKeySet();
            this.records = new LinkedHashSet<>();
        }

        public void addUser(User user) {
            users.add(user);
        }

        public void removeUser(User user) {
            users.remove(user);
        }

        public synchronized void appendRecord(Record record) {
            records.add(record);
        }

        public List<Record> records() {
            return new ArrayList<>(records);
        }

        @Override
        public String toString() {
            return "Expense{" +
                    "name='" + name + '\'' +
                    ", users=" + users +
                    ", records=" + records +
                    '}';
        }
    }


}

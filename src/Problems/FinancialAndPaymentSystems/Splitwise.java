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
        Map<String, Map<String, Double>> split(Map<String, Map<String, Double>> owe, Record record);
    }

    static class ExpenseSplitter implements SplittingStrategy {

    }


    record User(String name) {
    }

    record Record(String description, Double amount, User user, String<User> splitBetween, Transaction transaction, ZonedDateTime creationTime) {
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

package Problems.RuleExecution;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class TripExpense {
}

enum VendorType { MEALS, ENTERTAINMENT, TRAVEL, OFFICE_SUPPLIES, OTHER }

record Expense(
        String expenseId,
        String tripId,
        BigDecimal amount,       // money -> BigDecimal
        String vendorName,
        VendorType vendorType,
        LocalDate date           // added so date-based rules (same vendor/day) are expressible
) {}

record Violation(
        String ruleId,
        String description,
        String groupKey,              // tripId for per-trip rules, "ALL" for global
        List<Expense> offendingExpenses
) {}
@FunctionalInterface
interface ExpenseScope {                         // per-expense filter
    boolean includes(Expense e);
    default ExpenseScope and(ExpenseScope o) { return e -> includes(e) && o.includes(e); }
    default ExpenseScope or (ExpenseScope o) { return e -> includes(e) || o.includes(e); }
    default ExpenseScope negate()            { return e -> !includes(e); }
}

@FunctionalInterface
interface AggregateCheck {                       // scoped list -> offenders (empty = compliant)
    List<Expense> offenders(List<Expense> scoped);
}

record ExpenseGroup(String key, List<Expense> expenses) {}

@FunctionalInterface
interface GrainSelector {                        // which groups this rule evaluates
    Collection<ExpenseGroup> groups(Map<String, List<Expense>> byTrip);
}
final class Scopes {
    static final ExpenseScope ANY = e -> true;
    static ExpenseScope ofType(VendorType t)    { return e -> e.vendorType() == t; }
    static ExpenseScope amountOver(BigDecimal x){ return e -> e.amount().compareTo(x) > 0; }
    static ExpenseScope vendor(String name)     { return e -> e.vendorName().equals(name); }
}

final class Checks {
    /** total(scoped) must be <= limit; offenders = the whole scoped set that blew it. */
    static AggregateCheck totalAtMost(BigDecimal limit) {
        return scoped -> {
            BigDecimal sum = scoped.stream().map(Expense::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
            return sum.compareTo(limit) > 0 ? scoped : List.of();
        };
    }
    /** anything in scope is itself a violation (e.g., a banned vendorType). */
    static AggregateCheck forbidden() {
        return scoped -> scoped;                 // empty scoped -> empty offenders -> compliant
    }
    /** custom/pairwise logic still fits as an AggregateCheck. */
    static AggregateCheck noDuplicateVendorPerDay() {
        return scoped -> scoped.stream()
                .collect(Collectors.groupingBy(e -> List.of(e.vendorName(), e.date())))
                .values().stream()
                .filter(bucket -> bucket.size() > 1)
                .flatMap(List::stream)
                .toList();
    }
}

final class Grains {
    static final GrainSelector PER_TRIP = byTrip ->
            byTrip.entrySet().stream()
                    .map(en -> new ExpenseGroup(en.getKey(), en.getValue()))
                    .toList();

    static final GrainSelector GLOBAL = byTrip -> {
        List<Expense> all = byTrip.values().stream().flatMap(List::stream).toList();
        return List.of(new ExpenseGroup("ALL", all));
    };

    static GrainSelector forTrips(Set<String> tripIds) {
        return byTrip -> byTrip.entrySet().stream()
                .filter(en -> tripIds.contains(en.getKey()))
                .map(en -> new ExpenseGroup(en.getKey(), en.getValue()))
                .toList();
    }
}

final class Rule {
    private final String ruleId;
    private final String description;
    private final GrainSelector grain;
    private final ExpenseScope scope;
    private final AggregateCheck check;

    Rule(String ruleId, String description, GrainSelector grain, ExpenseScope scope, AggregateCheck check) {
        this.ruleId = ruleId; this.description = description;
        this.grain = grain;   this.scope = scope;   this.check = check;
    }

    List<Violation> evaluate(Map<String, List<Expense>> byTrip) {
        List<Violation> violations = new ArrayList<>();
        for (ExpenseGroup group : grain.groups(byTrip)) {
            List<Expense> scoped = group.expenses().stream().filter(scope::includes).toList();
            List<Expense> offenders = check.offenders(scoped);
            if (!offenders.isEmpty()) {
                violations.add(new Violation(ruleId, description, group.key(), offenders));
            }
        }
        return violations;
    }
}

final class RuleEngine {
    static List<Violation> evaluate(List<Rule> rules, List<Expense> expenses) {
        // one-pass grouping by trip — O(n)
        Map<String, List<Expense>> byTrip =
                expenses.stream().collect(Collectors.groupingBy(Expense::tripId));

        List<Violation> result = new ArrayList<>();
        for (Rule rule : rules) {                 // O(R)
            result.addAll(rule.evaluate(byTrip)); // each rule scans its groups — O(n)
        }
        return result;                            // total O(n + R·n), space O(n)
    }
}
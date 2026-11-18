package Problems.FinancialAndPaymentSystems.AuditLoggingSystem;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class Main {

    public static void main(String[] args) throws Exception {
        IssueFacade facade = IssueFacade.getInstance();

        System.out.println("=== Creating Issues ===");
        facade.createIssue("T1", "Payment Related", "Payment Failed",
                "My payment failed but money is debited", "testUser1@test.com");

        facade.createIssue("T2", "Mutual Fund Related", "Purchase Failed",
                "Unable to purchase Mutual Fund", "testUser2@test.com");

        facade.createIssue("T3", "Payment Related", "Payment Failed",
                "My payment failed but money is debited", "testUser2@test.com");

        System.out.println("\n=== Adding Agents ===");
        facade.addAgent("agent1@test.com", "Agent 1",
                Arrays.asList("Payment Related", "Gold Related"));

        facade.addAgent("agent2@test.com", "Agent 2",
                Arrays.asList("Mutual Fund Related"));

        System.out.println("\n=== Assigning Issues ===");
        CompletableFuture<Agent> f1 = facade.assignIssue("I1");
        CompletableFuture<Agent> f2 = facade.assignIssue("I2");
        CompletableFuture<Agent> f3 = facade.assignIssue("I3");

        f1.join();
        f2.join();

        System.out.println("\n=== Get Issues by Email ===");
        facade.getIssue(new AbstractMap.SimpleEntry<>("email", "testUser2@test.com"));

        System.out.println("\n=== Get Issues by Type ===");
        facade.getIssue(new AbstractMap.SimpleEntry<>("type", "Payment Related"));

        System.out.println("\n=== Update Issue ===");
        facade.updateIssue("I3", "In Progress", "Waiting for payment confirmation");

        System.out.println("\n=== Resolve Issue I1 (frees Agent 1 for I3) ===");
        facade.resolveIssue("I1", "Payment reversed successfully");

        f3.join();

        Thread.sleep(500);

        System.out.println("\n=== Resolve Issue I3 ===");
        facade.resolveIssue("I3", "PaymentFailed debited amount will get reversed");

        System.out.println("\n=== View Agent Work History ===");
        facade.viewAgentsWorkHistory("agent1@test.com");
        facade.viewAgentsWorkHistory("agent2@test.com");

        facade.shutdown();
    }
}
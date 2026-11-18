package Problems.FinancialAndPaymentSystems.AuditLoggingSystem;

import java.util.concurrent.CompletableFuture;

public class IssueAssignment {

    private final Issue issue;
    private final CompletableFuture<AssignResult> future;

    public IssueAssignment(Issue issue, CompletableFuture<AssignResult> future) {
        this.issue = issue;
        this.future = future;
    }

    public Issue getIssue() {
        return issue;
    }

    public CompletableFuture<AssignResult> getFuture() {
        return future;
    }
}
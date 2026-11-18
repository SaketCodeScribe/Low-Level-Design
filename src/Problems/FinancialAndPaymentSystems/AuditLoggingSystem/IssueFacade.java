package Problems.FinancialAndPaymentSystems.AuditLoggingSystem;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class IssueFacade {

    private static volatile IssueFacade instance;
    private static final Object lock = new Object();

    private final IssueService issueService;
    private final AgentService agentService;
    private final NotifyService notifyService;

    private IssueFacade() {
        this.issueService = IssueService.getInstance();
        this.agentService = AgentService.getInstance();
        this.notifyService = new NotifyService();
    }

    public static IssueFacade getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new IssueFacade();
                }
            }
        }
        return instance;
    }

    public Issue createIssue(String transactionId, String issueTypeStr, String subject,
                             String description, String email) {
        IssueType issueType = IssueType.getType(issueTypeStr);
        if (issueType == null) {
            throw new IllegalArgumentException("Unknown issue type: " + issueTypeStr);
        }
        Issue issue = issueService.createIssue(transactionId, issueType, subject, description, email);
        System.out.println("Issue " + issue.getIssueId() + " created against transaction \"" + transactionId + "\"");
        return issue;
    }

    public Agent addAgent(String agentEmail, String agentName, List<String> issueTypeStrs) {
        List<IssueType> issueTypes = issueTypeStrs.stream()
                .map(IssueType::getType)
                .collect(Collectors.toList());
        Agent agent = agentService.createAgent(agentEmail, agentName, issueTypes);
        System.out.println("Agent " + agentName + " created");
        return agent;
    }

    public CompletableFuture<Agent> assignIssue(String issueId) {
        return agentService.assignIssue(issueId).thenApply(result -> {
            Agent agent = result.getAgent();
            System.out.println("Issue " + issueId + " assigned to " + agent.getAgentName());
            notifyService.notify(agent, issueService.getIssue(issueId));
            return agent;
        });
    }

    public List<Issue> getIssue(Map.Entry<String, String> filter) {
        List<Issue> issues = issueService.getFilter(filter);
        for (Issue issue : issues) {
            System.out.println(issue.getIssueId() + " " + issue);
        }
        return issues;
    }

    public Issue updateIssue(String issueId, String statusStr, String resolution) {
        IssueStatus status = IssueStatus.valueOf(statusStr.toUpperCase().replace(" ", "_"));
        Issue issue = issueService.updateIssue(issueId, status, resolution);
        System.out.println(issueId + " status updated to " + status);
        return issue;
    }

    public void resolveIssue(String issueId, String resolution) {
        agentService.resolveIssue(issueId, resolution);
        System.out.println(issueId + " issue marked resolved");
    }

    public void viewAgentsWorkHistory(String agentEmail) {
        Agent agent = agentService.getAgent(agentEmail);
        if (agent == null) {
            throw new IllegalArgumentException("Agent not found: " + agentEmail);
        }
        System.out.println("Work history for " + agent.getAgentName() + " (" + agentEmail + "):");
        BlockingQueue<Issue> history = agentService.getAgentHistory(agentEmail);
        if (history.isEmpty()) {
            System.out.println("  No issues assigned");
        }
        for (Issue issue : history) {
            System.out.println("  " + issue.getIssueId() + " [" + issue.getIssueType().getValue()
                    + "] " + issue.getSubject() + " - " + issue.getStatus());
        }
    }

    public void shutdown() {
        agentService.shutdown();
    }
}
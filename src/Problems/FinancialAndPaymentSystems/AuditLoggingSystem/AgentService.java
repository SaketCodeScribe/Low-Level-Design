package Problems.FinancialAndPaymentSystems.AuditLoggingSystem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class AgentService {

    private static volatile AgentService instance;
    private static final Object lock = new Object();

    private final Map<String, Agent> agents = new ConcurrentHashMap<>();
    private final Map<IssueType, BlockingQueue<IssueAssignment>> issueQueues = new HashMap<>();
    private final Map<String, Agent> issueAgentMap = new ConcurrentHashMap<>();
    private final IssueService issueService = IssueService.getInstance();
    private final AssignStrategy assignStrategy;

    private AgentService(AssignStrategy assignStrategy) {
        this.assignStrategy = assignStrategy;
        for (IssueType type : IssueType.values()) {
            issueQueues.put(type, new LinkedBlockingQueue<>());
        }
        this.assignStrategy.start(issueQueues, agents);
    }

    public static AgentService getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new AgentService(new AssignFirstAvailableAgent());
                }
            }
        }
        return instance;
    }

    public Agent createAgent(String agentEmail, String agentName, List<IssueType> issueTypes) {
        Agent agent = new Agent(agentEmail, agentName, issueTypes);
        agents.put(agentEmail, agent);
        return agent;
    }

    public CompletableFuture<AssignResult> assignIssue(String issueId) {
        Issue issue = issueService.getIssue(issueId);
        if (issue == null) {
            throw new IllegalArgumentException("Issue not found: " + issueId);
        }
        CompletableFuture<AssignResult> future = new CompletableFuture<>();
        IssueAssignment assignment = new IssueAssignment(issue, future);
        issueQueues.get(issue.getIssueType()).add(assignment);

        return future.thenApply(result -> {
            issueAgentMap.put(issueId, result.getAgent());
            return result;
        });
    }

    public void resolveIssue(String issueId, String resolution) {
        Issue issue = issueService.getIssue(issueId);
        if (issue == null) {
            throw new IllegalArgumentException("Issue not found: " + issueId);
        }
        issue.setStatus(IssueStatus.RESOLVED);
        issue.setResolution(resolution);

        Agent agent = issueAgentMap.remove(issueId);
        if (agent != null) {
            agent.setAgentStatus(AgentStatus.AVAILABLE);
        }
    }

    public BlockingQueue<Issue> getAgentHistory(String agentEmail) {
        Agent agent = agents.get(agentEmail);
        if (agent == null) {
            throw new IllegalArgumentException("Agent not found: " + agentEmail);
        }
        return agent.getHistory();
    }

    public Map<String, Agent> getAgents() {
        return agents;
    }

    public Agent getAgent(String agentEmail) {
        return agents.get(agentEmail);
    }

    public void shutdown() {
        assignStrategy.stop();
    }
}
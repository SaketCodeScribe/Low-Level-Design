package Problems.FinancialAndPaymentSystems.AuditLoggingSystem;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Agent {

    private String agentEmail;
    private String agentName;
    private List<IssueType> issueTypes;
    private volatile AgentStatus agentStatus;
    private final BlockingQueue<Issue> history = new LinkedBlockingQueue<>();

    public Agent() {
        this.agentStatus = AgentStatus.AVAILABLE;
    }

    public Agent(String agentEmail, String agentName, List<IssueType> issueTypes) {
        this.agentEmail = agentEmail;
        this.agentName = agentName;
        this.issueTypes = issueTypes;
        this.agentStatus = AgentStatus.AVAILABLE;
    }

    public String getAgentEmail() {
        return agentEmail;
    }

    public void setAgentEmail(String agentEmail) {
        this.agentEmail = agentEmail;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public List<IssueType> getIssueTypes() {
        return issueTypes;
    }

    public void setIssueTypes(List<IssueType> issueTypes) {
        this.issueTypes = issueTypes;
    }

    public AgentStatus getAgentStatus() {
        return agentStatus;
    }

    public void setAgentStatus(AgentStatus agentStatus) {
        this.agentStatus = agentStatus;
    }

    public BlockingQueue<Issue> getHistory() {
        return history;
    }

    public void addToHistory(Issue issue) {
        history.offer(issue);
    }

    @Override
    public String toString() {
        return "Agent{" +
                "agentEmail='" + agentEmail + '\'' +
                ", agentName='" + agentName + '\'' +
                ", agentStatus=" + agentStatus +
                ", issueTypes=" + issueTypes +
                ", historySize=" + history.size() +
                '}';
    }
}
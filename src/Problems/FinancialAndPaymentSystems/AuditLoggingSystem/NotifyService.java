package Problems.FinancialAndPaymentSystems.AuditLoggingSystem;

public class NotifyService {

    public void notify(Agent agent, Issue issue) {
        System.out.println("Notifying agent: " + agent.getAgentName()
                + " (" + agent.getAgentEmail() + ")"
                + " about issue: " + issue.getIssueId()
                + " [" + issue.getIssueType() + "]"
                + " - " + issue.getSubject());
    }
}
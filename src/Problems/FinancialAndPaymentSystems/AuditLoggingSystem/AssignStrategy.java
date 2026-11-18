package Problems.FinancialAndPaymentSystems.AuditLoggingSystem;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

public interface AssignStrategy {
    void start(Map<IssueType, BlockingQueue<IssueAssignment>> queues, Map<String, Agent> agents);
    void stop();
}
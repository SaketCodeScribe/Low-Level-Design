package Problems.FinancialAndPaymentSystems.AuditLoggingSystem;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class AssignFirstAvailableAgent implements AssignStrategy {

    private volatile boolean running;
    private Thread pollerThread;

    @Override
    public void start(Map<IssueType, BlockingQueue<IssueAssignment>> queues, Map<String, Agent> agents) {
        running = true;
        pollerThread = new Thread(() -> {
            while (running) {
                for (Map.Entry<IssueType, BlockingQueue<IssueAssignment>> entry : queues.entrySet()) {
                    IssueType type = entry.getKey();
                    BlockingQueue<IssueAssignment> queue = entry.getValue();

                    IssueAssignment assignment = queue.peek();
                    if (assignment == null) {
                        continue;
                    }

                    Agent available = findAvailableAgent(agents, type);
                    if (available == null) {
                        continue;
                    }

                    queue.poll();
                    available.setAgentStatus(AgentStatus.OCCUPIED);
                    assignment.getIssue().setStatus(IssueStatus.IN_PROGRESS);
                    available.addToHistory(assignment.getIssue());

                    assignment.getFuture().complete(new AssignResult(available));
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        pollerThread.setDaemon(true);
        pollerThread.start();
    }

    @Override
    public void stop() {
        running = false;
        if (pollerThread != null) {
            pollerThread.interrupt();
        }
    }

    private Agent findAvailableAgent(Map<String, Agent> agents, IssueType type) {
        for (Agent agent : agents.values()) {
            if (agent.getAgentStatus() == AgentStatus.AVAILABLE
                    && agent.getIssueTypes().contains(type)) {
                return agent;
            }
        }
        return null;
    }
}
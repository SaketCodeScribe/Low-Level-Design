package Problems.FinancialAndPaymentSystems.AuditLoggingSystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class IssueService {
    private static volatile IssueService instance = null;
    private static final Object lock = new Object();
    private final Map<String, Issue> issueMap = new ConcurrentHashMap<>();
    private final Map<IssueType, List<Issue>> issueTypeMap = new ConcurrentHashMap<>(); // typ + email map<type, map<email, lis>
    // email + type
    private final Map<String, List<Issue>> emailMap = new ConcurrentHashMap<>();
    private final AtomicLong id = new AtomicLong(0);

    public static IssueService getInstance() {
        if (instance == null) {

            synchronized (lock) {
                if (instance == null) {
                    instance = new IssueService();
                }
            }
        }
        return instance;
    }

    public Issue createIssue(String transactionId, IssueType issueType, String subject,
                             String description, String email) {
        String issueId = "I" + id.incrementAndGet();
        Issue issue = issueMap.computeIfAbsent(issueId, key -> Issue.createIssue(issueId, transactionId, issueType, subject, description, email));
        // postgreSql -> write async + batch writes read operaiton -> elastic
        issueTypeMap.compute(issueType, (k, v) -> {
            if (v == null) v = new ArrayList<>();
            v.add(issue);
            return v;
        });
        emailMap.compute(email, (k, v) -> {
            if (v == null) v = new ArrayList<>();
            v.add(issue);
            return v;
        });
        return issue;
    }

    public Issue updateIssue(String issueId, IssueStatus status, String resolution) {
        Issue issue = issueMap.get(issueId);
        if (issue == null) {
            throw new IllegalArgumentException("Issue not found: " + issueId);
        }
        if (issue.getStatus() == IssueStatus.RESOLVED) return issue;
        issue.setStatus(status);
        issue.setResolution(resolution);
        return issue;
    }

    public Issue resolveIssue(String issueId, String resolution) {
        return updateIssue(issueId, IssueStatus.RESOLVED, resolution);
    }

    public Issue getIssue(String issueId) {
        return issueMap.get(issueId);
    }
    public List<Issue> getFilter(Map.Entry<String, String> filter) {
        String key = filter.getKey();
        String value = filter.getValue();

        if ("type".equalsIgnoreCase(key)) {
            IssueType issueType = IssueType.getType(value);
            if (issueType == null) {
                return Collections.emptyList();
            }
            return issueTypeMap.getOrDefault(issueType, Collections.emptyList());
        } else if ("email".equalsIgnoreCase(key)) {
            return emailMap.getOrDefault(value, Collections.emptyList());
        }

        return Collections.emptyList();
    }
}

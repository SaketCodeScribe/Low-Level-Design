package Problems.FinancialAndPaymentSystems.AuditLoggingSystem;

public class Issue {

    private String issueId;
    private String transactionId;
    private IssueType issueType;
    private String subject;
    private String description;
    private String email;
    private String resolution;
    private IssueStatus status;

    public Issue() {
        this.status = IssueStatus.CREATED;
    }

    public Issue(String issueId, String transactionId, IssueType issueType, String subject,
                 String description, String email) {
        this.issueId = issueId;
        this.transactionId = transactionId;
        this.issueType = issueType;
        this.subject = subject;
        this.description = description;
        this.email = email;
        this.status = IssueStatus.CREATED;
    }

    public static Issue createIssue(String issueId, String transactionId, IssueType issueType,
                                    String subject, String description, String email) {
        return new Issue(issueId, transactionId, issueType, subject, description, email);
    }

    public IssueStatus getStatus() {
        return status;
    }

    public void setStatus(IssueStatus status) {
        this.status = status;
    }

    public String getIssueId() {
        return issueId;
    }

    public void setIssueId(String issueId) {
        this.issueId = issueId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public IssueType getIssueType() {
        return issueType;
    }

    public void setIssueType(IssueType issueType) {
        this.issueType = issueType;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    @Override
    public String toString() {
        return "Issue{" +
                "issueId='" + issueId + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", issueType=" + issueType +
                ", status=" + status +
                ", subject='" + subject + '\'' +
                ", description='" + description + '\'' +
                ", email='" + email + '\'' +
                ", resolution='" + resolution + '\'' +
                '}';
    }
}
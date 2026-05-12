package Problems.DataStructuresAndSearch.SimpleSearchEngine;

public class Document {
    private String docId;
    private String content;
    private String title;

    public Document(String docId, String content, String title) {
        this.docId = docId;
        this.content = content;
        this.title = title;
    }

    public String getDocId() {
        return docId;
    }

    public String getContent() {
        return content;
    }

    public String getTitle() {
        return title;
    }
}

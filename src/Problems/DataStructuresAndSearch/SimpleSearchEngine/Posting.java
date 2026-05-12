package Problems.DataStructuresAndSearch.SimpleSearchEngine;

public class Posting {
    private final int frequency;
    private final String docId;

    public Posting(int frequency, String docId) {
        this.frequency = frequency;
        this.docId = docId;
    }

    public int getFrequency() {
        return frequency;
    }

    public String getDocId() {
        return docId;
    }
}

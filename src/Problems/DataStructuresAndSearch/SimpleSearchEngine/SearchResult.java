package Problems.DataStructuresAndSearch.SimpleSearchEngine;

public class SearchResult {
    private final Document doc;
    private final double score;

    public SearchResult(Document doc, double score) {
        this.doc = doc;
        this.score = score;
    }

    public Document getDocument() {
        return doc;
    }

    public double getScore() {
        return score;
    }
}

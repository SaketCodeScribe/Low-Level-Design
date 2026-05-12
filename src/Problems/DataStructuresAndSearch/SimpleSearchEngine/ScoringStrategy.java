package Problems.DataStructuresAndSearch.SimpleSearchEngine;

public interface ScoringStrategy {
    public double score(String term, Posting posting, String docTitle);
}

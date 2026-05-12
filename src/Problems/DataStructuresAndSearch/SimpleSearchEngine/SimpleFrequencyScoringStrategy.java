package Problems.DataStructuresAndSearch.SimpleSearchEngine;

public class SimpleFrequencyScoringStrategy implements ScoringStrategy{
    @Override
    public double score(String term, Posting posting, String docTitle) {
        return posting.getFrequency();
    }
}

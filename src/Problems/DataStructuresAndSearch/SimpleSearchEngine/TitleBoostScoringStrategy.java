package Problems.DataStructuresAndSearch.SimpleSearchEngine;

public class TitleBoostScoringStrategy implements ScoringStrategy{
    private final double TITLE_BOOST = 1.5;

    @Override
    public double score(String term, Posting posting, String docTitle) {
        return posting.getFrequency() * (docTitle.contains(term) ? TITLE_BOOST : 1.0);
    }
}

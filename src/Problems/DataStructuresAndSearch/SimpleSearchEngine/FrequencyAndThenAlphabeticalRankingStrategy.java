package Problems.DataStructuresAndSearch.SimpleSearchEngine;

import java.util.List;

public class FrequencyAndThenAlphabeticalRankingStrategy implements RankingStrategy{
    @Override
    public List<SearchResult> rank(List<SearchResult> results) {
        results.sort((a,b) -> {
            int compare = Double.compare(b.getScore(), a.getScore());
            return compare != 0 ? compare : a.getDocument().getTitle().compareTo(b.getDocument().getTitle());
        });
        return results;
    }
}

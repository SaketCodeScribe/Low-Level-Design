package Problems.DataStructuresAndSearch.SimpleSearchEngine;

import java.util.List;

public class FrequencyBasedRanking implements RankingStrategy{
    @Override
    public List<SearchResult> rank(List<SearchResult> results) {
        results.sort((a,b) -> Double.compare(b.getScore(), a.getScore()));
        return results;
    }
}

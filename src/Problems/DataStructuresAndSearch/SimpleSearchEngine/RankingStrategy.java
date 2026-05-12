package Problems.DataStructuresAndSearch.SimpleSearchEngine;

import java.util.List;

public interface RankingStrategy {
    public List<SearchResult> rank(List<SearchResult> results);
}

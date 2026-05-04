package Problems.DataStructuresAndSearch.AutoCompleteSystem;

import java.util.List;

public interface RankingStrategy {
    public List<Suggestion> rank(List<Suggestion> suggestions);
}

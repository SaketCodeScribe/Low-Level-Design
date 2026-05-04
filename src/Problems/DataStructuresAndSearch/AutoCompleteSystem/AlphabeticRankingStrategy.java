package Problems.DataStructuresAndSearch.AutoCompleteSystem;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AlphabeticRankingStrategy implements RankingStrategy{

    @Override
    public List<Suggestion> rank(List<Suggestion> suggestions) {
        return suggestions.stream()
                .sorted(Comparator.comparing(Suggestion::getWord))
                .collect(Collectors.toList());
    }
}

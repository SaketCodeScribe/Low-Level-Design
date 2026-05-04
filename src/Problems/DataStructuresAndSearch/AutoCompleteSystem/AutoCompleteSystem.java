package Problems.DataStructuresAndSearch.AutoCompleteSystem;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AutoCompleteSystem {
    private final Trie trie;
    private final RankingStrategy rankingStrategy;
    private final int maxSuggestions;

    private AutoCompleteSystem(Builder builder){
        this.trie = builder.trie;
        this.rankingStrategy = builder.rankingStrategy;
        this.maxSuggestions = builder.maxSuggestions;
    }

    public static Builder Builder(){
        return new Builder();
    }

    public void insert(String word){
        trie.insert(word.toLowerCase());
    }

    public List<String> getSuggestions(String prefix){
        TrieNode node = trie.search(prefix.toLowerCase());
        if (node == null){
            return Collections.emptyList();
        }
        List<Suggestion> suggestions = trie.collectSuggestions(node, prefix.toLowerCase());
        List<Suggestion> ranking = rankingStrategy.rank(suggestions);
        return ranking.stream()
                .limit(maxSuggestions)
                .map(Suggestion::getWord)
                .collect(Collectors.toList());
    }


    public static class Builder{
        private Trie trie;
        private RankingStrategy rankingStrategy;
        private int maxSuggestions = 10;

        public Builder withTrie(){
            this.trie = new Trie();
            return this;
        }

        public Builder withRankingStrategy(RankingStrategy strategy){
            this.rankingStrategy = strategy;
            return this;
        }

        public Builder withMaxSuggestions(int suggestions){
            this.maxSuggestions = suggestions;
            return this;
        }

        public AutoCompleteSystem build(){
            return new AutoCompleteSystem(this);
        }
    }
}

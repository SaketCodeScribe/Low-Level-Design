package Problems.DataStructuresAndSearch.AutoCompleteSystem;

import java.util.ArrayList;
import java.util.List;

public class Trie {
    private TrieNode root;

    public Trie() {
        this.root = new TrieNode();
    }

    public void insert(String word){
        TrieNode current = root;

        for(char ch:word.toCharArray()){
            current = current.getChildren().computeIfAbsent(ch, x -> new TrieNode());
        }
        current.setEndOfWord(true);
        current.incrementFrequency();
    }

    public TrieNode search(String prefix){

        TrieNode current = root;
        for(char ch:prefix.toCharArray()){
            current = current.getChildren().get(ch);
            if (current == null){
                return null;
            }
        }
        return current;
    }

    public List<Suggestion> collectSuggestions(TrieNode node, String prefix){
        List<Suggestion> suggestions = new ArrayList<>();
        collectSuggestions(node, prefix, suggestions);
        return suggestions;
    }

    private void collectSuggestions(TrieNode node, String prefix, List<Suggestion> suggestions) {
        if (node.isEndOfWord()){
            suggestions.add(new Suggestion(prefix, node.getFrequency()));
        }
        for(char ch:node.getChildren().keySet()){
            collectSuggestions(node.getChildren().get(ch), prefix+ch, suggestions);
        }
    }
}

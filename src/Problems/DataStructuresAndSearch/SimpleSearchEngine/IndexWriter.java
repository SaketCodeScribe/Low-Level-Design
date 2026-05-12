package Problems.DataStructuresAndSearch.SimpleSearchEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndexWriter {
    private final InvertedIndex index;

    public IndexWriter() {
        this.index = new InvertedIndex();
    }

    public void add(Document document){
        Map<String, Integer> wordFrequency = new HashMap<>();
        String[] words = document.getContent().split(" ");

        for(String word:words){
            wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
        }
        for(Map.Entry<String, Integer> entry: wordFrequency.entrySet()){
            index.add(entry.getKey(), document.getDocId(), entry.getValue());
        }
    }

    public List<Posting> getPostings(String key){
        return index.getPostings(key);
    }
}

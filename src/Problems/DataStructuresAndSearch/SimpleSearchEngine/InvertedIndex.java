package Problems.DataStructuresAndSearch.SimpleSearchEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InvertedIndex {
    private final ConcurrentMap<String, List<Posting>> index;

    public InvertedIndex() {
        this.index = new ConcurrentHashMap<>();
    }

    public void add(String word, String docId, int frequency){
        index.computeIfAbsent(word, x -> new CopyOnWriteArrayList<>()).add(new Posting(frequency, docId));
    }

    public List<Posting> getPostings(String word){
        return index.get(word);
    }
}

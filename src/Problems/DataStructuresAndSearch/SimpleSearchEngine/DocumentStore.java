package Problems.DataStructuresAndSearch.SimpleSearchEngine;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DocumentStore {
    private final ConcurrentMap<String, Document> map;

    public DocumentStore() {
        map = new ConcurrentHashMap<>();
    }

    public void add(Document doc){
        map.putIfAbsent(doc.getDocId(), doc);
    }

    public Document getDocument(String docId){
        return map.get(docId);
    }
}

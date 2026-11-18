package Problems.DataStructuresAndSearch;

import java.util.*;
import java.util.stream.Collectors;

interface RecencyTracker<T> {
    void offer(T key);

    void evict();

    List<T> mostRecent(int k);
}

public class ArticleManagementSystem {
}

record Article(String articleId, String author) {
}

class LRUTracker<T> implements RecencyTracker<T> {
    int capacity, size;
    Node head, tail;
    Map<T, Node> map = new HashMap<>();
    public LRUTracker(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public void offer(T key) {
        Node node = map.get(key);
        if (node != null) {
            removeNode(node);
            addFront(node);
            return;
        }
        node = new Node(key);
        map.put(key, node);
        addFront(node);
        if (map.size() > capacity) evict();
    }

    private void removeNode(Node node) {
        Node prev = node.prev;
        Node next = node.next;

        if (prev != null && next != null) {
            prev.next = next;
            next.prev = prev;
        } else if (next != null) {
            next.prev = null;
            this.head = next;
        } else if (prev != null) {
            prev.next = null;
            this.tail = prev;
        } else {
            this.head = this.tail = null;
        }
        node.prev = node.next = null;
    }

    private void addFront(Node node) {
        if (size >= this.capacity) evict();
        if (this.head == null) {
            this.head = this.tail = node;
        } else {
            node.next = this.head;
            this.head.prev = node;
            this.head = node;
        }
    }

    @Override
    public void evict() {
        if (this.tail == null) return;

        T key = this.tail.key;
        removeNode(this.tail);
        map.remove(key);
        size = map.size();
    }

    @Override
    public List<T> mostRecent(int k) {
        int len = 0;
        List<T> result = new ArrayList<>();
        Node head = this.head;

        while (len < k && head != null) {
            result.add(head.key);
            head = head.next;
            len++;
        }
        return result;
    }

    class Node {
        T key;
        Node prev, next;

        public Node(T key) {
            this.key = key;
        }
    }
}

record Response(String articleId, String mssg) {
};

class ArticleManager {
    Map<String, Article> articles = new HashMap<>();
    Map<String, Set<String>> upVotes = new HashMap<>(), downVotes = new HashMap<>();
    RecencyTracker<String> recencyTracker;

    public ArticleManager(RecencyTracker<String> recencyTracker) {
        this.recencyTracker = recencyTracker;
    }

    public Article addArticle(String articleId, String author) {
        return articles.computeIfAbsent(articleId, x -> new Article(articleId, author));
    }

    public Response upvote(String userId, String articleId) {
        if (!articles.containsKey(articleId)) return new Response(articleId, "Article not found");
        boolean isFlip = false;
        if (downVotes.computeIfAbsent(articleId, x -> new HashSet<>()).contains(userId)) {
            isFlip = true;
            downVotes.get(articleId).remove(userId);
        }
        upVotes.computeIfAbsent(articleId, x -> new HashSet<>()).add(userId);
        if (isFlip) recencyTracker.offer(articleId);
        return new Response(articleId, "Article upVoted by user: " + userId);
    }

    public Response downVote(String userId, String articleId) {
        if (!articles.containsKey(articleId)) return new Response(articleId, "Article not found");
        boolean isFlip = false;
        if (upVotes.computeIfAbsent(articleId, x -> new HashSet<>()).contains(userId)) {
            isFlip = true;
            upVotes.get(articleId).remove(userId);
        }
        downVotes.computeIfAbsent(articleId, x -> new HashSet<>()).add(userId);
        if (isFlip) recencyTracker.offer(articleId);
        return new Response(articleId, "Article downVoted by user: " + userId);
    }

    public List<Article> getKFlippedArticles(int k) {
        return recencyTracker.mostRecent(k).stream().map(key -> articles.get(key)).collect(Collectors.toList());
    }
}

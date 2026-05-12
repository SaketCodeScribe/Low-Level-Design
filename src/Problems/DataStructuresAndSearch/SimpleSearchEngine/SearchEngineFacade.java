package Problems.DataStructuresAndSearch.SimpleSearchEngine;

import java.util.ArrayList;
import java.util.List;

public class SearchEngineFacade {
    private final DocumentStore documentStore;
    private final IndexWriter indexWriter;
    private final RankingStrategy rankingStrategy;
    private final ScoringStrategy scoringStrategy;
    public static Builder builder(){
        return new Builder();
    }

    public SearchEngineFacade(Builder builder){
        this.documentStore = builder.documentStore;
        this.indexWriter = builder.indexWriter;
        this.rankingStrategy = builder.rankingStrategy;
        this.scoringStrategy = builder.scoringStrategy;
    }

    public void addDocument(Document document){
        documentStore.add(document);
    }

    public void indexDocument(Document document){
        indexWriter.add(document);
    }

    public List<SearchResult> search(String key){
        List<Posting> postings = indexWriter.getPostings(key);
        List<SearchResult> searchResults = new ArrayList<>();

        for(Posting posting:postings){
            SearchResult searchResult = new SearchResult(documentStore.getDocument(posting.getDocId()),
                    scoringStrategy.score(key, posting, documentStore.getDocument(posting.getDocId()).getTitle()));
            searchResults.add(searchResult);
        }
        return rankingStrategy.rank(searchResults);
    }


    static class Builder{
        private DocumentStore documentStore;
        private IndexWriter indexWriter;
        private RankingStrategy rankingStrategy;
        private ScoringStrategy scoringStrategy;

        public Builder(){
        }

        public Builder withDocumentStore(){
            this.documentStore = new DocumentStore();
            return this;
        }
        public Builder withIndexWriter(){
            this.indexWriter = new IndexWriter();
            return this;
        }
        public Builder withRankingStrategy(){
            this.rankingStrategy = new FrequencyBasedRanking();
            return this;
        }
        public Builder withScoringStrategy(){
            this.scoringStrategy = new SimpleFrequencyScoringStrategy();
            return this;
        }
        public SearchEngineFacade build(){
            return new SearchEngineFacade(this);
        }
    }
}

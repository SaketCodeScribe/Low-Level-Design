package Problems.DataStructuresAndSearch.BloomFilter;

public interface HashStrategy {
    public int getHash(String word, int bitArraySize, int seed);
}

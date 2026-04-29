package Problems.DataStructuresAndSearch.BloomFilter;

import java.util.Arrays;
import java.util.Objects;

public class BloomFilter {
    private final int[] bitArray;
    private final int size;
    private final HashStrategy hashStrategy;
    private final int seed;
    private BloomFilter(BloomFilterConfig config){
        this.size = config.size;
        this.bitArray = new int[config.size];
        this.seed = config.seed;
        this.hashStrategy = config.hashStrategy;
    };

    public void add(String word){
        int position = getPosition(word);
        bitArray[position] = 1;
    }

    public boolean checkElementExists(String word){
        int position = getPosition(word);
        return bitArray[position] == 1;
    }

    public void clear(){
        Arrays.fill(bitArray, 0);
    }

    private int getPosition(String word) {
        return hashStrategy.getHash(word, size, seed);
    }

    public static Builder builder(){
        return new Builder();
    }

    public static class Builder{
        private BloomFilterConfig config;

        public Builder withConfig(BloomFilterConfig config){
            this.config = Objects.requireNonNull(config);
            return this;
        }

        public BloomFilter build(){
            return new BloomFilter(config);
        }
    }
}

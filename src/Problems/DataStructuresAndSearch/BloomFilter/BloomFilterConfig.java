package Problems.DataStructuresAndSearch.BloomFilter;

import java.util.Objects;

public class BloomFilterConfig {
    int size;
    int seed;
    HashStrategy hashStrategy;
    private BloomFilterConfig(ConfigBuilder builder){
        this.size = builder.size;
        this.seed = builder.seed;
        this.hashStrategy = builder.hashStrategy;
    }

    public static ConfigBuilder builder(){
        return new ConfigBuilder();
    }
    public static class ConfigBuilder{
        private int size = 100_000_000;
        private int seed = 7;
        private HashStrategy hashStrategy;

        public ConfigBuilder withSize(int size){
            this.size = size;
            return this;
        }

        public ConfigBuilder withSeed(int seed){
            this.seed = seed;
            return this;
        }

        public ConfigBuilder withHashStrategy(HashStrategy hashStrategy){
            this.hashStrategy = hashStrategy;
            return this;
        }

        public BloomFilterConfig build(){
            validate();
            return new BloomFilterConfig(this);
        }

        private void validate(){
            Objects.requireNonNull(this.hashStrategy);
        }
    }
}

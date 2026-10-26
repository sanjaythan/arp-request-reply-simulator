public class NetworkStatistics {

    private int totalRequests;
    private int totalReplies;
    private int cacheHits;
    private int cacheMisses;
    private int failedResolutions;

    public void incrementRequests() {
        totalRequests++;
    }

    public void incrementReplies() {
        totalReplies++;
    }

    public void incrementCacheHits() {
        cacheHits++;
    }

    public void incrementCacheMisses() {
        cacheMisses++;
    }

    public void incrementFailedResolutions() {
        failedResolutions++;
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public int getTotalReplies() {
        return totalReplies;
    }

    public int getCacheHits() {
        return cacheHits;
    }

    public int getCacheMisses() {
        return cacheMisses;
    }

    public int getFailedResolutions() {
        return failedResolutions;
    }

    public void reset() {
        totalRequests = 0;
        totalReplies = 0;
        cacheHits = 0;
        cacheMisses = 0;
        failedResolutions = 0;
    }
}
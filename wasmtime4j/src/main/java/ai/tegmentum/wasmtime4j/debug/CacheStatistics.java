package ai.tegmentum.wasmtime4j.debug;

import java.time.Instant;
import java.util.Objects;

/**
 * Statistics for source map integration caches.
 *
 * <p>This class provides detailed statistics about the performance and usage
 * of various caches used by the source map integration system. It includes
 * hit/miss ratios, memory usage, and timing information.
 *
 * @since 1.0.0
 */
public final class CacheStatistics {

    private final long sourceMapHits;
    private final long sourceMapMisses;
    private final long symbolHits;
    private final long symbolMisses;
    private final long fileHits;
    private final long fileMisses;
    private final long totalMemoryUsage;
    private final int currentEntries;
    private final int maxEntries;
    private final Instant creationTime;
    private final Instant lastResetTime;

    /**
     * Creates new cache statistics.
     *
     * @param sourceMapHits number of source map cache hits
     * @param sourceMapMisses number of source map cache misses
     * @param symbolHits number of symbol cache hits
     * @param symbolMisses number of symbol cache misses
     * @param fileHits number of file cache hits
     * @param fileMisses number of file cache misses
     * @param totalMemoryUsage estimated total memory usage in bytes
     * @param currentEntries current number of cache entries
     * @param maxEntries maximum number of cache entries
     * @param creationTime when the statistics were created
     * @param lastResetTime when the statistics were last reset
     */
    public CacheStatistics(
            final long sourceMapHits,
            final long sourceMapMisses,
            final long symbolHits,
            final long symbolMisses,
            final long fileHits,
            final long fileMisses,
            final long totalMemoryUsage,
            final int currentEntries,
            final int maxEntries,
            final Instant creationTime,
            final Instant lastResetTime) {
        this.sourceMapHits = Math.max(0, sourceMapHits);
        this.sourceMapMisses = Math.max(0, sourceMapMisses);
        this.symbolHits = Math.max(0, symbolHits);
        this.symbolMisses = Math.max(0, symbolMisses);
        this.fileHits = Math.max(0, fileHits);
        this.fileMisses = Math.max(0, fileMisses);
        this.totalMemoryUsage = Math.max(0, totalMemoryUsage);
        this.currentEntries = Math.max(0, currentEntries);
        this.maxEntries = Math.max(0, maxEntries);
        this.creationTime = creationTime != null ? creationTime : Instant.now();
        this.lastResetTime = lastResetTime != null ? lastResetTime : this.creationTime;
    }

    /**
     * Gets the number of source map cache hits.
     *
     * @return the source map hits
     */
    public long getSourceMapHits() {
        return sourceMapHits;
    }

    /**
     * Gets the number of source map cache misses.
     *
     * @return the source map misses
     */
    public long getSourceMapMisses() {
        return sourceMapMisses;
    }

    /**
     * Gets the number of symbol cache hits.
     *
     * @return the symbol hits
     */
    public long getSymbolHits() {
        return symbolHits;
    }

    /**
     * Gets the number of symbol cache misses.
     *
     * @return the symbol misses
     */
    public long getSymbolMisses() {
        return symbolMisses;
    }

    /**
     * Gets the number of file cache hits.
     *
     * @return the file hits
     */
    public long getFileHits() {
        return fileHits;
    }

    /**
     * Gets the number of file cache misses.
     *
     * @return the file misses
     */
    public long getFileMisses() {
        return fileMisses;
    }

    /**
     * Gets the estimated total memory usage.
     *
     * @return the memory usage in bytes
     */
    public long getTotalMemoryUsage() {
        return totalMemoryUsage;
    }

    /**
     * Gets the current number of cache entries.
     *
     * @return the current entries
     */
    public int getCurrentEntries() {
        return currentEntries;
    }

    /**
     * Gets the maximum number of cache entries.
     *
     * @return the maximum entries
     */
    public int getMaxEntries() {
        return maxEntries;
    }

    /**
     * Gets the creation time.
     *
     * @return the creation time
     */
    public Instant getCreationTime() {
        return creationTime;
    }

    /**
     * Gets the last reset time.
     *
     * @return the last reset time
     */
    public Instant getLastResetTime() {
        return lastResetTime;
    }

    /**
     * Gets the total number of cache hits.
     *
     * @return the total hits
     */
    public long getTotalHits() {
        return sourceMapHits + symbolHits + fileHits;
    }

    /**
     * Gets the total number of cache misses.
     *
     * @return the total misses
     */
    public long getTotalMisses() {
        return sourceMapMisses + symbolMisses + fileMisses;
    }

    /**
     * Gets the total number of cache requests.
     *
     * @return the total requests
     */
    public long getTotalRequests() {
        return getTotalHits() + getTotalMisses();
    }

    /**
     * Calculates the overall hit ratio.
     *
     * @return the hit ratio (0.0 to 1.0), or 0.0 if no requests
     */
    public double getHitRatio() {
        final long totalRequests = getTotalRequests();
        if (totalRequests == 0) {
            return 0.0;
        }
        return (double) getTotalHits() / totalRequests;
    }

    /**
     * Calculates the source map hit ratio.
     *
     * @return the source map hit ratio (0.0 to 1.0), or 0.0 if no requests
     */
    public double getSourceMapHitRatio() {
        final long totalRequests = sourceMapHits + sourceMapMisses;
        if (totalRequests == 0) {
            return 0.0;
        }
        return (double) sourceMapHits / totalRequests;
    }

    /**
     * Calculates the symbol hit ratio.
     *
     * @return the symbol hit ratio (0.0 to 1.0), or 0.0 if no requests
     */
    public double getSymbolHitRatio() {
        final long totalRequests = symbolHits + symbolMisses;
        if (totalRequests == 0) {
            return 0.0;
        }
        return (double) symbolHits / totalRequests;
    }

    /**
     * Calculates the file hit ratio.
     *
     * @return the file hit ratio (0.0 to 1.0), or 0.0 if no requests
     */
    public double getFileHitRatio() {
        final long totalRequests = fileHits + fileMisses;
        if (totalRequests == 0) {
            return 0.0;
        }
        return (double) fileHits / totalRequests;
    }

    /**
     * Calculates the cache utilization percentage.
     *
     * @return the utilization percentage (0.0 to 100.0)
     */
    public double getUtilizationPercentage() {
        if (maxEntries == 0) {
            return 0.0;
        }
        return (double) currentEntries / maxEntries * 100.0;
    }

    /**
     * Gets the memory usage in human-readable format.
     *
     * @return formatted memory usage string
     */
    public String getFormattedMemoryUsage() {
        return formatBytes(totalMemoryUsage);
    }

    /**
     * Checks if the cache is performing well.
     *
     * @return true if hit ratio is above 0.8
     */
    public boolean isPerformingWell() {
        return getHitRatio() > 0.8;
    }

    /**
     * Checks if the cache is nearly full.
     *
     * @return true if utilization is above 90%
     */
    public boolean isNearlyFull() {
        return getUtilizationPercentage() > 90.0;
    }

    /**
     * Creates a summary string of key statistics.
     *
     * @return summary string
     */
    public String getSummary() {
        return String.format(
                "Cache: %d/%d entries (%.1f%%), Hit ratio: %.1f%%, Memory: %s",
                currentEntries, maxEntries, getUtilizationPercentage(),
                getHitRatio() * 100, getFormattedMemoryUsage()
        );
    }

    /**
     * Creates a detailed report string.
     *
     * @return detailed report
     */
    public String getDetailedReport() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Cache Statistics Report:\n");
        sb.append("=======================\n\n");

        sb.append("Overall Performance:\n");
        sb.append(String.format("  Total Requests: %,d\n", getTotalRequests()));
        sb.append(String.format("  Total Hits: %,d\n", getTotalHits()));
        sb.append(String.format("  Total Misses: %,d\n", getTotalMisses()));
        sb.append(String.format("  Hit Ratio: %.2f%%\n\n", getHitRatio() * 100));

        sb.append("Cache Breakdown:\n");
        sb.append(String.format("  Source Maps: %,d hits, %,d misses (%.2f%% hit ratio)\n",
                sourceMapHits, sourceMapMisses, getSourceMapHitRatio() * 100));
        sb.append(String.format("  Symbols: %,d hits, %,d misses (%.2f%% hit ratio)\n",
                symbolHits, symbolMisses, getSymbolHitRatio() * 100));
        sb.append(String.format("  Files: %,d hits, %,d misses (%.2f%% hit ratio)\n\n",
                fileHits, fileMisses, getFileHitRatio() * 100));

        sb.append("Memory Usage:\n");
        sb.append(String.format("  Total Memory: %s\n", getFormattedMemoryUsage()));
        sb.append(String.format("  Current Entries: %,d\n", currentEntries));
        sb.append(String.format("  Maximum Entries: %,d\n", maxEntries));
        sb.append(String.format("  Utilization: %.1f%%\n\n", getUtilizationPercentage()));

        sb.append("Timestamps:\n");
        sb.append(String.format("  Created: %s\n", creationTime));
        sb.append(String.format("  Last Reset: %s\n", lastResetTime));

        return sb.toString();
    }

    /**
     * Formats a byte count into a human-readable string.
     *
     * @param bytes the byte count
     * @return formatted string
     */
    private static String formatBytes(final long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        final String[] units = {"KB", "MB", "GB", "TB"};
        double value = bytes;
        int unitIndex = -1;

        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024;
            unitIndex++;
        }

        return String.format("%.1f %s", value, units[unitIndex]);
    }

    /**
     * Creates an empty statistics instance.
     *
     * @return empty statistics
     */
    public static CacheStatistics empty() {
        final Instant now = Instant.now();
        return new CacheStatistics(0, 0, 0, 0, 0, 0, 0, 0, 0, now, now);
    }

    /**
     * Creates a builder for cache statistics.
     *
     * @return a new statistics builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for cache statistics.
     */
    public static final class Builder {
        private long sourceMapHits = 0;
        private long sourceMapMisses = 0;
        private long symbolHits = 0;
        private long symbolMisses = 0;
        private long fileHits = 0;
        private long fileMisses = 0;
        private long totalMemoryUsage = 0;
        private int currentEntries = 0;
        private int maxEntries = 0;
        private Instant creationTime = Instant.now();
        private Instant lastResetTime = creationTime;

        private Builder() {
            // Use CacheStatistics.builder()
        }

        /**
         * Sets source map hits and misses.
         *
         * @param hits the hits
         * @param misses the misses
         * @return this builder
         */
        public Builder sourceMapStats(final long hits, final long misses) {
            this.sourceMapHits = hits;
            this.sourceMapMisses = misses;
            return this;
        }

        /**
         * Sets symbol hits and misses.
         *
         * @param hits the hits
         * @param misses the misses
         * @return this builder
         */
        public Builder symbolStats(final long hits, final long misses) {
            this.symbolHits = hits;
            this.symbolMisses = misses;
            return this;
        }

        /**
         * Sets file hits and misses.
         *
         * @param hits the hits
         * @param misses the misses
         * @return this builder
         */
        public Builder fileStats(final long hits, final long misses) {
            this.fileHits = hits;
            this.fileMisses = misses;
            return this;
        }

        /**
         * Sets memory usage.
         *
         * @param totalMemoryUsage the memory usage in bytes
         * @return this builder
         */
        public Builder memoryUsage(final long totalMemoryUsage) {
            this.totalMemoryUsage = totalMemoryUsage;
            return this;
        }

        /**
         * Sets entry counts.
         *
         * @param currentEntries the current entries
         * @param maxEntries the maximum entries
         * @return this builder
         */
        public Builder entries(final int currentEntries, final int maxEntries) {
            this.currentEntries = currentEntries;
            this.maxEntries = maxEntries;
            return this;
        }

        /**
         * Sets timestamps.
         *
         * @param creationTime the creation time
         * @param lastResetTime the last reset time
         * @return this builder
         */
        public Builder timestamps(final Instant creationTime, final Instant lastResetTime) {
            this.creationTime = creationTime;
            this.lastResetTime = lastResetTime;
            return this;
        }

        /**
         * Builds the cache statistics.
         *
         * @return new cache statistics
         */
        public CacheStatistics build() {
            return new CacheStatistics(
                    sourceMapHits, sourceMapMisses, symbolHits, symbolMisses,
                    fileHits, fileMisses, totalMemoryUsage, currentEntries, maxEntries,
                    creationTime, lastResetTime
            );
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final CacheStatistics that = (CacheStatistics) obj;
        return sourceMapHits == that.sourceMapHits
                && sourceMapMisses == that.sourceMapMisses
                && symbolHits == that.symbolHits
                && symbolMisses == that.symbolMisses
                && fileHits == that.fileHits
                && fileMisses == that.fileMisses
                && totalMemoryUsage == that.totalMemoryUsage
                && currentEntries == that.currentEntries
                && maxEntries == that.maxEntries
                && Objects.equals(creationTime, that.creationTime)
                && Objects.equals(lastResetTime, that.lastResetTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceMapHits, sourceMapMisses, symbolHits, symbolMisses,
                fileHits, fileMisses, totalMemoryUsage, currentEntries, maxEntries,
                creationTime, lastResetTime);
    }

    @Override
    public String toString() {
        return getSummary();
    }
}
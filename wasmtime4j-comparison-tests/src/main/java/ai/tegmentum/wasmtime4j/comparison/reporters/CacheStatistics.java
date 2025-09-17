package ai.tegmentum.wasmtime4j.comparison.reporters;

/**
 * Cache statistics.
 *
 * @since 1.0.0
 */
public final class CacheStatistics {
  private final int size;
  private final long hits;
  private final long misses;

  /**
   * Creates new cache statistics.
   *
   * @param size the current cache size
   * @param hits the number of cache hits
   * @param misses the number of cache misses
   */
  public CacheStatistics(final int size, final long hits, final long misses) {
    this.size = size;
    this.hits = hits;
    this.misses = misses;
  }

  public int getSize() {
    return size;
  }

  public long getHits() {
    return hits;
  }

  public long getMisses() {
    return misses;
  }

  public double getHitRatio() {
    final long total = hits + misses;
    return total == 0 ? 0.0 : (double) hits / total;
  }

  @Override
  public String toString() {
    return "CacheStatistics{"
        + "size="
        + size
        + ", hits="
        + hits
        + ", misses="
        + misses
        + ", hitRatio="
        + String.format("%.2f%%", getHitRatio() * 100)
        + '}';
  }
}

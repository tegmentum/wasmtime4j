package ai.tegmentum.wasmtime4j.comparison.reporters;

/** Progress update information for streaming exports. */
final class ProgressUpdate {
  private final long itemsProcessed;
  private final long totalItems;
  private final double progressPercent;
  private final long elapsedMs;
  private final long estimatedRemainingMs;
  private final long bytesWritten;

  public ProgressUpdate(
      final long itemsProcessed,
      final long totalItems,
      final double progressPercent,
      final long elapsedMs,
      final long estimatedRemainingMs,
      final long bytesWritten) {
    this.itemsProcessed = itemsProcessed;
    this.totalItems = totalItems;
    this.progressPercent = progressPercent;
    this.elapsedMs = elapsedMs;
    this.estimatedRemainingMs = estimatedRemainingMs;
    this.bytesWritten = bytesWritten;
  }

  public long getItemsProcessed() {
    return itemsProcessed;
  }

  public long getTotalItems() {
    return totalItems;
  }

  public double getProgressPercent() {
    return progressPercent;
  }

  public long getElapsedMs() {
    return elapsedMs;
  }

  public long getEstimatedRemainingMs() {
    return estimatedRemainingMs;
  }

  public long getBytesWritten() {
    return bytesWritten;
  }

  @Override
  public String toString() {
    return String.format(
        "Progress: %d/%d (%.1f%%) - %d bytes - %dms elapsed, ~%dms remaining",
        itemsProcessed,
        totalItems,
        progressPercent * 100,
        bytesWritten,
        elapsedMs,
        estimatedRemainingMs);
  }
}
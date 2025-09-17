package ai.tegmentum.wasmtime4j.comparison.reporters;

/** Configuration for streaming export operations. */
final class StreamingConfiguration {
  private final int chunkSize;
  private final int bufferSize;
  private final long chunkDelayMs;
  private final int threadPoolSize;
  private final boolean enableProgressReporting;

  private StreamingConfiguration(final Builder builder) {
    this.chunkSize = builder.chunkSize;
    this.bufferSize = builder.bufferSize;
    this.chunkDelayMs = builder.chunkDelayMs;
    this.threadPoolSize = builder.threadPoolSize;
    this.enableProgressReporting = builder.enableProgressReporting;
  }

  public int getChunkSize() {
    return chunkSize;
  }

  public int getBufferSize() {
    return bufferSize;
  }

  public long getChunkDelayMs() {
    return chunkDelayMs;
  }

  public int getThreadPoolSize() {
    return threadPoolSize;
  }

  public boolean isEnableProgressReporting() {
    return enableProgressReporting;
  }

  /** Builder for StreamingConfiguration. */
  public static final class Builder {
    private int chunkSize = 100;
    private int bufferSize = 64 * 1024;
    private long chunkDelayMs = 10;
    private int threadPoolSize = 2;
    private boolean enableProgressReporting = true;

    public Builder chunkSize(final int chunkSize) {
      this.chunkSize = Math.max(1, chunkSize);
      return this;
    }

    public Builder bufferSize(final int bufferSize) {
      this.bufferSize = Math.max(1024, bufferSize);
      return this;
    }

    public Builder chunkDelayMs(final long chunkDelayMs) {
      this.chunkDelayMs = Math.max(0, chunkDelayMs);
      return this;
    }

    public Builder threadPoolSize(final int threadPoolSize) {
      this.threadPoolSize = Math.max(1, threadPoolSize);
      return this;
    }

    public Builder enableProgressReporting(final boolean enableProgressReporting) {
      this.enableProgressReporting = enableProgressReporting;
      return this;
    }

    public StreamingConfiguration build() {
      return new StreamingConfiguration(this);
    }
  }
}

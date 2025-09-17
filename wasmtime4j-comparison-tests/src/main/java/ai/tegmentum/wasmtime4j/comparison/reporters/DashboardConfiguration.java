package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Objects;

/** Configuration for the dashboard generator. */
final class DashboardConfiguration {
  private final int port;
  private final String title;
  private final String theme;
  private final boolean enableRealTimeUpdates;
  private final int maxCachedReports;

  private DashboardConfiguration(final Builder builder) {
    this.port = builder.port;
    this.title = Objects.requireNonNull(builder.title, "title cannot be null");
    this.theme = Objects.requireNonNull(builder.theme, "theme cannot be null");
    this.enableRealTimeUpdates = builder.enableRealTimeUpdates;
    this.maxCachedReports = builder.maxCachedReports;
  }

  public int getPort() {
    return port;
  }

  public String getTitle() {
    return title;
  }

  public String getTheme() {
    return theme;
  }

  public boolean isRealTimeUpdatesEnabled() {
    return enableRealTimeUpdates;
  }

  public int getMaxCachedReports() {
    return maxCachedReports;
  }

  /** Creates a new builder with default configuration. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for DashboardConfiguration. */
  public static final class Builder {
    private int port = 8080;
    private String title = "Wasmtime4j Comparison Dashboard";
    private String theme = "default";
    private boolean enableRealTimeUpdates = false;
    private int maxCachedReports = 10;

    public Builder port(final int port) {
      if (port <= 0 || port > 65535) {
        throw new IllegalArgumentException("Port must be between 1 and 65535");
      }
      this.port = port;
      return this;
    }

    public Builder title(final String title) {
      this.title = Objects.requireNonNull(title, "title cannot be null");
      return this;
    }

    public Builder theme(final String theme) {
      this.theme = Objects.requireNonNull(theme, "theme cannot be null");
      return this;
    }

    public Builder enableRealTimeUpdates(final boolean enableRealTimeUpdates) {
      this.enableRealTimeUpdates = enableRealTimeUpdates;
      return this;
    }

    public Builder maxCachedReports(final int maxCachedReports) {
      if (maxCachedReports <= 0) {
        throw new IllegalArgumentException("maxCachedReports must be positive");
      }
      this.maxCachedReports = maxCachedReports;
      return this;
    }

    public DashboardConfiguration build() {
      return new DashboardConfiguration(this);
    }
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final DashboardConfiguration that = (DashboardConfiguration) obj;
    return port == that.port
        && enableRealTimeUpdates == that.enableRealTimeUpdates
        && maxCachedReports == that.maxCachedReports
        && Objects.equals(title, that.title)
        && Objects.equals(theme, that.theme);
  }

  @Override
  public int hashCode() {
    return Objects.hash(port, title, theme, enableRealTimeUpdates, maxCachedReports);
  }

  @Override
  public String toString() {
    return "DashboardConfiguration{"
        + "port="
        + port
        + ", title='"
        + title
        + '\''
        + ", theme='"
        + theme
        + '\''
        + ", realTimeUpdates="
        + enableRealTimeUpdates
        + '}';
  }
}
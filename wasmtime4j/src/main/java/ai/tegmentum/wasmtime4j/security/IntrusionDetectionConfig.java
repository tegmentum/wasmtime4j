package ai.tegmentum.wasmtime4j.security;

import java.time.Duration;
import java.util.Set;

/**
 * Configuration for intrusion detection capabilities.
 *
 * @since 1.0.0
 */
public final class IntrusionDetectionConfig {

  private final boolean enabled;
  private final Set<DetectionRule> detectionRules;
  private final Duration analysisWindow;
  private final int alertThreshold;
  private final boolean realTimeMonitoring;

  private IntrusionDetectionConfig(final Builder builder) {
    this.enabled = builder.enabled;
    this.detectionRules = Set.copyOf(builder.detectionRules);
    this.analysisWindow = builder.analysisWindow;
    this.alertThreshold = builder.alertThreshold;
    this.realTimeMonitoring = builder.realTimeMonitoring;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static IntrusionDetectionConfig disabled() {
    return builder().withEnabled(false).build();
  }

  public static IntrusionDetectionConfig standard() {
    return builder()
        .withEnabled(true)
        .withRealTimeMonitoring(true)
        .withAnalysisWindow(Duration.ofMinutes(5))
        .withAlertThreshold(5)
        .build();
  }

  public boolean isEnabled() {
    return enabled;
  }

  public Set<DetectionRule> getDetectionRules() {
    return detectionRules;
  }

  public Duration getAnalysisWindow() {
    return analysisWindow;
  }

  public int getAlertThreshold() {
    return alertThreshold;
  }

  public boolean isRealTimeMonitoring() {
    return realTimeMonitoring;
  }

  public enum DetectionRule {
    EXCESSIVE_MEMORY_ALLOCATION,
    RAPID_EXECUTION_ATTEMPTS,
    SUSPICIOUS_HOST_FUNCTION_CALLS,
    UNUSUAL_NETWORK_PATTERNS,
    ANOMALOUS_FILE_ACCESS
  }

  public static final class Builder {
    private boolean enabled = true;
    private Set<DetectionRule> detectionRules = Set.of(DetectionRule.values());
    private Duration analysisWindow = Duration.ofMinutes(5);
    private int alertThreshold = 3;
    private boolean realTimeMonitoring = true;

    public Builder withEnabled(final boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    public Builder withDetectionRules(final Set<DetectionRule> rules) {
      this.detectionRules = rules;
      return this;
    }

    public Builder withAnalysisWindow(final Duration window) {
      this.analysisWindow = window;
      return this;
    }

    public Builder withAlertThreshold(final int threshold) {
      this.alertThreshold = threshold;
      return this;
    }

    public Builder withRealTimeMonitoring(final boolean enabled) {
      this.realTimeMonitoring = enabled;
      return this;
    }

    public IntrusionDetectionConfig build() {
      return new IntrusionDetectionConfig(this);
    }
  }
}

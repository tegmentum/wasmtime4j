package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for report localization including locale, time zone, and custom messages.
 *
 * @since 1.0.0
 */
public final class LocalizationConfiguration {
  private final Locale locale;
  private final String timeZone;
  private final Map<String, String> customMessages;
  private final String resourceBundleName;

  private LocalizationConfiguration(final Builder builder) {
    this.locale = Objects.requireNonNull(builder.locale, "locale cannot be null");
    this.timeZone = Objects.requireNonNull(builder.timeZone, "timeZone cannot be null");
    this.customMessages = Map.copyOf(builder.customMessages);
    this.resourceBundleName = builder.resourceBundleName;
  }

  public Locale getLocale() {
    return locale;
  }

  public String getTimeZone() {
    return timeZone;
  }

  public Map<String, String> getCustomMessages() {
    return customMessages;
  }

  public String getResourceBundleName() {
    return resourceBundleName;
  }

  /** Creates default localization configuration. */
  public static LocalizationConfiguration defaultLocalization() {
    return new Builder(Locale.getDefault())
        .timeZone(java.time.ZoneId.systemDefault().getId())
        .resourceBundleName("comparison-messages")
        .build();
  }

  /** Creates English localization configuration. */
  public static LocalizationConfiguration englishLocalization() {
    return new Builder(Locale.ENGLISH)
        .timeZone("UTC")
        .resourceBundleName("comparison-messages")
        .build();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final LocalizationConfiguration that = (LocalizationConfiguration) obj;
    return Objects.equals(locale, that.locale)
        && Objects.equals(timeZone, that.timeZone)
        && Objects.equals(customMessages, that.customMessages)
        && Objects.equals(resourceBundleName, that.resourceBundleName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(locale, timeZone, customMessages, resourceBundleName);
  }

  @Override
  public String toString() {
    return "LocalizationConfiguration{"
        + "locale="
        + locale
        + ", timeZone='"
        + timeZone
        + '\''
        + ", resourceBundle='"
        + resourceBundleName
        + '\''
        + '}';
  }

  /** Builder for LocalizationConfiguration. */
  public static final class Builder {
    private final Locale locale;
    private String timeZone = "UTC";
    private Map<String, String> customMessages = Collections.emptyMap();
    private String resourceBundleName = "comparison-messages";

    /**
     * Creates a new builder for LocalizationConfiguration.
     *
     * @param locale the locale to use
     */
    public Builder(final Locale locale) {
      this.locale = Objects.requireNonNull(locale, "locale cannot be null");
    }

    /**
     * Sets the time zone for localization.
     *
     * @param timeZone the time zone to set
     * @return this builder instance
     */
    public Builder timeZone(final String timeZone) {
      this.timeZone = Objects.requireNonNull(timeZone, "timeZone cannot be null");
      return this;
    }

    /**
     * Sets custom messages for localization.
     *
     * @param customMessages the custom messages map to set
     * @return this builder instance
     */
    public Builder customMessages(final Map<String, String> customMessages) {
      this.customMessages =
          Objects.requireNonNull(customMessages, "customMessages cannot be null");
      return this;
    }

    public Builder resourceBundleName(final String resourceBundleName) {
      this.resourceBundleName = resourceBundleName;
      return this;
    }

    public LocalizationConfiguration build() {
      return new LocalizationConfiguration(this);
    }
  }
}
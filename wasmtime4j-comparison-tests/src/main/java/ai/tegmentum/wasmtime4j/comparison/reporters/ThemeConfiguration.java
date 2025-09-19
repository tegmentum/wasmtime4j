package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for report theming including colors, fonts, CSS, and branding options.
 *
 * @since 1.0.0
 */
public final class ThemeConfiguration {
  private final String themeName;
  private final Map<String, String> colors;
  private final Map<String, String> fonts;
  private final Map<String, String> customCss;
  private final boolean darkMode;
  private final String brandingLogo;
  private final String brandingFooter;

  private ThemeConfiguration(final Builder builder) {
    this.themeName = Objects.requireNonNull(builder.themeName, "themeName cannot be null");
    this.colors = Map.copyOf(builder.colors);
    this.fonts = Map.copyOf(builder.fonts);
    this.customCss = Map.copyOf(builder.customCss);
    this.darkMode = builder.darkMode;
    this.brandingLogo = builder.brandingLogo;
    this.brandingFooter = builder.brandingFooter;
  }

  public String getThemeName() {
    return themeName;
  }

  public Map<String, String> getColors() {
    return colors;
  }

  public Map<String, String> getFonts() {
    return fonts;
  }

  public Map<String, String> getCustomCss() {
    return customCss;
  }

  public boolean isDarkMode() {
    return darkMode;
  }

  public String getBrandingLogo() {
    return brandingLogo;
  }

  public String getBrandingFooter() {
    return brandingFooter;
  }

  /** Creates default theme configuration. */
  public static ThemeConfiguration defaultTheme() {
    return new Builder("default")
        .colors(
            Map.of(
                "primary", "#007bff",
                "success", "#28a745",
                "warning", "#ffc107",
                "danger", "#dc3545",
                "background", "#ffffff",
                "text", "#333333"))
        .fonts(
            Map.of(
                "heading", "system-ui, sans-serif",
                "body", "system-ui, sans-serif",
                "monospace", "Consolas, Monaco, monospace"))
        .darkMode(false)
        .build();
  }

  /** Creates minimal theme configuration. */
  public static ThemeConfiguration minimalTheme() {
    return new Builder("minimal")
        .colors(
            Map.of(
                "primary", "#000000",
                "background", "#ffffff",
                "text", "#333333"))
        .fonts(
            Map.of(
                "body", "system-ui, sans-serif",
                "monospace", "monospace"))
        .darkMode(false)
        .build();
  }

  /** Creates dark theme configuration. */
  public static ThemeConfiguration darkTheme() {
    return new Builder("dark")
        .colors(
            Map.of(
                "primary", "#17a2b8",
                "success", "#28a745",
                "warning", "#ffc107",
                "danger", "#dc3545",
                "background", "#1a1a1a",
                "text", "#ffffff"))
        .fonts(
            Map.of(
                "heading", "system-ui, sans-serif",
                "body", "system-ui, sans-serif",
                "monospace", "Consolas, Monaco, monospace"))
        .darkMode(true)
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

    final ThemeConfiguration that = (ThemeConfiguration) obj;
    return darkMode == that.darkMode
        && Objects.equals(themeName, that.themeName)
        && Objects.equals(colors, that.colors)
        && Objects.equals(fonts, that.fonts)
        && Objects.equals(customCss, that.customCss)
        && Objects.equals(brandingLogo, that.brandingLogo)
        && Objects.equals(brandingFooter, that.brandingFooter);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        themeName, colors, fonts, customCss, darkMode, brandingLogo, brandingFooter);
  }

  @Override
  public String toString() {
    return "ThemeConfiguration{"
        + "name='"
        + themeName
        + '\''
        + ", darkMode="
        + darkMode
        + ", colors="
        + colors.size()
        + ", fonts="
        + fonts.size()
        + '}';
  }

  /** Builder for ThemeConfiguration. */
  public static final class Builder {
    private final String themeName;
    private Map<String, String> colors = Collections.emptyMap();
    private Map<String, String> fonts = Collections.emptyMap();
    private Map<String, String> customCss = Collections.emptyMap();
    private boolean darkMode = false;
    private String brandingLogo;
    private String brandingFooter;

    public Builder(final String themeName) {
      this.themeName = Objects.requireNonNull(themeName, "themeName cannot be null");
    }

    public Builder colors(final Map<String, String> colors) {
      this.colors = Objects.requireNonNull(colors, "colors cannot be null");
      return this;
    }

    public Builder fonts(final Map<String, String> fonts) {
      this.fonts = Objects.requireNonNull(fonts, "fonts cannot be null");
      return this;
    }

    public Builder customCss(final Map<String, String> customCss) {
      this.customCss = Objects.requireNonNull(customCss, "customCss cannot be null");
      return this;
    }

    public Builder darkMode(final boolean darkMode) {
      this.darkMode = darkMode;
      return this;
    }

    public Builder brandingLogo(final String brandingLogo) {
      this.brandingLogo = brandingLogo;
      return this;
    }

    public Builder brandingFooter(final String brandingFooter) {
      this.brandingFooter = brandingFooter;
      return this;
    }

    public ThemeConfiguration build() {
      return new ThemeConfiguration(this);
    }
  }
}

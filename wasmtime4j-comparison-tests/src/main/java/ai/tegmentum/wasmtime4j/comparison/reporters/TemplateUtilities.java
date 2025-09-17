package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collection;
import java.util.Locale;

/**
 * Utility functions available in templates for formatting and data manipulation.
 *
 * @since 1.0.0
 */
public final class TemplateUtilities {

  /**
   * Formats a number as a percentage.
   *
   * @param value the value to format
   * @param decimals the number of decimal places
   * @return formatted percentage string
   */
  public String formatPercentage(final Number value, final int decimals) {
    if (value == null) {
      return "0%";
    }

    final DecimalFormat format = new DecimalFormat();
    format.setMaximumFractionDigits(decimals);
    format.setMinimumFractionDigits(decimals);
    return format.format(value.doubleValue() * 100) + "%";
  }

  /**
   * Formats a duration in a human-readable format.
   *
   * @param duration the duration to format
   * @return formatted duration string
   */
  public String formatDuration(final Duration duration) {
    if (duration == null) {
      return "0ms";
    }

    final long totalMillis = duration.toMillis();
    if (totalMillis < 1000) {
      return totalMillis + "ms";
    }

    final long seconds = duration.getSeconds();
    if (seconds < 60) {
      return seconds + "s";
    }

    final long minutes = seconds / 60;
    final long remainingSeconds = seconds % 60;
    if (minutes < 60) {
      return remainingSeconds == 0 ? minutes + "m" : minutes + "m " + remainingSeconds + "s";
    }

    final long hours = minutes / 60;
    final long remainingMinutes = minutes % 60;
    return remainingMinutes == 0 ? hours + "h" : hours + "h " + remainingMinutes + "m";
  }

  /**
   * Formats a timestamp as a human-readable string.
   *
   * @param instant the instant to format
   * @param locale the locale for formatting
   * @return formatted timestamp string
   */
  public String formatTimestamp(final Instant instant, final Locale locale) {
    if (instant == null) {
      return "";
    }

    final DateTimeFormatter formatter =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withLocale(locale != null ? locale : Locale.getDefault());

    return formatter.format(instant.atZone(java.time.ZoneId.systemDefault()));
  }

  /**
   * Formats a number with appropriate units (K, M, B).
   *
   * @param value the value to format
   * @return formatted number string
   */
  public String formatNumber(final Number value) {
    if (value == null) {
      return "0";
    }

    final double val = value.doubleValue();
    if (Math.abs(val) < 1000) {
      return NumberFormat.getIntegerInstance().format(val);
    }

    if (Math.abs(val) < 1_000_000) {
      return String.format("%.1fK", val / 1000);
    }

    if (Math.abs(val) < 1_000_000_000) {
      return String.format("%.1fM", val / 1_000_000);
    }

    return String.format("%.1fB", val / 1_000_000_000);
  }

  /**
   * Truncates a string to the specified length with ellipsis.
   *
   * @param text the text to truncate
   * @param maxLength the maximum length
   * @return truncated string
   */
  public String truncate(final String text, final int maxLength) {
    if (text == null || text.length() <= maxLength) {
      return text;
    }

    if (maxLength <= 3) {
      return text.substring(0, maxLength);
    }

    return text.substring(0, maxLength - 3) + "...";
  }

  /**
   * Capitalizes the first letter of a string.
   *
   * @param text the text to capitalize
   * @return capitalized string
   */
  public String capitalize(final String text) {
    if (text == null || text.isEmpty()) {
      return text;
    }

    return Character.toUpperCase(text.charAt(0)) + text.substring(1);
  }

  /**
   * Checks if a collection is empty or null.
   *
   * @param collection the collection to check
   * @return true if empty or null
   */
  public boolean isEmpty(final Collection<?> collection) {
    return collection == null || collection.isEmpty();
  }

  /**
   * Gets the size of a collection, returning 0 for null.
   *
   * @param collection the collection
   * @return the size
   */
  public int size(final Collection<?> collection) {
    return collection != null ? collection.size() : 0;
  }

  /**
   * Escapes HTML characters in a string.
   *
   * @param text the text to escape
   * @return escaped string
   */
  public String escapeHtml(final String text) {
    if (text == null) {
      return "";
    }

    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  /**
   * Converts a string to a CSS-safe class name.
   *
   * @param text the text to convert
   * @return CSS-safe class name
   */
  public String toCssClass(final String text) {
    if (text == null) {
      return "";
    }

    return text.toLowerCase()
        .replaceAll("[^a-z0-9-_]", "-")
        .replaceAll("-+", "-")
        .replaceAll("^-|-$", "");
  }
}

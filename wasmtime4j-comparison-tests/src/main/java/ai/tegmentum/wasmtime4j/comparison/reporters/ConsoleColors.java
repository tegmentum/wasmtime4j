package ai.tegmentum.wasmtime4j.comparison.reporters;

/**
 * ANSI color codes and formatting utilities for enhanced console output.
 * Provides cross-platform color support with automatic fallback for terminals
 * that don't support colors.
 *
 * @since 1.0.0
 */
public final class ConsoleColors {
  // Reset and general formatting
  public static final String RESET = "\u001B[0m";
  public static final String BOLD = "\u001B[1m";
  public static final String DIM = "\u001B[2m";
  public static final String ITALIC = "\u001B[3m";
  public static final String UNDERLINE = "\u001B[4m";

  // Standard colors
  public static final String BLACK = "\u001B[30m";
  public static final String RED = "\u001B[31m";
  public static final String GREEN = "\u001B[32m";
  public static final String YELLOW = "\u001B[33m";
  public static final String BLUE = "\u001B[34m";
  public static final String MAGENTA = "\u001B[35m";
  public static final String CYAN = "\u001B[36m";
  public static final String WHITE = "\u001B[37m";

  // Bright colors
  public static final String BRIGHT_BLACK = "\u001B[90m";
  public static final String BRIGHT_RED = "\u001B[91m";
  public static final String BRIGHT_GREEN = "\u001B[92m";
  public static final String BRIGHT_YELLOW = "\u001B[93m";
  public static final String BRIGHT_BLUE = "\u001B[94m";
  public static final String BRIGHT_MAGENTA = "\u001B[95m";
  public static final String BRIGHT_CYAN = "\u001B[96m";
  public static final String BRIGHT_WHITE = "\u001B[97m";

  // Background colors
  public static final String BG_BLACK = "\u001B[40m";
  public static final String BG_RED = "\u001B[41m";
  public static final String BG_GREEN = "\u001B[42m";
  public static final String BG_YELLOW = "\u001B[43m";
  public static final String BG_BLUE = "\u001B[44m";
  public static final String BG_MAGENTA = "\u001B[45m";
  public static final String BG_CYAN = "\u001B[46m";
  public static final String BG_WHITE = "\u001B[47m";

  private static final boolean COLOR_SUPPORT = detectColorSupport();

  private ConsoleColors() {
    // Utility class
  }

  /**
   * Detects if the current terminal supports ANSI color codes.
   *
   * @return true if colors are supported
   */
  private static boolean detectColorSupport() {
    // Check common environment variables that indicate color support
    final String term = System.getenv("TERM");
    final String colorTerm = System.getenv("COLORTERM");
    final String ciEnv = System.getenv("CI");

    // Disable colors in CI environments unless explicitly enabled
    if ("true".equalsIgnoreCase(ciEnv) && System.getenv("FORCE_COLOR") == null) {
      return false;
    }

    // Enable colors if explicitly requested
    if (System.getenv("FORCE_COLOR") != null || "1".equals(System.getenv("CLICOLOR"))) {
      return true;
    }

    // Disable colors if explicitly disabled
    if ("0".equals(System.getenv("CLICOLOR")) || System.getenv("NO_COLOR") != null) {
      return false;
    }

    // Check terminal type
    if (term != null) {
      return term.contains("color") || term.contains("256") || term.equals("xterm")
          || term.equals("screen") || term.equals("tmux");
    }

    if (colorTerm != null) {
      return true;
    }

    // Default to no colors for safety
    return false;
  }

  /**
   * Checks if color output is supported and enabled.
   *
   * @return true if colors should be used
   */
  public static boolean isColorSupported() {
    return COLOR_SUPPORT;
  }

  /**
   * Applies color formatting to text if colors are supported.
   *
   * @param text the text to format
   * @param color the color code to apply
   * @return formatted text with color codes, or plain text if colors aren't supported
   */
  public static String colorize(final String text, final String color) {
    if (!COLOR_SUPPORT || text == null || color == null) {
      return text;
    }
    return color + text + RESET;
  }

  /**
   * Creates success-colored text (green).
   *
   * @param text the text to format
   * @return colored text or plain text if colors aren't supported
   */
  public static String success(final String text) {
    return colorize(text, GREEN);
  }

  /**
   * Creates error-colored text (red).
   *
   * @param text the text to format
   * @return colored text or plain text if colors aren't supported
   */
  public static String error(final String text) {
    return colorize(text, RED);
  }

  /**
   * Creates warning-colored text (yellow).
   *
   * @param text the text to format
   * @return colored text or plain text if colors aren't supported
   */
  public static String warning(final String text) {
    return colorize(text, YELLOW);
  }

  /**
   * Creates info-colored text (blue).
   *
   * @param text the text to format
   * @return colored text or plain text if colors aren't supported
   */
  public static String info(final String text) {
    return colorize(text, BLUE);
  }

  /**
   * Creates emphasized text (bold).
   *
   * @param text the text to format
   * @return formatted text or plain text if colors aren't supported
   */
  public static String bold(final String text) {
    return colorize(text, BOLD);
  }

  /**
   * Creates dimmed text (gray).
   *
   * @param text the text to format
   * @return formatted text or plain text if colors aren't supported
   */
  public static String dim(final String text) {
    return colorize(text, DIM);
  }

  /**
   * Creates header text with background color.
   *
   * @param text the text to format
   * @return formatted header text
   */
  public static String header(final String text) {
    if (!COLOR_SUPPORT) {
      return "=== " + text + " ===";
    }
    return colorize(" " + text + " ", BOLD + BRIGHT_WHITE + BG_BLUE);
  }
}
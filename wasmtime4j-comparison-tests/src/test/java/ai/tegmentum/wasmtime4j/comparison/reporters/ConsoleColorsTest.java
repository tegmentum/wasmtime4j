package ai.tegmentum.wasmtime4j.comparison.reporters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Comprehensive unit tests for ConsoleColors ANSI color formatting utilities. Tests color code
 * application, terminal detection, and formatting helpers.
 */
final class ConsoleColorsTest {

  @Test
  void testColorConstants() {
    // Test that all color constants are defined and not null
    assertNotNull(ConsoleColors.RESET);
    assertNotNull(ConsoleColors.BOLD);
    assertNotNull(ConsoleColors.RED);
    assertNotNull(ConsoleColors.GREEN);
    assertNotNull(ConsoleColors.YELLOW);
    assertNotNull(ConsoleColors.BLUE);
    assertNotNull(ConsoleColors.BRIGHT_RED);
    assertNotNull(ConsoleColors.BRIGHT_GREEN);

    // Test specific color codes
    assertEquals("\u001B[0m", ConsoleColors.RESET);
    assertEquals("\u001B[1m", ConsoleColors.BOLD);
    assertEquals("\u001B[31m", ConsoleColors.RED);
    assertEquals("\u001B[32m", ConsoleColors.GREEN);
    assertEquals("\u001B[33m", ConsoleColors.YELLOW);
    assertEquals("\u001B[34m", ConsoleColors.BLUE);
  }

  @Test
  void testColorizeWithNullInputs() {
    // Test null safety
    assertEquals(null, ConsoleColors.colorize(null, ConsoleColors.RED));
    assertEquals("test", ConsoleColors.colorize("test", null));
    assertEquals(null, ConsoleColors.colorize(null, null));
  }

  @Test
  void testColorizeWithValidInputs() {
    final String text = "test";
    final String result = ConsoleColors.colorize(text, ConsoleColors.RED);

    // Result should either be colored (with ANSI codes) or plain text
    assertTrue(result.equals(text) || result.contains("\u001B["));
  }

  @Test
  void testConvenienceColorMethods() {
    final String text = "test message";

    // Test convenience methods
    final String success = ConsoleColors.success(text);
    final String error = ConsoleColors.error(text);
    final String warning = ConsoleColors.warning(text);
    final String info = ConsoleColors.info(text);
    final String bold = ConsoleColors.bold(text);
    final String dim = ConsoleColors.dim(text);

    assertNotNull(success);
    assertNotNull(error);
    assertNotNull(warning);
    assertNotNull(info);
    assertNotNull(bold);
    assertNotNull(dim);

    // All methods should return at least the original text
    assertTrue(success.contains(text) || success.equals(text));
    assertTrue(error.contains(text) || error.equals(text));
    assertTrue(warning.contains(text) || warning.equals(text));
    assertTrue(info.contains(text) || info.equals(text));
    assertTrue(bold.contains(text) || bold.equals(text));
    assertTrue(dim.contains(text) || dim.equals(text));
  }

  @Test
  void testHeaderFormatting() {
    final String title = "Test Header";
    final String header = ConsoleColors.header(title);

    assertNotNull(header);
    assertTrue(header.contains(title));

    // Header should either be colored or have text decorations
    assertTrue(header.contains("\u001B[") || header.contains("==="));
  }

  @Test
  void testColorSupportDetection() {
    // Color support detection should return a boolean
    final boolean isSupported = ConsoleColors.isColorSupported();
    // We can't assert a specific value since it depends on environment
    // but we can verify the method doesn't throw exceptions
    assertTrue(isSupported || !isSupported); // Tautology to verify no exception
  }

  @EnabledOnOs(OS.LINUX)
  @Test
  void testColorSupportOnLinux() {
    // On Linux CI environments, colors are typically disabled unless forced
    final boolean hasForceColor = System.getenv("FORCE_COLOR") != null;
    final boolean hasNoColor = System.getenv("NO_COLOR") != null;

    if (hasForceColor && !hasNoColor) {
      // If FORCE_COLOR is set and NO_COLOR is not, colors should be enabled
      // (This is environment dependent, so we can't always assert true)
    }
  }

  @Test
  void testColorizedTextLength() {
    final String text = "test";
    final String colored = ConsoleColors.colorize(text, ConsoleColors.RED);

    if (ConsoleColors.isColorSupported()) {
      // If colors are supported, the colored text should be longer due to ANSI codes
      assertTrue(colored.length() >= text.length());
    } else {
      // If colors are not supported, the text should be unchanged
      assertEquals(text, colored);
    }
  }

  @Test
  void testResetCodeIncluded() {
    final String text = "test";
    final String colored = ConsoleColors.colorize(text, ConsoleColors.RED);

    if (ConsoleColors.isColorSupported() && !text.equals(colored)) {
      // If coloring was applied, it should include the reset code
      assertTrue(colored.endsWith(ConsoleColors.RESET));
    }
  }

  @Test
  void testEmptyStringHandling() {
    final String empty = "";
    final String result = ConsoleColors.colorize(empty, ConsoleColors.RED);

    assertNotNull(result);
    // Empty string should remain empty or become reset code only
    assertTrue(result.isEmpty() || result.equals(ConsoleColors.RESET));
  }

  @Test
  void testBackgroundColors() {
    // Test that background color constants are defined
    assertNotNull(ConsoleColors.BG_RED);
    assertNotNull(ConsoleColors.BG_GREEN);
    assertNotNull(ConsoleColors.BG_BLUE);

    // Test specific background color codes
    assertEquals("\u001B[41m", ConsoleColors.BG_RED);
    assertEquals("\u001B[42m", ConsoleColors.BG_GREEN);
    assertEquals("\u001B[44m", ConsoleColors.BG_BLUE);
  }

  @Test
  void testBrightColors() {
    // Test that bright color constants are defined and different from normal colors
    assertNotNull(ConsoleColors.BRIGHT_RED);
    assertNotNull(ConsoleColors.BRIGHT_GREEN);
    assertNotNull(ConsoleColors.BRIGHT_BLUE);

    // Bright colors should have different codes from normal colors
    assertTrue(!ConsoleColors.BRIGHT_RED.equals(ConsoleColors.RED));
    assertTrue(!ConsoleColors.BRIGHT_GREEN.equals(ConsoleColors.GREEN));
    assertTrue(!ConsoleColors.BRIGHT_BLUE.equals(ConsoleColors.BLUE));
  }
}

package ai.tegmentum.wasmtime4j.comparison.reporters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive unit tests for VerbosityLevel enum functionality.
 * Tests parsing, level comparison, and inclusion logic.
 */
final class VerbosityLevelTest {

  @Test
  void testVerbosityLevelProperties() {
    assertEquals(0, VerbosityLevel.QUIET.getLevel());
    assertEquals(1, VerbosityLevel.NORMAL.getLevel());
    assertEquals(2, VerbosityLevel.VERBOSE.getLevel());
    assertEquals(3, VerbosityLevel.DEBUG.getLevel());

    assertEquals("Quiet", VerbosityLevel.QUIET.getDisplayName());
    assertEquals("Normal", VerbosityLevel.NORMAL.getDisplayName());
    assertEquals("Verbose", VerbosityLevel.VERBOSE.getDisplayName());
    assertEquals("Debug", VerbosityLevel.DEBUG.getDisplayName());
  }

  @Test
  void testInclusionLogic() {
    // QUIET includes only itself
    assertTrue(VerbosityLevel.QUIET.includes(VerbosityLevel.QUIET));
    assertFalse(VerbosityLevel.QUIET.includes(VerbosityLevel.NORMAL));
    assertFalse(VerbosityLevel.QUIET.includes(VerbosityLevel.VERBOSE));
    assertFalse(VerbosityLevel.QUIET.includes(VerbosityLevel.DEBUG));

    // NORMAL includes QUIET and NORMAL
    assertTrue(VerbosityLevel.NORMAL.includes(VerbosityLevel.QUIET));
    assertTrue(VerbosityLevel.NORMAL.includes(VerbosityLevel.NORMAL));
    assertFalse(VerbosityLevel.NORMAL.includes(VerbosityLevel.VERBOSE));
    assertFalse(VerbosityLevel.NORMAL.includes(VerbosityLevel.DEBUG));

    // VERBOSE includes QUIET, NORMAL, and VERBOSE
    assertTrue(VerbosityLevel.VERBOSE.includes(VerbosityLevel.QUIET));
    assertTrue(VerbosityLevel.VERBOSE.includes(VerbosityLevel.NORMAL));
    assertTrue(VerbosityLevel.VERBOSE.includes(VerbosityLevel.VERBOSE));
    assertFalse(VerbosityLevel.VERBOSE.includes(VerbosityLevel.DEBUG));

    // DEBUG includes all levels
    assertTrue(VerbosityLevel.DEBUG.includes(VerbosityLevel.QUIET));
    assertTrue(VerbosityLevel.DEBUG.includes(VerbosityLevel.NORMAL));
    assertTrue(VerbosityLevel.DEBUG.includes(VerbosityLevel.VERBOSE));
    assertTrue(VerbosityLevel.DEBUG.includes(VerbosityLevel.DEBUG));
  }

  @ParameterizedTest
  @CsvSource({
      "quiet, QUIET",
      "q, QUIET",
      "0, QUIET",
      "normal, NORMAL",
      "n, NORMAL",
      "1, NORMAL",
      "verbose, VERBOSE",
      "v, VERBOSE",
      "2, VERBOSE",
      "debug, DEBUG",
      "d, DEBUG",
      "3, DEBUG"
  })
  void testValidParsing(final String input, final VerbosityLevel expected) {
    assertEquals(expected, VerbosityLevel.fromString(input));
  }

  @ParameterizedTest
  @CsvSource({
      "QUIET, QUIET",
      "Normal, NORMAL",
      "VERBOSE, VERBOSE",
      "Debug, DEBUG",
      "  quiet  , QUIET",
      "  NORMAL  , NORMAL"
  })
  void testCaseInsensitiveAndTrimmedParsing(final String input, final VerbosityLevel expected) {
    assertEquals(expected, VerbosityLevel.fromString(input));
  }

  @Test
  void testNullAndEmptyStringParsing() {
    assertEquals(VerbosityLevel.NORMAL, VerbosityLevel.fromString(null));
    assertEquals(VerbosityLevel.NORMAL, VerbosityLevel.fromString(""));
    assertEquals(VerbosityLevel.NORMAL, VerbosityLevel.fromString("   "));
  }

  @ParameterizedTest
  @ValueSource(strings = {"invalid", "4", "loud", "silent", "xyz"})
  void testInvalidParsing(final String input) {
    assertThrows(IllegalArgumentException.class, () -> VerbosityLevel.fromString(input));
  }

  @Test
  void testParsingErrorMessage() {
    final IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> VerbosityLevel.fromString("invalid"));
    assertEquals("Unknown verbosity level: invalid", exception.getMessage());
  }
}
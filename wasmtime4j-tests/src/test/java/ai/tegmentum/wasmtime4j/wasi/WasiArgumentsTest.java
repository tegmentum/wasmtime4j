package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmEngine;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiContext;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for WASI CLI argument support and handling.
 * 
 * This test class validates:
 * - CLI argument configuration and retrieval
 * - Argument parsing and special character handling
 * - Large argument list support
 * - Unicode argument support
 * - Argument security and validation
 * - Cross-runtime argument consistency
 */
@Tag(TestCategories.WASI_ARGUMENTS)
@Tag(TestCategories.COMPREHENSIVE_TESTING)
@DisplayName("WASI CLI Arguments Tests")
@Timeout(value = 5, unit = TimeUnit.MINUTES)
public final class WasiArgumentsTest extends BaseIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(WasiArgumentsTest.class.getName());

  /**
   * Tests basic CLI argument configuration and access.
   * Validates that arguments can be set and retrieved correctly.
   */
  @Test
  @DisplayName("CLI Arguments - Basic Configuration and Access")
  void testBasicArgumentAccess() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing basic CLI argument access with " + runtimeType + " runtime");

      try (final WasmEngine engine = runtime.createEngine()) {
        
        final String[] arguments = {
            "test-program",
            "--verbose",
            "--input", "input.txt",
            "--output", "output.txt",
            "--count", "42"
        };

        final WasiContext context = createWasiContextWithArguments(runtimeType, arguments);
        registerForCleanup(context);

        // Test argument retrieval
        final String[] retrievedArgs = context.getArguments();
        assertNotNull(retrievedArgs, "Arguments should not be null");
        assertEquals(arguments.length, retrievedArgs.length, "Argument count should match");

        // Test individual arguments
        for (int i = 0; i < arguments.length; i++) {
          assertEquals(arguments[i], retrievedArgs[i], 
              "Argument at index " + i + " should match");
        }

        // Test that returned array is a defensive copy
        retrievedArgs[0] = "modified";
        final String[] secondRetrieval = context.getArguments();
        assertEquals("test-program", secondRetrieval[0], 
            "Original arguments should be preserved (defensive copy)");

        LOGGER.info("Successfully validated basic CLI argument access");
      }
    });
  }

  /**
   * Tests CLI arguments with special characters and quoting.
   * Validates proper handling of complex argument values.
   */
  @Test
  @DisplayName("CLI Arguments - Special Characters and Quoting")
  void testArgumentSpecialCharacters() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing CLI arguments with special characters using " + runtimeType + " runtime");

      try (final WasmEngine engine = runtime.createEngine()) {
        
        final String[] arguments = {
            "test-program",
            "--message", "Hello, World!",
            "--path", "/usr/bin:/bin",
            "--special", "!@#$%^&*()_+-=[]{}|;':\",./<>?",
            "--spaces", "argument with spaces",
            "--quotes", "\"double quotes\" and 'single quotes'",
            "--empty", "",
            "--equals", "key=value",
            "--semicolon", "command; another_command",
            "--ampersand", "command && other_command"
        };

        final WasiContext context = createWasiContextWithArguments(runtimeType, arguments);
        registerForCleanup(context);

        final String[] retrievedArgs = context.getArguments();
        assertEquals(arguments.length, retrievedArgs.length, "Argument count should match");

        // Validate special characters are preserved
        assertEquals("Hello, World!", retrievedArgs[2], "Punctuation should be preserved");
        assertEquals("/usr/bin:/bin", retrievedArgs[4], "Path separators should be preserved");
        assertEquals("!@#$%^&*()_+-=[]{}|;':\",./<>?", retrievedArgs[6], 
            "Special characters should be preserved");
        assertEquals("argument with spaces", retrievedArgs[8], 
            "Spaces in arguments should be preserved");
        assertEquals("\"double quotes\" and 'single quotes'", retrievedArgs[10], 
            "Quotes should be preserved");
        assertEquals("", retrievedArgs[12], "Empty arguments should be supported");
        assertEquals("key=value", retrievedArgs[14], "Equals sign should be preserved");
        assertEquals("command; another_command", retrievedArgs[16], 
            "Semicolons should be preserved");
        assertEquals("command && other_command", retrievedArgs[18], 
            "Shell operators should be preserved");

        LOGGER.info("Successfully validated special character argument handling");
      }
    });
  }

  /**
   * Tests CLI arguments with Unicode characters.
   * Validates proper Unicode support in arguments.
   */
  @Test
  @DisplayName("CLI Arguments - Unicode Character Support")
  void testArgumentUnicodeSupport() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing CLI arguments with Unicode characters using " + runtimeType + " runtime");

      try (final WasmEngine engine = runtime.createEngine()) {
        
        final String[] arguments = {
            "тест-программа", // Cyrillic
            "--сообщение", "Привет, мир!", // Cyrillic
            "--message", "Hello, 世界!", // Chinese
            "--arabic", "مرحبا بالعالم", // Arabic
            "--emoji", "🚀 🌟 ✨", // Emojis
            "--mixed", "English 中文 العربية русский 🎯",
            "--mathematical", "∑∏∫∆∇≤≥≠±×÷",
            "--currency", "€$¥£₹₿"
        };

        final WasiContext context = createWasiContextWithArguments(runtimeType, arguments);
        registerForCleanup(context);

        final String[] retrievedArgs = context.getArguments();
        assertEquals(arguments.length, retrievedArgs.length, "Argument count should match");

        // Validate Unicode preservation
        for (int i = 0; i < arguments.length; i++) {
          assertEquals(arguments[i], retrievedArgs[i], 
              "Unicode argument at index " + i + " should be preserved");
        }

        LOGGER.info("Successfully validated Unicode argument support");
      }
    });
  }

  /**
   * Tests large CLI argument configurations.
   * Validates handling of many arguments and large argument values.
   */
  @Test
  @DisplayName("CLI Arguments - Large Configuration Handling")
  void testLargeArgumentConfiguration() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing large CLI argument configuration with " + runtimeType + " runtime");

      try (final WasmEngine engine = runtime.createEngine()) {
        
        // Create many arguments
        final String[] arguments = new String[201]; // Program name + 200 arguments
        arguments[0] = "test-program";
        
        for (int i = 1; i <= 100; i++) {
          arguments[i] = "--option-" + i;
          arguments[i + 100] = "value-" + i;
        }

        final WasiContext context = createWasiContextWithArguments(runtimeType, arguments);
        registerForCleanup(context);

        final String[] retrievedArgs = context.getArguments();
        assertEquals(arguments.length, retrievedArgs.length, "Argument count should match");

        // Validate all arguments are preserved
        for (int i = 0; i < arguments.length; i++) {
          assertEquals(arguments[i], retrievedArgs[i], 
              "Argument at index " + i + " should match");
        }

        // Test very long argument value
        final StringBuilder longValue = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
          longValue.append("Very long argument value part ").append(i).append(" ");
        }

        final String[] longArguments = {
            "test-program",
            "--long-value",
            longValue.toString()
        };

        final WasiContext longContext = createWasiContextWithArguments(runtimeType, longArguments);
        registerForCleanup(longContext);

        final String[] longRetrievedArgs = longContext.getArguments();
        assertEquals(3, longRetrievedArgs.length, "Long argument count should match");
        assertEquals(longValue.toString(), longRetrievedArgs[2], 
            "Long argument value should be preserved");

        LOGGER.info("Successfully validated large argument configuration");
      }
    });
  }

  /**
   * Tests CLI argument validation and error handling.
   * Validates proper handling of invalid argument configurations.
   */
  @Test
  @DisplayName("CLI Arguments - Validation and Error Handling")
  void testArgumentValidationAndErrors() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing CLI argument validation with " + runtimeType + " runtime");

      try (final WasmEngine engine = runtime.createEngine()) {
        
        // Test null argument
        assertThrows(IllegalArgumentException.class, () -> {
          createWasiContextBuilder(runtimeType)
              .withArgument(null)
              .build();
        }, "Null argument should throw exception");

        // Test null arguments array
        assertThrows(IllegalArgumentException.class, () -> {
          createWasiContextBuilder(runtimeType)
              .withArguments((String[]) null)
              .build();
        }, "Null arguments array should throw exception");

        // Test empty arguments is valid (no program name)
        final WasiContext emptyArgsContext = createWasiContextBuilder(runtimeType).build();
        registerForCleanup(emptyArgsContext);
        
        final String[] emptyArgs = emptyArgsContext.getArguments();
        assertNotNull(emptyArgs, "Empty arguments should return non-null array");
        assertEquals(0, emptyArgs.length, "Empty arguments should return empty array");

        // Test accessing arguments after context close
        emptyArgsContext.close();
        assertThrows(Exception.class, () -> {
          emptyArgsContext.getArguments();
        }, "Argument access should fail after context close");

        LOGGER.info("Successfully validated argument validation and error handling");
      }
    });
  }

  /**
   * Tests CLI argument immutability and isolation.
   * Validates that arguments cannot be modified after context creation.
   */
  @Test
  @DisplayName("CLI Arguments - Immutability and Isolation")
  void testArgumentImmutability() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing CLI argument immutability with " + runtimeType + " runtime");

      try (final WasmEngine engine = runtime.createEngine()) {
        
        final String[] originalArgs = {
            "test-program",
            "--input", "input.txt",
            "--output", "output.txt"
        };

        final WasiContext context = createWasiContextWithArguments(runtimeType, originalArgs);
        registerForCleanup(context);

        // Get arguments and modify them
        final String[] retrievedArgs = context.getArguments();
        retrievedArgs[0] = "modified-program";
        retrievedArgs[2] = "modified-input.txt";

        // Verify context arguments are unchanged
        final String[] contextArgs = context.getArguments();
        assertEquals(originalArgs.length, contextArgs.length, 
            "Context arguments should not be affected by external modifications");
        assertEquals("test-program", contextArgs[0], 
            "Program name should be preserved");
        assertEquals("input.txt", contextArgs[2], 
            "Original argument values should be preserved");

        // Test multiple retrievals return consistent results
        final String[] secondRetrieval = context.getArguments();
        assertArrayEquals(contextArgs, secondRetrieval, 
            "Multiple argument retrievals should return consistent results");

        LOGGER.info("Successfully validated argument immutability");
      }
    });
  }

  /**
   * Tests concurrent CLI argument access.
   * Validates thread safety of argument operations.
   */
  @Test
  @DisplayName("CLI Arguments - Concurrent Access Thread Safety")
  void testArgumentConcurrentAccess() {
    skipIfCategoryNotEnabled(TestCategories.PERFORMANCE_TESTING);
    
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing concurrent CLI argument access with " + runtimeType + " runtime");

      try (final WasmEngine engine = runtime.createEngine()) {
        
        final String[] arguments = new String[21]; // Program + 20 args
        arguments[0] = "test-program";
        for (int i = 1; i < 21; i++) {
          arguments[i] = "arg-" + i;
        }

        final WasiContext context = createWasiContextWithArguments(runtimeType, arguments);
        registerForCleanup(context);

        final int threadCount = 10;
        final Thread[] threads = new Thread[threadCount];
        final Exception[] exceptions = new Exception[threadCount];

        for (int i = 0; i < threadCount; i++) {
          final int threadIndex = i;
          threads[i] = new Thread(() -> {
            try {
              for (int j = 0; j < 100; j++) {
                // Test argument access
                final String[] args = context.getArguments();
                assertEquals(21, args.length, 
                    "Thread " + threadIndex + " should get all arguments");
                assertEquals("test-program", args[0], 
                    "Thread " + threadIndex + " should get correct program name");
                
                // Test specific argument
                final int argIndex = (j % 20) + 1;
                assertEquals("arg-" + argIndex, args[argIndex], 
                    "Thread " + threadIndex + " should get correct argument at index " + argIndex);
                
                // Verify array immutability
                args[0] = "modified-" + threadIndex;
                final String[] freshArgs = context.getArguments();
                assertEquals("test-program", freshArgs[0], 
                    "Thread " + threadIndex + " modifications should not affect context");
              }
            } catch (final Exception e) {
              exceptions[threadIndex] = e;
            }
          });
        }

        // Start all threads
        for (final Thread thread : threads) {
          thread.start();
        }

        // Wait for completion
        for (final Thread thread : threads) {
          thread.join(10000); // 10 second timeout
        }

        // Check for exceptions
        for (int i = 0; i < threadCount; i++) {
          if (exceptions[i] != null) {
            throw new AssertionError("Thread " + i + " encountered exception", exceptions[i]);
          }
        }

        LOGGER.info("Successfully tested concurrent argument access with " + threadCount + " threads");
      }
    });
  }

  /**
   * Tests CLI argument parsing patterns.
   * Validates common CLI parsing scenarios and patterns.
   */
  @Test
  @DisplayName("CLI Arguments - Common Parsing Patterns")
  void testArgumentParsingPatterns() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing CLI argument parsing patterns with " + runtimeType + " runtime");

      try (final WasmEngine engine = runtime.createEngine()) {
        
        // Test common CLI patterns
        final String[] arguments = {
            "myprogram",
            "-v", // Short flag
            "--verbose", // Long flag
            "-f", "file.txt", // Short option with value
            "--output", "result.txt", // Long option with value
            "--config=settings.json", // Equals-style option
            "-abc", // Combined short flags
            "--", // End of options marker
            "positional1",
            "positional2",
            "--not-a-flag" // Should be treated as positional after --
        };

        final WasiContext context = createWasiContextWithArguments(runtimeType, arguments);
        registerForCleanup(context);

        final String[] retrievedArgs = context.getArguments();
        assertEquals(arguments.length, retrievedArgs.length, "All arguments should be preserved");

        // Validate specific patterns
        assertEquals("myprogram", retrievedArgs[0], "Program name should be preserved");
        assertEquals("-v", retrievedArgs[1], "Short flags should be preserved");
        assertEquals("--verbose", retrievedArgs[2], "Long flags should be preserved");
        assertEquals("-f", retrievedArgs[3], "Short options should be preserved");
        assertEquals("file.txt", retrievedArgs[4], "Option values should be preserved");
        assertEquals("--config=settings.json", retrievedArgs[6], "Equals-style options should be preserved");
        assertEquals("-abc", retrievedArgs[7], "Combined flags should be preserved");
        assertEquals("--", retrievedArgs[8], "Option terminator should be preserved");
        assertEquals("--not-a-flag", retrievedArgs[11], "Post-terminator args should be preserved");

        // Test GNU-style long options
        final String[] gnuArgs = {
            "program",
            "--help",
            "--version",
            "--input=/path/to/input",
            "--output=/path/to/output",
            "--verbose=true",
            "--count=42"
        };

        final WasiContext gnuContext = createWasiContextWithArguments(runtimeType, gnuArgs);
        registerForCleanup(gnuContext);

        final String[] gnuRetrievedArgs = gnuContext.getArguments();
        assertArrayEquals(gnuArgs, gnuRetrievedArgs, "GNU-style arguments should be preserved");

        LOGGER.info("Successfully validated CLI argument parsing patterns");
      }
    });
  }

  /**
   * Creates a WASI context with specified CLI arguments.
   */
  private WasiContext createWasiContextWithArguments(
      final RuntimeType runtimeType, 
      final String[] arguments) throws Exception {
    
    if (runtimeType == RuntimeType.JNI) {
      return ai.tegmentum.wasmtime4j.jni.wasi.WasiContext.builder()
          .withArguments(arguments)
          .withWorkingDirectory("/app")
          .build();
    } else {
      return (WasiContext) ai.tegmentum.wasmtime4j.panama.wasi.WasiContext.builder()
          .withArguments(arguments)
          .withWorkingDirectory("/app")
          .build();
    }
  }

  /**
   * Creates a WASI context builder for the specified runtime type.
   */
  private Object createWasiContextBuilder(final RuntimeType runtimeType) {
    if (runtimeType == RuntimeType.JNI) {
      return ai.tegmentum.wasmtime4j.jni.wasi.WasiContext.builder();
    } else {
      return ai.tegmentum.wasmtime4j.panama.wasi.WasiContext.builder();
    }
  }
}
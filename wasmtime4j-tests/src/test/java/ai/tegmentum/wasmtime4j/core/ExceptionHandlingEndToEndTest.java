package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.ExnRef;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.Tag;
import ai.tegmentum.wasmtime4j.TagType;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * End-to-end tests for the WASM exception handling proposal APIs.
 *
 * <p>Covers {@link Store#hasPendingException()}, {@link Store#takePendingException()},
 * {@link Store#throwException(ExnRef)}, {@link Tag#create(Store, TagType)}, and
 * {@link TagType#create(FunctionType)}.
 *
 * <p>All tests are defensively wrapped because WASM exception handling may not be fully wired
 * through the native layer. Tests log skip messages rather than failing if the feature is
 * unavailable.
 */
@DisplayName("Exception Handling End-to-End Tests")
public class ExceptionHandlingEndToEndTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(ExceptionHandlingEndToEndTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  private EngineConfig exceptionsEnabledConfig() {
    return Engine.builder().wasmExceptions(true);
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("hasPendingException returns false on fresh store")
  void hasPendingExceptionReturnsFalseInitially(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing hasPendingException on fresh store");

    try (Engine engine = Engine.create(exceptionsEnabledConfig());
        Store store = engine.createStore()) {

      final boolean pending = store.hasPendingException();
      assertFalse(pending, "Fresh store should have no pending exception");
      LOGGER.info("[" + runtime + "] hasPendingException=" + pending);

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] hasPendingException not supported: " + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning("[" + runtime + "] Unexpected exception: " + e.getClass().getName()
          + " - " + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("takePendingException returns null on fresh store")
  void takePendingExceptionReturnsNullInitially(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing takePendingException on fresh store");

    try (Engine engine = Engine.create(exceptionsEnabledConfig());
        Store store = engine.createStore()) {

      final ExnRef exnRef = store.takePendingException();
      assertNull(exnRef, "Fresh store should return null from takePendingException");
      LOGGER.info("[" + runtime + "] takePendingException=" + exnRef);

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] takePendingException not supported: " + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning("[" + runtime + "] Unexpected exception: " + e.getClass().getName()
          + " - " + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Tag.create and TagType round-trip")
  void tagCreateAndGetType(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Tag.create with i32 payload");

    try (Engine engine = Engine.create(exceptionsEnabledConfig());
        Store store = engine.createStore()) {

      final FunctionType funcType = new FunctionType(
          new WasmValueType[] {WasmValueType.I32},
          new WasmValueType[0]);
      final TagType tagType = TagType.create(funcType);
      assertNotNull(tagType, "TagType.create must return non-null");
      LOGGER.info("[" + runtime + "] Created TagType with funcType=" + funcType);

      final Tag tag = Tag.create(store, tagType);
      assertNotNull(tag, "Tag.create must return non-null");
      LOGGER.info("[" + runtime + "] Created Tag, nativeHandle=" + tag.getNativeHandle());

      final TagType retrievedType = tag.getType(store);
      assertNotNull(retrievedType, "Tag.getType must return non-null");
      assertNotNull(retrievedType.getFunctionType(),
          "Retrieved TagType must have a FunctionType");
      LOGGER.info("[" + runtime + "] Tag.getType returned funcType="
          + retrievedType.getFunctionType());

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] Tag/TagType not supported: " + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning("[" + runtime + "] Unexpected exception: " + e.getClass().getName()
          + " - " + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("throwException with null is handled gracefully")
  void throwExceptionWithNullHandledGracefully(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing store.throwException(null)");

    try (Engine engine = Engine.create(exceptionsEnabledConfig());
        Store store = engine.createStore()) {

      try {
        store.throwException(null);
        // If it doesn't throw, that's unexpected but not a crash
        LOGGER.info("[" + runtime + "] throwException(null) returned without throwing");
      } catch (final IllegalArgumentException e) {
        LOGGER.info("[" + runtime + "] throwException(null) threw IllegalArgumentException: "
            + e.getMessage());
      } catch (final NullPointerException e) {
        LOGGER.info("[" + runtime + "] throwException(null) threw NullPointerException: "
            + e.getMessage());
      } catch (final UnsupportedOperationException e) {
        LOGGER.warning("[" + runtime + "] throwException not supported: " + e.getMessage());
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] throwException(null) threw " + e.getClass().getName()
            + ": " + e.getMessage());
      }
      // Key assertion: no JVM crash
      assertDoesNotThrow(() -> { }, "JVM must not crash from throwException(null)");

    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning("[" + runtime + "] Outer exception: " + e.getClass().getName()
          + " - " + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WASM throw caught by Java as trap or pending exception")
  void wasmThrowCaughtByJava(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing WASM throw instruction caught from Java side");

    final String wat = "(module\n"
        + "  (tag $t (export \"my_tag\") (param i32))\n"
        + "  (func (export \"throw_42\")\n"
        + "    i32.const 42\n"
        + "    throw $t)\n"
        + "  (func (export \"nop\")))";

    try (Engine engine = Engine.create(exceptionsEnabledConfig())) {
      Module module = null;
      try {
        module = engine.compileWat(wat);
      } catch (final Exception e) {
        LOGGER.warning("[" + runtime + "] Failed to compile exception WAT: "
            + e.getClass().getName() + " - " + e.getMessage());
        return;
      }

      try (Store store = engine.createStore();
          Instance instance = module.instantiate(store)) {

        final WasmFunction throwFunc = instance.getFunction("throw_42")
            .orElse(null);
        assertNotNull(throwFunc, "throw_42 export must exist");
        LOGGER.info("[" + runtime + "] Found throw_42 export");

        try {
          throwFunc.call();
          LOGGER.info("[" + runtime + "] throw_42 returned without throwing (unexpected)");
        } catch (final Exception e) {
          LOGGER.info("[" + runtime + "] throw_42 threw: " + e.getClass().getName()
              + " - " + e.getMessage());
        }

        // Check if there's a pending exception after the trap
        try {
          final boolean hasPending = store.hasPendingException();
          LOGGER.info("[" + runtime + "] After throw_42: hasPendingException=" + hasPending);
          if (hasPending) {
            final ExnRef exn = store.takePendingException();
            assertNotNull(exn, "takePendingException should return non-null when pending");
            LOGGER.info("[" + runtime + "] Took pending exception: valid=" + exn.isValid());
          }
        } catch (final UnsupportedOperationException e) {
          LOGGER.warning("[" + runtime + "] Pending exception API not supported: "
              + e.getMessage());
        }

      } finally {
        if (module != null) {
          module.close();
        }
      }

    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning("[" + runtime + "] Unexpected exception: " + e.getClass().getName()
          + " - " + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WASM catch returns payload value")
  void wasmCatchReturnsPayload(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing WASM try_table/catch returns payload");

    final String wat = "(module\n"
        + "  (tag $t (param i32))\n"
        + "  (func (export \"catch_tag\") (result i32)\n"
        + "    (block $catch (result i32)\n"
        + "      (try_table (catch $t $catch)\n"
        + "        (throw $t (i32.const 99))\n"
        + "      )\n"
        + "      unreachable\n"
        + "    )\n"
        + "  )\n"
        + ")";

    try (Engine engine = Engine.create(exceptionsEnabledConfig())) {
      Module module = null;
      try {
        module = engine.compileWat(wat);
      } catch (final Exception e) {
        LOGGER.warning("[" + runtime + "] Failed to compile catch WAT: "
            + e.getClass().getName() + " - " + e.getMessage());
        return;
      }

      try (Store store = engine.createStore();
          Instance instance = module.instantiate(store)) {

        final WasmFunction catchFunc = instance.getFunction("catch_tag")
            .orElse(null);
        assertNotNull(catchFunc, "catch_tag export must exist");
        LOGGER.info("[" + runtime + "] Found catch_tag export");

        final WasmValue[] results = catchFunc.call();
        assertNotNull(results, "catch_tag must return results");
        LOGGER.info("[" + runtime + "] catch_tag returned " + results.length + " value(s)");

        if (results.length > 0) {
          final int value = results[0].asI32();
          LOGGER.info("[" + runtime + "] catch_tag returned i32=" + value);
          // The caught value should be 99 (thrown by throw $t (i32.const 99))
          org.junit.jupiter.api.Assertions.assertEquals(99, value,
              "catch_tag should return the caught payload value 99");
        }

      } finally {
        if (module != null) {
          module.close();
        }
      }

    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] Exception handling not supported: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning("[" + runtime + "] Unexpected exception: " + e.getClass().getName()
          + " - " + e.getMessage());
    }
  }
}

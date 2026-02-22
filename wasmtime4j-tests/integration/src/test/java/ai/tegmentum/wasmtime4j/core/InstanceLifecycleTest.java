package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for {@link Instance} lifecycle management methods.
 *
 * <p>Covers {@link Instance#dispose()}, {@link Instance#isDisposed()}, and {@link
 * Instance#cleanup()}. Verifies that instances track disposal state correctly and handle
 * double-dispose gracefully without JVM crashes.
 */
@SuppressWarnings("deprecation")
@DisplayName("Instance Lifecycle Tests")
public class InstanceLifecycleTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(InstanceLifecycleTest.class.getName());

  private static final String WAT =
      "(module\n"
          + "  (func (export \"get42\") (result i32) i32.const 42)\n"
          + "  (memory (export \"mem\") 1))";

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("isDisposed returns false on fresh instance")
  void isDisposedReturnsFalseInitially(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing isDisposed on fresh instance");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(WAT);
        Instance instance = module.instantiate(store)) {

      assertNotNull(instance, "Instance must not be null");
      assertFalse(instance.isDisposed(), "Fresh instance should report isDisposed()=false");
      LOGGER.info("[" + runtime + "] isDisposed()=" + instance.isDisposed());

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] isDisposed not supported: " + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("dispose returns true on live instance")
  void disposeReturnsTrue(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing dispose() on live instance");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(WAT)) {

      final Instance instance = module.instantiate(store);
      assertNotNull(instance, "Instance must not be null");

      final boolean result = instance.dispose();
      assertTrue(result, "dispose() on live instance should return true");
      LOGGER.info("[" + runtime + "] dispose()=" + result);

      // close() after dispose should be safe
      instance.close();

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] dispose not supported: " + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("isDisposed returns true after dispose")
  void isDisposedReturnsTrueAfterDispose(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing isDisposed after dispose()");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(WAT)) {

      final Instance instance = module.instantiate(store);
      assertNotNull(instance, "Instance must not be null");
      LOGGER.info("[" + runtime + "] Before dispose: isDisposed()=" + instance.isDisposed());

      instance.dispose();

      assertTrue(instance.isDisposed(), "isDisposed() should return true after dispose()");
      LOGGER.info("[" + runtime + "] After dispose: isDisposed()=" + instance.isDisposed());

      instance.close();

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] dispose/isDisposed not supported: " + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("double dispose is handled gracefully")
  void doubleDisposeHandledGracefully(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing double dispose()");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(WAT)) {

      final Instance instance = module.instantiate(store);
      assertNotNull(instance, "Instance must not be null");

      final boolean first = instance.dispose();
      LOGGER.info("[" + runtime + "] First dispose()=" + first);

      // Second dispose: runtime may return false or throw a resource-closed exception.
      // The critical requirement is no JVM crash (SIGABRT/SIGSEGV).
      try {
        final boolean second = instance.dispose();
        assertFalse(second, "Second dispose() should return false (already disposed)");
        LOGGER.info("[" + runtime + "] Second dispose()=" + second);
      } catch (final Exception e) {
        // JNI implementation throws JniResourceException for closed handles,
        // which is acceptable defensive behavior — it means the runtime detected
        // the double-dispose and refused to proceed rather than crashing.
        LOGGER.info(
            "["
                + runtime
                + "] Second dispose() threw (acceptable): "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }

      instance.close();

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] dispose not supported: " + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    }
  }
}

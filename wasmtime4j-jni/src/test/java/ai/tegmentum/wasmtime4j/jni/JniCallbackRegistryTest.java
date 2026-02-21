package ai.tegmentum.wasmtime4j.jni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.CallbackRegistry;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniCallbackRegistry}.
 *
 * <p>Tests validation logic, initial state, close lifecycle, and operations-after-close behavior.
 * All validation tests exercise pre-native-call code paths that run entirely in Java.
 *
 * <p>Callback registration tests that require native resources (JniFunctionReference) are covered
 * by integration tests. These unit tests focus on input validation, lifecycle management, and
 * metrics tracking that can be verified without native bindings.
 */
@DisplayName("JniCallbackRegistry Tests")
class JniCallbackRegistryTest {

  private static final long VALID_HANDLE = 0x12345678L;

  private JniEngine testEngine;
  private JniStore testStore;
  private JniCallbackRegistry registry;

  @BeforeEach
  void setUp() {
    testEngine = new JniEngine(VALID_HANDLE);
    testStore = new JniStore(VALID_HANDLE, testEngine);
    registry = new JniCallbackRegistry(testStore);
  }

  @AfterEach
  void tearDown() {
    if (registry != null) {
      try {
        registry.close();
      } catch (WasmException e) {
        // Expected - close may fail since there are no real native resources
      }
    }
    if (testStore != null) {
      testStore.markClosedForTesting();
    }
    if (testEngine != null) {
      testEngine.markClosedForTesting();
    }
  }

  @Nested
  @DisplayName("Constructor Validation")
  class ConstructorValidation {

    @Test
    @DisplayName("Null store should throw NullPointerException")
    void nullStoreShouldThrow() {
      assertThatThrownBy(() -> new JniCallbackRegistry(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Store cannot be null");
    }

    @Test
    @DisplayName("Valid store should create registry successfully")
    void validStoreShouldCreateRegistry() {
      assertThat(registry).isNotNull();
      assertThat(registry.getCallbackCount()).isZero();
    }
  }

  @Nested
  @DisplayName("Initial State")
  class InitialState {

    @Test
    @DisplayName("New registry should have zero callback count")
    void newRegistryShouldHaveZeroCallbackCount() {
      assertThat(registry.getCallbackCount())
          .as("New registry should have no callbacks")
          .isZero();
    }

    @Test
    @DisplayName("New registry should have no callback for any name")
    void newRegistryShouldHaveNoCallbacks() {
      assertThat(registry.hasCallback("nonexistent"))
          .as("New registry should not have any callbacks")
          .isFalse();
    }

    @Test
    @DisplayName("New registry metrics should be zero")
    void newRegistryMetricsShouldBeZero() {
      final CallbackRegistry.CallbackMetrics metrics = registry.getMetrics();
      assertThat(metrics.getTotalInvocations()).isZero();
      assertThat(metrics.getFailureCount()).isZero();
      assertThat(metrics.getTimeoutCount()).isZero();
      assertThat(metrics.getTotalExecutionTimeNanos()).isZero();
      assertThat(metrics.getAverageExecutionTimeNanos()).isEqualTo(0.0);
    }
  }

  @Nested
  @DisplayName("Callback Registration Validation")
  class CallbackRegistrationValidation {

    @Test
    @DisplayName("registerCallback with null name should throw WasmException")
    void registerWithNullNameShouldThrow() {
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32});

      assertThatThrownBy(() -> registry.registerCallback(null, params -> params, funcType))
          .isInstanceOf(WasmException.class)
          .hasMessageContaining("Callback name cannot be null");
    }

    @Test
    @DisplayName("registerCallback with null callback should throw WasmException")
    void registerWithNullCallbackShouldThrow() {
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32});

      assertThatThrownBy(() -> registry.registerCallback("test", null, funcType))
          .isInstanceOf(WasmException.class)
          .hasMessageContaining("Callback cannot be null");
    }

    @Test
    @DisplayName("registerCallback with null function type should throw WasmException")
    void registerWithNullFunctionTypeShouldThrow() {
      assertThatThrownBy(() -> registry.registerCallback("test", params -> params, null))
          .isInstanceOf(WasmException.class)
          .hasMessageContaining("Function type cannot be null");
    }
  }

  @Nested
  @DisplayName("Async Callback Registration Validation")
  class AsyncCallbackRegistrationValidation {

    @Test
    @DisplayName("registerAsyncCallback with null name should throw WasmException")
    void registerAsyncWithNullNameShouldThrow() {
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32});

      assertThatThrownBy(
              () ->
                  registry.registerAsyncCallback(
                      null,
                      params -> java.util.concurrent.CompletableFuture.completedFuture(params),
                      funcType))
          .isInstanceOf(WasmException.class)
          .hasMessageContaining("Callback name cannot be null");
    }

    @Test
    @DisplayName("registerAsyncCallback with null callback should throw WasmException")
    void registerAsyncWithNullCallbackShouldThrow() {
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32});

      assertThatThrownBy(() -> registry.registerAsyncCallback("test", null, funcType))
          .isInstanceOf(WasmException.class)
          .hasMessageContaining("Callback cannot be null");
    }

    @Test
    @DisplayName("registerAsyncCallback with null function type should throw WasmException")
    void registerAsyncWithNullFunctionTypeShouldThrow() {
      assertThatThrownBy(
              () ->
                  registry.registerAsyncCallback(
                      "test",
                      params -> java.util.concurrent.CompletableFuture.completedFuture(params),
                      null))
          .isInstanceOf(WasmException.class)
          .hasMessageContaining("Function type cannot be null");
    }
  }

  @Nested
  @DisplayName("Unregistration Validation")
  class UnregistrationValidation {

    @Test
    @DisplayName("unregisterCallback with null handle should throw NullPointerException")
    void unregisterNullHandleShouldThrow() {
      assertThatThrownBy(() -> registry.unregisterCallback(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Callback handle cannot be null");
    }
  }

  @Nested
  @DisplayName("Invocation Validation")
  class InvocationValidation {

    @Test
    @DisplayName("invokeCallback with null handle should throw NullPointerException")
    void invokeNullHandleShouldThrow() {
      assertThatThrownBy(() -> registry.invokeCallback(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Callback handle cannot be null");
    }
  }

  @Nested
  @DisplayName("HasCallback Validation")
  class HasCallbackValidation {

    @Test
    @DisplayName("hasCallback with null name should throw NullPointerException")
    void hasCallbackNullNameShouldThrow() {
      assertThatThrownBy(() -> registry.hasCallback(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Callback name cannot be null");
    }
  }

  @Nested
  @DisplayName("Close Lifecycle")
  class CloseLifecycle {

    @Test
    @DisplayName("Double close should not throw")
    void doubleCloseShouldNotThrow() throws WasmException {
      registry.close();
      assertThatCode(() -> registry.close())
          .as("Second close should be a no-op")
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("registerCallback after close should throw IllegalStateException")
    void registerAfterCloseShouldThrow() throws WasmException {
      registry.close();
      assertThatThrownBy(
              () -> registry.registerCallback("test", params -> params, createSimpleFunctionType()))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("closed");
    }

    @Test
    @DisplayName("registerAsyncCallback after close should throw IllegalStateException")
    void registerAsyncAfterCloseShouldThrow() throws WasmException {
      registry.close();
      assertThatThrownBy(
              () ->
                  registry.registerAsyncCallback(
                      "test",
                      params -> java.util.concurrent.CompletableFuture.completedFuture(params),
                      createSimpleFunctionType()))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("closed");
    }

    @Test
    @DisplayName("hasCallback after close should return false (no ensureNotClosed guard)")
    void hasCallbackAfterCloseShouldReturnFalse() throws WasmException {
      registry.close();
      // hasCallback and getCallbackCount do not call ensureNotClosed
      assertThat(registry.hasCallback("test"))
          .as("hasCallback should return false on closed empty registry")
          .isFalse();
    }

    @Test
    @DisplayName("getCallbackCount after close should still work")
    void getCallbackCountAfterCloseShouldWork() throws WasmException {
      registry.close();
      // getCallbackCount doesn't call ensureNotClosed
      assertThat(registry.getCallbackCount()).isZero();
    }

    @Test
    @DisplayName("getMetrics after close should still work")
    void getMetricsAfterCloseShouldWork() throws WasmException {
      registry.close();
      // getMetrics doesn't call ensureNotClosed
      assertThat(registry.getMetrics()).isNotNull();
    }
  }

  @Nested
  @DisplayName("Implements CallbackRegistry")
  class ImplementsCallbackRegistry {

    @Test
    @DisplayName("JniCallbackRegistry should implement CallbackRegistry interface")
    void shouldImplementCallbackRegistry() {
      assertThat(registry)
          .as("JniCallbackRegistry should implement CallbackRegistry")
          .isInstanceOf(CallbackRegistry.class);
    }
  }

  private FunctionType createSimpleFunctionType() {
    return new FunctionType(
        new WasmValueType[] {WasmValueType.I32},
        new WasmValueType[] {WasmValueType.I32});
  }
}

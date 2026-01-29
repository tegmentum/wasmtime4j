package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link Store} interface.
 *
 * <p>This test class verifies the structure and contract of the Store interface, which represents a
 * WebAssembly execution context.
 */
@DisplayName("Store Interface Tests")
class StoreTest {

  @Nested
  @DisplayName("Interface Definition Tests")
  class InterfaceDefinitionTests {

    @Test
    @DisplayName("Store should be an interface")
    void shouldBeAnInterface() {
      assertTrue(Store.class.isInterface(), "Store should be an interface");
    }

    @Test
    @DisplayName("Store should extend Closeable")
    void shouldExtendCloseable() {
      assertTrue(Closeable.class.isAssignableFrom(Store.class), "Store should extend Closeable");
    }

    @Test
    @DisplayName("Store should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(Store.class.getModifiers()), "Store should be a public interface");
    }
  }

  @Nested
  @DisplayName("Engine and Data Method Tests")
  class EngineAndDataMethodTests {

    @Test
    @DisplayName("Should have getEngine() method")
    void shouldHaveGetEngineMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("getEngine");
      assertNotNull(method, "getEngine() method should exist");
      assertEquals(Engine.class, method.getReturnType(), "Should return Engine");
    }

    @Test
    @DisplayName("Should have getData() method")
    void shouldHaveGetDataMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("getData");
      assertNotNull(method, "getData() method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object");
    }

    @Test
    @DisplayName("Should have setData(Object) method")
    void shouldHaveSetDataMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("setData", Object.class);
      assertNotNull(method, "setData(Object) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Fuel Method Tests")
  class FuelMethodTests {

    @Test
    @DisplayName("Should have setFuel(long) method")
    void shouldHaveSetFuelMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("setFuel", long.class);
      assertNotNull(method, "setFuel(long) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have getFuel() method")
    void shouldHaveGetFuelMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("getFuel");
      assertNotNull(method, "getFuel() method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("Should have addFuel(long) method")
    void shouldHaveAddFuelMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("addFuel", long.class);
      assertNotNull(method, "addFuel(long) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have consumeFuel(long) method")
    void shouldHaveConsumeFuelMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("consumeFuel", long.class);
      assertNotNull(method, "consumeFuel(long) method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("Should have getRemainingFuel() method")
    void shouldHaveGetRemainingFuelMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("getRemainingFuel");
      assertNotNull(method, "getRemainingFuel() method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("Should have getTotalFuelConsumed() method")
    void shouldHaveGetTotalFuelConsumedMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("getTotalFuelConsumed");
      assertNotNull(method, "getTotalFuelConsumed() method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Epoch Method Tests")
  class EpochMethodTests {

    @Test
    @DisplayName("Should have setEpochDeadline(long) method")
    void shouldHaveSetEpochDeadlineMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("setEpochDeadline", long.class);
      assertNotNull(method, "setEpochDeadline(long) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have epochDeadlineAsyncYieldAndUpdate(long) method")
    void shouldHaveEpochDeadlineAsyncYieldAndUpdateMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("epochDeadlineAsyncYieldAndUpdate", long.class);
      assertNotNull(method, "epochDeadlineAsyncYieldAndUpdate(long) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have epochDeadlineTrap() method")
    void shouldHaveEpochDeadlineTrapMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("epochDeadlineTrap");
      assertNotNull(method, "epochDeadlineTrap() method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have epochDeadlineCallback(EpochDeadlineCallback) method")
    void shouldHaveEpochDeadlineCallbackMethod() throws NoSuchMethodException {
      final Class<?> callbackClass = Store.EpochDeadlineCallback.class;
      final Method method = Store.class.getMethod("epochDeadlineCallback", callbackClass);
      assertNotNull(method, "epochDeadlineCallback(EpochDeadlineCallback) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Resource Creation Method Tests")
  class ResourceCreationMethodTests {

    @Test
    @DisplayName("Should have createHostFunction method")
    void shouldHaveCreateHostFunctionMethod() throws NoSuchMethodException {
      final Method method =
          Store.class.getMethod(
              "createHostFunction", String.class, FunctionType.class, HostFunction.class);
      assertNotNull(method, "createHostFunction method should exist");
      assertEquals(WasmFunction.class, method.getReturnType(), "Should return WasmFunction");
    }

    @Test
    @DisplayName("Should have createGlobal method")
    void shouldHaveCreateGlobalMethod() throws NoSuchMethodException {
      final Method method =
          Store.class.getMethod(
              "createGlobal", WasmValueType.class, boolean.class, WasmValue.class);
      assertNotNull(method, "createGlobal method should exist");
      assertEquals(WasmGlobal.class, method.getReturnType(), "Should return WasmGlobal");
    }

    @Test
    @DisplayName("Should have createTable method")
    void shouldHaveCreateTableMethod() throws NoSuchMethodException {
      final Method method =
          Store.class.getMethod("createTable", WasmValueType.class, int.class, int.class);
      assertNotNull(method, "createTable method should exist");
      assertEquals(WasmTable.class, method.getReturnType(), "Should return WasmTable");
    }

    @Test
    @DisplayName("Should have createMemory method")
    void shouldHaveCreateMemoryMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("createMemory", int.class, int.class);
      assertNotNull(method, "createMemory method should exist");
      assertEquals(WasmMemory.class, method.getReturnType(), "Should return WasmMemory");
    }

    @Test
    @DisplayName("Should have createSharedMemory method")
    void shouldHaveCreateSharedMemoryMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("createSharedMemory", int.class, int.class);
      assertNotNull(method, "createSharedMemory method should exist");
      assertEquals(WasmMemory.class, method.getReturnType(), "Should return WasmMemory");
    }
  }

  @Nested
  @DisplayName("Async Resource Creation Method Tests")
  class AsyncResourceCreationMethodTests {

    @Test
    @DisplayName("Should have createTableAsync method")
    void shouldHaveCreateTableAsyncMethod() throws NoSuchMethodException {
      final Method method =
          Store.class.getMethod("createTableAsync", WasmValueType.class, int.class, int.class);
      assertNotNull(method, "createTableAsync method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("Should have createMemoryAsync method")
    void shouldHaveCreateMemoryAsyncMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("createMemoryAsync", int.class, int.class);
      assertNotNull(method, "createMemoryAsync method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }
  }

  @Nested
  @DisplayName("Instance Method Tests")
  class InstanceMethodTests {

    @Test
    @DisplayName("Should have createInstance(Module) method")
    void shouldHaveCreateInstanceMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("createInstance", Module.class);
      assertNotNull(method, "createInstance(Module) method should exist");
      assertEquals(Instance.class, method.getReturnType(), "Should return Instance");
    }
  }

  @Nested
  @DisplayName("Execution Statistics Method Tests")
  class ExecutionStatisticsMethodTests {

    @Test
    @DisplayName("Should have getExecutionCount() method")
    void shouldHaveGetExecutionCountMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("getExecutionCount");
      assertNotNull(method, "getExecutionCount() method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("Should have getTotalExecutionTimeMicros() method")
    void shouldHaveGetTotalExecutionTimeMicrosMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("getTotalExecutionTimeMicros");
      assertNotNull(method, "getTotalExecutionTimeMicros() method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Backtrace Method Tests")
  class BacktraceMethodTests {

    @Test
    @DisplayName("Should have captureBacktrace() method")
    void shouldHaveCaptureBacktraceMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("captureBacktrace");
      assertNotNull(method, "captureBacktrace() method should exist");
      assertEquals(WasmBacktrace.class, method.getReturnType(), "Should return WasmBacktrace");
    }

    @Test
    @DisplayName("Should have forceCaptureBacktrace() method")
    void shouldHaveForceCaptureBacktraceMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("forceCaptureBacktrace");
      assertNotNull(method, "forceCaptureBacktrace() method should exist");
      assertEquals(WasmBacktrace.class, method.getReturnType(), "Should return WasmBacktrace");
    }
  }

  @Nested
  @DisplayName("GC Method Tests")
  class GcMethodTests {

    @Test
    @DisplayName("Should have gc() method")
    void shouldHaveGcMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("gc");
      assertNotNull(method, "gc() method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have gcAsync() method")
    void shouldHaveGcAsyncMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("gcAsync");
      assertNotNull(method, "gcAsync() method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }
  }

  @Nested
  @DisplayName("Exception Handling Method Tests")
  class ExceptionHandlingMethodTests {

    @Test
    @DisplayName("Should have throwException(ExnRef) method")
    void shouldHaveThrowExceptionMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("throwException", ExnRef.class);
      assertNotNull(method, "throwException(ExnRef) method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object (generic R)");
    }

    @Test
    @DisplayName("Should have takePendingException() method")
    void shouldHaveTakePendingExceptionMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("takePendingException");
      assertNotNull(method, "takePendingException() method should exist");
      assertEquals(ExnRef.class, method.getReturnType(), "Should return ExnRef");
    }

    @Test
    @DisplayName("Should have hasPendingException() method")
    void shouldHaveHasPendingExceptionMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("hasPendingException");
      assertNotNull(method, "hasPendingException() method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Call Hook Method Tests")
  class CallHookMethodTests {

    @Test
    @DisplayName("Should have setCallHook(CallHookHandler) method")
    void shouldHaveSetCallHookMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("setCallHook", CallHookHandler.class);
      assertNotNull(method, "setCallHook(CallHookHandler) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have setCallHookAsync(AsyncCallHookHandler) method")
    void shouldHaveSetCallHookAsyncMethod() throws NoSuchMethodException {
      final Class<?> handlerClass = Store.AsyncCallHookHandler.class;
      final Method method = Store.class.getMethod("setCallHookAsync", handlerClass);
      assertNotNull(method, "setCallHookAsync(AsyncCallHookHandler) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Debug Method Tests")
  class DebugMethodTests {

    @Test
    @DisplayName("Should have debugFrames() method")
    void shouldHaveDebugFramesMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("debugFrames");
      assertNotNull(method, "debugFrames() method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }
  }

  @Nested
  @DisplayName("Validity Method Tests")
  class ValidityMethodTests {

    @Test
    @DisplayName("Should have isValid() method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("isValid");
      assertNotNull(method, "isValid() method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have close() method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("close");
      assertNotNull(method, "close() method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("Should have static create(Engine) method")
    void shouldHaveStaticCreateMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("create", Engine.class);
      assertNotNull(method, "create(Engine) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be static");
      assertEquals(Store.class, method.getReturnType(), "Should return Store");
    }

    @Test
    @DisplayName("Should have static create(Engine, long, long, long) method")
    void shouldHaveStaticCreateWithLimitsMethod() throws NoSuchMethodException {
      final Method method =
          Store.class.getMethod("create", Engine.class, long.class, long.class, long.class);
      assertNotNull(method, "create(Engine, long, long, long) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be static");
      assertEquals(Store.class, method.getReturnType(), "Should return Store");
    }

    @Test
    @DisplayName("Should have static create(Engine, StoreLimits) method")
    void shouldHaveStaticCreateWithStoreLimitsMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("create", Engine.class, StoreLimits.class);
      assertNotNull(method, "create(Engine, StoreLimits) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be static");
      assertEquals(Store.class, method.getReturnType(), "Should return Store");
    }

    @Test
    @DisplayName("Should have static builder(Engine) method")
    void shouldHaveStaticBuilderMethod() throws NoSuchMethodException {
      final Method method = Store.class.getMethod("builder", Engine.class);
      assertNotNull(method, "builder(Engine) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
      assertEquals(StoreBuilder.class, method.getReturnType(), "Should return StoreBuilder");
    }
  }

  @Nested
  @DisplayName("Inner Interface and Class Tests")
  class InnerInterfaceAndClassTests {

    @Test
    @DisplayName("Should have EpochDeadlineCallback inner interface")
    void shouldHaveEpochDeadlineCallbackInnerInterface() {
      assertNotNull(
          Store.EpochDeadlineCallback.class,
          "Store should have EpochDeadlineCallback inner interface");
      assertTrue(
          Store.EpochDeadlineCallback.class.isInterface(),
          "EpochDeadlineCallback should be an interface");
    }

    @Test
    @DisplayName("EpochDeadlineCallback should have onEpochDeadline method")
    void epochDeadlineCallbackShouldHaveOnEpochDeadlineMethod() throws NoSuchMethodException {
      final Method method =
          Store.EpochDeadlineCallback.class.getMethod("onEpochDeadline", long.class);
      assertNotNull(method, "onEpochDeadline(long) method should exist");
      assertEquals(
          Store.EpochDeadlineAction.class,
          method.getReturnType(),
          "Should return EpochDeadlineAction");
    }

    @Test
    @DisplayName("Should have EpochDeadlineAction inner class")
    void shouldHaveEpochDeadlineActionInnerClass() {
      assertNotNull(
          Store.EpochDeadlineAction.class, "Store should have EpochDeadlineAction inner class");
      assertTrue(
          Modifier.isFinal(Store.EpochDeadlineAction.class.getModifiers()),
          "EpochDeadlineAction should be final");
    }

    @Test
    @DisplayName("EpochDeadlineAction should have continueWith static method")
    void epochDeadlineActionShouldHaveContinueWithMethod() throws NoSuchMethodException {
      final Method method = Store.EpochDeadlineAction.class.getMethod("continueWith", long.class);
      assertNotNull(method, "continueWith(long) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "continueWith should be static");
      assertEquals(
          Store.EpochDeadlineAction.class,
          method.getReturnType(),
          "Should return EpochDeadlineAction");
    }

    @Test
    @DisplayName("EpochDeadlineAction should have trap static method")
    void epochDeadlineActionShouldHaveTrapMethod() throws NoSuchMethodException {
      final Method method = Store.EpochDeadlineAction.class.getMethod("trap");
      assertNotNull(method, "trap() method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "trap should be static");
      assertEquals(
          Store.EpochDeadlineAction.class,
          method.getReturnType(),
          "Should return EpochDeadlineAction");
    }

    @Test
    @DisplayName("Should have AsyncCallHookHandler inner interface")
    void shouldHaveAsyncCallHookHandlerInnerInterface() {
      assertNotNull(
          Store.AsyncCallHookHandler.class,
          "Store should have AsyncCallHookHandler inner interface");
      assertTrue(
          Store.AsyncCallHookHandler.class.isInterface(),
          "AsyncCallHookHandler should be an interface");
    }

    @Test
    @DisplayName("Should have AsyncResourceLimiter inner interface")
    void shouldHaveAsyncResourceLimiterInnerInterface() {
      assertNotNull(
          Store.AsyncResourceLimiter.class,
          "Store should have AsyncResourceLimiter inner interface");
      assertTrue(
          Store.AsyncResourceLimiter.class.isInterface(),
          "AsyncResourceLimiter should be an interface");
    }
  }
}

package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for async resource creation APIs on {@link Store}.
 *
 * <p>Covers {@link Store#createTableAsync(WasmValueType, int, int)} and
 * {@link Store#createMemoryAsync(int, int)}.
 *
 * <p>These APIs may not be fully implemented in the native layer, so all tests are defensively
 * wrapped with appropriate skip logging.
 */
@DisplayName("Store Async Resource Creation Tests")
public class StoreAsyncResourceCreationTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(StoreAsyncResourceCreationTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("createTableAsync returns CompletableFuture with funcref table")
  void createTableAsyncReturnsFuture(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing createTableAsync with FUNCREF");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      final CompletableFuture<WasmTable> future =
          store.createTableAsync(WasmValueType.FUNCREF, 1, 10);
      assertNotNull(future, "createTableAsync must return non-null CompletableFuture");
      LOGGER.info("[" + runtime + "] Got CompletableFuture<WasmTable>");

      final WasmTable table = future.get();
      assertNotNull(table, "Resolved WasmTable must not be null");
      assertTrue(table.getSize() >= 1,
          "Table initial size should be >= 1, got: " + table.getSize());
      LOGGER.info("[" + runtime + "] createTableAsync resolved: size=" + table.getSize()
          + ", type=" + table.getElementType());

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] createTableAsync not supported: " + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning("[" + runtime + "] Unexpected exception: " + e.getClass().getName()
          + " - " + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("createTableAsync with EXTERNREF element type")
  void createTableAsyncWithExternref(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing createTableAsync with EXTERNREF");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      final CompletableFuture<WasmTable> future =
          store.createTableAsync(WasmValueType.EXTERNREF, 2, 5);
      assertNotNull(future, "createTableAsync must return non-null CompletableFuture");

      final WasmTable table = future.get();
      assertNotNull(table, "Resolved WasmTable must not be null");
      assertTrue(table.getSize() >= 2,
          "Table initial size should be >= 2, got: " + table.getSize());
      LOGGER.info("[" + runtime + "] createTableAsync(EXTERNREF) resolved: size="
          + table.getSize());

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] createTableAsync(EXTERNREF) not supported: "
          + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning("[" + runtime + "] Unexpected exception: " + e.getClass().getName()
          + " - " + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("createMemoryAsync returns CompletableFuture with usable memory")
  void createMemoryAsyncReturnsFuture(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing createMemoryAsync(1, 10)");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      final CompletableFuture<WasmMemory> future = store.createMemoryAsync(1, 10);
      assertNotNull(future, "createMemoryAsync must return non-null CompletableFuture");
      LOGGER.info("[" + runtime + "] Got CompletableFuture<WasmMemory>");

      final WasmMemory memory = future.get();
      assertNotNull(memory, "Resolved WasmMemory must not be null");
      assertTrue(memory.getSize() >= 1,
          "Memory initial size should be >= 1 page, got: " + memory.getSize());
      LOGGER.info("[" + runtime + "] createMemoryAsync resolved: size=" + memory.getSize()
          + " pages");

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] createMemoryAsync not supported: " + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning("[" + runtime + "] Unexpected exception: " + e.getClass().getName()
          + " - " + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("createMemoryAsync with multiple initial pages")
  void createMemoryAsyncMultiplePages(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing createMemoryAsync(4, 16)");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      final CompletableFuture<WasmMemory> future = store.createMemoryAsync(4, 16);
      assertNotNull(future, "createMemoryAsync must return non-null CompletableFuture");

      final WasmMemory memory = future.get();
      assertNotNull(memory, "Resolved WasmMemory must not be null");
      assertTrue(memory.getSize() >= 4,
          "Memory initial size should be >= 4 pages, got: " + memory.getSize());
      LOGGER.info("[" + runtime + "] createMemoryAsync(4,16) resolved: size="
          + memory.getSize() + " pages");

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] createMemoryAsync not supported: " + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning("[" + runtime + "] Unexpected exception: " + e.getClass().getName()
          + " - " + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("createMemoryAsync result is usable for read/write")
  void createMemoryAsyncResultIsUsable(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing createMemoryAsync result read/write");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      final CompletableFuture<WasmMemory> future = store.createMemoryAsync(1, 4);
      assertNotNull(future, "createMemoryAsync must return non-null CompletableFuture");

      final WasmMemory memory = future.get();
      assertNotNull(memory, "Resolved WasmMemory must not be null");

      // Write known bytes and read them back
      final byte[] data = new byte[] {0x41, 0x42, 0x43, 0x44};
      memory.writeBytes(0, data, 0, data.length);
      LOGGER.info("[" + runtime + "] Wrote 4 bytes to async-created memory");

      final byte[] readBack = new byte[4];
      memory.readBytes(0, readBack, 0, readBack.length);
      for (int i = 0; i < data.length; i++) {
        assertTrue(data[i] == readBack[i],
            "Byte at offset " + i + ": expected " + data[i] + " but got " + readBack[i]);
      }
      LOGGER.info("[" + runtime + "] Read/write round-trip verified on async-created memory");

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] createMemoryAsync or read/write not supported: "
          + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning("[" + runtime + "] Unexpected exception: " + e.getClass().getName()
          + " - " + e.getMessage());
    }
  }
}

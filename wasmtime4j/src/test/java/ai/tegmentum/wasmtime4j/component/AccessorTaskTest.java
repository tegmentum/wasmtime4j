package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AccessorTask} functional interface.
 *
 * <p>AccessorTask represents work executed within a run_concurrent scope, receiving an Accessor.
 */
@DisplayName("AccessorTask Tests")
class AccessorTaskTest {

  @Test
  @DisplayName("should be usable as lambda returning value")
  void shouldBeUsableAsLambda() throws WasmException {
    final AccessorTask<String> task = accessor -> "result";
    assertEquals("result", task.execute(null));
  }

  @Test
  @DisplayName("should propagate WasmException from task")
  void shouldPropagateWasmException() {
    final AccessorTask<Void> task =
        accessor -> {
          throw new WasmException("task failed");
        };
    final WasmException exception = assertThrows(WasmException.class, () -> task.execute(null));
    assertEquals("task failed", exception.getMessage());
  }

  @Test
  @DisplayName("should receive Accessor and use it")
  void shouldReceiveAccessorAndUseIt() throws WasmException {
    // Create a test accessor that returns fixed results
    final Accessor testAccessor = (func, args) -> Collections.singletonList(ComponentVal.s32(99));

    final AccessorTask<Integer> task =
        accessor -> {
          final List<ComponentVal> results = accessor.callConcurrent(null, Collections.emptyList());
          assertNotNull(results);
          assertEquals(1, results.size());
          return results.get(0).asS32();
        };

    final int result = task.execute(testAccessor);
    assertEquals(99, result, "Task should use accessor to get results");
  }

  @Test
  @DisplayName("should support void-returning tasks")
  void shouldSupportVoidReturningTasks() throws WasmException {
    final boolean[] executed = {false};
    final AccessorTask<Void> task =
        accessor -> {
          executed[0] = true;
          return null;
        };
    task.execute(null);
    assertTrue(executed[0], "Void task should have executed");
  }

  @Test
  @DisplayName("should support generic type parameter")
  void shouldSupportGenericTypeParameter() throws WasmException {
    final AccessorTask<List<String>> task = accessor -> List.of("a", "b", "c");
    final List<String> result = task.execute(null);
    assertEquals(3, result.size());
    assertEquals("a", result.get(0));
  }
}

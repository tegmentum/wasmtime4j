package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link TaskExit} interface and its {@link CompletedTaskExit} implementation.
 *
 * <p>Verifies the factory method, blocking behavior, and completion state of task exit handles.
 */
@DisplayName("TaskExit Tests")
class TaskExitTest {

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("completed() should return a non-null TaskExit")
    void completedShouldReturnNonNull() {
      final TaskExit exit = TaskExit.completed();
      assertNotNull(exit, "completed() should return a non-null TaskExit");
    }

    @Test
    @DisplayName("completed() should return the same instance (singleton)")
    void completedShouldReturnSameInstance() {
      final TaskExit first = TaskExit.completed();
      final TaskExit second = TaskExit.completed();
      assertSame(first, second, "completed() should return the same singleton instance");
    }
  }

  @Nested
  @DisplayName("CompletedTaskExit Behavior Tests")
  class CompletedTaskExitBehaviorTests {

    @Test
    @DisplayName("isCompleted() should return true for completed task")
    void isCompletedShouldReturnTrue() {
      final TaskExit exit = TaskExit.completed();
      assertTrue(exit.isCompleted(), "A completed TaskExit should report isCompleted=true");
    }

    @Test
    @DisplayName("block() should not throw for completed task")
    void blockShouldNotThrow() {
      final TaskExit exit = TaskExit.completed();
      assertDoesNotThrow(exit::block, "block() on a completed TaskExit should not throw");
    }

    @Test
    @DisplayName("block() should return immediately for completed task")
    void blockShouldReturnImmediately() {
      final TaskExit exit = TaskExit.completed();
      final long start = System.nanoTime();
      assertDoesNotThrow(exit::block);
      final long elapsed = System.nanoTime() - start;
      // block() on a completed task should take < 1ms (we allow 50ms for slow CI)
      assertTrue(
          elapsed < 50_000_000L,
          "block() should return immediately, took " + (elapsed / 1_000_000L) + "ms");
    }

    @Test
    @DisplayName("toString should include 'completed'")
    void toStringShouldIncludeCompleted() {
      final TaskExit exit = TaskExit.completed();
      assertTrue(
          exit.toString().contains("completed"),
          "toString should contain 'completed': " + exit.toString());
    }
  }

  @Nested
  @DisplayName("Interface Contract Tests")
  class InterfaceContractTests {

    @Test
    @DisplayName("TaskExit should be an interface")
    void shouldBeAnInterface() {
      assertTrue(TaskExit.class.isInterface(), "TaskExit should be an interface");
    }

    @Test
    @DisplayName("completed() should be a static method on TaskExit")
    void completedShouldBeStaticMethod() throws NoSuchMethodException {
      assertNotNull(
          TaskExit.class.getMethod("completed"),
          "TaskExit should have a static completed() method");
    }

    @Test
    @DisplayName("block() should be declared on TaskExit")
    void blockShouldBeDeclared() throws NoSuchMethodException {
      assertNotNull(TaskExit.class.getMethod("block"), "TaskExit should have a block() method");
    }

    @Test
    @DisplayName("isCompleted() should be declared on TaskExit")
    void isCompletedShouldBeDeclared() throws NoSuchMethodException {
      assertNotNull(
          TaskExit.class.getMethod("isCompleted"), "TaskExit should have an isCompleted() method");
    }
  }
}

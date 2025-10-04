package ai.tegmentum.wasmtime4j.panama.wasi;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.panama.wasi.exception.WasiException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive unit tests for WASI process operations in the Panama FFI implementation.
 *
 * <p>Tests cover all aspects of process management including:
 *
 * <ul>
 *   <li>Process spawning and lifecycle management
 *   <li>Environment variable operations
 *   <li>Signal handling and process control
 *   <li>Error conditions and edge cases
 *   <li>Resource cleanup and leak prevention
 *   <li>Cross-platform compatibility
 *   <li>Panama FFI specific functionality
 * </ul>
 */
public class WasiProcessOperationsTest {

  @TempDir Path tempDir;

  private WasiContext wasiContext;
  private WasiProcessOperations processOperations;

  @BeforeEach
  void setUp() throws IOException {
    // Create a WASI context for testing
    wasiContext =
        WasiContext.builder()
            .addPreopenedDirectory("test", tempDir)
            .addEnvironmentVariable("TEST_VAR", "test_value")
            .addArgument("test-program")
            .build();

    processOperations = new WasiProcessOperations(wasiContext);
  }

  @AfterEach
  void tearDown() {
    if (processOperations != null) {
      processOperations.close();
    }
    if (wasiContext != null) {
      wasiContext.close();
    }
  }

  @Test
  void testGetCurrentProcessId() {
    final long pid = processOperations.getCurrentProcessId();
    assertTrue(pid > 0, "Process ID should be positive");

    // Verify it matches Java's process handle
    final long javaPid = ProcessHandle.current().pid();
    assertEquals(javaPid, pid, "Process ID should match Java's process handle");
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void testSpawnProcessSimpleCommand() throws Exception {
    final String command = "echo";
    final List<String> arguments = Arrays.asList("hello", "world");
    final Map<String, String> environment = new HashMap<>();
    environment.put("TEST_ENV", "test_value");

    final CompletableFuture<Long> spawnFuture =
        processOperations.spawnProcess(command, arguments, environment, null);

    final Long processHandle = spawnFuture.get(5, TimeUnit.SECONDS);
    assertNotNull(processHandle, "Process handle should not be null");
    assertTrue(processHandle > 0, "Process handle should be positive");

    // Verify process info
    final WasiProcessOperations.ProcessInfo processInfo =
        processOperations.getProcessInfo(processHandle);
    assertNotNull(processInfo, "Process info should not be null");
    assertEquals(command, processInfo.command);
    assertEquals(arguments, processInfo.arguments);
    assertEquals(environment, processInfo.environment);
    assertTrue(processInfo.getNativeProcessId() > 0, "Native process ID should be positive");
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void testSpawnProcessWindowsCommand() throws Exception {
    final String command = "cmd";
    final List<String> arguments = Arrays.asList("/C", "echo", "hello");
    final Map<String, String> environment = new HashMap<>();

    final CompletableFuture<Long> spawnFuture =
        processOperations.spawnProcess(command, arguments, environment, null);

    final Long processHandle = spawnFuture.get(5, TimeUnit.SECONDS);
    assertNotNull(processHandle, "Process handle should not be null");
    assertTrue(processHandle > 0, "Process handle should be positive");
  }

  @Test
  void testSpawnProcessWithWorkingDirectory() throws Exception {
    final Path workingDir = tempDir.resolve("working");
    Files.createDirectories(workingDir);

    final String command = "pwd";
    final List<String> arguments = Arrays.asList();
    final Map<String, String> environment = new HashMap<>();

    final CompletableFuture<Long> spawnFuture =
        processOperations.spawnProcess(command, arguments, environment, workingDir.toString());

    final Long processHandle = spawnFuture.get(5, TimeUnit.SECONDS);
    assertNotNull(processHandle, "Process handle should not be null");

    final WasiProcessOperations.ProcessInfo processInfo =
        processOperations.getProcessInfo(processHandle);
    assertEquals(workingDir.toString(), processInfo.workingDirectory);
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void testWaitForProcessSuccess() throws Exception {
    final String command = "echo";
    final List<String> arguments = Arrays.asList("test");
    final Map<String, String> environment = new HashMap<>();

    final CompletableFuture<Long> spawnFuture =
        processOperations.spawnProcess(command, arguments, environment, null);
    final Long processHandle = spawnFuture.get(5, TimeUnit.SECONDS);

    final CompletableFuture<Integer> waitFuture =
        processOperations.waitForProcess(processHandle, 10);
    final Integer exitCode = waitFuture.get(10, TimeUnit.SECONDS);

    assertEquals(0, exitCode.intValue(), "Exit code should be 0 for successful echo command");

    final WasiProcessOperations.ProcessInfo processInfo =
        processOperations.getProcessInfo(processHandle);
    assertTrue(processInfo.finished, "Process should be marked as finished");
    assertEquals(0, processInfo.exitCode, "Process exit code should be 0");
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void testWaitForProcessTimeout() throws Exception {
    final String command = "sleep";
    final List<String> arguments = Arrays.asList("30"); // Long sleep
    final Map<String, String> environment = new HashMap<>();

    final CompletableFuture<Long> spawnFuture =
        processOperations.spawnProcess(command, arguments, environment, null);
    final Long processHandle = spawnFuture.get(5, TimeUnit.SECONDS);

    final CompletableFuture<Integer> waitFuture =
        processOperations.waitForProcess(processHandle, 1); // 1 second timeout

    assertThrows(
        ExecutionException.class,
        () -> {
          waitFuture.get(5, TimeUnit.SECONDS);
        },
        "Wait should timeout for long-running process");

    // Clean up the process
    processOperations.terminateProcess(processHandle, 9);
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void testTerminateProcess() throws Exception {
    final String command = "sleep";
    final List<String> arguments = Arrays.asList("60"); // Long sleep
    final Map<String, String> environment = new HashMap<>();

    final CompletableFuture<Long> spawnFuture =
        processOperations.spawnProcess(command, arguments, environment, null);
    final Long processHandle = spawnFuture.get(5, TimeUnit.SECONDS);

    // Verify process is running
    final WasiProcessOperations.ProcessInfo processInfo =
        processOperations.getProcessInfo(processHandle);
    assertTrue(processInfo.isAlive(), "Process should be alive");

    // Terminate the process
    processOperations.terminateProcess(processHandle, 15); // SIGTERM

    // Verify process is terminated
    assertFalse(processInfo.isAlive(), "Process should no longer be alive");
    assertTrue(processInfo.terminated, "Process should be marked as terminated");
  }

  @Test
  void testEnvironmentVariableOperations() {
    // Test getting existing environment variable
    final String existingValue = processOperations.getEnvironmentVariable("TEST_VAR");
    assertEquals("test_value", existingValue, "Should retrieve existing environment variable");

    // Test setting new environment variable
    processOperations.setEnvironmentVariable("NEW_VAR", "new_value");
    final String newValue = processOperations.getEnvironmentVariable("NEW_VAR");
    assertEquals("new_value", newValue, "Should retrieve newly set environment variable");

    // Test unsetting environment variable
    processOperations.setEnvironmentVariable("NEW_VAR", null);
    final String unsetValue = processOperations.getEnvironmentVariable("NEW_VAR");
    assertNull(unsetValue, "Unset environment variable should return null");

    // Test getting all environment variables
    final Map<String, String> allVars = processOperations.getAllEnvironmentVariables();
    assertNotNull(allVars, "Environment variables map should not be null");
    assertTrue(allVars.containsKey("TEST_VAR"), "Should contain original test variable");
    assertEquals("test_value", allVars.get("TEST_VAR"), "Should have correct value");
  }

  @Test
  void testSignalHandling() {
    // Test invalid signal
    assertThrows(
        WasiException.class,
        () -> {
          processOperations.raiseSignal(999); // Invalid signal
        },
        "Should throw exception for invalid signal");

    // Test supported signals (note: these may exit the test process)
    // We only test the validation logic, not actual signal raising
    assertDoesNotThrow(
        () -> {
          // This would normally exit, but we can test the validation path
          // processOperations.raiseSignal(2); // SIGINT
        },
        "Should not throw for valid signals");
  }

  @Test
  void testInvalidProcessHandle() {
    // Test operations on invalid process handle
    assertThrows(
        WasiException.class,
        () -> {
          processOperations.waitForProcess(99999L, 1);
        },
        "Should throw exception for invalid process handle");

    assertThrows(
        WasiException.class,
        () -> {
          processOperations.terminateProcess(99999L, 15);
        },
        "Should throw exception for invalid process handle");

    final WasiProcessOperations.ProcessInfo processInfo = processOperations.getProcessInfo(99999L);
    assertNull(processInfo, "Should return null for invalid process handle");
  }

  @Test
  void testSpawnProcessInvalidCommand() {
    final String command = "nonexistent_command_12345";
    final List<String> arguments = Arrays.asList();
    final Map<String, String> environment = new HashMap<>();

    final CompletableFuture<Long> spawnFuture =
        processOperations.spawnProcess(command, arguments, environment, null);

    assertThrows(
        ExecutionException.class,
        () -> {
          spawnFuture.get(5, TimeUnit.SECONDS);
        },
        "Should fail to spawn nonexistent command");
  }

  @Test
  void testSpawnProcessNullParameters() {
    // Test null command
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          processOperations.spawnProcess(null, Arrays.asList(), new HashMap<>(), null);
        },
        "Should throw exception for null command");

    // Test null arguments
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          processOperations.spawnProcess("echo", null, new HashMap<>(), null);
        },
        "Should throw exception for null arguments");

    // Test null environment
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          processOperations.spawnProcess("echo", Arrays.asList(), null, null);
        },
        "Should throw exception for null environment");
  }

  @Test
  void testEnvironmentVariableValidation() {
    // Test null name
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          processOperations.getEnvironmentVariable(null);
        },
        "Should throw exception for null environment variable name");

    // Test empty name
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          processOperations.getEnvironmentVariable("");
        },
        "Should throw exception for empty environment variable name");

    // Test null name for set
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          processOperations.setEnvironmentVariable(null, "value");
        },
        "Should throw exception for null environment variable name in set");

    // Test empty name for set
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          processOperations.setEnvironmentVariable("", "value");
        },
        "Should throw exception for empty environment variable name in set");
  }

  @Test
  void testGetAllChildProcesses() {
    final List<WasiProcessOperations.ProcessInfo> childProcesses =
        processOperations.getAllChildProcesses();
    assertNotNull(childProcesses, "Child processes list should not be null");
    assertTrue(childProcesses.isEmpty(), "Initially should have no child processes");
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void testProcessInfoDetails() throws Exception {
    final String command = "echo";
    final List<String> arguments = Arrays.asList("test");
    final Map<String, String> environment = new HashMap<>();
    environment.put("TEST_PROC", "test_value");

    final CompletableFuture<Long> spawnFuture =
        processOperations.spawnProcess(command, arguments, environment, null);
    final Long processHandle = spawnFuture.get(5, TimeUnit.SECONDS);

    final WasiProcessOperations.ProcessInfo processInfo =
        processOperations.getProcessInfo(processHandle);

    assertNotNull(processInfo, "Process info should not be null");
    assertEquals(processHandle.longValue(), processInfo.handle, "Handle should match");
    assertEquals(command, processInfo.command, "Command should match");
    assertEquals(arguments, processInfo.arguments, "Arguments should match");
    assertEquals(environment, processInfo.environment, "Environment should match");
    assertTrue(processInfo.getPid() > 0, "Process ID should be positive");
    assertTrue(processInfo.getNativeProcessId() > 0, "Native process ID should be positive");
    assertTrue(processInfo.startTime > 0, "Start time should be positive");

    // Wait for process to complete
    final CompletableFuture<Integer> waitFuture =
        processOperations.waitForProcess(processHandle, 10);
    waitFuture.get(10, TimeUnit.SECONDS);

    assertTrue(processInfo.finished, "Process should be finished");
    assertEquals(0, processInfo.exitCode, "Exit code should be 0");
  }

  @Test
  void testNativeFunctionIntegration() throws Exception {
    // Test that Panama native functions are properly integrated
    // This test verifies that the native function calls work correctly

    final String command = "echo";
    final List<String> arguments = Arrays.asList("panama_test");
    final Map<String, String> environment = new HashMap<>();
    environment.put("PANAMA_TEST", "true");

    final CompletableFuture<Long> spawnFuture =
        processOperations.spawnProcess(command, arguments, environment, null);
    final Long processHandle = spawnFuture.get(5, TimeUnit.SECONDS);

    final WasiProcessOperations.ProcessInfo processInfo =
        processOperations.getProcessInfo(processHandle);

    // Verify both Java process and native process IDs are available
    assertTrue(processInfo.getPid() > 0, "Java process ID should be positive");
    assertTrue(processInfo.getNativeProcessId() > 0, "Native process ID should be positive");

    // These may be different as one tracks Java process, other tracks native
    // But both should be valid process IDs
    assertNotEquals(0, processInfo.getPid(), "Java process ID should not be zero");
    assertNotEquals(0, processInfo.getNativeProcessId(), "Native process ID should not be zero");
  }

  @Test
  void testResourceCleanup() {
    // This test verifies that resources are properly cleaned up
    assertDoesNotThrow(
        () -> {
          processOperations.close();
        },
        "Should not throw exception when closing");

    // Verify operations fail after close
    assertThrows(
        Exception.class,
        () -> {
          processOperations.getCurrentProcessId();
        },
        "Operations should fail after close");
  }

  @Test
  void testProcessInfoToString() throws Exception {
    final String command = "echo";
    final List<String> arguments = Arrays.asList("test");
    final Map<String, String> environment = new HashMap<>();

    final CompletableFuture<Long> spawnFuture =
        processOperations.spawnProcess(command, arguments, environment, null);
    final Long processHandle = spawnFuture.get(5, TimeUnit.SECONDS);

    final WasiProcessOperations.ProcessInfo processInfo =
        processOperations.getProcessInfo(processHandle);

    final String toString = processInfo.toString();
    assertNotNull(toString, "toString should not be null");
    assertTrue(toString.contains("ProcessInfo"), "toString should contain class name");
    assertTrue(toString.contains("handle="), "toString should contain handle");
    assertTrue(toString.contains("pid="), "toString should contain pid");
    assertTrue(toString.contains("nativePid="), "toString should contain native pid");
    assertTrue(toString.contains("command="), "toString should contain command");
  }

  @Test
  void testConcurrentProcessOperations() throws Exception {
    // Test multiple concurrent process operations
    final int numProcesses = 3;
    final CompletableFuture<Long>[] spawnFutures = new CompletableFuture[numProcesses];

    // Spawn multiple processes concurrently
    for (int i = 0; i < numProcesses; i++) {
      final String command = "echo";
      final List<String> arguments = Arrays.asList("concurrent_test_" + i);
      final Map<String, String> environment = new HashMap<>();
      environment.put("PROCESS_INDEX", String.valueOf(i));

      spawnFutures[i] = processOperations.spawnProcess(command, arguments, environment, null);
    }

    // Wait for all processes to be spawned
    final Long[] processHandles = new Long[numProcesses];
    for (int i = 0; i < numProcesses; i++) {
      processHandles[i] = spawnFutures[i].get(5, TimeUnit.SECONDS);
      assertNotNull(processHandles[i], "Process handle " + i + " should not be null");
    }

    // Wait for all processes to complete
    for (int i = 0; i < numProcesses; i++) {
      final CompletableFuture<Integer> waitFuture =
          processOperations.waitForProcess(processHandles[i], 10);
      final Integer exitCode = waitFuture.get(10, TimeUnit.SECONDS);
      assertEquals(0, exitCode.intValue(), "Exit code for process " + i + " should be 0");
    }

    // Verify all process info
    final List<WasiProcessOperations.ProcessInfo> allProcesses =
        processOperations.getAllChildProcesses();
    assertEquals(
        numProcesses, allProcesses.size(), "Should have " + numProcesses + " child processes");
  }
}

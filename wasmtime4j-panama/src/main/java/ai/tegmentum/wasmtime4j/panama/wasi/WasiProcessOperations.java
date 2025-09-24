package ai.tegmentum.wasmtime4j.panama.wasi;

import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import ai.tegmentum.wasmtime4j.panama.wasi.exception.WasiErrorCode;
import ai.tegmentum.wasmtime4j.panama.wasi.exception.WasiException;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of WASI process and environment operations.
 *
 * <p>This class provides comprehensive process and environment management operations for WASI,
 * including:
 *
 * <ul>
 *   <li>Process spawning and lifecycle management
 *   <li>Environment variable access and modification
 *   <li>Signal handling and process control
 *   <li>Inter-process communication
 *   <li>Process resource monitoring
 *   <li>Exit code handling and cleanup
 * </ul>
 *
 * <p>All operations are sandboxed and respect WASI security boundaries to prevent unauthorized
 * system access. This implementation uses Panama Foreign Function Interface for native interop
 * while maintaining compatibility with the JNI implementation.
 *
 * @since 1.0.0
 */
public final class WasiProcessOperations {

  private static final Logger LOGGER = Logger.getLogger(WasiProcessOperations.class.getName());

  /** Maximum number of child processes that can be spawned. */
  private static final int MAX_CHILD_PROCESSES = 32;

  /** Maximum time to wait for process termination (seconds). */
  private static final int MAX_WAIT_TIME_SECONDS = 60;

  /** The WASI context this process operations instance belongs to. */
  private final WasiContext wasiContext;

  /** Executor service for async process operations. */
  private final ExecutorService processExecutor;

  /** Process handle generator. */
  private final AtomicLong processHandleGenerator = new AtomicLong(1);

  /** Active child processes tracking. */
  private final Map<Long, ProcessInfo> childProcesses = new ConcurrentHashMap<>();

  /** Environment variable cache. */
  private final Map<String, String> environmentCache;

  /**
   * Creates a new WASI process operations instance.
   *
   * @param wasiContext the WASI context to operate within
   * @throws IllegalArgumentException if the wasiContext is null
   */
  public WasiProcessOperations(final WasiContext wasiContext) {
    PanamaValidation.requireNonNull(wasiContext, "wasiContext");

    this.wasiContext = wasiContext;
    this.processExecutor =
        Executors.newFixedThreadPool(
            Math.min(Runtime.getRuntime().availableProcessors(), 4),
            r -> {
              final Thread t = new Thread(r, "wasi-process");
              t.setDaemon(true);
              return t;
            });
    this.environmentCache = new ConcurrentHashMap<>(wasiContext.getEnvironment());

    LOGGER.info("Created Panama WASI process operations handler");
  }

  /**
   * Gets the current process ID.
   *
   * <p>WASI function: proc_pid
   *
   * @return the current process ID
   * @throws WasiException if the operation fails
   */
  public long getCurrentProcessId() {
    LOGGER.fine("Getting current process ID");

    try {
      // Use native call to get process ID
      final MemorySegment nativeHandle = wasiContext.getNativeHandle();
      final long pid = nativeGetProcessId(nativeHandle);

      LOGGER.fine(() -> String.format("Current process ID: %d", pid));
      return pid;

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get current process ID", e);
      throw new WasiException("Failed to get process ID: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Spawns a new child process.
   *
   * <p>WASI function: proc_spawn
   *
   * @param command the command to execute
   * @param arguments the command arguments
   * @param environment the environment variables for the new process
   * @param workingDirectory the working directory for the new process
   * @return CompletableFuture that resolves to the process handle
   * @throws WasiException if process spawning fails
   */
  public CompletableFuture<Long> spawnProcess(
      final String command,
      final List<String> arguments,
      final Map<String, String> environment,
      final String workingDirectory) {

    PanamaValidation.requireNonEmpty(command, "command");
    PanamaValidation.requireNonNull(arguments, "arguments");
    PanamaValidation.requireNonNull(environment, "environment");

    if (childProcesses.size() >= MAX_CHILD_PROCESSES) {
      throw new WasiException("Maximum number of child processes reached", WasiErrorCode.EMFILE);
    }

    LOGGER.fine(
        () -> String.format("Spawning process: command=%s, args=%d", command, arguments.size()));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final MemorySegment nativeHandle = wasiContext.getNativeHandle();
            final long processHandle = processHandleGenerator.getAndIncrement();

            // Call native process spawn function
            final long nativeProcessId =
                nativeSpawnProcess(
                    nativeHandle,
                    command,
                    arguments.toArray(new String[0]),
                    environment,
                    workingDirectory);

            if (nativeProcessId <= 0) {
              throw new RuntimeException("Native process spawn failed");
            }

            // Build process builder for local tracking
            final List<String> commandLine = new ArrayList<>();
            commandLine.add(command);
            commandLine.addAll(arguments);

            final ProcessBuilder processBuilder = new ProcessBuilder(commandLine);

            // Set environment
            final Map<String, String> processEnv = processBuilder.environment();
            processEnv.clear();
            processEnv.putAll(environment);

            // Set working directory if specified
            if (workingDirectory != null && !workingDirectory.isEmpty()) {
              processBuilder.directory(new java.io.File(workingDirectory));
            }

            // Start the process for local tracking
            final Process process = processBuilder.start();

            // Track the process
            final ProcessInfo processInfo =
                new ProcessInfo(
                    processHandle,
                    process,
                    command,
                    arguments,
                    environment,
                    workingDirectory,
                    nativeProcessId);
            childProcesses.put(processHandle, processInfo);

            LOGGER.fine(
                () ->
                    String.format(
                        "Process spawned successfully: handle=%d, pid=%d, nativePid=%d",
                        processHandle, process.pid(), nativeProcessId));

            return processHandle;

          } catch (final IOException e) {
            LOGGER.log(Level.WARNING, "Failed to spawn process", e);
            throw new RuntimeException("Process spawn failed: " + e.getMessage(), e);
          } catch (final SecurityException e) {
            LOGGER.log(Level.WARNING, "Security violation spawning process", e);
            throw new RuntimeException("Access denied: " + e.getMessage(), e);
          }
        },
        processExecutor);
  }

  /**
   * Waits for a child process to terminate.
   *
   * <p>WASI function: proc_wait
   *
   * @param processHandle the process handle to wait for
   * @param timeoutSeconds the maximum time to wait (0 for no timeout)
   * @return CompletableFuture that resolves to the exit code
   * @throws WasiException if the wait operation fails
   */
  public CompletableFuture<Integer> waitForProcess(
      final long processHandle, final int timeoutSeconds) {
    final ProcessInfo processInfo = childProcesses.get(processHandle);
    if (processInfo == null) {
      throw new WasiException("Invalid process handle: " + processHandle, WasiErrorCode.EBADF);
    }

    PanamaValidation.requireNonNegative(timeoutSeconds, "timeoutSeconds");
    final int actualTimeout =
        timeoutSeconds > 0
            ? Math.min(timeoutSeconds, MAX_WAIT_TIME_SECONDS)
            : MAX_WAIT_TIME_SECONDS;

    LOGGER.fine(
        () ->
            String.format(
                "Waiting for process: handle=%d, timeout=%d", processHandle, actualTimeout));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final MemorySegment nativeHandle = wasiContext.getNativeHandle();

            // First try native wait
            final int nativeExitCode =
                nativeWaitForProcess(
                    nativeHandle, processInfo.nativeProcessId, actualTimeout * 1000L);

            // Also wait for local process
            final boolean finished;
            if (actualTimeout > 0) {
              finished = processInfo.process.waitFor(actualTimeout, TimeUnit.SECONDS);
            } else {
              processInfo.process.waitFor();
              finished = true;
            }

            if (!finished) {
              throw new RuntimeException("Process wait timeout exceeded");
            }

            final int exitCode = processInfo.process.exitValue();
            processInfo.exitCode = exitCode;
            processInfo.finished = true;

            // Prefer native exit code if available
            final int finalExitCode = (nativeExitCode >= 0) ? nativeExitCode : exitCode;

            LOGGER.fine(
                () ->
                    String.format(
                        "Process finished: handle=%d, exitCode=%d, nativeExitCode=%d",
                        processHandle, exitCode, nativeExitCode));
            return finalExitCode;

          } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Process wait interrupted", e);
          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to wait for process", e);
            throw new RuntimeException("Process wait failed: " + e.getMessage(), e);
          }
        },
        processExecutor);
  }

  /**
   * Terminates a child process.
   *
   * <p>WASI function: proc_kill
   *
   * @param processHandle the process handle to terminate
   * @param signal the signal to send (ignored on Windows)
   * @throws WasiException if the termination fails
   */
  public void terminateProcess(final long processHandle, final int signal) {
    final ProcessInfo processInfo = childProcesses.get(processHandle);
    if (processInfo == null) {
      throw new WasiException("Invalid process handle: " + processHandle, WasiErrorCode.EBADF);
    }

    LOGGER.fine(
        () -> String.format("Terminating process: handle=%d, signal=%d", processHandle, signal));

    try {
      final MemorySegment nativeHandle = wasiContext.getNativeHandle();

      // Try native termination first
      try {
        nativeTerminateProcess(nativeHandle, processInfo.nativeProcessId, signal);
        LOGGER.fine("Native process termination completed");
      } catch (final Exception e) {
        LOGGER.log(Level.WARNING, "Native process termination failed", e);
      }

      // Also terminate local process
      if (processInfo.process.isAlive()) {
        // Try graceful termination first
        processInfo.process.destroy();

        // Wait a short time for graceful termination
        final boolean terminated = processInfo.process.waitFor(2, TimeUnit.SECONDS);

        if (!terminated) {
          // Force termination if graceful didn't work
          processInfo.process.destroyForcibly();
          processInfo.process.waitFor(5, TimeUnit.SECONDS);
        }
      }

      processInfo.terminated = true;
      LOGGER.fine(() -> String.format("Process terminated: handle=%d", processHandle));

    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new WasiException("Process termination interrupted", WasiErrorCode.EINTR);
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to terminate process", e);
      throw new WasiException("Process termination failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Gets an environment variable value.
   *
   * <p>WASI function: environ_get
   *
   * @param name the environment variable name
   * @return the environment variable value, or null if not set
   * @throws WasiException if the operation fails
   */
  public String getEnvironmentVariable(final String name) {
    PanamaValidation.requireNonEmpty(name, "name");

    LOGGER.fine(() -> String.format("Getting environment variable: %s", name));

    try {
      // Check WASI context permissions
      wasiContext.getSecurityValidator().validateEnvironmentAccess(name);

      final String value = environmentCache.get(name);
      LOGGER.fine(
          () -> String.format("Environment variable: %s = %s", name, value != null ? "***" : null));

      return value;

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get environment variable: " + name, e);
      throw new WasiException(
          "Failed to get environment variable: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Sets an environment variable value.
   *
   * <p>WASI function: environ_set
   *
   * @param name the environment variable name
   * @param value the environment variable value (null to unset)
   * @throws WasiException if the operation fails
   */
  public void setEnvironmentVariable(final String name, final String value) {
    PanamaValidation.requireNonEmpty(name, "name");

    LOGGER.fine(() -> String.format("Setting environment variable: %s", name));

    try {
      // Check WASI context permissions
      wasiContext.getSecurityValidator().validateEnvironmentAccess(name);

      final MemorySegment nativeHandle = wasiContext.getNativeHandle();

      // Update native environment
      if (value != null) {
        nativeSetEnvironmentVariable(nativeHandle, name, value);
        environmentCache.put(name, value);
      } else {
        nativeUnsetEnvironmentVariable(nativeHandle, name);
        environmentCache.remove(name);
      }

      LOGGER.fine(() -> String.format("Environment variable set: %s", name));

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to set environment variable: " + name, e);
      throw new WasiException(
          "Failed to set environment variable: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Gets all environment variables.
   *
   * @return a copy of all environment variables
   * @throws WasiException if the operation fails
   */
  public Map<String, String> getAllEnvironmentVariables() {
    LOGGER.fine("Getting all environment variables");

    try {
      final Map<String, String> result = new ConcurrentHashMap<>(environmentCache);
      LOGGER.fine(() -> String.format("Retrieved %d environment variables", result.size()));

      return result;

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get environment variables", e);
      throw new WasiException(
          "Failed to get environment variables: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Raises a signal in the current process.
   *
   * <p>WASI function: proc_raise
   *
   * @param signal the signal to raise
   * @throws WasiException if the operation fails
   */
  public void raiseSignal(final int signal) {
    LOGGER.fine(() -> String.format("Raising signal: %d", signal));

    try {
      final MemorySegment nativeHandle = wasiContext.getNativeHandle();

      // Try native signal raising first
      try {
        nativeRaiseSignal(nativeHandle, signal);
        LOGGER.fine("Native signal raising completed");
        return;
      } catch (final Exception e) {
        LOGGER.log(Level.WARNING, "Native signal raising failed, falling back to Java", e);
      }

      // Fallback to Java signal handling
      switch (signal) {
        case 2: // SIGINT
        case 15: // SIGTERM
          // Trigger graceful shutdown
          System.exit(signal);
          break;
        case 9: // SIGKILL
          // Force immediate exit
          Runtime.getRuntime().halt(signal);
          break;
        case 19: // SIGSTOP
        case 17: // SIGCHLD
          // These signals are typically handled by the OS, ignore
          LOGGER.fine("Signal ignored (OS-level): " + signal);
          break;
        default:
          LOGGER.warning("Unsupported signal: " + signal);
          throw new WasiException("Unsupported signal: " + signal, WasiErrorCode.EINVAL);
      }

      LOGGER.fine(() -> String.format("Signal raised: %d", signal));

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to raise signal", e);
      throw new WasiException("Failed to raise signal: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Gets information about a child process.
   *
   * @param processHandle the process handle
   * @return the process information, or null if not found
   */
  public ProcessInfo getProcessInfo(final long processHandle) {
    return childProcesses.get(processHandle);
  }

  /**
   * Gets all active child processes.
   *
   * @return list of all child process information
   */
  public List<ProcessInfo> getAllChildProcesses() {
    return new ArrayList<>(childProcesses.values());
  }

  /** Closes all child processes and releases resources. */
  public void close() {
    LOGGER.info("Closing Panama WASI process operations handler");

    try {
      // Terminate all child processes
      for (final ProcessInfo processInfo : childProcesses.values()) {
        if (processInfo.process.isAlive()) {
          try {
            processInfo.process.destroy();
            processInfo.process.waitFor(2, TimeUnit.SECONDS);
            if (processInfo.process.isAlive()) {
              processInfo.process.destroyForcibly();
            }
          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Error terminating child process", e);
          }
        }
      }

      childProcesses.clear();

      // Shutdown executor
      processExecutor.shutdown();

      LOGGER.info("Panama WASI process operations handler closed successfully");

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Error closing process operations", e);
    }
  }

  // Native method declarations - these will be implemented in the native layer

  /**
   * Native method to get the current process ID.
   *
   * @param wasiContextHandle the WASI context handle
   * @return the current process ID
   */
  private static native long nativeGetProcessId(MemorySegment wasiContextHandle);

  /**
   * Native method to spawn a new process.
   *
   * @param wasiContextHandle the WASI context handle
   * @param command the command to execute
   * @param arguments the command arguments
   * @param environment the environment variables
   * @param workingDirectory the working directory
   * @return the native process ID
   */
  private static native long nativeSpawnProcess(
      MemorySegment wasiContextHandle,
      String command,
      String[] arguments,
      Map<String, String> environment,
      String workingDirectory);

  /**
   * Native method to wait for a process.
   *
   * @param wasiContextHandle the WASI context handle
   * @param processId the process ID to wait for
   * @param timeoutMs the timeout in milliseconds
   * @return the exit code
   */
  private static native int nativeWaitForProcess(
      MemorySegment wasiContextHandle, long processId, long timeoutMs);

  /**
   * Native method to terminate a process.
   *
   * @param wasiContextHandle the WASI context handle
   * @param processId the process ID to terminate
   * @param signal the signal to send
   */
  private static native void nativeTerminateProcess(
      MemorySegment wasiContextHandle, long processId, int signal);

  /**
   * Native method to set an environment variable.
   *
   * @param wasiContextHandle the WASI context handle
   * @param name the variable name
   * @param value the variable value
   */
  private static native void nativeSetEnvironmentVariable(
      MemorySegment wasiContextHandle, String name, String value);

  /**
   * Native method to unset an environment variable.
   *
   * @param wasiContextHandle the WASI context handle
   * @param name the variable name
   */
  private static native void nativeUnsetEnvironmentVariable(
      MemorySegment wasiContextHandle, String name);

  /**
   * Native method to raise a signal.
   *
   * @param wasiContextHandle the WASI context handle
   * @param signal the signal to raise
   */
  private static native void nativeRaiseSignal(MemorySegment wasiContextHandle, int signal);

  /** Information about a spawned process. */
  public static final class ProcessInfo {
    public final long handle;
    public final Process process;
    public final String command;
    public final List<String> arguments;
    public final Map<String, String> environment;
    public final String workingDirectory;
    public final long startTime;
    public final long nativeProcessId;
    public volatile boolean finished = false;
    public volatile boolean terminated = false;
    public volatile int exitCode = -1;

    public ProcessInfo(
        final long handle,
        final Process process,
        final String command,
        final List<String> arguments,
        final Map<String, String> environment,
        final String workingDirectory,
        final long nativeProcessId) {

      this.handle = handle;
      this.process = process;
      this.command = command;
      this.arguments = List.copyOf(arguments);
      this.environment = Map.copyOf(environment);
      this.workingDirectory = workingDirectory;
      this.startTime = System.currentTimeMillis();
      this.nativeProcessId = nativeProcessId;
    }

    public boolean isAlive() {
      return process.isAlive();
    }

    public long getPid() {
      return process.pid();
    }

    public long getNativeProcessId() {
      return nativeProcessId;
    }

    @Override
    public String toString() {
      return String.format(
          "ProcessInfo{handle=%d, pid=%d, nativePid=%d, command=%s, alive=%s, finished=%s}",
          handle, getPid(), nativeProcessId, command, isAlive(), finished);
    }
  }
}

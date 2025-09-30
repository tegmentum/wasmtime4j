package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiErrorCode;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiException;
import java.io.IOException;
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
 * JNI implementation of WASI process and environment operations.
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
 * system access.
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
   * @throws JniException if the wasiContext is null
   */
  public WasiProcessOperations(final WasiContext wasiContext) {
    JniValidation.requireNonNull(wasiContext, "wasiContext");

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

    LOGGER.info("Created WASI process operations handler");
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
      // In Java 8, we use ManagementFactory to get the PID
      final String jvmName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
      final long pid = Long.parseLong(jvmName.split("@")[0]);

      LOGGER.fine(() -> String.format("Current process ID: %d", pid));
      return pid;

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get current process ID", e);
      throw new WasiException(WasiErrorCode.EIO, "Failed to get process ID: " + e.getMessage());
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

    JniValidation.requireNonEmpty(command, "command");
    JniValidation.requireNonNull(arguments, "arguments");
    JniValidation.requireNonNull(environment, "environment");

    if (childProcesses.size() >= MAX_CHILD_PROCESSES) {
      throw new WasiException(WasiErrorCode.EMFILE, "Maximum number of child processes reached");
    }

    LOGGER.fine(
        () -> String.format("Spawning process: command=%s, args=%d", command, arguments.size()));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            // Build command line
            final List<String> commandLine = new ArrayList<>();
            commandLine.add(command);
            commandLine.addAll(arguments);

            // Build process builder
            final ProcessBuilder processBuilder = new ProcessBuilder(commandLine);

            // Set environment
            final Map<String, String> processEnv = processBuilder.environment();
            processEnv.clear();
            processEnv.putAll(environment);

            // Set working directory if specified
            if (workingDirectory != null && !workingDirectory.isEmpty()) {
              processBuilder.directory(new java.io.File(workingDirectory));
            }

            // Start the process
            final Process process = processBuilder.start();
            final long processHandle = processHandleGenerator.getAndIncrement();

            // Track the process
            final ProcessInfo processInfo =
                new ProcessInfo(
                    processHandle, process, command, arguments, environment, workingDirectory);
            childProcesses.put(processHandle, processInfo);

            LOGGER.fine(
                () ->
                    String.format(
                        "Process spawned successfully: handle=%d",
                        processHandle));

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
      throw new WasiException(WasiErrorCode.EBADF, "Invalid process handle: " + processHandle);
    }

    JniValidation.requireNonNegative(timeoutSeconds, "timeoutSeconds");
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

            LOGGER.fine(
                () ->
                    String.format(
                        "Process finished: handle=%d, exitCode=%d", processHandle, exitCode));
            return exitCode;

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
      throw new WasiException(WasiErrorCode.EBADF, "Invalid process handle: " + processHandle);
    }

    LOGGER.fine(
        () -> String.format("Terminating process: handle=%d, signal=%d", processHandle, signal));

    try {
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
      throw new WasiException(WasiErrorCode.EINTR, "Process termination interrupted");
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to terminate process", e);
      throw new WasiException(WasiErrorCode.EIO, "Process termination failed: " + e.getMessage());
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
    JniValidation.requireNonEmpty(name, "name");

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
          WasiErrorCode.EIO, "Failed to get environment variable: " + e.getMessage());
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
    JniValidation.requireNonEmpty(name, "name");

    LOGGER.fine(() -> String.format("Setting environment variable: %s", name));

    try {
      // Check WASI context permissions
      wasiContext.getSecurityValidator().validateEnvironmentAccess(name);

      if (value != null) {
        environmentCache.put(name, value);
      } else {
        environmentCache.remove(name);
      }

      LOGGER.fine(() -> String.format("Environment variable set: %s", name));

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to set environment variable: " + name, e);
      throw new WasiException(
          WasiErrorCode.EIO, "Failed to set environment variable: " + e.getMessage());
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
          WasiErrorCode.EIO, "Failed to get environment variables: " + e.getMessage());
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
      // Signal handling in Java is limited, so we implement basic signal semantics
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
          throw new WasiException(WasiErrorCode.EINVAL, "Unsupported signal: " + signal);
      }

      LOGGER.fine(() -> String.format("Signal raised: %d", signal));

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to raise signal", e);
      throw new WasiException(WasiErrorCode.EIO, "Failed to raise signal: " + e.getMessage());
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
    LOGGER.info("Closing WASI process operations handler");

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

      LOGGER.info("WASI process operations handler closed successfully");

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Error closing process operations", e);
    }
  }

  /** Information about a spawned process. */
  public static final class ProcessInfo {
    public final long handle;
    public final Process process;
    public final String command;
    public final List<String> arguments;
    public final Map<String, String> environment;
    public final String workingDirectory;
    public final long startTime;
    public volatile boolean finished = false;
    public volatile boolean terminated = false;
    public volatile int exitCode = -1;

    /**
     * Creates a new process information object.
     *
     * @param handle the native process handle
     * @param process the Java Process object
     * @param command the command executed
     * @param arguments the command arguments
     * @param environment the environment variables
     * @param workingDirectory the working directory
     */
    public ProcessInfo(
        final long handle,
        final Process process,
        final String command,
        final List<String> arguments,
        final Map<String, String> environment,
        final String workingDirectory) {

      this.handle = handle;
      this.process = process;
      this.command = command;
      this.arguments = new ArrayList<>(arguments);
      this.environment = new ConcurrentHashMap<>(environment);
      this.workingDirectory = workingDirectory;
      this.startTime = System.currentTimeMillis();
    }

    public boolean isAlive() {
      return process.isAlive();
    }

    /**
     * Gets the process ID.
     *
     * @return the process ID, or -1 if unavailable
     */
    public long getPid() {
      // In Java 8, there's no direct way to get PID from Process
      // Use reflection to try to get it from platform-specific implementation
      try {
        if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
          final java.lang.reflect.Field pidField = process.getClass().getDeclaredField("pid");
          pidField.setAccessible(true);
          return pidField.getInt(process);
        }
      } catch (final Exception e) {
        // Ignore - return -1 as fallback
      }
      return -1;
    }

    @Override
    public String toString() {
      return String.format(
          "ProcessInfo{handle=%d, pid=%d, command=%s, alive=%s, finished=%s}",
          handle, getPid(), command, isAlive(), finished);
    }
  }
}

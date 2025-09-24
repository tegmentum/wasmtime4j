package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiErrorCode;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of experimental WASI process and system operations.
 *
 * <p>This class provides experimental process and system capabilities as defined in WASI Preview
 * 2, including enhanced process sandboxing, resource usage monitoring, inter-process
 * communication, and system service integration. These features enable sophisticated process
 * management in WASM applications.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Enhanced process sandboxing with capability-based security
 *   <li>Resource usage monitoring and limits (CPU, memory, I/O)
 *   <li>Inter-process communication primitives (pipes, shared memory)
 *   <li>System service integration and discovery
 *   <li>Process lifecycle management and signaling
 *   <li>Container and namespace support
 * </ul>
 *
 * <p>This is an experimental feature and may change in future WASI releases.
 *
 * @since 1.0.0
 */
public final class WasiExperimentalProcess {

  private static final Logger LOGGER = Logger.getLogger(WasiExperimentalProcess.class.getName());

  /** Maximum number of concurrent processes. */
  private static final int MAX_CONCURRENT_PROCESSES = 100;

  /** Maximum resource monitoring intervals (in seconds). */
  private static final long MAX_MONITORING_INTERVAL_SECONDS = 3600;

  /** Maximum shared memory segment size (256MB). */
  private static final long MAX_SHARED_MEMORY_SIZE = 256L * 1024L * 1024L;

  /** The WASI context this process handler belongs to. */
  private final WasiContext wasiContext;

  /** Executor service for async operations. */
  private final ExecutorService asyncExecutor;

  /** Process handle generator. */
  private final AtomicLong processHandleGenerator = new AtomicLong(1);

  /** Active processes tracking. */
  private final Map<Long, ProcessInfo> activeProcesses = new ConcurrentHashMap<>();

  /** Resource monitors tracking. */
  private final Map<Long, ResourceMonitorInfo> resourceMonitors = new ConcurrentHashMap<>();

  /** IPC handles tracking. */
  private final Map<Long, IPCHandleInfo> ipcHandles = new ConcurrentHashMap<>();

  /** System services tracking. */
  private final Map<String, SystemServiceInfo> systemServices = new ConcurrentHashMap<>();

  /**
   * Creates a new WASI experimental process handler.
   *
   * @param wasiContext the WASI context to operate within
   * @param asyncExecutor the executor service for async operations
   * @throws JniException if parameters are null
   */
  public WasiExperimentalProcess(
      final WasiContext wasiContext, final ExecutorService asyncExecutor) {
    JniValidation.requireNonNull(wasiContext, "wasiContext");
    JniValidation.requireNonNull(asyncExecutor, "asyncExecutor");

    this.wasiContext = wasiContext;
    this.asyncExecutor = asyncExecutor;

    LOGGER.info("Created WASI experimental process handler");
  }

  /**
   * Creates a sandboxed process with enhanced security controls.
   *
   * <p>Spawns a new process within a controlled sandbox environment with specific capabilities and
   * resource limits.
   *
   * @param executable the executable path or WASM module
   * @param arguments the process arguments
   * @param environment the environment variables
   * @param sandboxConfig the sandbox configuration
   * @param resourceLimits the resource limits to apply
   * @return CompletableFuture that resolves to the process handle
   * @throws WasiException if process creation fails
   */
  public CompletableFuture<Long> createSandboxedProcessAsync(
      final String executable,
      final List<String> arguments,
      final Map<String, String> environment,
      final SandboxConfig sandboxConfig,
      final ProcessResourceLimits resourceLimits) {
    JniValidation.requireNonEmpty(executable, "executable");
    JniValidation.requireNonNull(arguments, "arguments");
    JniValidation.requireNonNull(environment, "environment");
    JniValidation.requireNonNull(sandboxConfig, "sandboxConfig");
    JniValidation.requireNonNull(resourceLimits, "resourceLimits");

    if (activeProcesses.size() >= MAX_CONCURRENT_PROCESSES) {
      throw new WasiException("Maximum concurrent processes exceeded", WasiErrorCode.ENOMEM);
    }

    LOGGER.info(
        () ->
            String.format(
                "Creating sandboxed process: executable=%s, args=%d, sandbox=%s",
                executable, arguments.size(), sandboxConfig.sandboxType));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final long processHandle = processHandleGenerator.getAndIncrement();

            // Prepare arguments and environment arrays
            final String[] argumentArray = arguments.toArray(new String[0]);
            final String[] environmentArray = new String[environment.size() * 2];
            int index = 0;
            for (final Map.Entry<String, String> entry : environment.entrySet()) {
              environmentArray[index++] = entry.getKey();
              environmentArray[index++] = entry.getValue();
            }

            final ProcessCreationResult result =
                nativeCreateSandboxedProcess(
                    wasiContext.getNativeHandle(),
                    processHandle,
                    executable,
                    argumentArray,
                    environmentArray,
                    sandboxConfig.sandboxType.ordinal(),
                    sandboxConfig.capabilities.value,
                    sandboxConfig.allowNetworking,
                    sandboxConfig.allowFileSystemAccess,
                    sandboxConfig.allowProcessControl,
                    resourceLimits.maxMemoryBytes,
                    resourceLimits.maxCpuPercent,
                    resourceLimits.maxFileDescriptors,
                    resourceLimits.maxProcesses,
                    resourceLimits.timeoutSeconds);

            if (result.errorCode != 0) {
              final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result.errorCode);
              throw new WasiException(
                  "Failed to create sandboxed process: "
                      + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
                  errorCode != null ? errorCode : WasiErrorCode.EIO);
            }

            // Track active process
            final ProcessInfo processInfo =
                new ProcessInfo(
                    processHandle,
                    executable,
                    arguments,
                    result.systemProcessId,
                    ProcessState.RUNNING,
                    sandboxConfig,
                    resourceLimits,
                    System.currentTimeMillis());
            activeProcesses.put(processHandle, processInfo);

            LOGGER.info(
                () ->
                    String.format(
                        "Created sandboxed process: handle=%d, pid=%d",
                        processHandle, result.systemProcessId));

            return processHandle;

          } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create sandboxed process", e);
            throw new RuntimeException("Process creation failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Creates a resource usage monitor for a process.
   *
   * <p>Monitors CPU, memory, I/O, and other resource usage for a process with configurable
   * thresholds and alerts.
   *
   * @param processHandle the process to monitor
   * @param monitoringConfig the monitoring configuration
   * @param alertCallback the callback to invoke for threshold violations
   * @return CompletableFuture that resolves to the monitor handle
   * @throws WasiException if monitor creation fails
   */
  public CompletableFuture<Long> createResourceMonitorAsync(
      final long processHandle,
      final ResourceMonitoringConfig monitoringConfig,
      final Consumer<ResourceUsageAlert> alertCallback) {
    JniValidation.requireNonNull(monitoringConfig, "monitoringConfig");
    JniValidation.requireNonNull(alertCallback, "alertCallback");

    final ProcessInfo process = activeProcesses.get(processHandle);
    if (process == null) {
      throw new WasiException("Invalid process handle: " + processHandle, WasiErrorCode.EBADF);
    }

    if (monitoringConfig.intervalSeconds > MAX_MONITORING_INTERVAL_SECONDS) {
      throw new WasiException(
          "Monitoring interval too large: " + monitoringConfig.intervalSeconds,
          WasiErrorCode.EINVAL);
    }

    LOGGER.fine(
        () ->
            String.format(
                "Creating resource monitor: process=%d, interval=%ds",
                processHandle, monitoringConfig.intervalSeconds));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final long monitorHandle = processHandleGenerator.getAndIncrement();

            final ResourceMonitorResult result =
                nativeCreateResourceMonitor(
                    wasiContext.getNativeHandle(),
                    monitorHandle,
                    processHandle,
                    monitoringConfig.intervalSeconds,
                    monitoringConfig.memoryThresholdBytes,
                    monitoringConfig.cpuThresholdPercent,
                    monitoringConfig.ioThresholdBytesPerSecond,
                    monitoringConfig.networkThresholdBytesPerSecond,
                    monitoringConfig.enableDetailedStats);

            if (result.errorCode != 0) {
              final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result.errorCode);
              throw new WasiException(
                  "Failed to create resource monitor: "
                      + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
                  errorCode != null ? errorCode : WasiErrorCode.EIO);
            }

            // Track resource monitor
            final ResourceMonitorInfo monitorInfo =
                new ResourceMonitorInfo(
                    monitorHandle,
                    processHandle,
                    monitoringConfig,
                    alertCallback,
                    true,
                    System.currentTimeMillis());
            resourceMonitors.put(monitorHandle, monitorInfo);

            LOGGER.fine(
                () ->
                    String.format(
                        "Created resource monitor: handle=%d, baseline=%s",
                        monitorHandle, result.baselineUsage));

            return monitorHandle;

          } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create resource monitor", e);
            throw new RuntimeException("Resource monitor creation failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Creates an inter-process communication channel.
   *
   * <p>Establishes communication channels between processes using pipes, shared memory, or message
   * queues.
   *
   * @param sourceProcessHandle the source process handle
   * @param targetProcessHandle the target process handle
   * @param channelType the type of IPC channel
   * @param channelConfig the channel configuration
   * @return CompletableFuture that resolves to the IPC handle
   * @throws WasiException if IPC creation fails
   */
  public CompletableFuture<Long> createIPCChannelAsync(
      final long sourceProcessHandle,
      final long targetProcessHandle,
      final IPCChannelType channelType,
      final IPCChannelConfig channelConfig) {
    JniValidation.requireNonNull(channelType, "channelType");
    JniValidation.requireNonNull(channelConfig, "channelConfig");

    final ProcessInfo sourceProcess = activeProcesses.get(sourceProcessHandle);
    final ProcessInfo targetProcess = activeProcesses.get(targetProcessHandle);

    if (sourceProcess == null) {
      throw new WasiException(
          "Invalid source process handle: " + sourceProcessHandle, WasiErrorCode.EBADF);
    }
    if (targetProcess == null) {
      throw new WasiException(
          "Invalid target process handle: " + targetProcessHandle, WasiErrorCode.EBADF);
    }

    LOGGER.fine(
        () ->
            String.format(
                "Creating IPC channel: source=%d, target=%d, type=%s",
                sourceProcessHandle, targetProcessHandle, channelType));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final long ipcHandle = processHandleGenerator.getAndIncrement();

            final IPCCreationResult result =
                nativeCreateIPCChannel(
                    wasiContext.getNativeHandle(),
                    ipcHandle,
                    sourceProcessHandle,
                    targetProcessHandle,
                    channelType.ordinal(),
                    channelConfig.bufferSize,
                    channelConfig.maxMessages,
                    channelConfig.bidirectional,
                    channelConfig.persistent);

            if (result.errorCode != 0) {
              final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result.errorCode);
              throw new WasiException(
                  "Failed to create IPC channel: "
                      + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
                  errorCode != null ? errorCode : WasiErrorCode.EIO);
            }

            // Track IPC handle
            final IPCHandleInfo ipcInfo =
                new IPCHandleInfo(
                    ipcHandle,
                    sourceProcessHandle,
                    targetProcessHandle,
                    channelType,
                    channelConfig,
                    IPCState.CONNECTED,
                    System.currentTimeMillis());
            ipcHandles.put(ipcHandle, ipcInfo);

            LOGGER.fine(
                () ->
                    String.format(
                        "Created IPC channel: handle=%d, channelId=%s",
                        ipcHandle, result.channelIdentifier));

            return ipcHandle;

          } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create IPC channel", e);
            throw new RuntimeException("IPC channel creation failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Registers a system service for discovery and interaction.
   *
   * <p>Registers a service that other processes can discover and communicate with.
   *
   * @param serviceName the unique service name
   * @param serviceMetadata the service metadata and capabilities
   * @param serviceHandler the handler for service requests
   * @return CompletableFuture that resolves to the service registration handle
   * @throws WasiException if service registration fails
   */
  public CompletableFuture<String> registerSystemServiceAsync(
      final String serviceName,
      final SystemServiceMetadata serviceMetadata,
      final Consumer<ServiceRequest> serviceHandler) {
    JniValidation.requireNonEmpty(serviceName, "serviceName");
    JniValidation.requireNonNull(serviceMetadata, "serviceMetadata");
    JniValidation.requireNonNull(serviceHandler, "serviceHandler");

    if (systemServices.containsKey(serviceName)) {
      throw new WasiException("Service already registered: " + serviceName, WasiErrorCode.EEXIST);
    }

    LOGGER.info(
        () ->
            String.format(
                "Registering system service: name=%s, version=%s",
                serviceName, serviceMetadata.version));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            // Prepare service metadata
            final String[] capabilitiesArray = serviceMetadata.capabilities.toArray(new String[0]);
            final String[] endpointsArray = serviceMetadata.endpoints.toArray(new String[0]);

            final ServiceRegistrationResult result =
                nativeRegisterSystemService(
                    wasiContext.getNativeHandle(),
                    serviceName,
                    serviceMetadata.version,
                    serviceMetadata.description,
                    capabilitiesArray,
                    endpointsArray,
                    serviceMetadata.port,
                    serviceMetadata.secure);

            if (result.errorCode != 0) {
              final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result.errorCode);
              throw new WasiException(
                  "Failed to register system service: "
                      + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
                  errorCode != null ? errorCode : WasiErrorCode.EIO);
            }

            // Track system service
            final SystemServiceInfo serviceInfo =
                new SystemServiceInfo(
                    serviceName,
                    serviceMetadata,
                    serviceHandler,
                    ServiceState.ACTIVE,
                    result.serviceId,
                    System.currentTimeMillis());
            systemServices.put(serviceName, serviceInfo);

            LOGGER.info(
                () ->
                    String.format(
                        "Registered system service: name=%s, id=%s", serviceName, result.serviceId));

            return result.serviceId;

          } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to register system service", e);
            throw new RuntimeException("Service registration failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Discovers available system services.
   *
   * @param servicePattern the service name pattern to match
   * @param requiredCapabilities the required service capabilities
   * @return CompletableFuture that resolves to the list of discovered services
   * @throws WasiException if service discovery fails
   */
  public CompletableFuture<List<SystemServiceInfo>> discoverSystemServicesAsync(
      final String servicePattern, final List<String> requiredCapabilities) {
    JniValidation.requireNonEmpty(servicePattern, "servicePattern");
    JniValidation.requireNonNull(requiredCapabilities, "requiredCapabilities");

    LOGGER.fine(
        () ->
            String.format(
                "Discovering services: pattern=%s, capabilities=%d",
                servicePattern, requiredCapabilities.size()));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final String[] capabilitiesArray = requiredCapabilities.toArray(new String[0]);

            final ServiceDiscoveryResult[] results =
                nativeDiscoverSystemServices(
                    wasiContext.getNativeHandle(), servicePattern, capabilitiesArray);

            final List<SystemServiceInfo> discoveredServices = new java.util.ArrayList<>();
            for (final ServiceDiscoveryResult result : results) {
              final SystemServiceMetadata metadata =
                  new SystemServiceMetadata(
                      result.version,
                      result.description,
                      List.of(result.capabilities),
                      List.of(result.endpoints),
                      result.port,
                      result.secure);

              discoveredServices.add(
                  new SystemServiceInfo(
                      result.serviceName,
                      metadata,
                      null, // No handler for discovered services
                      ServiceState.DISCOVERED,
                      result.serviceId,
                      result.registeredAt));
            }

            LOGGER.fine(
                () -> String.format("Discovered %d services", discoveredServices.size()));

            return discoveredServices;

          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to discover system services", e);
            throw new RuntimeException("Service discovery failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Gets current resource usage for a process.
   *
   * @param processHandle the process handle
   * @return CompletableFuture that resolves to the current resource usage
   * @throws WasiException if resource usage retrieval fails
   */
  public CompletableFuture<ProcessResourceUsage> getProcessResourceUsageAsync(
      final long processHandle) {
    final ProcessInfo process = activeProcesses.get(processHandle);
    if (process == null) {
      throw new WasiException("Invalid process handle: " + processHandle, WasiErrorCode.EBADF);
    }

    LOGGER.fine(() -> String.format("Getting resource usage: process=%d", processHandle));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final ResourceUsageResult result =
                nativeGetProcessResourceUsage(wasiContext.getNativeHandle(), processHandle);

            if (result.errorCode != 0) {
              final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result.errorCode);
              throw new WasiException(
                  "Failed to get process resource usage: "
                      + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
                  errorCode != null ? errorCode : WasiErrorCode.EIO);
            }

            return new ProcessResourceUsage(
                result.cpuUsagePercent,
                result.memoryUsageBytes,
                result.ioReadBytes,
                result.ioWriteBytes,
                result.networkRxBytes,
                result.networkTxBytes,
                result.fileDescriptorCount,
                result.threadCount,
                Duration.ofNanos(result.executionTimeNanos),
                Instant.ofEpochMilli(result.lastUpdated));

          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get process resource usage", e);
            throw new RuntimeException("Resource usage retrieval failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Terminates a process gracefully.
   *
   * @param processHandle the process handle to terminate
   * @param timeoutSeconds the timeout for graceful termination
   * @return CompletableFuture that completes when process is terminated
   * @throws WasiException if process termination fails
   */
  public CompletableFuture<Void> terminateProcessAsync(
      final long processHandle, final int timeoutSeconds) {
    final ProcessInfo process = activeProcesses.get(processHandle);
    if (process == null) {
      throw new WasiException("Invalid process handle: " + processHandle, WasiErrorCode.EBADF);
    }

    if (process.state == ProcessState.TERMINATED) {
      return CompletableFuture.completedFuture(null);
    }

    LOGGER.info(
        () ->
            String.format(
                "Terminating process: handle=%d, timeout=%ds", processHandle, timeoutSeconds));

    return CompletableFuture.runAsync(
        () -> {
          try {
            final int result =
                nativeTerminateProcess(
                    wasiContext.getNativeHandle(), processHandle, timeoutSeconds);

            if (result != 0) {
              final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
              throw new WasiException(
                  "Failed to terminate process: "
                      + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
                  errorCode != null ? errorCode : WasiErrorCode.EIO);
            }

            // Update process state
            process.state = ProcessState.TERMINATED;
            LOGGER.info(() -> String.format("Terminated process: handle=%d", processHandle));

          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to terminate process: " + processHandle, e);
            throw new RuntimeException("Process termination failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /** Closes the experimental process handler and cleans up resources. */
  public void close() {
    LOGGER.info("Closing experimental process handler");

    // Terminate all active processes
    for (final ProcessInfo process : activeProcesses.values()) {
      if (process.state == ProcessState.RUNNING) {
        try {
          nativeTerminateProcess(wasiContext.getNativeHandle(), process.handle, 5);
        } catch (final Exception e) {
          LOGGER.log(Level.WARNING, "Error terminating process during shutdown", e);
        }
      }
    }

    // Stop all resource monitors
    for (final ResourceMonitorInfo monitor : resourceMonitors.values()) {
      if (monitor.active) {
        try {
          nativeStopResourceMonitor(wasiContext.getNativeHandle(), monitor.handle);
        } catch (final Exception e) {
          LOGGER.log(Level.WARNING, "Error stopping resource monitor during shutdown", e);
        }
      }
    }

    // Close all IPC channels
    for (final IPCHandleInfo ipc : ipcHandles.values()) {
      if (ipc.state == IPCState.CONNECTED) {
        try {
          nativeCloseIPCChannel(wasiContext.getNativeHandle(), ipc.handle);
        } catch (final Exception e) {
          LOGGER.log(Level.WARNING, "Error closing IPC channel during shutdown", e);
        }
      }
    }

    // Unregister all system services
    for (final SystemServiceInfo service : systemServices.values()) {
      if (service.state == ServiceState.ACTIVE) {
        try {
          nativeUnregisterSystemService(wasiContext.getNativeHandle(), service.serviceName);
        } catch (final Exception e) {
          LOGGER.log(Level.WARNING, "Error unregistering system service during shutdown", e);
        }
      }
    }

    activeProcesses.clear();
    resourceMonitors.clear();
    ipcHandles.clear();
    systemServices.clear();

    LOGGER.info("Experimental process handler closed successfully");
  }

  // Native method declarations
  private static native ProcessCreationResult nativeCreateSandboxedProcess(
      long contextHandle,
      long processHandle,
      String executable,
      String[] arguments,
      String[] environment,
      int sandboxType,
      int capabilities,
      boolean allowNetworking,
      boolean allowFileSystemAccess,
      boolean allowProcessControl,
      long maxMemoryBytes,
      int maxCpuPercent,
      int maxFileDescriptors,
      int maxProcesses,
      long timeoutSeconds);

  private static native ResourceMonitorResult nativeCreateResourceMonitor(
      long contextHandle,
      long monitorHandle,
      long processHandle,
      long intervalSeconds,
      long memoryThresholdBytes,
      int cpuThresholdPercent,
      long ioThresholdBytesPerSecond,
      long networkThresholdBytesPerSecond,
      boolean enableDetailedStats);

  private static native IPCCreationResult nativeCreateIPCChannel(
      long contextHandle,
      long ipcHandle,
      long sourceProcessHandle,
      long targetProcessHandle,
      int channelType,
      int bufferSize,
      int maxMessages,
      boolean bidirectional,
      boolean persistent);

  private static native ServiceRegistrationResult nativeRegisterSystemService(
      long contextHandle,
      String serviceName,
      String version,
      String description,
      String[] capabilities,
      String[] endpoints,
      int port,
      boolean secure);

  private static native ServiceDiscoveryResult[] nativeDiscoverSystemServices(
      long contextHandle, String servicePattern, String[] requiredCapabilities);

  private static native ResourceUsageResult nativeGetProcessResourceUsage(
      long contextHandle, long processHandle);

  private static native int nativeTerminateProcess(
      long contextHandle, long processHandle, int timeoutSeconds);

  private static native int nativeStopResourceMonitor(long contextHandle, long monitorHandle);

  private static native int nativeCloseIPCChannel(long contextHandle, long ipcHandle);

  private static native int nativeUnregisterSystemService(
      long contextHandle, String serviceName);

  /** Process state enumeration. */
  public enum ProcessState {
    CREATED,
    RUNNING,
    SUSPENDED,
    TERMINATED
  }

  /** Sandbox type enumeration. */
  public enum SandboxType {
    MINIMAL,
    STANDARD,
    STRICT,
    CONTAINER
  }

  /** IPC channel type enumeration. */
  public enum IPCChannelType {
    PIPE,
    SHARED_MEMORY,
    MESSAGE_QUEUE,
    SOCKET
  }

  /** IPC state enumeration. */
  public enum IPCState {
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    ERROR
  }

  /** Service state enumeration. */
  public enum ServiceState {
    REGISTERING,
    ACTIVE,
    INACTIVE,
    DISCOVERED
  }

  /** Sandbox capabilities. */
  public static final class SandboxCapabilities {
    public static final SandboxCapabilities NONE = new SandboxCapabilities(0x00);
    public static final SandboxCapabilities FILE_READ = new SandboxCapabilities(0x01);
    public static final SandboxCapabilities FILE_WRITE = new SandboxCapabilities(0x02);
    public static final SandboxCapabilities NETWORK_CLIENT = new SandboxCapabilities(0x04);
    public static final SandboxCapabilities NETWORK_SERVER = new SandboxCapabilities(0x08);
    public static final SandboxCapabilities PROCESS_SPAWN = new SandboxCapabilities(0x10);
    public static final SandboxCapabilities SYSTEM_INFO = new SandboxCapabilities(0x20);

    public final int value;

    private SandboxCapabilities(final int value) {
      this.value = value;
    }

    public SandboxCapabilities combine(final SandboxCapabilities other) {
      return new SandboxCapabilities(this.value | other.value);
    }
  }

  /** Sandbox configuration. */
  public static final class SandboxConfig {
    public final SandboxType sandboxType;
    public final SandboxCapabilities capabilities;
    public final boolean allowNetworking;
    public final boolean allowFileSystemAccess;
    public final boolean allowProcessControl;

    public SandboxConfig(
        final SandboxType sandboxType,
        final SandboxCapabilities capabilities,
        final boolean allowNetworking,
        final boolean allowFileSystemAccess,
        final boolean allowProcessControl) {
      this.sandboxType = sandboxType;
      this.capabilities = capabilities;
      this.allowNetworking = allowNetworking;
      this.allowFileSystemAccess = allowFileSystemAccess;
      this.allowProcessControl = allowProcessControl;
    }

    public static SandboxConfig minimal() {
      return new SandboxConfig(SandboxType.MINIMAL, SandboxCapabilities.NONE, false, false, false);
    }

    public static SandboxConfig standard() {
      return new SandboxConfig(
          SandboxType.STANDARD,
          SandboxCapabilities.FILE_READ.combine(SandboxCapabilities.SYSTEM_INFO),
          false,
          true,
          false);
    }
  }

  /** Process resource limits. */
  public static final class ProcessResourceLimits {
    public final long maxMemoryBytes;
    public final int maxCpuPercent;
    public final int maxFileDescriptors;
    public final int maxProcesses;
    public final long timeoutSeconds;

    public ProcessResourceLimits(
        final long maxMemoryBytes,
        final int maxCpuPercent,
        final int maxFileDescriptors,
        final int maxProcesses,
        final long timeoutSeconds) {
      this.maxMemoryBytes = Math.max(0, maxMemoryBytes);
      this.maxCpuPercent = Math.max(1, Math.min(100, maxCpuPercent));
      this.maxFileDescriptors = Math.max(1, maxFileDescriptors);
      this.maxProcesses = Math.max(1, maxProcesses);
      this.timeoutSeconds = Math.max(0, timeoutSeconds);
    }

    public static ProcessResourceLimits defaultLimits() {
      return new ProcessResourceLimits(512 * 1024 * 1024, 50, 256, 10, 300);
    }
  }

  /** Resource monitoring configuration. */
  public static final class ResourceMonitoringConfig {
    public final long intervalSeconds;
    public final long memoryThresholdBytes;
    public final int cpuThresholdPercent;
    public final long ioThresholdBytesPerSecond;
    public final long networkThresholdBytesPerSecond;
    public final boolean enableDetailedStats;

    public ResourceMonitoringConfig(
        final long intervalSeconds,
        final long memoryThresholdBytes,
        final int cpuThresholdPercent,
        final long ioThresholdBytesPerSecond,
        final long networkThresholdBytesPerSecond,
        final boolean enableDetailedStats) {
      this.intervalSeconds = Math.max(1, Math.min(MAX_MONITORING_INTERVAL_SECONDS, intervalSeconds));
      this.memoryThresholdBytes = Math.max(0, memoryThresholdBytes);
      this.cpuThresholdPercent = Math.max(0, Math.min(100, cpuThresholdPercent));
      this.ioThresholdBytesPerSecond = Math.max(0, ioThresholdBytesPerSecond);
      this.networkThresholdBytesPerSecond = Math.max(0, networkThresholdBytesPerSecond);
      this.enableDetailedStats = enableDetailedStats;
    }
  }

  /** IPC channel configuration. */
  public static final class IPCChannelConfig {
    public final int bufferSize;
    public final int maxMessages;
    public final boolean bidirectional;
    public final boolean persistent;

    public IPCChannelConfig(
        final int bufferSize,
        final int maxMessages,
        final boolean bidirectional,
        final boolean persistent) {
      this.bufferSize = Math.max(1024, bufferSize);
      this.maxMessages = Math.max(1, maxMessages);
      this.bidirectional = bidirectional;
      this.persistent = persistent;
    }

    public static IPCChannelConfig defaultConfig() {
      return new IPCChannelConfig(64 * 1024, 1000, true, false);
    }
  }

  /** System service metadata. */
  public static final class SystemServiceMetadata {
    public final String version;
    public final String description;
    public final List<String> capabilities;
    public final List<String> endpoints;
    public final int port;
    public final boolean secure;

    public SystemServiceMetadata(
        final String version,
        final String description,
        final List<String> capabilities,
        final List<String> endpoints,
        final int port,
        final boolean secure) {
      this.version = version;
      this.description = description;
      this.capabilities = List.copyOf(capabilities);
      this.endpoints = List.copyOf(endpoints);
      this.port = port;
      this.secure = secure;
    }
  }

  /** Process resource usage. */
  public static final class ProcessResourceUsage {
    public final double cpuUsagePercent;
    public final long memoryUsageBytes;
    public final long ioReadBytes;
    public final long ioWriteBytes;
    public final long networkRxBytes;
    public final long networkTxBytes;
    public final int fileDescriptorCount;
    public final int threadCount;
    public final Duration executionTime;
    public final Instant lastUpdated;

    public ProcessResourceUsage(
        final double cpuUsagePercent,
        final long memoryUsageBytes,
        final long ioReadBytes,
        final long ioWriteBytes,
        final long networkRxBytes,
        final long networkTxBytes,
        final int fileDescriptorCount,
        final int threadCount,
        final Duration executionTime,
        final Instant lastUpdated) {
      this.cpuUsagePercent = cpuUsagePercent;
      this.memoryUsageBytes = memoryUsageBytes;
      this.ioReadBytes = ioReadBytes;
      this.ioWriteBytes = ioWriteBytes;
      this.networkRxBytes = networkRxBytes;
      this.networkTxBytes = networkTxBytes;
      this.fileDescriptorCount = fileDescriptorCount;
      this.threadCount = threadCount;
      this.executionTime = executionTime;
      this.lastUpdated = lastUpdated;
    }
  }

  /** Resource usage alert. */
  public static final class ResourceUsageAlert {
    public final long processHandle;
    public final String alertType;
    public final String message;
    public final double thresholdValue;
    public final double currentValue;
    public final Instant timestamp;

    public ResourceUsageAlert(
        final long processHandle,
        final String alertType,
        final String message,
        final double thresholdValue,
        final double currentValue,
        final Instant timestamp) {
      this.processHandle = processHandle;
      this.alertType = alertType;
      this.message = message;
      this.thresholdValue = thresholdValue;
      this.currentValue = currentValue;
      this.timestamp = timestamp;
    }
  }

  /** Service request. */
  public static final class ServiceRequest {
    public final String operation;
    public final Map<String, String> parameters;
    public final byte[] data;
    public final String clientId;

    public ServiceRequest(
        final String operation,
        final Map<String, String> parameters,
        final byte[] data,
        final String clientId) {
      this.operation = operation;
      this.parameters = parameters != null ? Map.copyOf(parameters) : Map.of();
      this.data = data != null ? data.clone() : new byte[0];
      this.clientId = clientId;
    }
  }

  /** Process information. */
  private static final class ProcessInfo {
    public final long handle;
    public final String executable;
    public final List<String> arguments;
    public final long systemProcessId;
    public volatile ProcessState state;
    public final SandboxConfig sandboxConfig;
    public final ProcessResourceLimits resourceLimits;
    public final long createdAt;

    public ProcessInfo(
        final long handle,
        final String executable,
        final List<String> arguments,
        final long systemProcessId,
        final ProcessState state,
        final SandboxConfig sandboxConfig,
        final ProcessResourceLimits resourceLimits,
        final long createdAt) {
      this.handle = handle;
      this.executable = executable;
      this.arguments = List.copyOf(arguments);
      this.systemProcessId = systemProcessId;
      this.state = state;
      this.sandboxConfig = sandboxConfig;
      this.resourceLimits = resourceLimits;
      this.createdAt = createdAt;
    }
  }

  /** Resource monitor information. */
  private static final class ResourceMonitorInfo {
    public final long handle;
    public final long processHandle;
    public final ResourceMonitoringConfig config;
    public final Consumer<ResourceUsageAlert> alertCallback;
    public volatile boolean active;
    public final long createdAt;

    public ResourceMonitorInfo(
        final long handle,
        final long processHandle,
        final ResourceMonitoringConfig config,
        final Consumer<ResourceUsageAlert> alertCallback,
        final boolean active,
        final long createdAt) {
      this.handle = handle;
      this.processHandle = processHandle;
      this.config = config;
      this.alertCallback = alertCallback;
      this.active = active;
      this.createdAt = createdAt;
    }
  }

  /** IPC handle information. */
  private static final class IPCHandleInfo {
    public final long handle;
    public final long sourceProcessHandle;
    public final long targetProcessHandle;
    public final IPCChannelType channelType;
    public final IPCChannelConfig config;
    public volatile IPCState state;
    public final long createdAt;

    public IPCHandleInfo(
        final long handle,
        final long sourceProcessHandle,
        final long targetProcessHandle,
        final IPCChannelType channelType,
        final IPCChannelConfig config,
        final IPCState state,
        final long createdAt) {
      this.handle = handle;
      this.sourceProcessHandle = sourceProcessHandle;
      this.targetProcessHandle = targetProcessHandle;
      this.channelType = channelType;
      this.config = config;
      this.state = state;
      this.createdAt = createdAt;
    }
  }

  /** System service information. */
  public static final class SystemServiceInfo {
    public final String serviceName;
    public final SystemServiceMetadata metadata;
    public final Consumer<ServiceRequest> handler;
    public volatile ServiceState state;
    public final String serviceId;
    public final long registeredAt;

    public SystemServiceInfo(
        final String serviceName,
        final SystemServiceMetadata metadata,
        final Consumer<ServiceRequest> handler,
        final ServiceState state,
        final String serviceId,
        final long registeredAt) {
      this.serviceName = serviceName;
      this.metadata = metadata;
      this.handler = handler;
      this.state = state;
      this.serviceId = serviceId;
      this.registeredAt = registeredAt;
    }
  }

  // Native result classes
  private static final class ProcessCreationResult {
    public final int errorCode;
    public final long systemProcessId;

    public ProcessCreationResult(final int errorCode, final long systemProcessId) {
      this.errorCode = errorCode;
      this.systemProcessId = systemProcessId;
    }
  }

  private static final class ResourceMonitorResult {
    public final int errorCode;
    public final String baselineUsage;

    public ResourceMonitorResult(final int errorCode, final String baselineUsage) {
      this.errorCode = errorCode;
      this.baselineUsage = baselineUsage;
    }
  }

  private static final class IPCCreationResult {
    public final int errorCode;
    public final String channelIdentifier;

    public IPCCreationResult(final int errorCode, final String channelIdentifier) {
      this.errorCode = errorCode;
      this.channelIdentifier = channelIdentifier;
    }
  }

  private static final class ServiceRegistrationResult {
    public final int errorCode;
    public final String serviceId;

    public ServiceRegistrationResult(final int errorCode, final String serviceId) {
      this.errorCode = errorCode;
      this.serviceId = serviceId;
    }
  }

  private static final class ServiceDiscoveryResult {
    public final String serviceName;
    public final String serviceId;
    public final String version;
    public final String description;
    public final String[] capabilities;
    public final String[] endpoints;
    public final int port;
    public final boolean secure;
    public final long registeredAt;

    public ServiceDiscoveryResult(
        final String serviceName,
        final String serviceId,
        final String version,
        final String description,
        final String[] capabilities,
        final String[] endpoints,
        final int port,
        final boolean secure,
        final long registeredAt) {
      this.serviceName = serviceName;
      this.serviceId = serviceId;
      this.version = version;
      this.description = description;
      this.capabilities = capabilities;
      this.endpoints = endpoints;
      this.port = port;
      this.secure = secure;
      this.registeredAt = registeredAt;
    }
  }

  private static final class ResourceUsageResult {
    public final int errorCode;
    public final double cpuUsagePercent;
    public final long memoryUsageBytes;
    public final long ioReadBytes;
    public final long ioWriteBytes;
    public final long networkRxBytes;
    public final long networkTxBytes;
    public final int fileDescriptorCount;
    public final int threadCount;
    public final long executionTimeNanos;
    public final long lastUpdated;

    public ResourceUsageResult(
        final int errorCode,
        final double cpuUsagePercent,
        final long memoryUsageBytes,
        final long ioReadBytes,
        final long ioWriteBytes,
        final long networkRxBytes,
        final long networkTxBytes,
        final int fileDescriptorCount,
        final int threadCount,
        final long executionTimeNanos,
        final long lastUpdated) {
      this.errorCode = errorCode;
      this.cpuUsagePercent = cpuUsagePercent;
      this.memoryUsageBytes = memoryUsageBytes;
      this.ioReadBytes = ioReadBytes;
      this.ioWriteBytes = ioWriteBytes;
      this.networkRxBytes = networkRxBytes;
      this.networkTxBytes = networkTxBytes;
      this.fileDescriptorCount = fileDescriptorCount;
      this.threadCount = threadCount;
      this.executionTimeNanos = executionTimeNanos;
      this.lastUpdated = lastUpdated;
    }
  }
}
package ai.tegmentum.wasmtime4j.performance.profiling;

import ai.tegmentum.wasmtime4j.performance.ExportFormat;
import ai.tegmentum.wasmtime4j.performance.ProfileSnapshot;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Integration with JVM profiling tools for detailed performance analysis.
 *
 * <p>This class provides integration with multiple JVM profiling tools:
 * <ul>
 *   <li>async-profiler - Low-overhead sampling profiler</li>
 *   <li>Java Flight Recorder (JFR) - Built-in JDK profiling</li>
 *   <li>JVM built-in tools - ThreadMXBean, MemoryMXBean, etc.</li>
 * </ul>
 *
 * <p>The integration automatically detects available profiling tools and provides
 * a unified interface for profiling WebAssembly execution.
 *
 * <p>Usage example:
 * <pre>{@code
 * JvmProfilerIntegration profiler = JvmProfilerIntegration.create();
 *
 * // Start profiling with async-profiler if available
 * ProfileSession session = profiler.startProfiling(ProfilingMode.CPU_SAMPLING);
 *
 * // ... run WebAssembly operations ...
 *
 * // Stop profiling and get results
 * ProfilingResult result = session.stop();
 * String flameGraph = result.export(ExportFormat.FLAME_GRAPH);
 * }</pre>
 *
 * @since 1.0.0
 */
public final class JvmProfilerIntegration {
  private static final Logger LOGGER = Logger.getLogger(JvmProfilerIntegration.class.getName());

  private static final String ASYNC_PROFILER_CLASS = "one.profiler.AsyncProfiler";
  private static final String JFR_RECORDING_CLASS = "jdk.jfr.Recording";

  private final boolean asyncProfilerAvailable;
  private final boolean jfrAvailable;
  private final MBeanServer mBeanServer;
  private final List<ActiveSession> activeSessions;

  private JvmProfilerIntegration() {
    this.asyncProfilerAvailable = checkAsyncProfilerAvailability();
    this.jfrAvailable = checkJfrAvailability();
    this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
    this.activeSessions = new ArrayList<>();
  }

  /**
   * Creates a new JVM profiler integration instance.
   *
   * @return profiler integration instance
   */
  public static JvmProfilerIntegration create() {
    return new JvmProfilerIntegration();
  }

  /**
   * Gets the available profiling tools on this system.
   *
   * @return list of available profiling tools
   */
  public List<ProfilingTool> getAvailableTools() {
    final List<ProfilingTool> tools = new ArrayList<>();

    if (asyncProfilerAvailable) {
      tools.add(ProfilingTool.ASYNC_PROFILER);
    }
    if (jfrAvailable) {
      tools.add(ProfilingTool.JAVA_FLIGHT_RECORDER);
    }

    // Built-in JVM tools are always available
    tools.add(ProfilingTool.JVM_BUILTIN);

    return tools;
  }

  /**
   * Starts a profiling session with the specified configuration.
   *
   * @param mode the profiling mode
   * @return active profiling session
   * @throws ProfilingException if profiling cannot be started
   */
  public ProfileSession startProfiling(final ProfilingMode mode) throws ProfilingException {
    return startProfiling(mode, ProfilingConfig.defaultConfig());
  }

  /**
   * Starts a profiling session with custom configuration.
   *
   * @param mode the profiling mode
   * @param config the profiling configuration
   * @return active profiling session
   * @throws ProfilingException if profiling cannot be started
   */
  public ProfileSession startProfiling(final ProfilingMode mode, final ProfilingConfig config)
      throws ProfilingException {
    Objects.requireNonNull(mode, "mode cannot be null");
    Objects.requireNonNull(config, "config cannot be null");

    final ProfilingTool selectedTool = selectBestTool(mode);
    final ActiveSession session = createSession(selectedTool, mode, config);

    activeSessions.add(session);
    session.start();

    LOGGER.info(String.format("Started profiling session with %s in %s mode",
        selectedTool, mode));

    return session;
  }

  /**
   * Stops all active profiling sessions.
   */
  public void stopAllSessions() {
    for (final ActiveSession session : new ArrayList<>(activeSessions)) {
      try {
        session.stop();
      } catch (final Exception e) {
        LOGGER.warning("Failed to stop profiling session: " + e.getMessage());
      }
    }
    activeSessions.clear();
  }

  /**
   * Checks if any profiling sessions are currently active.
   *
   * @return true if sessions are active
   */
  public boolean hasActiveSessions() {
    return !activeSessions.isEmpty();
  }

  /**
   * Gets the number of active profiling sessions.
   *
   * @return active session count
   */
  public int getActiveSessionCount() {
    return activeSessions.size();
  }

  /**
   * Gets recommendations for optimal profiling configuration.
   *
   * @param mode the intended profiling mode
   * @return profiling recommendations
   */
  public ProfilingRecommendations getRecommendations(final ProfilingMode mode) {
    final ProfilingTool bestTool = selectBestTool(mode);
    final ProfilingConfig recommendedConfig = getRecommendedConfig(bestTool, mode);

    return new ProfilingRecommendations(bestTool, recommendedConfig, getToolSpecificAdvice(bestTool, mode));
  }

  private boolean checkAsyncProfilerAvailability() {
    try {
      Class.forName(ASYNC_PROFILER_CLASS);
      // Also check if the native library is loaded
      return checkAsyncProfilerNativeLibrary();
    } catch (final ClassNotFoundException e) {
      return false;
    }
  }

  private boolean checkAsyncProfilerNativeLibrary() {
    try {
      // Try to access AsyncProfiler instance
      final Class<?> asyncProfilerClass = Class.forName(ASYNC_PROFILER_CLASS);
      final Object instance = asyncProfilerClass.getMethod("getInstance").invoke(null);

      // Check if we can get version (indicates native library is working)
      asyncProfilerClass.getMethod("getVersion").invoke(instance);
      return true;
    } catch (final Exception e) {
      LOGGER.fine("async-profiler native library not available: " + e.getMessage());
      return false;
    }
  }

  private boolean checkJfrAvailability() {
    try {
      Class.forName(JFR_RECORDING_CLASS);
      // Check if JFR is enabled (might be disabled with -XX:-FlightRecorder)
      return checkJfrEnabled();
    } catch (final ClassNotFoundException e) {
      return false;
    }
  }

  private boolean checkJfrEnabled() {
    try {
      final ObjectName jfrMBean = new ObjectName("jdk.management.jfr:type=FlightRecorder");
      return mBeanServer.isRegistered(jfrMBean);
    } catch (final Exception e) {
      return false;
    }
  }

  private ProfilingTool selectBestTool(final ProfilingMode mode) {
    switch (mode) {
      case CPU_SAMPLING:
      case MEMORY_ALLOCATION:
        return asyncProfilerAvailable ? ProfilingTool.ASYNC_PROFILER :
               jfrAvailable ? ProfilingTool.JAVA_FLIGHT_RECORDER : ProfilingTool.JVM_BUILTIN;

      case FULL_SYSTEM:
        return jfrAvailable ? ProfilingTool.JAVA_FLIGHT_RECORDER :
               asyncProfilerAvailable ? ProfilingTool.ASYNC_PROFILER : ProfilingTool.JVM_BUILTIN;

      case LOW_OVERHEAD:
        return asyncProfilerAvailable ? ProfilingTool.ASYNC_PROFILER : ProfilingTool.JVM_BUILTIN;

      default:
        return ProfilingTool.JVM_BUILTIN;
    }
  }

  private ActiveSession createSession(final ProfilingTool tool, final ProfilingMode mode,
                                     final ProfilingConfig config) throws ProfilingException {
    switch (tool) {
      case ASYNC_PROFILER:
        return new AsyncProfilerSession(mode, config);
      case JAVA_FLIGHT_RECORDER:
        return new JfrSession(mode, config);
      case JVM_BUILTIN:
        return new JvmBuiltinSession(mode, config);
      default:
        throw new ProfilingException("Unsupported profiling tool: " + tool);
    }
  }

  private ProfilingConfig getRecommendedConfig(final ProfilingTool tool, final ProfilingMode mode) {
    final ProfilingConfig.Builder builder = ProfilingConfig.builder();

    switch (tool) {
      case ASYNC_PROFILER:
        return builder
            .samplingInterval(Duration.ofMillis(10)) // 10ms for good resolution
            .outputFormat(ExportFormat.FLAME_GRAPH)
            .includeSystemThreads(false)
            .build();

      case JAVA_FLIGHT_RECORDER:
        return builder
            .samplingInterval(Duration.ofMillis(20)) // 20ms default for JFR
            .outputFormat(ExportFormat.JFR)
            .includeSystemThreads(true)
            .enableGcAnalysis(true)
            .build();

      case JVM_BUILTIN:
        return builder
            .samplingInterval(Duration.ofMillis(100)) // Less frequent for lower overhead
            .outputFormat(ExportFormat.JSON)
            .includeSystemThreads(false)
            .build();

      default:
        return ProfilingConfig.defaultConfig();
    }
  }

  private String getToolSpecificAdvice(final ProfilingTool tool, final ProfilingMode mode) {
    switch (tool) {
      case ASYNC_PROFILER:
        return "async-profiler provides excellent low-overhead CPU and allocation profiling. " +
               "Ensure the native library is properly installed for your platform.";

      case JAVA_FLIGHT_RECORDER:
        return "JFR provides comprehensive system profiling with minimal overhead. " +
               "Consider using predefined profiles for common scenarios.";

      case JVM_BUILTIN:
        return "Built-in JVM tools provide basic profiling capabilities. " +
               "Consider upgrading to async-profiler or enabling JFR for better insights.";

      default:
        return "No specific advice available for this tool.";
    }
  }

  void removeSession(final ActiveSession session) {
    activeSessions.remove(session);
  }

  /**
   * Available profiling tools.
   */
  public enum ProfilingTool {
    ASYNC_PROFILER("async-profiler", "Low-overhead sampling profiler"),
    JAVA_FLIGHT_RECORDER("JFR", "Built-in JDK profiling"),
    JVM_BUILTIN("JVM Built-in", "Standard JVM MXBeans");

    private final String displayName;
    private final String description;

    ProfilingTool(final String displayName, final String description) {
      this.displayName = displayName;
      this.description = description;
    }

    public String getDisplayName() {
      return displayName;
    }

    public String getDescription() {
      return description;
    }
  }

  /**
   * Profiling modes for different analysis needs.
   */
  public enum ProfilingMode {
    CPU_SAMPLING("CPU Sampling", "Profile CPU usage and hot methods"),
    MEMORY_ALLOCATION("Memory Allocation", "Track memory allocation patterns"),
    FULL_SYSTEM("Full System", "Comprehensive system profiling"),
    LOW_OVERHEAD("Low Overhead", "Minimal impact profiling");

    private final String displayName;
    private final String description;

    ProfilingMode(final String displayName, final String description) {
      this.displayName = displayName;
      this.description = description;
    }

    public String getDisplayName() {
      return displayName;
    }

    public String getDescription() {
      return description;
    }
  }

  /**
   * Configuration for profiling sessions.
   */
  public static final class ProfilingConfig {
    private final Duration samplingInterval;
    private final ExportFormat outputFormat;
    private final boolean includeSystemThreads;
    private final boolean enableGcAnalysis;
    private final Path outputDirectory;
    private final int maxSamples;

    private ProfilingConfig(final Builder builder) {
      this.samplingInterval = builder.samplingInterval;
      this.outputFormat = builder.outputFormat;
      this.includeSystemThreads = builder.includeSystemThreads;
      this.enableGcAnalysis = builder.enableGcAnalysis;
      this.outputDirectory = builder.outputDirectory;
      this.maxSamples = builder.maxSamples;
    }

    public static Builder builder() {
      return new Builder();
    }

    public static ProfilingConfig defaultConfig() {
      return builder().build();
    }

    // Getters
    public Duration getSamplingInterval() { return samplingInterval; }
    public ExportFormat getOutputFormat() { return outputFormat; }
    public boolean isIncludeSystemThreads() { return includeSystemThreads; }
    public boolean isEnableGcAnalysis() { return enableGcAnalysis; }
    public Path getOutputDirectory() { return outputDirectory; }
    public int getMaxSamples() { return maxSamples; }

    public static final class Builder {
      private Duration samplingInterval = Duration.ofMillis(20);
      private ExportFormat outputFormat = ExportFormat.JSON;
      private boolean includeSystemThreads = false;
      private boolean enableGcAnalysis = false;
      private Path outputDirectory = Paths.get(System.getProperty("java.io.tmpdir"), "wasmtime4j-profiling");
      private int maxSamples = 100000;

      public Builder samplingInterval(final Duration interval) {
        this.samplingInterval = interval;
        return this;
      }

      public Builder outputFormat(final ExportFormat format) {
        this.outputFormat = format;
        return this;
      }

      public Builder includeSystemThreads(final boolean include) {
        this.includeSystemThreads = include;
        return this;
      }

      public Builder enableGcAnalysis(final boolean enable) {
        this.enableGcAnalysis = enable;
        return this;
      }

      public Builder outputDirectory(final Path directory) {
        this.outputDirectory = directory;
        return this;
      }

      public Builder maxSamples(final int max) {
        this.maxSamples = max;
        return this;
      }

      public ProfilingConfig build() {
        return new ProfilingConfig(this);
      }
    }
  }

  /**
   * Recommendations for optimal profiling setup.
   */
  public static final class ProfilingRecommendations {
    private final ProfilingTool recommendedTool;
    private final ProfilingConfig recommendedConfig;
    private final String advice;

    public ProfilingRecommendations(final ProfilingTool tool, final ProfilingConfig config, final String advice) {
      this.recommendedTool = tool;
      this.recommendedConfig = config;
      this.advice = advice;
    }

    public ProfilingTool getRecommendedTool() { return recommendedTool; }
    public ProfilingConfig getRecommendedConfig() { return recommendedConfig; }
    public String getAdvice() { return advice; }
  }

  /**
   * Exception thrown when profiling operations fail.
   */
  public static final class ProfilingException extends Exception {
    public ProfilingException(final String message) {
      super(message);
    }

    public ProfilingException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }

  /**
   * Base class for active profiling sessions.
   */
  abstract static class ActiveSession implements ProfileSession {
    protected final ProfilingMode mode;
    protected final ProfilingConfig config;
    protected final Instant startTime;
    protected volatile boolean active;

    protected ActiveSession(final ProfilingMode mode, final ProfilingConfig config) {
      this.mode = mode;
      this.config = config;
      this.startTime = Instant.now();
      this.active = false;
    }

    @Override
    public boolean isActive() {
      return active;
    }

    @Override
    public Duration getElapsedTime() {
      return Duration.between(startTime, Instant.now());
    }

    @Override
    public ProfilingMode getMode() {
      return mode;
    }

    @Override
    public ProfilingConfig getConfig() {
      return config;
    }

    protected abstract void doStart() throws ProfilingException;
    protected abstract ProfilingResult doStop() throws ProfilingException;

    public void start() throws ProfilingException {
      if (active) {
        throw new ProfilingException("Session is already active");
      }
      doStart();
      active = true;
    }

    @Override
    public ProfilingResult stop() throws ProfilingException {
      if (!active) {
        throw new ProfilingException("Session is not active");
      }
      try {
        return doStop();
      } finally {
        active = false;
      }
    }
  }

  /**
   * async-profiler integration session.
   */
  static class AsyncProfilerSession extends ActiveSession {
    private Object asyncProfilerInstance;

    AsyncProfilerSession(final ProfilingMode mode, final ProfilingConfig config) {
      super(mode, config);
    }

    @Override
    protected void doStart() throws ProfilingException {
      try {
        final Class<?> asyncProfilerClass = Class.forName(ASYNC_PROFILER_CLASS);
        asyncProfilerInstance = asyncProfilerClass.getMethod("getInstance").invoke(null);

        // Build profiling command based on mode
        final String command = buildAsyncProfilerCommand();

        // Start profiling
        asyncProfilerClass.getMethod("execute", String.class).invoke(asyncProfilerInstance, command);

      } catch (final Exception e) {
        throw new ProfilingException("Failed to start async-profiler", e);
      }
    }

    @Override
    protected ProfilingResult doStop() throws ProfilingException {
      try {
        final Class<?> asyncProfilerClass = Class.forName(ASYNC_PROFILER_CLASS);

        // Create output file path
        final Path outputFile = config.getOutputDirectory().resolve(
            "profile-" + System.currentTimeMillis() + "." + config.getOutputFormat().getFileExtension());

        // Ensure output directory exists
        Files.createDirectories(config.getOutputDirectory());

        // Stop profiling and save results
        final String stopCommand = "stop,file=" + outputFile.toString();
        asyncProfilerClass.getMethod("execute", String.class).invoke(asyncProfilerInstance, stopCommand);

        return new AsyncProfilerResult(outputFile, config.getOutputFormat(), getElapsedTime());

      } catch (final Exception e) {
        throw new ProfilingException("Failed to stop async-profiler", e);
      }
    }

    private String buildAsyncProfilerCommand() {
      final StringBuilder command = new StringBuilder("start");

      // Add event type based on mode
      switch (mode) {
        case CPU_SAMPLING:
          command.append(",event=cpu");
          break;
        case MEMORY_ALLOCATION:
          command.append(",event=alloc");
          break;
        default:
          command.append(",event=cpu");
      }

      // Add sampling interval
      final long intervalMs = config.getSamplingInterval().toMillis();
      command.append(",interval=").append(intervalMs).append("ms");

      // Add thread filter
      if (!config.isIncludeSystemThreads()) {
        command.append(",threads");
      }

      return command.toString();
    }
  }

  /**
   * Java Flight Recorder integration session.
   */
  static class JfrSession extends ActiveSession {
    private Object recording;

    JfrSession(final ProfilingMode mode, final ProfilingConfig config) {
      super(mode, config);
    }

    @Override
    protected void doStart() throws ProfilingException {
      try {
        final Class<?> recordingClass = Class.forName(JFR_RECORDING_CLASS);
        recording = recordingClass.getDeclaredConstructor().newInstance();

        // Configure recording based on mode
        configureJfrRecording();

        // Start recording
        recordingClass.getMethod("start").invoke(recording);

      } catch (final Exception e) {
        throw new ProfilingException("Failed to start JFR recording", e);
      }
    }

    @Override
    protected ProfilingResult doStop() throws ProfilingException {
      try {
        final Class<?> recordingClass = Class.forName(JFR_RECORDING_CLASS);

        // Stop recording
        recordingClass.getMethod("stop").invoke(recording);

        // Create output file
        final Path outputFile = config.getOutputDirectory().resolve(
            "profile-" + System.currentTimeMillis() + ".jfr");
        Files.createDirectories(config.getOutputDirectory());

        // Dump recording to file
        recordingClass.getMethod("dump", Path.class).invoke(recording, outputFile);

        // Close recording
        recordingClass.getMethod("close").invoke(recording);

        return new JfrResult(outputFile, getElapsedTime());

      } catch (final Exception e) {
        throw new ProfilingException("Failed to stop JFR recording", e);
      }
    }

    private void configureJfrRecording() throws Exception {
      // This would configure JFR settings based on the profiling mode
      // Implementation details would depend on specific JFR API usage
    }
  }

  /**
   * JVM built-in tools session.
   */
  static class JvmBuiltinSession extends ActiveSession {
    private CompletableFuture<ProfilingResult> samplingTask;

    JvmBuiltinSession(final ProfilingMode mode, final ProfilingConfig config) {
      super(mode, config);
    }

    @Override
    protected void doStart() throws ProfilingException {
      // Start background sampling task
      samplingTask = CompletableFuture.supplyAsync(() -> {
        try {
          return collectBuiltinMetrics();
        } catch (final Exception e) {
          throw new RuntimeException(e);
        }
      });
    }

    @Override
    protected ProfilingResult doStop() throws ProfilingException {
      try {
        return samplingTask.get(30, TimeUnit.SECONDS);
      } catch (final Exception e) {
        throw new ProfilingException("Failed to collect built-in metrics", e);
      }
    }

    private ProfilingResult collectBuiltinMetrics() {
      // Collect metrics using JVM MXBeans
      // This is a simplified implementation
      final Path outputFile = config.getOutputDirectory().resolve(
          "builtin-metrics-" + System.currentTimeMillis() + ".json");

      try {
        Files.createDirectories(config.getOutputDirectory());
        Files.writeString(outputFile, "{}"); // Placeholder
        return new JvmBuiltinResult(outputFile, getElapsedTime());
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Base class for profiling results.
   */
  abstract static class BaseProfilingResult implements ProfilingResult {
    protected final Path outputFile;
    protected final Duration profilingDuration;

    protected BaseProfilingResult(final Path outputFile, final Duration duration) {
      this.outputFile = outputFile;
      this.profilingDuration = duration;
    }

    @Override
    public Duration getProfilingDuration() {
      return profilingDuration;
    }

    @Override
    public boolean hasResults() {
      return Files.exists(outputFile) && outputFile.toFile().length() > 0;
    }

    @Override
    public Path getOutputFile() {
      return outputFile;
    }
  }

  static class AsyncProfilerResult extends BaseProfilingResult {
    private final ExportFormat format;

    AsyncProfilerResult(final Path outputFile, final ExportFormat format, final Duration duration) {
      super(outputFile, duration);
      this.format = format;
    }

    @Override
    public String export(final ExportFormat exportFormat) throws ProfilingException {
      try {
        if (exportFormat == this.format) {
          return Files.readString(outputFile);
        } else {
          // Would need conversion logic for different formats
          throw new ProfilingException("Format conversion not implemented: " + exportFormat);
        }
      } catch (final IOException e) {
        throw new ProfilingException("Failed to read profiling results", e);
      }
    }
  }

  static class JfrResult extends BaseProfilingResult {
    JfrResult(final Path outputFile, final Duration duration) {
      super(outputFile, duration);
    }

    @Override
    public String export(final ExportFormat format) throws ProfilingException {
      if (format == ExportFormat.JFR) {
        try {
          return outputFile.toString(); // Return file path for binary format
        } catch (final Exception e) {
          throw new ProfilingException("Failed to export JFR results", e);
        }
      } else {
        throw new ProfilingException("JFR format conversion not implemented: " + format);
      }
    }
  }

  static class JvmBuiltinResult extends BaseProfilingResult {
    JvmBuiltinResult(final Path outputFile, final Duration duration) {
      super(outputFile, duration);
    }

    @Override
    public String export(final ExportFormat format) throws ProfilingException {
      try {
        if (format == ExportFormat.JSON) {
          return Files.readString(outputFile);
        } else {
          throw new ProfilingException("Built-in tools format conversion not implemented: " + format);
        }
      } catch (final IOException e) {
        throw new ProfilingException("Failed to read built-in metrics", e);
      }
    }
  }

  /**
   * Interface for active profiling sessions.
   */
  public interface ProfileSession {
    boolean isActive();
    Duration getElapsedTime();
    ProfilingMode getMode();
    ProfilingConfig getConfig();
    ProfilingResult stop() throws ProfilingException;
  }

  /**
   * Interface for profiling results.
   */
  public interface ProfilingResult {
    Duration getProfilingDuration();
    boolean hasResults();
    Path getOutputFile();
    String export(ExportFormat format) throws ProfilingException;
  }
}
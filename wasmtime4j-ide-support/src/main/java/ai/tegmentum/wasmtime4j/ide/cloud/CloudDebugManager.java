package ai.tegmentum.wasmtime4j.ide.cloud;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cloud debugging manager for remote WebAssembly instance management. Provides distributed
 * debugging capabilities with cloud-hosted WebAssembly instances.
 */
public final class CloudDebugManager {

  private static final Logger LOGGER = Logger.getLogger(CloudDebugManager.class.getName());
  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final ExecutorService executorService;
  private final Map<String, CloudDebugSession> activeSessions;
  private final CloudConnectionConfig config;
  private final ScheduledExecutorService heartbeatScheduler;

  /** Creates a new cloud debug manager with default configuration. */
  public CloudDebugManager() {
    this(new CloudConnectionConfig.Builder().build());
  }

  /**
   * Creates a new cloud debug manager with the specified configuration.
   *
   * @param config Cloud connection configuration
   */
  public CloudDebugManager(final CloudConnectionConfig config) {
    this.config = Objects.requireNonNull(config, "Config cannot be null");
    this.httpClient = HttpClient.newBuilder().connectTimeout(DEFAULT_TIMEOUT).build();
    this.objectMapper = new ObjectMapper();
    this.executorService = Executors.newCachedThreadPool();
    this.activeSessions = new ConcurrentHashMap<>();
    this.heartbeatScheduler = Executors.newScheduledThreadPool(1);

    // Start heartbeat for active sessions
    startHeartbeat();
  }

  /**
   * Creates a new cloud debug session for a WebAssembly module.
   *
   * @param moduleBytes WebAssembly module binary
   * @param sessionName Human-readable session name
   * @return Future containing cloud debug session
   */
  public CompletableFuture<CloudDebugSession> createDebugSession(
      final byte[] moduleBytes, final String sessionName) {
    Objects.requireNonNull(moduleBytes, "Module bytes cannot be null");
    Objects.requireNonNull(sessionName, "Session name cannot be null");

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            LOGGER.info("Creating cloud debug session: " + sessionName);

            // Create session request
            final CreateSessionRequest request =
                new CreateSessionRequest(
                    sessionName,
                    Base64.getEncoder().encodeToString(moduleBytes),
                    config.getRegion(),
                    config.getResourceLimits());

            // Send to cloud service
            final CreateSessionResponse response =
                sendRequest("/api/v1/debug/sessions", "POST", request, CreateSessionResponse.class);

            // Create local session representation
            final CloudDebugSession session =
                new CloudDebugSession(
                    response.sessionId,
                    sessionName,
                    response.endpointUrl,
                    response.accessToken,
                    this);

            activeSessions.put(response.sessionId, session);

            LOGGER.info("Cloud debug session created: " + response.sessionId);
            return session;

          } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create cloud debug session", e);
            throw new CloudDebugException("Failed to create cloud debug session", e);
          }
        },
        executorService);
  }

  /**
   * Gets an existing cloud debug session.
   *
   * @param sessionId Session identifier
   * @return Cloud debug session or null if not found
   */
  public CloudDebugSession getDebugSession(final String sessionId) {
    return activeSessions.get(sessionId);
  }

  /**
   * Lists all active cloud debug sessions.
   *
   * @return List of active sessions
   */
  public List<CloudDebugSession> getActiveSessions() {
    return new ArrayList<>(activeSessions.values());
  }

  /**
   * Closes a cloud debug session.
   *
   * @param sessionId Session identifier
   * @return Future that completes when session is closed
   */
  public CompletableFuture<Void> closeDebugSession(final String sessionId) {
    return CompletableFuture.runAsync(
        () -> {
          try {
            final CloudDebugSession session = activeSessions.remove(sessionId);
            if (session != null) {
              LOGGER.info("Closing cloud debug session: " + sessionId);

              // Send close request to cloud service
              sendRequest("/api/v1/debug/sessions/" + sessionId, "DELETE", null, Void.class);

              session.close();
              LOGGER.info("Cloud debug session closed: " + sessionId);
            }
          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to close cloud debug session: " + sessionId, e);
            throw new CloudDebugException("Failed to close cloud debug session", e);
          }
        },
        executorService);
  }

  /**
   * Executes a debug command on a remote instance.
   *
   * @param sessionId Session identifier
   * @param command Debug command to execute
   * @return Future containing command result
   */
  public CompletableFuture<DebugCommandResult> executeDebugCommand(
      final String sessionId, final DebugCommand command) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final CloudDebugSession session = activeSessions.get(sessionId);
            if (session == null) {
              throw new CloudDebugException("Session not found: " + sessionId);
            }

            LOGGER.fine(
                "Executing debug command: " + command.getType() + " on session " + sessionId);

            // Send command to cloud service
            final DebugCommandResult result =
                sendRequest(
                    "/api/v1/debug/sessions/" + sessionId + "/commands",
                    "POST",
                    command,
                    DebugCommandResult.class);

            LOGGER.fine("Debug command completed: " + command.getType());
            return result;

          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Debug command failed", e);
            throw new CloudDebugException("Debug command failed", e);
          }
        },
        executorService);
  }

  /**
   * Gets the current state of a remote WebAssembly instance.
   *
   * @param sessionId Session identifier
   * @return Future containing instance state
   */
  public CompletableFuture<RemoteInstanceState> getInstanceState(final String sessionId) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final CloudDebugSession session = activeSessions.get(sessionId);
            if (session == null) {
              throw new CloudDebugException("Session not found: " + sessionId);
            }

            final RemoteInstanceState state =
                sendRequest(
                    "/api/v1/debug/sessions/" + sessionId + "/state",
                    "GET",
                    null,
                    RemoteInstanceState.class);

            return state;

          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get instance state", e);
            throw new CloudDebugException("Failed to get instance state", e);
          }
        },
        executorService);
  }

  /** Shuts down the cloud debug manager and closes all sessions. */
  public void shutdown() {
    LOGGER.info("Shutting down cloud debug manager");

    // Close all active sessions
    final List<CompletableFuture<Void>> closeFutures = new ArrayList<>();
    for (final String sessionId : activeSessions.keySet()) {
      closeFutures.add(closeDebugSession(sessionId));
    }

    // Wait for all sessions to close
    CompletableFuture.allOf(closeFutures.toArray(new CompletableFuture[0]))
        .orTimeout(10, TimeUnit.SECONDS)
        .join();

    // Shutdown thread pools
    heartbeatScheduler.shutdown();
    executorService.shutdown();

    try {
      if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }
      if (!heartbeatScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        heartbeatScheduler.shutdownNow();
      }
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    LOGGER.info("Cloud debug manager shutdown complete");
  }

  private void startHeartbeat() {
    heartbeatScheduler.scheduleAtFixedRate(
        () -> {
          for (final CloudDebugSession session : activeSessions.values()) {
            try {
              // Send heartbeat to keep session alive
              final HeartbeatRequest heartbeat = new HeartbeatRequest(session.getSessionId());
              sendRequest(
                  "/api/v1/debug/sessions/" + session.getSessionId() + "/heartbeat",
                  "POST",
                  heartbeat,
                  Void.class);

            } catch (final Exception e) {
              LOGGER.log(
                  Level.WARNING, "Heartbeat failed for session: " + session.getSessionId(), e);
              // Remove failed sessions
              activeSessions.remove(session.getSessionId());
              session.close();
            }
          }
        },
        30,
        30,
        TimeUnit.SECONDS);
  }

  private <T> T sendRequest(
      final String path, final String method, final Object requestBody, final Class<T> responseType)
      throws Exception {
    final String url = config.getBaseUrl() + path;

    HttpRequest.Builder requestBuilder =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(DEFAULT_TIMEOUT)
            .header("Authorization", "Bearer " + config.getApiKey())
            .header("Content-Type", "application/json");

    // Add request body for POST/PUT
    if (requestBody != null) {
      final String json = objectMapper.writeValueAsString(requestBody);
      requestBuilder = requestBuilder.method(method, HttpRequest.BodyPublishers.ofString(json));
    } else {
      requestBuilder = requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
    }

    final HttpRequest request = requestBuilder.build();
    final HttpResponse<String> response =
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() >= 400) {
      throw new CloudDebugException("HTTP error " + response.statusCode() + ": " + response.body());
    }

    if (responseType == Void.class) {
      return null;
    }

    return objectMapper.readValue(response.body(), responseType);
  }

  // Request/Response classes for cloud API

  private static final class CreateSessionRequest {
    public final String sessionName;
    public final String moduleBase64;
    public final String region;
    public final ResourceLimits resourceLimits;

    public CreateSessionRequest(
        final String sessionName,
        final String moduleBase64,
        final String region,
        final ResourceLimits resourceLimits) {
      this.sessionName = sessionName;
      this.moduleBase64 = moduleBase64;
      this.region = region;
      this.resourceLimits = resourceLimits;
    }
  }

  private static final class CreateSessionResponse {
    public String sessionId;
    public String endpointUrl;
    public String accessToken;
    public long expiresAt;
  }

  private static final class HeartbeatRequest {
    public final String sessionId;
    public final long timestamp;

    public HeartbeatRequest(final String sessionId) {
      this.sessionId = sessionId;
      this.timestamp = System.currentTimeMillis();
    }
  }

  /** Configuration for cloud debugging connections. */
  public static final class CloudConnectionConfig {
    private final String baseUrl;
    private final String apiKey;
    private final String region;
    private final ResourceLimits resourceLimits;

    private CloudConnectionConfig(final Builder builder) {
      this.baseUrl = builder.baseUrl;
      this.apiKey = builder.apiKey;
      this.region = builder.region;
      this.resourceLimits = builder.resourceLimits;
    }

    public String getBaseUrl() {
      return baseUrl;
    }

    public String getApiKey() {
      return apiKey;
    }

    public String getRegion() {
      return region;
    }

    public ResourceLimits getResourceLimits() {
      return resourceLimits;
    }

    public static final class Builder {
      private String baseUrl = "https://api.wasmtime4j-cloud.example.com";
      private String apiKey = System.getProperty("wasmtime4j.cloud.apikey", "");
      private String region = "us-east-1";
      private ResourceLimits resourceLimits = new ResourceLimits();

      public Builder baseUrl(final String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
      }

      public Builder apiKey(final String apiKey) {
        this.apiKey = apiKey;
        return this;
      }

      public Builder region(final String region) {
        this.region = region;
        return this;
      }

      public Builder resourceLimits(final ResourceLimits resourceLimits) {
        this.resourceLimits = resourceLimits;
        return this;
      }

      public CloudConnectionConfig build() {
        return new CloudConnectionConfig(this);
      }
    }
  }

  /** Resource limits for cloud debugging sessions. */
  public static final class ResourceLimits {
    public final long maxMemoryBytes;
    public final long maxCpuTimeMs;
    public final int maxFunctionCalls;
    public final long sessionTimeoutMs;

    public ResourceLimits() {
      this(64 * 1024 * 1024, 10000, 1000000, 3600000); // 64MB, 10s CPU, 1M calls, 1h timeout
    }

    public ResourceLimits(
        final long maxMemoryBytes,
        final long maxCpuTimeMs,
        final int maxFunctionCalls,
        final long sessionTimeoutMs) {
      this.maxMemoryBytes = maxMemoryBytes;
      this.maxCpuTimeMs = maxCpuTimeMs;
      this.maxFunctionCalls = maxFunctionCalls;
      this.sessionTimeoutMs = sessionTimeoutMs;
    }
  }

  /** Debug command to be executed on remote instance. */
  public static final class DebugCommand {
    private final DebugCommandType type;
    private final Map<String, Object> parameters;

    public DebugCommand(final DebugCommandType type, final Map<String, Object> parameters) {
      this.type = Objects.requireNonNull(type, "Type cannot be null");
      this.parameters = Objects.requireNonNull(parameters, "Parameters cannot be null");
    }

    public DebugCommandType getType() {
      return type;
    }

    public Map<String, Object> getParameters() {
      return parameters;
    }

    public enum DebugCommandType {
      SET_BREAKPOINT,
      REMOVE_BREAKPOINT,
      STEP_OVER,
      STEP_INTO,
      STEP_OUT,
      CONTINUE,
      PAUSE,
      EVALUATE_EXPRESSION,
      GET_STACK_TRACE,
      GET_VARIABLES,
      READ_MEMORY,
      WRITE_MEMORY
    }
  }

  /** Result of executing a debug command. */
  public static final class DebugCommandResult {
    public boolean success;
    public String message;
    public Map<String, Object> data;
    public long executionTimeMs;
  }

  /** State of a remote WebAssembly instance. */
  public static final class RemoteInstanceState {
    public String instanceId;
    public String status; // RUNNING, PAUSED, STOPPED, ERROR
    public long currentInstructionPointer;
    public Map<String, Object> callStack;
    public Map<String, Object> localVariables;
    public Map<String, Object> globalVariables;
    public Map<String, Object> memoryRegions;
    public List<String> activeBreakpoints;
    public long lastUpdateTime;
  }

  /** Exception thrown when cloud debugging operations fail. */
  public static final class CloudDebugException extends RuntimeException {
    public CloudDebugException(final String message) {
      super(message);
    }

    public CloudDebugException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}

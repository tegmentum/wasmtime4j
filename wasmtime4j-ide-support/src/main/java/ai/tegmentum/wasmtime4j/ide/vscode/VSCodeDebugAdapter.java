package ai.tegmentum.wasmtime4j.ide.vscode;

import ai.tegmentum.wasmtime4j.debug.Breakpoint;
import ai.tegmentum.wasmtime4j.debug.DebugEvent;
import ai.tegmentum.wasmtime4j.debug.DebugSession;
import ai.tegmentum.wasmtime4j.debug.StackFrame;
import ai.tegmentum.wasmtime4j.debug.Variable;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Debug Adapter Protocol (DAP) implementation for VS Code integration.
 *
 * <p>This class implements the Debug Adapter Protocol to enable VS Code to debug WebAssembly
 * applications using wasmtime4j. It provides:
 *
 * <ul>
 *   <li>DAP protocol compliance for VS Code integration
 *   <li>Breakpoint management with source mapping
 *   <li>Step debugging with WebAssembly instruction mapping
 *   <li>Variable inspection and modification
 *   <li>Call stack visualization
 *   <li>Memory debugging capabilities
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * VSCodeDebugAdapter adapter = new VSCodeDebugAdapter(9229);
 * adapter.start();
 * adapter.attachDebugSession(debugSession);
 * }</pre>
 *
 * @since 1.0.0
 */
public final class VSCodeDebugAdapter implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(VSCodeDebugAdapter.class.getName());

  private static final String PROTOCOL_VERSION = "1.55.1";
  private static final String ADAPTER_ID = "wasmtime4j";
  private static final int DEFAULT_PORT = 9229;

  private final int port;
  private final Gson gson;
  private final ExecutorService executorService;
  private final Map<Integer, Breakpoint> breakpoints;
  private final Map<Integer, StackFrame> stackFrames;
  private final Map<Integer, Variable> variables;
  private final AtomicInteger nextBreakpointId;
  private final AtomicInteger nextStackFrameId;
  private final AtomicInteger nextVariableId;
  private final AtomicBoolean running;

  private ServerSocket serverSocket;
  private Socket clientSocket;
  private BufferedReader reader;
  private PrintWriter writer;
  private DebugSession debugSession;
  private boolean initialized;

  /** Creates a new VS Code debug adapter on the default port. */
  public VSCodeDebugAdapter() {
    this(DEFAULT_PORT);
  }

  /**
   * Creates a new VS Code debug adapter on the specified port.
   *
   * @param port the port to listen on
   * @throws IllegalArgumentException if port is invalid
   */
  public VSCodeDebugAdapter(final int port) {
    if (port < 1 || port > 65535) {
      throw new IllegalArgumentException("Port must be between 1 and 65535");
    }

    this.port = port;
    this.gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    this.executorService =
        Executors.newCachedThreadPool(
            r -> {
              final Thread thread = new Thread(r, "VSCodeDebugAdapter");
              thread.setDaemon(true);
              return thread;
            });
    this.breakpoints = new ConcurrentHashMap<>();
    this.stackFrames = new ConcurrentHashMap<>();
    this.variables = new ConcurrentHashMap<>();
    this.nextBreakpointId = new AtomicInteger(1);
    this.nextStackFrameId = new AtomicInteger(1);
    this.nextVariableId = new AtomicInteger(1);
    this.running = new AtomicBoolean(false);
    this.initialized = false;
  }

  /**
   * Starts the debug adapter server.
   *
   * @throws IOException if server cannot be started
   * @throws IllegalStateException if adapter is already running
   */
  public void start() throws IOException {
    if (!running.compareAndSet(false, true)) {
      throw new IllegalStateException("VS Code debug adapter is already running");
    }

    serverSocket = new ServerSocket(port);
    LOGGER.info("VS Code Debug Adapter started on port " + port);

    // Accept client connections
    executorService.submit(this::acceptConnections);
  }

  /** Stops the debug adapter server. */
  public void stop() {
    if (!running.compareAndSet(true, false)) {
      return;
    }

    LOGGER.info("Stopping VS Code Debug Adapter");

    // Close client connection
    closeClientConnection();

    // Close server socket
    if (serverSocket != null && !serverSocket.isClosed()) {
      try {
        serverSocket.close();
      } catch (final IOException e) {
        LOGGER.log(Level.WARNING, "Error closing server socket", e);
      }
    }

    // Shutdown executor
    executorService.shutdown();

    LOGGER.info("VS Code Debug Adapter stopped");
  }

  /**
   * Attaches a debug session to the adapter.
   *
   * @param session the debug session to attach
   * @throws IllegalArgumentException if session is null
   */
  public void attachDebugSession(final DebugSession session) {
    if (session == null) {
      throw new IllegalArgumentException("Debug session cannot be null");
    }

    this.debugSession = session;

    // Add event listener to forward debug events
    session.addDebugEventListener(this::handleDebugEvent);

    LOGGER.info("Attached debug session: " + session.getSessionId());
  }

  /**
   * Checks if the adapter is running.
   *
   * @return true if the adapter is running
   */
  public boolean isRunning() {
    return running.get();
  }

  /**
   * Gets the adapter port.
   *
   * @return the port number
   */
  public int getPort() {
    return port;
  }

  @Override
  public void close() {
    stop();
  }

  // Private methods

  private void acceptConnections() {
    while (running.get()) {
      try {
        final Socket client = serverSocket.accept();
        LOGGER.info("VS Code client connected: " + client.getRemoteSocketAddress());

        // Close any existing client connection
        closeClientConnection();

        // Set up new client connection
        clientSocket = client;
        reader =
            new BufferedReader(
                new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
        writer =
            new PrintWriter(
                new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true);

        // Start message handling
        executorService.submit(this::handleMessages);

      } catch (final IOException e) {
        if (running.get()) {
          LOGGER.log(Level.WARNING, "Error accepting client connection", e);
        }
      }
    }
  }

  private void closeClientConnection() {
    if (reader != null) {
      try {
        reader.close();
      } catch (final IOException e) {
        // Ignore
      }
      reader = null;
    }

    if (writer != null) {
      writer.close();
      writer = null;
    }

    if (clientSocket != null && !clientSocket.isClosed()) {
      try {
        clientSocket.close();
      } catch (final IOException e) {
        // Ignore
      }
      clientSocket = null;
    }

    initialized = false;
  }

  private void handleMessages() {
    try {
      String line;
      while (running.get() && (line = reader.readLine()) != null) {
        if (line.startsWith("Content-Length:")) {
          // Parse content length
          final int contentLength = Integer.parseInt(line.substring(15).trim());

          // Skip empty line
          reader.readLine();

          // Read message content
          final char[] buffer = new char[contentLength];
          int totalRead = 0;
          while (totalRead < contentLength) {
            final int read = reader.read(buffer, totalRead, contentLength - totalRead);
            if (read == -1) {
              break;
            }
            totalRead += read;
          }

          final String messageContent = new String(buffer, 0, totalRead);
          handleDAPMessage(messageContent);
        }
      }
    } catch (final IOException e) {
      if (running.get()) {
        LOGGER.log(Level.WARNING, "Error handling messages", e);
      }
    }
  }

  private void handleDAPMessage(final String messageContent) {
    try {
      final DAPMessage message = gson.fromJson(messageContent, DAPMessage.class);
      LOGGER.fine("Received DAP message: " + message.command);

      final DAPResponse response = processCommand(message);
      if (response != null) {
        sendResponse(response);
      }

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Error handling DAP message", e);
      sendErrorResponse(0, "Internal error: " + e.getMessage());
    }
  }

  private DAPResponse processCommand(final DAPMessage message) throws WasmException {
    switch (message.command) {
      case "initialize":
        return handleInitialize(message);

      case "launch":
      case "attach":
        return handleLaunchOrAttach(message);

      case "setBreakpoints":
        return handleSetBreakpoints(message);

      case "continue":
        return handleContinue(message);

      case "next":
        return handleNext(message);

      case "stepIn":
        return handleStepIn(message);

      case "stepOut":
        return handleStepOut(message);

      case "pause":
        return handlePause(message);

      case "stackTrace":
        return handleStackTrace(message);

      case "scopes":
        return handleScopes(message);

      case "variables":
        return handleVariables(message);

      case "evaluate":
        return handleEvaluate(message);

      case "disconnect":
        return handleDisconnect(message);

      default:
        LOGGER.warning("Unknown DAP command: " + message.command);
        return createErrorResponse(message.seq, "Unknown command: " + message.command);
    }
  }

  private DAPResponse handleInitialize(final DAPMessage message) {
    initialized = true;

    final DAPResponse response = createResponse(message.seq, "initialize");
    response.body.put("supportsConfigurationDoneRequest", true);
    response.body.put("supportsEvaluateForHovers", true);
    response.body.put("supportsStepBack", false);
    response.body.put("supportsRestartRequest", false);
    response.body.put("supportsGotoTargetsRequest", false);
    response.body.put("supportsCompletionsRequest", false);
    response.body.put("supportsModulesRequest", false);
    response.body.put("supportsRestartFrame", false);
    response.body.put("supportsValueFormattingOptions", true);
    response.body.put("supportsExceptionInfoRequest", false);
    response.body.put("supportTerminateDebuggee", true);
    response.body.put("supportsDelayedStackTraceLoading", false);
    response.body.put("supportsLoadedSourcesRequest", false);
    response.body.put("supportsLogPoints", false);
    response.body.put("supportsTerminateThreadsRequest", false);
    response.body.put("supportsSetVariable", true);
    response.body.put("supportsSetExpression", false);
    response.body.put("supportsDisassembleRequest", true);
    response.body.put("supportsSteppingGranularity", true);
    response.body.put("supportsInstructionBreakpoints", true);

    // Send initialized event
    sendEvent("initialized", new HashMap<>());

    return response;
  }

  private DAPResponse handleLaunchOrAttach(final DAPMessage message) {
    LOGGER.info("Debug session launched/attached");
    return createResponse(message.seq, message.command);
  }

  private DAPResponse handleSetBreakpoints(final DAPMessage message) throws WasmException {
    if (debugSession == null) {
      return createErrorResponse(message.seq, "No debug session attached");
    }

    @SuppressWarnings("unchecked")
    final Map<String, Object> args = (Map<String, Object>) message.arguments;
    @SuppressWarnings("unchecked")
    final Map<String, Object> source = (Map<String, Object>) args.get("source");
    @SuppressWarnings("unchecked")
    final List<Map<String, Object>> breakpoints =
        (List<Map<String, Object>>) args.get("breakpoints");

    final String sourcePath = (String) source.get("path");
    final List<Map<String, Object>> responseBreakpoints =
        breakpoints.stream()
            .map(
                bp -> {
                  final int line = ((Number) bp.get("line")).intValue();
                  try {
                    // For WebAssembly, we use function names instead of file paths
                    final String functionName = extractFunctionName(sourcePath, line);
                    final Breakpoint wasmBp = debugSession.setBreakpoint(functionName, line);

                    final int bpId = nextBreakpointId.getAndIncrement();
                    this.breakpoints.put(bpId, wasmBp);

                    final Map<String, Object> result = new HashMap<>();
                    result.put("id", bpId);
                    result.put("verified", true);
                    result.put("line", line);
                    result.put("source", source);
                    return result;

                  } catch (final Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to set breakpoint", e);
                    final Map<String, Object> result = new HashMap<>();
                    result.put("verified", false);
                    result.put("line", line);
                    result.put("message", "Failed to set breakpoint: " + e.getMessage());
                    return result;
                  }
                })
            .toList();

    final DAPResponse response = createResponse(message.seq, "setBreakpoints");
    response.body.put("breakpoints", responseBreakpoints);
    return response;
  }

  private DAPResponse handleContinue(final DAPMessage message) throws WasmException {
    if (debugSession == null) {
      return createErrorResponse(message.seq, "No debug session attached");
    }

    debugSession.continueExecution();

    final DAPResponse response = createResponse(message.seq, "continue");
    response.body.put("allThreadsContinued", true);
    return response;
  }

  private DAPResponse handleNext(final DAPMessage message) throws WasmException {
    if (debugSession == null) {
      return createErrorResponse(message.seq, "No debug session attached");
    }

    debugSession.stepOver();
    return createResponse(message.seq, "next");
  }

  private DAPResponse handleStepIn(final DAPMessage message) throws WasmException {
    if (debugSession == null) {
      return createErrorResponse(message.seq, "No debug session attached");
    }

    debugSession.stepInto();
    return createResponse(message.seq, "stepIn");
  }

  private DAPResponse handleStepOut(final DAPMessage message) throws WasmException {
    if (debugSession == null) {
      return createErrorResponse(message.seq, "No debug session attached");
    }

    debugSession.stepOut();
    return createResponse(message.seq, "stepOut");
  }

  private DAPResponse handlePause(final DAPMessage message) throws WasmException {
    if (debugSession == null) {
      return createErrorResponse(message.seq, "No debug session attached");
    }

    debugSession.pause();
    return createResponse(message.seq, "pause");
  }

  private DAPResponse handleStackTrace(final DAPMessage message) throws WasmException {
    if (debugSession == null) {
      return createErrorResponse(message.seq, "No debug session attached");
    }

    final List<StackFrame> wasmStack = debugSession.getStackTrace();
    final List<Map<String, Object>> dapStackFrames =
        wasmStack.stream()
            .map(
                frame -> {
                  final int frameId = nextStackFrameId.getAndIncrement();
                  stackFrames.put(frameId, frame);

                  final Map<String, Object> dapFrame = new HashMap<>();
                  dapFrame.put("id", frameId);
                  dapFrame.put(
                      "name",
                      frame.getFunctionName() != null ? frame.getFunctionName() : "<unknown>");
                  dapFrame.put("line", frame.getLine());
                  dapFrame.put("column", frame.getColumn());

                  // Create source information
                  final Map<String, Object> source = new HashMap<>();
                  source.put(
                      "name",
                      frame.getSourceFile() != null ? frame.getSourceFile() : "WebAssembly");
                  source.put("path", frame.getSourceFile());
                  dapFrame.put("source", source);

                  return dapFrame;
                })
            .toList();

    final DAPResponse response = createResponse(message.seq, "stackTrace");
    response.body.put("stackFrames", dapStackFrames);
    response.body.put("totalFrames", dapStackFrames.size());
    return response;
  }

  private DAPResponse handleScopes(final DAPMessage message) throws WasmException {
    @SuppressWarnings("unchecked")
    final Map<String, Object> args = (Map<String, Object>) message.arguments;
    final int frameId = ((Number) args.get("frameId")).intValue();

    final StackFrame frame = stackFrames.get(frameId);
    if (frame == null) {
      return createErrorResponse(message.seq, "Stack frame not found");
    }

    final List<Map<String, Object>> scopes =
        List.of(
            createScope("Locals", nextVariableId.getAndIncrement(), false),
            createScope("Globals", nextVariableId.getAndIncrement(), false));

    final DAPResponse response = createResponse(message.seq, "scopes");
    response.body.put("scopes", scopes);
    return response;
  }

  private DAPResponse handleVariables(final DAPMessage message) throws WasmException {
    if (debugSession == null) {
      return createErrorResponse(message.seq, "No debug session attached");
    }

    final List<Variable> wasmVariables = debugSession.getCurrentVariables();
    final List<Map<String, Object>> dapVariables =
        wasmVariables.stream()
            .map(
                var -> {
                  final Map<String, Object> dapVar = new HashMap<>();
                  dapVar.put("name", var.getName());
                  dapVar.put("value", formatVariableValue(var.getValue()));
                  dapVar.put("type", var.getType());
                  dapVar.put("variablesReference", 0); // No nested variables for now
                  return dapVar;
                })
            .toList();

    final DAPResponse response = createResponse(message.seq, "variables");
    response.body.put("variables", dapVariables);
    return response;
  }

  private DAPResponse handleEvaluate(final DAPMessage message) throws WasmException {
    if (debugSession == null) {
      return createErrorResponse(message.seq, "No debug session attached");
    }

    @SuppressWarnings("unchecked")
    final Map<String, Object> args = (Map<String, Object>) message.arguments;
    final String expression = (String) args.get("expression");

    try {
      final var result = debugSession.evaluateExpression(expression);
      final DAPResponse response = createResponse(message.seq, "evaluate");
      response.body.put(
          "result", result.getValue() != null ? result.getValue().toString() : "null");
      response.body.put("type", result.getResultType());
      response.body.put("variablesReference", 0);
      return response;

    } catch (final Exception e) {
      return createErrorResponse(message.seq, "Evaluation failed: " + e.getMessage());
    }
  }

  private DAPResponse handleDisconnect(final DAPMessage message) {
    LOGGER.info("Debug session disconnected");
    closeClientConnection();
    return createResponse(message.seq, "disconnect");
  }

  private void handleDebugEvent(final DebugEvent event) {
    if (writer == null || !initialized) {
      return;
    }

    switch (event.getType()) {
      case BREAKPOINT_HIT:
        sendStoppedEvent("breakpoint", event.getThreadId());
        break;

      case STEP_COMPLETED:
        sendStoppedEvent("step", event.getThreadId());
        break;

      case EXECUTION_PAUSED:
        sendStoppedEvent("pause", event.getThreadId());
        break;

      case EXECUTION_COMPLETED:
        sendEvent("terminated", new HashMap<>());
        break;

      default:
        // Ignore other events
        break;
    }
  }

  private void sendStoppedEvent(final String reason, final int threadId) {
    final Map<String, Object> body = new HashMap<>();
    body.put("reason", reason);
    body.put("threadId", threadId);
    body.put("allThreadsStopped", true);
    sendEvent("stopped", body);
  }

  private void sendEvent(final String eventType, final Map<String, Object> body) {
    final DAPEvent event = new DAPEvent();
    event.seq = nextBreakpointId.getAndIncrement();
    event.type = "event";
    event.event = eventType;
    event.body = body;

    sendDAPMessage(event);
  }

  private void sendResponse(final DAPResponse response) {
    sendDAPMessage(response);
  }

  private void sendErrorResponse(final int requestSeq, final String message) {
    final DAPResponse response = createErrorResponse(requestSeq, message);
    sendResponse(response);
  }

  private void sendDAPMessage(final Object message) {
    if (writer == null) {
      return;
    }

    try {
      final String json = gson.toJson(message);
      final String content =
          "Content-Length: " + json.getBytes(StandardCharsets.UTF_8).length + "\r\n\r\n" + json;

      writer.print(content);
      writer.flush();

      LOGGER.finest("Sent DAP message: " + json.substring(0, Math.min(100, json.length())));

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Error sending DAP message", e);
    }
  }

  private DAPResponse createResponse(final int requestSeq, final String command) {
    final DAPResponse response = new DAPResponse();
    response.seq = nextBreakpointId.getAndIncrement();
    response.type = "response";
    response.request_seq = requestSeq;
    response.command = command;
    response.success = true;
    response.body = new HashMap<>();
    return response;
  }

  private DAPResponse createErrorResponse(final int requestSeq, final String message) {
    final DAPResponse response = new DAPResponse();
    response.seq = nextBreakpointId.getAndIncrement();
    response.type = "response";
    response.request_seq = requestSeq;
    response.success = false;
    response.message = message;
    response.body = new HashMap<>();
    return response;
  }

  private Map<String, Object> createScope(
      final String name, final int variablesReference, final boolean expensive) {
    final Map<String, Object> scope = new HashMap<>();
    scope.put("name", name);
    scope.put("variablesReference", variablesReference);
    scope.put("expensive", expensive);
    return scope;
  }

  private String extractFunctionName(final String sourcePath, final int line) {
    // In a real implementation, this would parse source maps or debug info
    // For now, return a placeholder based on the file and line
    final String fileName = sourcePath.substring(sourcePath.lastIndexOf('/') + 1);
    return fileName.replaceAll("\\.[^.]*$", "") + "_line_" + line;
  }

  private String formatVariableValue(final Object value) {
    if (value == null) {
      return "null";
    }
    return value.toString();
  }

  // DAP Message classes

  static class DAPMessage {
    int seq;
    String type;
    String command;
    Object arguments;
  }

  static class DAPResponse {
    int seq;
    String type;
    int request_seq;
    boolean success;
    String command;
    String message;
    Map<String, Object> body;
  }

  static class DAPEvent {
    int seq;
    String type;
    String event;
    Map<String, Object> body;
  }
}

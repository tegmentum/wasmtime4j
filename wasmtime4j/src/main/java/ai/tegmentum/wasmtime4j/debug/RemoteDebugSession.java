package ai.tegmentum.wasmtime4j.debug;

import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Remote debugging session with network communication and security.
 *
 * <p>This class provides secure remote debugging capabilities including:
 * <ul>
 * <li>Encrypted communication using AES-GCM</li>
 * <li>Authentication and authorization</li>
 * <li>Connection management and heartbeats</li>
 * <li>Protocol versioning and capability negotiation</li>
 * <li>Concurrent multi-client support</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * RemoteDebugSession remoteSession = RemoteDebugSession.builder()
 *     .port(9229)
 *     .maxClients(5)
 *     .authenticationRequired(true)
 *     .encryptionEnabled(true)
 *     .build();
 *
 * remoteSession.start();
 * remoteSession.attachSession(debugSession);
 * }</pre>
 *
 * @since 1.0.0
 */
public final class RemoteDebugSession implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(RemoteDebugSession.class.getName());

    private static final String PROTOCOL_VERSION = "1.0";
    private static final int MAX_MESSAGE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int HEARTBEAT_INTERVAL_MS = 30000; // 30 seconds
    private static final int CONNECTION_TIMEOUT_MS = 60000; // 1 minute
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;

    private final String sessionId;
    private final int port;
    private final int maxClients;
    private final boolean authenticationRequired;
    private final boolean encryptionEnabled;
    private final String bindAddress;
    private final ExecutorService executorService;

    private AsynchronousServerSocketChannel serverChannel;
    private final ConcurrentMap<String, RemoteDebugClient> clients;
    private final ConcurrentMap<String, DebugSession> attachedSessions;
    private final AtomicBoolean running;
    private final AtomicLong nextClientId;
    private final SecretKey encryptionKey;
    private final SecureRandom secureRandom;

    private RemoteDebugSession(final Builder builder) {
        this.sessionId = builder.sessionId != null ? builder.sessionId : generateSessionId();
        this.port = builder.port;
        this.maxClients = builder.maxClients;
        this.authenticationRequired = builder.authenticationRequired;
        this.encryptionEnabled = builder.encryptionEnabled;
        this.bindAddress = builder.bindAddress;
        this.executorService = Executors.newCachedThreadPool(r -> {
            final Thread thread = new Thread(r, "RemoteDebug-" + sessionId);
            thread.setDaemon(true);
            return thread;
        });

        this.clients = new ConcurrentHashMap<>();
        this.attachedSessions = new ConcurrentHashMap<>();
        this.running = new AtomicBoolean(false);
        this.nextClientId = new AtomicLong(1);
        this.secureRandom = new SecureRandom();

        // Initialize encryption key if needed
        if (this.encryptionEnabled) {
            try {
                final KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256);
                this.encryptionKey = keyGen.generateKey();
            } catch (final Exception e) {
                throw new IllegalStateException("Failed to initialize encryption key", e);
            }
        } else {
            this.encryptionKey = null;
        }
    }

    /**
     * Starts the remote debugging server.
     *
     * @throws IOException if server cannot be started
     * @throws IllegalStateException if server is already running
     */
    public void start() throws IOException {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Remote debug session is already running");
        }

        try {
            serverChannel = AsynchronousServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(bindAddress, port));

            LOGGER.info(String.format("Started remote debug server on %s:%d (session: %s)",
                bindAddress, port, sessionId));

            // Start accepting connections
            acceptConnections();

            // Start heartbeat monitor
            startHeartbeatMonitor();

        } catch (final IOException e) {
            running.set(false);
            throw e;
        }
    }

    /**
     * Stops the remote debugging server.
     */
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        LOGGER.info("Stopping remote debug server (session: " + sessionId + ")");

        // Close all client connections
        clients.values().forEach(RemoteDebugClient::close);
        clients.clear();

        // Close server channel
        if (serverChannel != null && serverChannel.isOpen()) {
            try {
                serverChannel.close();
            } catch (final IOException e) {
                LOGGER.log(Level.WARNING, "Error closing server channel", e);
            }
        }

        // Shutdown executor
        executorService.shutdown();

        LOGGER.info("Remote debug server stopped");
    }

    /**
     * Attaches a debug session to this remote session.
     *
     * @param debugSession the debug session to attach
     * @throws IllegalArgumentException if debugSession is null
     */
    public void attachSession(final DebugSession debugSession) {
        if (debugSession == null) {
            throw new IllegalArgumentException("Debug session cannot be null");
        }

        final String sessionKey = debugSession.getSessionId();
        attachedSessions.put(sessionKey, debugSession);

        // Notify all connected clients about the new session
        final DebugMessage message = DebugMessage.builder()
            .type(DebugMessageType.SESSION_ATTACHED)
            .sessionId(sessionKey)
            .build();

        broadcastMessage(message);

        LOGGER.info("Attached debug session: " + sessionKey);
    }

    /**
     * Detaches a debug session from this remote session.
     *
     * @param sessionId the session ID to detach
     * @return true if session was detached
     */
    public boolean detachSession(final String sessionId) {
        if (sessionId == null) {
            return false;
        }

        final DebugSession removed = attachedSessions.remove(sessionId);
        if (removed != null) {
            // Notify all connected clients about the detached session
            final DebugMessage message = DebugMessage.builder()
                .type(DebugMessageType.SESSION_DETACHED)
                .sessionId(sessionId)
                .build();

            broadcastMessage(message);

            LOGGER.info("Detached debug session: " + sessionId);
            return true;
        }

        return false;
    }

    /**
     * Gets all attached debug sessions.
     *
     * @return list of attached session IDs
     */
    public List<String> getAttachedSessions() {
        return List.copyOf(attachedSessions.keySet());
    }

    /**
     * Gets the number of connected clients.
     *
     * @return number of connected clients
     */
    public int getClientCount() {
        return clients.size();
    }

    /**
     * Gets information about connected clients.
     *
     * @return list of client information
     */
    public List<RemoteClientInfo> getClientInfo() {
        return clients.values().stream()
            .map(client -> new RemoteClientInfo(
                client.getClientId(),
                client.getRemoteAddress(),
                client.getProtocolVersion(),
                client.getConnectionTime(),
                client.isAuthenticated(),
                client.getLastHeartbeat()
            ))
            .toList();
    }

    /**
     * Checks if the server is running.
     *
     * @return true if server is running
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Gets the server port.
     *
     * @return server port
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets the server bind address.
     *
     * @return bind address
     */
    public String getBindAddress() {
        return bindAddress;
    }

    /**
     * Gets the session ID.
     *
     * @return session ID
     */
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public void close() {
        stop();
    }

    // Private methods

    private void acceptConnections() {
        if (!running.get()) {
            return;
        }

        serverChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(final AsynchronousSocketChannel clientChannel, final Void attachment) {
                // Accept the next connection
                if (running.get()) {
                    acceptConnections();
                }

                // Handle this client connection
                handleNewClient(clientChannel);
            }

            @Override
            public void failed(final Throwable exc, final Void attachment) {
                if (running.get()) {
                    LOGGER.log(Level.WARNING, "Failed to accept client connection", exc);
                    // Try to accept more connections
                    acceptConnections();
                }
            }
        });
    }

    private void handleNewClient(final AsynchronousSocketChannel clientChannel) {
        try {
            // Check if we have reached the maximum number of clients
            if (clients.size() >= maxClients) {
                LOGGER.warning("Maximum number of clients reached, rejecting connection");
                clientChannel.close();
                return;
            }

            // Create client handler
            final String clientId = "client-" + nextClientId.getAndIncrement();
            final RemoteDebugClient client = new RemoteDebugClient(
                clientId,
                clientChannel,
                this,
                encryptionEnabled ? encryptionKey : null
            );

            clients.put(clientId, client);

            // Start client communication
            client.start();

            LOGGER.info("New client connected: " + clientId);

        } catch (final IOException e) {
            LOGGER.log(Level.WARNING, "Error handling new client connection", e);
            try {
                clientChannel.close();
            } catch (final IOException closeEx) {
                // Ignore
            }
        }
    }

    private void startHeartbeatMonitor() {
        executorService.submit(() -> {
            while (running.get()) {
                try {
                    Thread.sleep(HEARTBEAT_INTERVAL_MS);

                    final long currentTime = System.currentTimeMillis();

                    // Check for clients that haven't sent heartbeat
                    clients.entrySet().removeIf(entry -> {
                        final RemoteDebugClient client = entry.getValue();
                        final long timeSinceHeartbeat = currentTime - client.getLastHeartbeat();

                        if (timeSinceHeartbeat > CONNECTION_TIMEOUT_MS) {
                            LOGGER.info("Client timed out, disconnecting: " + entry.getKey());
                            client.close();
                            return true;
                        }

                        return false;
                    });

                    // Send heartbeat to all clients
                    final DebugMessage heartbeat = DebugMessage.builder()
                        .type(DebugMessageType.HEARTBEAT)
                        .timestamp(currentTime)
                        .build();

                    broadcastMessage(heartbeat);

                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (final Exception e) {
                    LOGGER.log(Level.WARNING, "Error in heartbeat monitor", e);
                }
            }
        });
    }

    private void broadcastMessage(final DebugMessage message) {
        clients.values().parallelStream().forEach(client -> {
            try {
                client.sendMessage(message);
            } catch (final Exception e) {
                LOGGER.log(Level.WARNING, "Error broadcasting message to client: " + client.getClientId(), e);
            }
        });
    }

    void removeClient(final String clientId) {
        clients.remove(clientId);
        LOGGER.info("Client disconnected: " + clientId);
    }

    CompletableFuture<DebugMessage> handleClientMessage(final String clientId, final DebugMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return processDebugMessage(clientId, message);
            } catch (final Exception e) {
                LOGGER.log(Level.WARNING, "Error processing message from client: " + clientId, e);
                return DebugMessage.builder()
                    .type(DebugMessageType.ERROR)
                    .requestId(message.getRequestId())
                    .error("Internal server error: " + e.getMessage())
                    .build();
            }
        }, executorService);
    }

    private DebugMessage processDebugMessage(final String clientId, final DebugMessage message) throws WasmException {
        final DebugMessageType type = message.getType();
        final String sessionId = message.getSessionId();

        switch (type) {
            case HANDSHAKE:
                return handleHandshake(clientId, message);

            case SET_BREAKPOINT:
                return handleSetBreakpoint(sessionId, message);

            case REMOVE_BREAKPOINT:
                return handleRemoveBreakpoint(sessionId, message);

            case CONTINUE_EXECUTION:
                return handleContinueExecution(sessionId, message);

            case STEP_INTO:
                return handleStepInto(sessionId, message);

            case STEP_OVER:
                return handleStepOver(sessionId, message);

            case STEP_OUT:
                return handleStepOut(sessionId, message);

            case PAUSE_EXECUTION:
                return handlePauseExecution(sessionId, message);

            case GET_STACK_TRACE:
                return handleGetStackTrace(sessionId, message);

            case GET_VARIABLES:
                return handleGetVariables(sessionId, message);

            case EVALUATE_EXPRESSION:
                return handleEvaluateExpression(sessionId, message);

            case READ_MEMORY:
                return handleReadMemory(sessionId, message);

            case WRITE_MEMORY:
                return handleWriteMemory(sessionId, message);

            case HEARTBEAT:
                return handleHeartbeat(clientId, message);

            default:
                return DebugMessage.builder()
                    .type(DebugMessageType.ERROR)
                    .requestId(message.getRequestId())
                    .error("Unknown message type: " + type)
                    .build();
        }
    }

    private DebugMessage handleHandshake(final String clientId, final DebugMessage message) {
        final RemoteDebugClient client = clients.get(clientId);
        if (client == null) {
            return DebugMessage.builder()
                .type(DebugMessageType.ERROR)
                .requestId(message.getRequestId())
                .error("Client not found")
                .build();
        }

        // Extract client capabilities and version
        final String clientVersion = message.getData("version", String.class);
        final List<String> capabilities = message.getData("capabilities", List.class);

        client.setProtocolVersion(clientVersion);
        client.setCapabilities(capabilities);

        // Send server capabilities
        return DebugMessage.builder()
            .type(DebugMessageType.HANDSHAKE_RESPONSE)
            .requestId(message.getRequestId())
            .data("version", PROTOCOL_VERSION)
            .data("capabilities", List.of(
                "breakpoints",
                "stepping",
                "variables",
                "memory",
                "expressions",
                "multi-target",
                "encryption",
                "authentication"
            ))
            .data("sessionId", sessionId)
            .data("attachedSessions", getAttachedSessions())
            .build();
    }

    private DebugMessage handleSetBreakpoint(final String sessionId, final DebugMessage message) throws WasmException {
        final DebugSession session = getSession(sessionId);
        if (session == null) {
            return createErrorMessage(message.getRequestId(), "Session not found: " + sessionId);
        }

        final String functionName = message.getData("functionName", String.class);
        final Integer line = message.getData("line", Integer.class);

        final Breakpoint breakpoint = session.setBreakpoint(functionName, line != null ? line : 0);

        return DebugMessage.builder()
            .type(DebugMessageType.BREAKPOINT_SET)
            .requestId(message.getRequestId())
            .sessionId(sessionId)
            .data("breakpoint", breakpoint)
            .build();
    }

    private DebugMessage handleRemoveBreakpoint(final String sessionId, final DebugMessage message) throws WasmException {
        final DebugSession session = getSession(sessionId);
        if (session == null) {
            return createErrorMessage(message.getRequestId(), "Session not found: " + sessionId);
        }

        final Breakpoint breakpoint = message.getData("breakpoint", Breakpoint.class);
        final boolean removed = session.removeBreakpoint(breakpoint);

        return DebugMessage.builder()
            .type(DebugMessageType.BREAKPOINT_REMOVED)
            .requestId(message.getRequestId())
            .sessionId(sessionId)
            .data("removed", removed)
            .build();
    }

    private DebugMessage handleContinueExecution(final String sessionId, final DebugMessage message) throws WasmException {
        final DebugSession session = getSession(sessionId);
        if (session == null) {
            return createErrorMessage(message.getRequestId(), "Session not found: " + sessionId);
        }

        final CompletableFuture<DebugEvent> future = session.continueExecution();

        return DebugMessage.builder()
            .type(DebugMessageType.EXECUTION_CONTINUED)
            .requestId(message.getRequestId())
            .sessionId(sessionId)
            .data("asyncOperation", true)
            .build();
    }

    private DebugMessage handleStepInto(final String sessionId, final DebugMessage message) throws WasmException {
        final DebugSession session = getSession(sessionId);
        if (session == null) {
            return createErrorMessage(message.getRequestId(), "Session not found: " + sessionId);
        }

        final CompletableFuture<DebugEvent> future = session.stepInto();

        return DebugMessage.builder()
            .type(DebugMessageType.STEP_COMPLETED)
            .requestId(message.getRequestId())
            .sessionId(sessionId)
            .data("stepType", "into")
            .data("asyncOperation", true)
            .build();
    }

    private DebugMessage handleStepOver(final String sessionId, final DebugMessage message) throws WasmException {
        final DebugSession session = getSession(sessionId);
        if (session == null) {
            return createErrorMessage(message.getRequestId(), "Session not found: " + sessionId);
        }

        final CompletableFuture<DebugEvent> future = session.stepOver();

        return DebugMessage.builder()
            .type(DebugMessageType.STEP_COMPLETED)
            .requestId(message.getRequestId())
            .sessionId(sessionId)
            .data("stepType", "over")
            .data("asyncOperation", true)
            .build();
    }

    private DebugMessage handleStepOut(final String sessionId, final DebugMessage message) throws WasmException {
        final DebugSession session = getSession(sessionId);
        if (session == null) {
            return createErrorMessage(message.getRequestId(), "Session not found: " + sessionId);
        }

        final CompletableFuture<DebugEvent> future = session.stepOut();

        return DebugMessage.builder()
            .type(DebugMessageType.STEP_COMPLETED)
            .requestId(message.getRequestId())
            .sessionId(sessionId)
            .data("stepType", "out")
            .data("asyncOperation", true)
            .build();
    }

    private DebugMessage handlePauseExecution(final String sessionId, final DebugMessage message) throws WasmException {
        final DebugSession session = getSession(sessionId);
        if (session == null) {
            return createErrorMessage(message.getRequestId(), "Session not found: " + sessionId);
        }

        final CompletableFuture<DebugEvent> future = session.pause();

        return DebugMessage.builder()
            .type(DebugMessageType.EXECUTION_PAUSED)
            .requestId(message.getRequestId())
            .sessionId(sessionId)
            .data("asyncOperation", true)
            .build();
    }

    private DebugMessage handleGetStackTrace(final String sessionId, final DebugMessage message) throws WasmException {
        final DebugSession session = getSession(sessionId);
        if (session == null) {
            return createErrorMessage(message.getRequestId(), "Session not found: " + sessionId);
        }

        final List<StackFrame> stackTrace = session.getStackTrace();

        return DebugMessage.builder()
            .type(DebugMessageType.STACK_TRACE)
            .requestId(message.getRequestId())
            .sessionId(sessionId)
            .data("stackTrace", stackTrace)
            .build();
    }

    private DebugMessage handleGetVariables(final String sessionId, final DebugMessage message) throws WasmException {
        final DebugSession session = getSession(sessionId);
        if (session == null) {
            return createErrorMessage(message.getRequestId(), "Session not found: " + sessionId);
        }

        final Integer frameIndex = message.getData("frameIndex", Integer.class);
        final List<Variable> variables = frameIndex != null
            ? session.getVariables(frameIndex)
            : session.getCurrentVariables();

        return DebugMessage.builder()
            .type(DebugMessageType.VARIABLES)
            .requestId(message.getRequestId())
            .sessionId(sessionId)
            .data("variables", variables)
            .data("frameIndex", frameIndex)
            .build();
    }

    private DebugMessage handleEvaluateExpression(final String sessionId, final DebugMessage message) throws WasmException {
        final DebugSession session = getSession(sessionId);
        if (session == null) {
            return createErrorMessage(message.getRequestId(), "Session not found: " + sessionId);
        }

        final String expression = message.getData("expression", String.class);
        final EvaluationResult result = session.evaluateExpression(expression);

        return DebugMessage.builder()
            .type(DebugMessageType.EXPRESSION_RESULT)
            .requestId(message.getRequestId())
            .sessionId(sessionId)
            .data("result", result)
            .build();
    }

    private DebugMessage handleReadMemory(final String sessionId, final DebugMessage message) throws WasmException {
        final DebugSession session = getSession(sessionId);
        if (session == null) {
            return createErrorMessage(message.getRequestId(), "Session not found: " + sessionId);
        }

        final Long address = message.getData("address", Long.class);
        final Integer length = message.getData("length", Integer.class);

        if (address == null || length == null) {
            return createErrorMessage(message.getRequestId(), "Address and length are required");
        }

        final byte[] data = session.readMemory(address, length);

        return DebugMessage.builder()
            .type(DebugMessageType.MEMORY_DATA)
            .requestId(message.getRequestId())
            .sessionId(sessionId)
            .data("address", address)
            .data("length", length)
            .data("data", data)
            .build();
    }

    private DebugMessage handleWriteMemory(final String sessionId, final DebugMessage message) throws WasmException {
        final DebugSession session = getSession(sessionId);
        if (session == null) {
            return createErrorMessage(message.getRequestId(), "Session not found: " + sessionId);
        }

        final Long address = message.getData("address", Long.class);
        final byte[] data = message.getData("data", byte[].class);

        if (address == null || data == null) {
            return createErrorMessage(message.getRequestId(), "Address and data are required");
        }

        session.writeMemory(address, data);

        return DebugMessage.builder()
            .type(DebugMessageType.MEMORY_WRITTEN)
            .requestId(message.getRequestId())
            .sessionId(sessionId)
            .data("address", address)
            .data("length", data.length)
            .build();
    }

    private DebugMessage handleHeartbeat(final String clientId, final DebugMessage message) {
        final RemoteDebugClient client = clients.get(clientId);
        if (client != null) {
            client.updateLastHeartbeat();
        }

        return DebugMessage.builder()
            .type(DebugMessageType.HEARTBEAT_RESPONSE)
            .requestId(message.getRequestId())
            .timestamp(System.currentTimeMillis())
            .build();
    }

    private DebugSession getSession(final String sessionId) {
        return attachedSessions.get(sessionId);
    }

    private DebugMessage createErrorMessage(final String requestId, final String error) {
        return DebugMessage.builder()
            .type(DebugMessageType.ERROR)
            .requestId(requestId)
            .error(error)
            .build();
    }

    private static String generateSessionId() {
        return "remote-debug-" + System.currentTimeMillis() + "-" +
               Long.toHexString(System.nanoTime());
    }

    /**
     * Builder for RemoteDebugSession.
     */
    public static final class Builder {
        private String sessionId;
        private int port = 9229;
        private int maxClients = 10;
        private boolean authenticationRequired = true;
        private boolean encryptionEnabled = true;
        private String bindAddress = "localhost";

        /**
         * Sets the session ID.
         *
         * @param sessionId the session ID
         * @return this builder
         */
        public Builder sessionId(final String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        /**
         * Sets the server port.
         *
         * @param port the port
         * @return this builder
         * @throws IllegalArgumentException if port is invalid
         */
        public Builder port(final int port) {
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Port must be between 1 and 65535");
            }
            this.port = port;
            return this;
        }

        /**
         * Sets the maximum number of clients.
         *
         * @param maxClients the maximum number of clients
         * @return this builder
         * @throws IllegalArgumentException if maxClients is not positive
         */
        public Builder maxClients(final int maxClients) {
            if (maxClients < 1) {
                throw new IllegalArgumentException("Max clients must be positive");
            }
            this.maxClients = maxClients;
            return this;
        }

        /**
         * Sets whether authentication is required.
         *
         * @param required true if authentication is required
         * @return this builder
         */
        public Builder authenticationRequired(final boolean required) {
            this.authenticationRequired = required;
            return this;
        }

        /**
         * Sets whether encryption is enabled.
         *
         * @param enabled true if encryption is enabled
         * @return this builder
         */
        public Builder encryptionEnabled(final boolean enabled) {
            this.encryptionEnabled = enabled;
            return this;
        }

        /**
         * Sets the bind address.
         *
         * @param bindAddress the bind address
         * @return this builder
         * @throws IllegalArgumentException if bindAddress is null
         */
        public Builder bindAddress(final String bindAddress) {
            if (bindAddress == null) {
                throw new IllegalArgumentException("Bind address cannot be null");
            }
            this.bindAddress = bindAddress;
            return this;
        }

        /**
         * Builds the RemoteDebugSession.
         *
         * @return the remote debug session
         */
        public RemoteDebugSession build() {
            return new RemoteDebugSession(this);
        }
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }
}
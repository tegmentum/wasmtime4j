package ai.tegmentum.wasmtime4j.ide.collaboration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WebSocket handler for real-time collaboration between developers.
 * Manages WebSocket connections and message routing for collaborative debugging.
 */
@WebSocket
public final class CollaborationWebSocketHandler extends WebSocketAdapter {

    private static final Logger LOGGER = Logger.getLogger(CollaborationWebSocketHandler.class.getName());

    private final CollaborationServer collaborationServer;
    private final ObjectMapper objectMapper;
    private final Map<Session, DeveloperConnection> connections;

    /**
     * Creates a new collaboration WebSocket handler.
     *
     * @param collaborationServer The collaboration server
     */
    public CollaborationWebSocketHandler(final CollaborationServer collaborationServer) {
        this.collaborationServer = collaborationServer;
        this.objectMapper = collaborationServer.getObjectMapper();
        this.connections = new ConcurrentHashMap<>();
    }

    @Override
    public void onWebSocketConnect(final Session session) {
        super.onWebSocketConnect(session);
        LOGGER.info("WebSocket connection established: " + session.getRemoteAddress());

        // Store connection for later use
        final DeveloperConnection connection = new DeveloperConnection(session);
        connections.put(session, connection);

        // Send welcome message
        final CollaborationMessage welcome = new CollaborationMessage(
            CollaborationMessage.Type.CONNECTION_STATUS_CHANGED,
            "system",
            Map.of("status", "connected", "message", "Connected to collaboration server")
        );

        sendMessage(session, welcome);
    }

    @Override
    public void onWebSocketText(final String message) {
        final Session session = getSession();
        if (session == null) {
            return;
        }

        try {
            LOGGER.fine("Received WebSocket message: " + message);

            // Parse incoming message
            final CollaborationMessage collaborationMessage = objectMapper.readValue(message, CollaborationMessage.class);
            handleMessage(session, collaborationMessage);

        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to process WebSocket message", e);

            final CollaborationMessage error = new CollaborationMessage(
                CollaborationMessage.Type.ERROR_OCCURRED,
                "system",
                Map.of("error", "Failed to process message: " + e.getMessage())
            );

            sendMessage(session, error);
        }
    }

    @Override
    public void onWebSocketClose(final int statusCode, final String reason) {
        final Session session = getSession();
        if (session != null) {
            LOGGER.info("WebSocket connection closed: " + session.getRemoteAddress() +
                       " (code: " + statusCode + ", reason: " + reason + ")");

            final DeveloperConnection connection = connections.remove(session);
            if (connection != null && connection.getSessionId() != null && connection.getDeveloperId() != null) {
                // Remove from collaboration session
                collaborationServer.leaveSession(connection.getSessionId(), connection.getDeveloperId());
            }
        }

        super.onWebSocketClose(statusCode, reason);
    }

    @Override
    public void onWebSocketError(final Throwable cause) {
        LOGGER.log(Level.WARNING, "WebSocket error occurred", cause);
        super.onWebSocketError(cause);
    }

    private void handleMessage(final Session session, final CollaborationMessage message) {
        final DeveloperConnection connection = connections.get(session);
        if (connection == null) {
            return;
        }

        switch (message.getType()) {
            case DEVELOPER_JOINED:
                handleDeveloperJoined(session, connection, message);
                break;

            case DEVELOPER_LEFT:
                handleDeveloperLeft(session, connection, message);
                break;

            case DEVELOPER_CURSOR_MOVED:
                handleCursorMoved(connection, message);
                break;

            case DEVELOPER_FILE_CHANGED:
                handleFileChanged(connection, message);
                break;

            case DOCUMENT_CHANGED:
                handleDocumentChanged(connection, message);
                break;

            case DEBUG_SESSION_STARTED:
                handleDebugSessionStarted(connection, message);
                break;

            case DEBUG_CONTROL_CHANGED:
                handleDebugControlChanged(connection, message);
                break;

            case CHAT_MESSAGE:
                handleChatMessage(connection, message);
                break;

            case ANALYSIS_RESULT_SHARED:
                handleAnalysisResultShared(connection, message);
                break;

            default:
                LOGGER.fine("Unhandled message type: " + message.getType());
                break;
        }
    }

    private void handleDeveloperJoined(final Session session, final DeveloperConnection connection,
                                     final CollaborationMessage message) {
        final Map<String, Object> data = message.getData();
        final String sessionId = (String) data.get("sessionId");
        final String developerId = message.getSenderId();
        final String developerName = (String) data.get("developerName");

        if (sessionId != null && developerId != null && developerName != null) {
            connection.setSessionId(sessionId);
            connection.setDeveloperId(developerId);

            // Join collaboration session
            final CollaborationServer.CollaborationSession collaborationSession =
                collaborationServer.joinSession(sessionId, developerId, developerName);

            // Send confirmation
            final CollaborationMessage response = new CollaborationMessage(
                CollaborationMessage.Type.DEVELOPER_JOINED,
                "system",
                Map.of(
                    "sessionId", sessionId,
                    "developerId", developerId,
                    "developers", collaborationSession.getDevelopers().size()
                )
            );

            sendMessage(session, response);
        }
    }

    private void handleDeveloperLeft(final Session session, final DeveloperConnection connection,
                                   final CollaborationMessage message) {
        if (connection.getSessionId() != null && connection.getDeveloperId() != null) {
            collaborationServer.leaveSession(connection.getSessionId(), connection.getDeveloperId());

            final CollaborationMessage response = new CollaborationMessage(
                CollaborationMessage.Type.DEVELOPER_LEFT,
                "system",
                Map.of("status", "left")
            );

            sendMessage(session, response);
        }
    }

    private void handleCursorMoved(final DeveloperConnection connection, final CollaborationMessage message) {
        if (connection.getSessionId() != null) {
            // Broadcast cursor position to other developers
            collaborationServer.broadcastToSession(connection.getSessionId(), message);
        }
    }

    private void handleFileChanged(final DeveloperConnection connection, final CollaborationMessage message) {
        if (connection.getSessionId() != null) {
            // Broadcast file change to other developers
            collaborationServer.broadcastToSession(connection.getSessionId(), message);
        }
    }

    private void handleDocumentChanged(final DeveloperConnection connection, final CollaborationMessage message) {
        if (connection.getSessionId() != null) {
            final Map<String, Object> data = message.getData();
            final String uri = (String) data.get("uri");

            if (uri != null) {
                // Apply change to shared document
                final CollaborationServer.CollaborationSession session =
                    collaborationServer.getSessions().get(connection.getSessionId());

                if (session != null) {
                    final CollaborationServer.SharedDocument document = session.getOrCreateDocument(uri);

                    // Extract change information
                    final Map<String, Object> changeData = (Map<String, Object>) data.get("change");
                    if (changeData != null) {
                        final CollaborationServer.DocumentChange change =
                            new CollaborationServer.DocumentChange(
                                message.getSenderId(),
                                (Integer) changeData.get("version"),
                                (Integer) changeData.get("start"),
                                (Integer) changeData.get("end"),
                                (String) changeData.get("newText")
                            );

                        document.applyChange(change);
                    }
                }
            }
        }
    }

    private void handleDebugSessionStarted(final DeveloperConnection connection, final CollaborationMessage message) {
        if (connection.getSessionId() != null) {
            // Broadcast debug session start to other developers
            collaborationServer.broadcastToSession(connection.getSessionId(), message);
        }
    }

    private void handleDebugControlChanged(final DeveloperConnection connection, final CollaborationMessage message) {
        if (connection.getSessionId() != null) {
            final CollaborationServer.CollaborationSession session =
                collaborationServer.getSessions().get(connection.getSessionId());

            if (session != null) {
                session.getDebugger().requestDebugControl(message.getSenderId());
            }
        }
    }

    private void handleChatMessage(final DeveloperConnection connection, final CollaborationMessage message) {
        if (connection.getSessionId() != null) {
            // Broadcast chat message to other developers
            collaborationServer.broadcastToSession(connection.getSessionId(), message);
        }
    }

    private void handleAnalysisResultShared(final DeveloperConnection connection, final CollaborationMessage message) {
        if (connection.getSessionId() != null) {
            // Broadcast analysis results to other developers
            collaborationServer.broadcastToSession(connection.getSessionId(), message);
        }
    }

    private void sendMessage(final Session session, final CollaborationMessage message) {
        if (session == null || !session.isOpen()) {
            return;
        }

        try {
            final String json = objectMapper.writeValueAsString(message);
            session.getRemote().sendString(json);
            LOGGER.fine("Sent WebSocket message: " + json);

        } catch (final IOException e) {
            LOGGER.log(Level.WARNING, "Failed to send WebSocket message", e);
        }
    }

    /**
     * Sends a message to all connections in a specific collaboration session.
     *
     * @param sessionId Session identifier
     * @param message Message to send
     */
    public void broadcastToSession(final String sessionId, final CollaborationMessage message) {
        for (final Map.Entry<Session, DeveloperConnection> entry : connections.entrySet()) {
            final DeveloperConnection connection = entry.getValue();
            if (sessionId.equals(connection.getSessionId())) {
                sendMessage(entry.getKey(), message);
            }
        }
    }

    /**
     * Represents a developer's WebSocket connection.
     */
    private static final class DeveloperConnection {
        private final Session session;
        private String sessionId;
        private String developerId;

        public DeveloperConnection(final Session session) {
            this.session = session;
        }

        public Session getSession() { return session; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(final String sessionId) { this.sessionId = sessionId; }
        public String getDeveloperId() { return developerId; }
        public void setDeveloperId(final String developerId) { this.developerId = developerId; }
    }
}
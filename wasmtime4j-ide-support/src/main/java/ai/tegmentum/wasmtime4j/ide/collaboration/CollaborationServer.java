package ai.tegmentum.wasmtime4j.ide.collaboration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

/**
 * Real-time collaboration server for multi-developer WebAssembly debugging and development.
 * Provides synchronized debugging sessions, code sharing, and collaborative analysis.
 */
public final class CollaborationServer {

  private static final Logger LOGGER = Logger.getLogger(CollaborationServer.class.getName());
  private static final int DEFAULT_PORT = 8765;

  private final int port;
  private final Server jettyServer;
  private final ObjectMapper objectMapper;
  private final Map<String, CollaborationSession> sessions;
  private final Map<String, Developer> developers;
  private final CollaborationWebSocketHandler websocketHandler;

  /** Creates a collaboration server on the default port. */
  public CollaborationServer() {
    this(DEFAULT_PORT);
  }

  /**
   * Creates a collaboration server on the specified port.
   *
   * @param port Server port
   */
  public CollaborationServer(final int port) {
    this.port = port;
    this.objectMapper = new ObjectMapper();
    this.sessions = new ConcurrentHashMap<>();
    this.developers = new ConcurrentHashMap<>();
    this.websocketHandler = new CollaborationWebSocketHandler(this);
    this.jettyServer = createJettyServer();
  }

  /**
   * Starts the collaboration server.
   *
   * @throws Exception If server startup fails
   */
  public void start() throws Exception {
    LOGGER.info("Starting collaboration server on port " + port);
    jettyServer.start();
    LOGGER.info("Collaboration server started successfully");
  }

  /**
   * Stops the collaboration server.
   *
   * @throws Exception If server shutdown fails
   */
  public void stop() throws Exception {
    LOGGER.info("Stopping collaboration server");

    // Close all sessions
    for (final CollaborationSession session : sessions.values()) {
      session.close();
    }
    sessions.clear();
    developers.clear();

    jettyServer.stop();
    LOGGER.info("Collaboration server stopped");
  }

  /**
   * Creates or joins a collaboration session.
   *
   * @param sessionId Session identifier
   * @param developerId Developer identifier
   * @param developerName Developer name
   * @return Collaboration session
   */
  public CollaborationSession joinSession(
      final String sessionId, final String developerId, final String developerName) {
    Objects.requireNonNull(sessionId, "Session ID cannot be null");
    Objects.requireNonNull(developerId, "Developer ID cannot be null");
    Objects.requireNonNull(developerName, "Developer name cannot be null");

    // Register developer
    final Developer developer = new Developer(developerId, developerName);
    developers.put(developerId, developer);

    // Get or create session
    final CollaborationSession session =
        sessions.computeIfAbsent(sessionId, id -> new CollaborationSession(id, this));

    // Add developer to session
    session.addDeveloper(developer);

    LOGGER.info("Developer " + developerName + " joined session " + sessionId);
    return session;
  }

  /**
   * Leaves a collaboration session.
   *
   * @param sessionId Session identifier
   * @param developerId Developer identifier
   */
  public void leaveSession(final String sessionId, final String developerId) {
    final CollaborationSession session = sessions.get(sessionId);
    if (session != null) {
      final Developer developer = developers.get(developerId);
      if (developer != null) {
        session.removeDeveloper(developer);
        LOGGER.info("Developer " + developer.getName() + " left session " + sessionId);

        // Remove empty sessions
        if (session.getDevelopers().isEmpty()) {
          sessions.remove(sessionId);
          session.close();
          LOGGER.info("Session " + sessionId + " closed (no developers)");
        }
      }
    }
  }

  /**
   * Broadcasts a message to all developers in a session.
   *
   * @param sessionId Session identifier
   * @param message Message to broadcast
   */
  public void broadcastToSession(final String sessionId, final CollaborationMessage message) {
    final CollaborationSession session = sessions.get(sessionId);
    if (session != null) {
      session.broadcast(message);
    }
  }

  /**
   * Gets all active sessions.
   *
   * @return Map of session ID to session
   */
  public Map<String, CollaborationSession> getSessions() {
    return Collections.unmodifiableMap(sessions);
  }

  /**
   * Gets WebSocket handler for collaboration.
   *
   * @return WebSocket handler
   */
  public CollaborationWebSocketHandler getWebSocketHandler() {
    return websocketHandler;
  }

  /**
   * Gets JSON object mapper.
   *
   * @return Object mapper
   */
  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  private Server createJettyServer() {
    final Server server = new Server(port);

    final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");

    // Configure WebSocket support
    JettyWebSocketServletContainerInitializer.configure(
        context,
        (servletContext, wsContainer) -> {
          wsContainer.setMaxTextMessageSize(65536);
          wsContainer.addMapping("/collaborate", websocketHandler);
        });

    // Add REST API servlet
    final ServletHolder restServlet = new ServletHolder(new CollaborationRestServlet(this));
    context.addServlet(restServlet, "/api/*");

    server.setHandler(context);
    return server;
  }

  /** REST API servlet for collaboration server management. */
  private static final class CollaborationRestServlet extends HttpServlet {
    private final CollaborationServer collaborationServer;

    public CollaborationRestServlet(final CollaborationServer collaborationServer) {
      this.collaborationServer = collaborationServer;
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
        throws IOException {
      response.setContentType("application/json");

      final String pathInfo = request.getPathInfo();
      if ("/sessions".equals(pathInfo)) {
        // Get all sessions
        final Map<String, Object> sessionInfo = new HashMap<>();
        for (final Map.Entry<String, CollaborationSession> entry :
            collaborationServer.getSessions().entrySet()) {
          final CollaborationSession session = entry.getValue();
          final Map<String, Object> info = new HashMap<>();
          info.put("id", session.getSessionId());
          info.put("developers", session.getDevelopers().size());
          info.put("createdAt", session.getCreatedAt());
          sessionInfo.put(entry.getKey(), info);
        }

        collaborationServer.getObjectMapper().writeValue(response.getOutputStream(), sessionInfo);
      } else {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      }
    }
  }

  /** Represents a developer participating in collaboration. */
  public static final class Developer {
    private final String id;
    private final String name;
    private final long joinedAt;
    private String currentFile;
    private int cursorLine;
    private int cursorColumn;

    public Developer(final String id, final String name) {
      this.id = Objects.requireNonNull(id, "ID cannot be null");
      this.name = Objects.requireNonNull(name, "Name cannot be null");
      this.joinedAt = System.currentTimeMillis();
    }

    public String getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public long getJoinedAt() {
      return joinedAt;
    }

    public String getCurrentFile() {
      return currentFile;
    }

    public void setCurrentFile(final String currentFile) {
      this.currentFile = currentFile;
    }

    public int getCursorLine() {
      return cursorLine;
    }

    public void setCursorLine(final int cursorLine) {
      this.cursorLine = cursorLine;
    }

    public int getCursorColumn() {
      return cursorColumn;
    }

    public void setCursorColumn(final int cursorColumn) {
      this.cursorColumn = cursorColumn;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      final Developer developer = (Developer) obj;
      return Objects.equals(id, developer.id);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id);
    }

    @Override
    public String toString() {
      return "Developer{id='" + id + "', name='" + name + "'}";
    }
  }

  /** Represents a collaboration session with multiple developers. */
  public static final class CollaborationSession {
    private final String sessionId;
    private final CollaborationServer server;
    private final long createdAt;
    private final List<Developer> developers;
    private final Map<String, SharedDocument> sharedDocuments;
    private final CollaborativeDebugger debugger;
    private boolean isClosed = false;

    public CollaborationSession(final String sessionId, final CollaborationServer server) {
      this.sessionId = Objects.requireNonNull(sessionId, "Session ID cannot be null");
      this.server = Objects.requireNonNull(server, "Server cannot be null");
      this.createdAt = System.currentTimeMillis();
      this.developers = new CopyOnWriteArrayList<>();
      this.sharedDocuments = new ConcurrentHashMap<>();
      this.debugger = new CollaborativeDebugger(this);
    }

    public void addDeveloper(final Developer developer) {
      if (!isClosed && !developers.contains(developer)) {
        developers.add(developer);

        // Notify other developers
        final CollaborationMessage message =
            new CollaborationMessage(
                CollaborationMessage.Type.DEVELOPER_JOINED,
                developer.getId(),
                Map.of("developerName", developer.getName()));
        broadcast(message, developer);
      }
    }

    public void removeDeveloper(final Developer developer) {
      if (developers.remove(developer)) {
        // Notify other developers
        final CollaborationMessage message =
            new CollaborationMessage(
                CollaborationMessage.Type.DEVELOPER_LEFT,
                developer.getId(),
                Map.of("developerName", developer.getName()));
        broadcast(message, developer);
      }
    }

    public void broadcast(final CollaborationMessage message) {
      broadcast(message, null);
    }

    public void broadcast(final CollaborationMessage message, final Developer exclude) {
      if (isClosed) return;

      try {
        final String json = server.getObjectMapper().writeValueAsString(message);
        // In a real implementation, this would send via WebSocket connections
        LOGGER.fine("Broadcasting message to session " + sessionId + ": " + json);
      } catch (final Exception e) {
        LOGGER.log(Level.WARNING, "Failed to broadcast message", e);
      }
    }

    public SharedDocument getOrCreateDocument(final String uri) {
      return sharedDocuments.computeIfAbsent(uri, key -> new SharedDocument(key, this));
    }

    public void close() {
      isClosed = true;
      developers.clear();
      sharedDocuments.clear();
      debugger.close();
    }

    // Getters
    public String getSessionId() {
      return sessionId;
    }

    public long getCreatedAt() {
      return createdAt;
    }

    public List<Developer> getDevelopers() {
      return Collections.unmodifiableList(developers);
    }

    public Map<String, SharedDocument> getSharedDocuments() {
      return Collections.unmodifiableMap(sharedDocuments);
    }

    public CollaborativeDebugger getDebugger() {
      return debugger;
    }

    public boolean isClosed() {
      return isClosed;
    }
  }

  /** Represents a document shared among developers in a session. */
  public static final class SharedDocument {
    private final String uri;
    private final CollaborationSession session;
    private String content;
    private int version;
    private final List<DocumentChange> changeHistory;

    public SharedDocument(final String uri, final CollaborationSession session) {
      this.uri = Objects.requireNonNull(uri, "URI cannot be null");
      this.session = Objects.requireNonNull(session, "Session cannot be null");
      this.content = "";
      this.version = 0;
      this.changeHistory = new ArrayList<>();
    }

    public synchronized void applyChange(final DocumentChange change) {
      if (change.getVersion() <= version) {
        return; // Ignore old changes
      }

      // Apply change to content
      final StringBuilder sb = new StringBuilder(content);
      sb.replace(change.getStart(), change.getEnd(), change.getNewText());
      content = sb.toString();
      version = change.getVersion();
      changeHistory.add(change);

      // Broadcast change to other developers
      final CollaborationMessage message =
          new CollaborationMessage(
              CollaborationMessage.Type.DOCUMENT_CHANGED,
              change.getDeveloperId(),
              Map.of("uri", uri, "change", change));
      session.broadcast(message);
    }

    // Getters
    public String getUri() {
      return uri;
    }

    public String getContent() {
      return content;
    }

    public int getVersion() {
      return version;
    }

    public List<DocumentChange> getChangeHistory() {
      return Collections.unmodifiableList(changeHistory);
    }
  }

  /** Represents a change made to a shared document. */
  public static final class DocumentChange {
    private final String developerId;
    private final int version;
    private final int start;
    private final int end;
    private final String newText;
    private final long timestamp;

    public DocumentChange(
        final String developerId,
        final int version,
        final int start,
        final int end,
        final String newText) {
      this.developerId = Objects.requireNonNull(developerId, "Developer ID cannot be null");
      this.version = version;
      this.start = start;
      this.end = end;
      this.newText = Objects.requireNonNull(newText, "New text cannot be null");
      this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public String getDeveloperId() {
      return developerId;
    }

    public int getVersion() {
      return version;
    }

    public int getStart() {
      return start;
    }

    public int getEnd() {
      return end;
    }

    public String getNewText() {
      return newText;
    }

    public long getTimestamp() {
      return timestamp;
    }
  }

  /** Collaborative debugger for multi-developer debugging sessions. */
  public static final class CollaborativeDebugger {
    private final CollaborationSession session;
    private final Map<String, DebugSession> debugSessions;
    private String activeDebugger; // Developer ID who controls debugging

    public CollaborativeDebugger(final CollaborationSession session) {
      this.session = Objects.requireNonNull(session, "Session cannot be null");
      this.debugSessions = new ConcurrentHashMap<>();
    }

    public void requestDebugControl(final String developerId) {
      if (activeDebugger == null || activeDebugger.equals(developerId)) {
        activeDebugger = developerId;

        final CollaborationMessage message =
            new CollaborationMessage(
                CollaborationMessage.Type.DEBUG_CONTROL_CHANGED,
                developerId,
                Map.of("activeDebugger", developerId));
        session.broadcast(message);
      }
    }

    public void releaseDebugControl(final String developerId) {
      if (developerId.equals(activeDebugger)) {
        activeDebugger = null;

        final CollaborationMessage message =
            new CollaborationMessage(
                CollaborationMessage.Type.DEBUG_CONTROL_RELEASED, developerId, Map.of());
        session.broadcast(message);
      }
    }

    public void close() {
      debugSessions.clear();
      activeDebugger = null;
    }

    // Getters
    public String getActiveDebugger() {
      return activeDebugger;
    }

    public Map<String, DebugSession> getDebugSessions() {
      return Collections.unmodifiableMap(debugSessions);
    }
  }

  /** Individual debug session within collaborative debugging. */
  public static final class DebugSession {
    private final String sessionId;
    private final String developerId;
    private final String moduleUri;
    private boolean isActive;

    public DebugSession(final String sessionId, final String developerId, final String moduleUri) {
      this.sessionId = Objects.requireNonNull(sessionId, "Session ID cannot be null");
      this.developerId = Objects.requireNonNull(developerId, "Developer ID cannot be null");
      this.moduleUri = Objects.requireNonNull(moduleUri, "Module URI cannot be null");
      this.isActive = false;
    }

    // Getters
    public String getSessionId() {
      return sessionId;
    }

    public String getDeveloperId() {
      return developerId;
    }

    public String getModuleUri() {
      return moduleUri;
    }

    public boolean isActive() {
      return isActive;
    }

    public void setActive(final boolean active) {
      isActive = active;
    }
  }
}

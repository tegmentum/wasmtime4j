package ai.tegmentum.wasmtime4j.ide.collaboration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;

/**
 * Message format for real-time collaboration between developers.
 * Used for synchronizing debugging sessions, code changes, and developer presence.
 */
public final class CollaborationMessage {

    private final Type type;
    private final String senderId;
    private final Map<String, Object> data;
    private final long timestamp;

    /**
     * Creates a new collaboration message.
     *
     * @param type Message type
     * @param senderId ID of the sender developer
     * @param data Message data payload
     */
    @JsonCreator
    public CollaborationMessage(@JsonProperty("type") final Type type,
                               @JsonProperty("senderId") final String senderId,
                               @JsonProperty("data") final Map<String, Object> data) {
        this.type = Objects.requireNonNull(type, "Type cannot be null");
        this.senderId = Objects.requireNonNull(senderId, "Sender ID cannot be null");
        this.data = Objects.requireNonNull(data, "Data cannot be null");
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Gets the message type.
     *
     * @return Message type
     */
    public Type getType() {
        return type;
    }

    /**
     * Gets the sender developer ID.
     *
     * @return Sender ID
     */
    public String getSenderId() {
        return senderId;
    }

    /**
     * Gets the message data payload.
     *
     * @return Message data
     */
    public Map<String, Object> getData() {
        return data;
    }

    /**
     * Gets the message timestamp.
     *
     * @return Timestamp in milliseconds since epoch
     */
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final CollaborationMessage that = (CollaborationMessage) obj;
        return timestamp == that.timestamp &&
               type == that.type &&
               Objects.equals(senderId, that.senderId) &&
               Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, senderId, data, timestamp);
    }

    @Override
    public String toString() {
        return "CollaborationMessage{" +
               "type=" + type +
               ", senderId='" + senderId + '\'' +
               ", data=" + data +
               ", timestamp=" + timestamp +
               '}';
    }

    /**
     * Types of collaboration messages.
     */
    public enum Type {
        // Developer presence
        DEVELOPER_JOINED,
        DEVELOPER_LEFT,
        DEVELOPER_CURSOR_MOVED,
        DEVELOPER_FILE_CHANGED,

        // Document collaboration
        DOCUMENT_OPENED,
        DOCUMENT_CLOSED,
        DOCUMENT_CHANGED,
        DOCUMENT_SAVED,

        // Debugging collaboration
        DEBUG_SESSION_STARTED,
        DEBUG_SESSION_ENDED,
        DEBUG_BREAKPOINT_SET,
        DEBUG_BREAKPOINT_REMOVED,
        DEBUG_STEP_OVER,
        DEBUG_STEP_INTO,
        DEBUG_STEP_OUT,
        DEBUG_CONTINUE,
        DEBUG_PAUSE,
        DEBUG_CONTROL_CHANGED,
        DEBUG_CONTROL_RELEASED,

        // WebAssembly specific
        WASM_MODULE_LOADED,
        WASM_MODULE_ANALYZED,
        WASM_FUNCTION_CALLED,
        WASM_MEMORY_CHANGED,

        // Analysis sharing
        ANALYSIS_RESULT_SHARED,
        PERFORMANCE_DATA_SHARED,
        SECURITY_ALERT_SHARED,

        // General communication
        CHAT_MESSAGE,
        ANNOTATION_ADDED,
        ANNOTATION_REMOVED,

        // System messages
        ERROR_OCCURRED,
        CONNECTION_STATUS_CHANGED
    }
}
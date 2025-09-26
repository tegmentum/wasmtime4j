package ai.tegmentum.wasmtime4j.debug;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Debug protocol message for remote debugging communication.
 *
 * <p>This class represents a protocol message used for communication between
 * debug clients and the remote debug server. Messages include metadata such as
 * type, session ID, request ID, and arbitrary data payload.
 *
 * <p>Example usage:
 * <pre>{@code
 * DebugMessage message = DebugMessage.builder()
 *     .type(DebugMessageType.SET_BREAKPOINT)
 *     .sessionId("session-123")
 *     .data("functionName", "main")
 *     .data("line", 42)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class DebugMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final DebugMessageType type;
    private final String requestId;
    private final String sessionId;
    private final long timestamp;
    private final Map<String, Object> data;
    private final String error;

    private DebugMessage(final Builder builder) {
        this.type = builder.type;
        this.requestId = builder.requestId != null ? builder.requestId : UUID.randomUUID().toString();
        this.sessionId = builder.sessionId;
        this.timestamp = builder.timestamp != 0 ? builder.timestamp : System.currentTimeMillis();
        this.data = builder.data != null ? Map.copyOf(builder.data) : Collections.emptyMap();
        this.error = builder.error;
    }

    /**
     * Gets the message type.
     *
     * @return the message type
     */
    public DebugMessageType getType() {
        return type;
    }

    /**
     * Gets the request ID for correlation.
     *
     * @return the request ID
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Gets the debug session ID.
     *
     * @return the session ID, may be null
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Gets the message timestamp.
     *
     * @return the timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Gets all message data.
     *
     * @return immutable map of message data
     */
    public Map<String, Object> getData() {
        return data;
    }

    /**
     * Gets a specific data value by key.
     *
     * @param key the data key
     * @return the data value, may be null
     */
    public Object getData(final String key) {
        return data.get(key);
    }

    /**
     * Gets a specific data value by key with type casting.
     *
     * @param <T> the expected type
     * @param key the data key
     * @param type the expected type class
     * @return the typed data value, may be null
     * @throws ClassCastException if the value cannot be cast to the expected type
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(final String key, final Class<T> type) {
        final Object value = data.get(key);
        return value != null && type.isInstance(value) ? (T) value : null;
    }

    /**
     * Checks if the message contains data for the specified key.
     *
     * @param key the data key
     * @return true if data exists for the key
     */
    public boolean hasData(final String key) {
        return data.containsKey(key);
    }

    /**
     * Gets the error message if this is an error response.
     *
     * @return the error message, may be null
     */
    public String getError() {
        return error;
    }

    /**
     * Checks if this is an error message.
     *
     * @return true if this is an error message
     */
    public boolean isError() {
        return error != null;
    }

    /**
     * Checks if this is a request message (requires a response).
     *
     * @return true if this is a request message
     */
    public boolean isRequest() {
        return type != null && type.isRequest();
    }

    /**
     * Checks if this is a response message.
     *
     * @return true if this is a response message
     */
    public boolean isResponse() {
        return type != null && type.isResponse();
    }

    /**
     * Checks if this is a notification message (no response expected).
     *
     * @return true if this is a notification message
     */
    public boolean isNotification() {
        return type != null && type.isNotification();
    }

    @Override
    public String toString() {
        return "DebugMessage{" +
               "type=" + type +
               ", requestId='" + requestId + '\'' +
               ", sessionId='" + sessionId + '\'' +
               ", timestamp=" + timestamp +
               ", dataSize=" + data.size() +
               ", error='" + error + '\'' +
               '}';
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final DebugMessage that = (DebugMessage) obj;
        return timestamp == that.timestamp &&
               type == that.type &&
               requestId.equals(that.requestId) &&
               java.util.Objects.equals(sessionId, that.sessionId) &&
               data.equals(that.data) &&
               java.util.Objects.equals(error, that.error);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(type, requestId, sessionId, timestamp, data, error);
    }

    /**
     * Creates a response message for this request.
     *
     * @param responseType the response message type
     * @return a new builder for the response message
     * @throws IllegalStateException if this is not a request message
     */
    public Builder createResponse(final DebugMessageType responseType) {
        if (!isRequest()) {
            throw new IllegalStateException("Cannot create response for non-request message");
        }

        return builder()
            .type(responseType)
            .requestId(this.requestId)
            .sessionId(this.sessionId);
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for DebugMessage.
     */
    public static final class Builder {
        private DebugMessageType type;
        private String requestId;
        private String sessionId;
        private long timestamp;
        private Map<String, Object> data;
        private String error;

        private Builder() {
            // Private constructor
        }

        /**
         * Sets the message type.
         *
         * @param type the message type
         * @return this builder
         * @throws IllegalArgumentException if type is null
         */
        public Builder type(final DebugMessageType type) {
            if (type == null) {
                throw new IllegalArgumentException("Message type cannot be null");
            }
            this.type = type;
            return this;
        }

        /**
         * Sets the request ID.
         *
         * @param requestId the request ID
         * @return this builder
         */
        public Builder requestId(final String requestId) {
            this.requestId = requestId;
            return this;
        }

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
         * Sets the timestamp.
         *
         * @param timestamp the timestamp in milliseconds
         * @return this builder
         */
        public Builder timestamp(final long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * Adds a data entry.
         *
         * @param key the data key
         * @param value the data value
         * @return this builder
         * @throws IllegalArgumentException if key is null
         */
        public Builder data(final String key, final Object value) {
            if (key == null) {
                throw new IllegalArgumentException("Data key cannot be null");
            }
            if (this.data == null) {
                this.data = new HashMap<>();
            }
            this.data.put(key, value);
            return this;
        }

        /**
         * Adds multiple data entries.
         *
         * @param data the data map
         * @return this builder
         * @throws IllegalArgumentException if data is null
         */
        public Builder data(final Map<String, Object> data) {
            if (data == null) {
                throw new IllegalArgumentException("Data map cannot be null");
            }
            if (this.data == null) {
                this.data = new HashMap<>();
            }
            this.data.putAll(data);
            return this;
        }

        /**
         * Sets the error message.
         *
         * @param error the error message
         * @return this builder
         */
        public Builder error(final String error) {
            this.error = error;
            return this;
        }

        /**
         * Builds the DebugMessage.
         *
         * @return the debug message
         * @throws IllegalArgumentException if required fields are missing
         */
        public DebugMessage build() {
            if (type == null) {
                throw new IllegalArgumentException("Message type is required");
            }
            return new DebugMessage(this);
        }
    }
}
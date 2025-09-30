package ai.tegmentum.wasmtime4j.debug;

import java.util.List;

/**
 * Information about a remote debug client connection.
 *
 * <p>This record contains metadata about a connected debug client,
 * including connection details, authentication status, and capabilities.
 *
 * @param clientId the unique client identifier
 * @param remoteAddress the client's network address
 * @param protocolVersion the debug protocol version used by the client
 * @param connectionTime the time when the client connected (epoch milliseconds)
 * @param authenticated whether the client is authenticated
 * @param lastHeartbeat the time of the last heartbeat from the client (epoch milliseconds)
 *
 * @since 1.0.0
 */
public record RemoteClientInfo(
        String clientId,
        String remoteAddress,
        String protocolVersion,
        long connectionTime,
        boolean authenticated,
        long lastHeartbeat
) {

    /**
     * Checks if the client connection is considered alive.
     *
     * <p>A client is considered alive if the last heartbeat was received
     * within a reasonable time threshold (60 seconds).
     *
     * @return true if the client connection is alive
     */
    public boolean isAlive() {
        final long currentTime = System.currentTimeMillis();
        final long heartbeatAge = currentTime - lastHeartbeat;
        return heartbeatAge < 60_000; // 60 seconds
    }

    /**
     * Gets the connection duration in milliseconds.
     *
     * @return the connection duration
     */
    public long getConnectionDuration() {
        return System.currentTimeMillis() - connectionTime;
    }

    /**
     * Gets the time since last heartbeat in milliseconds.
     *
     * @return the time since last heartbeat
     */
    public long getTimeSinceLastHeartbeat() {
        return System.currentTimeMillis() - lastHeartbeat;
    }

    /**
     * Formats the connection information as a human-readable string.
     *
     * @return formatted connection information
     */
    public String formatInfo() {
        return String.format(
            "Client %s: %s (v%s) - Connected: %dms ago, Last heartbeat: %dms ago, Auth: %s",
            clientId,
            remoteAddress,
            protocolVersion != null ? protocolVersion : "unknown",
            getConnectionDuration(),
            getTimeSinceLastHeartbeat(),
            authenticated ? "yes" : "no"
        );
    }
}
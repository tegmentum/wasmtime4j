package ai.tegmentum.wasmtime4j.debug;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Remote debugging client connection handler.
 *
 * <p>This class manages individual client connections to the remote debug server,
 * handling message serialization, encryption, and communication protocol.
 *
 * @since 1.0.0
 */
final class RemoteDebugClient implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(RemoteDebugClient.class.getName());

    private static final int BUFFER_SIZE = 8192;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;

    private final String clientId;
    private final AsynchronousSocketChannel channel;
    private final RemoteDebugSession remoteSession;
    private final SecretKey encryptionKey;
    private final AtomicBoolean connected;
    private final AtomicLong lastHeartbeat;

    private String protocolVersion;
    private List<String> capabilities;
    private boolean authenticated;
    private long connectionTime;

    RemoteDebugClient(
            final String clientId,
            final AsynchronousSocketChannel channel,
            final RemoteDebugSession remoteSession,
            final SecretKey encryptionKey) {
        this.clientId = clientId;
        this.channel = channel;
        this.remoteSession = remoteSession;
        this.encryptionKey = encryptionKey;
        this.connected = new AtomicBoolean(true);
        this.lastHeartbeat = new AtomicLong(System.currentTimeMillis());
        this.connectionTime = System.currentTimeMillis();
        this.authenticated = encryptionKey == null; // Auto-authenticate if no encryption
    }

    void start() {
        startReadLoop();
    }

    void sendMessage(final DebugMessage message) throws IOException {
        if (!connected.get()) {
            throw new IOException("Client is not connected");
        }

        try {
            // Serialize message
            final byte[] messageData = serializeMessage(message);

            // Encrypt if encryption is enabled
            final byte[] finalData = encryptionKey != null
                ? encryptMessage(messageData)
                : messageData;

            // Create buffer with length prefix
            final ByteBuffer buffer = ByteBuffer.allocate(4 + finalData.length);
            buffer.putInt(finalData.length);
            buffer.put(finalData);
            buffer.flip();

            // Send asynchronously
            channel.write(buffer, null, new CompletionHandler<Integer, Void>() {
                @Override
                public void completed(final Integer result, final Void attachment) {
                    // Message sent successfully
                }

                @Override
                public void failed(final Throwable exc, final Void attachment) {
                    LOGGER.log(Level.WARNING, "Failed to send message to client: " + clientId, exc);
                    close();
                }
            });

        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Error sending message to client: " + clientId, e);
            throw new IOException("Failed to send message", e);
        }
    }

    CompletableFuture<DebugMessage> sendRequest(final DebugMessage request) {
        final CompletableFuture<DebugMessage> future = new CompletableFuture<>();

        try {
            sendMessage(request);
            // In a full implementation, this would track the request ID and complete the future
            // when the response is received
            future.complete(null); // Placeholder
        } catch (final IOException e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    void updateLastHeartbeat() {
        lastHeartbeat.set(System.currentTimeMillis());
    }

    String getClientId() {
        return clientId;
    }

    String getRemoteAddress() {
        try {
            return channel.getRemoteAddress().toString();
        } catch (final IOException e) {
            return "unknown";
        }
    }

    String getProtocolVersion() {
        return protocolVersion;
    }

    void setProtocolVersion(final String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    List<String> getCapabilities() {
        return capabilities;
    }

    void setCapabilities(final List<String> capabilities) {
        this.capabilities = capabilities;
    }

    long getConnectionTime() {
        return connectionTime;
    }

    boolean isAuthenticated() {
        return authenticated;
    }

    void setAuthenticated(final boolean authenticated) {
        this.authenticated = authenticated;
    }

    long getLastHeartbeat() {
        return lastHeartbeat.get();
    }

    boolean isConnected() {
        return connected.get() && channel.isOpen();
    }

    @Override
    public void close() {
        if (!connected.compareAndSet(true, false)) {
            return;
        }

        try {
            if (channel.isOpen()) {
                channel.close();
            }
        } catch (final IOException e) {
            LOGGER.log(Level.WARNING, "Error closing client channel: " + clientId, e);
        }

        // Notify remote session
        remoteSession.removeClient(clientId);
    }

    // Private methods

    private void startReadLoop() {
        readMessage();
    }

    private void readMessage() {
        if (!connected.get()) {
            return;
        }

        // First read the message length (4 bytes)
        final ByteBuffer lengthBuffer = ByteBuffer.allocate(4);

        channel.read(lengthBuffer, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(final Integer bytesRead, final Void attachment) {
                if (bytesRead == -1) {
                    // Connection closed by client
                    close();
                    return;
                }

                if (lengthBuffer.hasRemaining()) {
                    // Need to read more bytes for the length
                    channel.read(lengthBuffer, null, this);
                    return;
                }

                // Length is complete, read the message
                lengthBuffer.flip();
                final int messageLength = lengthBuffer.getInt();

                if (messageLength <= 0 || messageLength > 10 * 1024 * 1024) { // 10MB limit
                    LOGGER.warning("Invalid message length from client: " + clientId + ", length: " + messageLength);
                    close();
                    return;
                }

                readMessageContent(messageLength);
            }

            @Override
            public void failed(final Throwable exc, final Void attachment) {
                LOGGER.log(Level.WARNING, "Error reading message length from client: " + clientId, exc);
                close();
            }
        });
    }

    private void readMessageContent(final int messageLength) {
        final ByteBuffer messageBuffer = ByteBuffer.allocate(messageLength);

        channel.read(messageBuffer, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(final Integer bytesRead, final Void attachment) {
                if (bytesRead == -1) {
                    // Connection closed by client
                    close();
                    return;
                }

                if (messageBuffer.hasRemaining()) {
                    // Need to read more bytes for the message
                    channel.read(messageBuffer, null, this);
                    return;
                }

                // Message is complete
                messageBuffer.flip();
                final byte[] messageData = new byte[messageBuffer.remaining()];
                messageBuffer.get(messageData);

                // Process the message
                processReceivedMessage(messageData);

                // Continue reading next message
                readMessage();
            }

            @Override
            public void failed(final Throwable exc, final Void attachment) {
                LOGGER.log(Level.WARNING, "Error reading message content from client: " + clientId, exc);
                close();
            }
        });
    }

    private void processReceivedMessage(final byte[] messageData) {
        try {
            // Decrypt if encryption is enabled
            final byte[] decryptedData = encryptionKey != null
                ? decryptMessage(messageData)
                : messageData;

            // Deserialize message
            final DebugMessage message = deserializeMessage(decryptedData);

            // Update heartbeat
            updateLastHeartbeat();

            // Handle message asynchronously
            remoteSession.handleClientMessage(clientId, message)
                .thenAccept(response -> {
                    if (response != null) {
                        try {
                            sendMessage(response);
                        } catch (final IOException e) {
                            LOGGER.log(Level.WARNING, "Error sending response to client: " + clientId, e);
                        }
                    }
                })
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING, "Error handling message from client: " + clientId, throwable);
                    return null;
                });

        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Error processing message from client: " + clientId, e);

            // Send error response
            try {
                final DebugMessage errorResponse = DebugMessage.builder()
                    .type(DebugMessageType.ERROR)
                    .error("Failed to process message: " + e.getMessage())
                    .build();
                sendMessage(errorResponse);
            } catch (final IOException sendError) {
                LOGGER.log(Level.WARNING, "Error sending error response to client: " + clientId, sendError);
                close();
            }
        }
    }

    private byte[] serializeMessage(final DebugMessage message) throws IOException {
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
             final ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(message);
            oos.flush();
            return baos.toByteArray();
        }
    }

    private DebugMessage deserializeMessage(final byte[] data) throws IOException, ClassNotFoundException {
        try (final ByteArrayInputStream bais = new ByteArrayInputStream(data);
             final ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (DebugMessage) ois.readObject();
        }
    }

    private byte[] encryptMessage(final byte[] data) throws Exception {
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        // Generate random IV
        final byte[] iv = new byte[GCM_IV_LENGTH];
        new java.security.SecureRandom().nextBytes(iv);

        final GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, gcmSpec);

        final byte[] encryptedData = cipher.doFinal(data);

        // Combine IV and encrypted data
        final byte[] result = new byte[GCM_IV_LENGTH + encryptedData.length];
        System.arraycopy(iv, 0, result, 0, GCM_IV_LENGTH);
        System.arraycopy(encryptedData, 0, result, GCM_IV_LENGTH, encryptedData.length);

        return result;
    }

    private byte[] decryptMessage(final byte[] data) throws Exception {
        if (data.length < GCM_IV_LENGTH) {
            throw new IllegalArgumentException("Encrypted data too short");
        }

        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        // Extract IV
        final byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(data, 0, iv, 0, GCM_IV_LENGTH);

        // Extract encrypted data
        final byte[] encryptedData = new byte[data.length - GCM_IV_LENGTH];
        System.arraycopy(data, GCM_IV_LENGTH, encryptedData, 0, encryptedData.length);

        final GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey, gcmSpec);

        return cipher.doFinal(encryptedData);
    }
}
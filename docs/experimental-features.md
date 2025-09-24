# WASI Preview 2 Experimental Features

This document describes the experimental WASI Preview 2 features implemented in wasmtime4j. These features provide cutting-edge capabilities for filesystem operations, networking, I/O, and process management.

## Overview

The experimental features in wasmtime4j implement the latest WASI Preview 2 specifications and upcoming WASI 0.3 features. These features are designed to provide:

- **Advanced Filesystem Operations**: Snapshots, incremental backups, and advanced file management
- **Modern Networking Protocols**: HTTP/2, HTTP/3, WebSocket, and advanced TCP/UDP features
- **High-Performance I/O**: Async file operations, memory mapping, and vectored I/O
- **Process Management**: Sandboxing, resource monitoring, and inter-process communication

## Feature Stability and Support Lifecycle

### Stability Levels

| Level | Description | API Changes | Production Use |
|-------|-------------|-------------|----------------|
| **Experimental** | Early implementation, active development | Breaking changes possible | Not recommended |
| **Preview** | Feature-complete, stabilizing API | Minor changes possible | Careful evaluation recommended |
| **Stable** | Production-ready, stable API | Backwards compatible | Recommended |
| **Deprecated** | Being phased out | No new changes | Migration recommended |

### Current Feature Status

| Feature | JNI Status | Panama Status | Stability Level | Expected Stable |
|---------|------------|---------------|-----------------|-----------------|
| Filesystem Snapshots | Experimental | Experimental | Experimental | Q2 2025 |
| HTTP/2 Client | Experimental | Experimental | Preview | Q1 2025 |
| HTTP/3 Client | Experimental | Experimental | Experimental | Q3 2025 |
| WebSocket Support | Experimental | Experimental | Preview | Q1 2025 |
| Async File I/O | Experimental | Experimental | Experimental | Q2 2025 |
| Memory Mapping | Experimental | Experimental | Experimental | Q3 2025 |
| Process Sandboxing | Experimental | Experimental | Experimental | Q4 2025 |
| Resource Monitoring | Experimental | Experimental | Experimental | Q2 2025 |
| IPC Channels | Experimental | Experimental | Experimental | Q4 2025 |
| System Services | Experimental | Experimental | Experimental | 2026 |

## Filesystem Snapshots

### Overview
Filesystem snapshots provide point-in-time capture and restoration of filesystem state, including file contents, metadata, permissions, and directory structure.

### Key Features
- **Full Snapshots**: Complete filesystem state capture
- **Incremental Snapshots**: Delta-based snapshots for efficiency
- **Cross-Platform Compatibility**: Portable snapshot format
- **Compression and Encryption**: Optional data protection
- **Integrity Verification**: Checksum-based validation

### Usage Example

```java
// Create filesystem snapshot handler
WasiFilesystemSnapshot snapshotHandler = new WasiFilesystemSnapshot(wasiContext, asyncExecutor);

// Configure snapshot options
WasiFilesystemSnapshot.SnapshotOptions options =
    new WasiFilesystemSnapshot.SnapshotOptions(
        false,  // includeHiddenFiles
        6,      // compressionLevel (0-9)
        true,   // encryptionEnabled
        encryptionKey);

// Create full snapshot
CompletableFuture<Long> snapshotFuture =
    snapshotHandler.createFullSnapshotAsync("/path/to/directory", options);
Long snapshotHandle = snapshotFuture.get(30, TimeUnit.SECONDS);

// Create incremental snapshot
CompletableFuture<Long> incrementalFuture =
    snapshotHandler.createIncrementalSnapshotAsync(
        "/path/to/directory", snapshotHandle, options);

// Restore from snapshot
WasiFilesystemSnapshot.RestoreOptions restoreOptions =
    WasiFilesystemSnapshot.RestoreOptions.defaultOptions();
CompletableFuture<Void> restoreFuture =
    snapshotHandler.restoreFromSnapshotAsync(
        snapshotHandle, "/path/to/restore", restoreOptions);

// Verify snapshot integrity
CompletableFuture<SnapshotVerificationResult> verifyFuture =
    snapshotHandler.verifySnapshotAsync(snapshotHandle);
```

### Limitations
- Maximum snapshot size: 1GB
- Maximum concurrent snapshots: 100
- Experimental API may change

## Advanced Networking

### Overview
Advanced networking provides modern protocol support including HTTP/2, HTTP/3, WebSocket, and enhanced TCP/UDP capabilities.

### Supported Protocols
- **HTTP/2**: Multiplexed connections with server push
- **HTTP/3**: QUIC-based protocol for improved performance
- **WebSocket**: Bidirectional real-time communication
- **Enhanced TCP/UDP**: Advanced socket options and multicast

### HTTP/2 Usage

```java
// Create advanced networking handler
WasiAdvancedNetworking networkHandler = new WasiAdvancedNetworking(wasiContext, asyncExecutor);

// Configure HTTP/2 options
WasiAdvancedNetworking.Http2Options http2Options =
    new WasiAdvancedNetworking.Http2Options(
        100,    // maxConcurrentStreams
        65536,  // windowSize
        true,   // enableServerPush
        4096,   // headerTableSize
        30000   // connectionTimeoutMs
    );

// Create HTTP/2 connection
CompletableFuture<Long> connectionFuture =
    networkHandler.createHttp2ConnectionAsync("example.com", 443, true, http2Options);
Long connectionHandle = connectionFuture.get(15, TimeUnit.SECONDS);

// Make HTTP/2 request
Map<String, String> headers = Map.of("Accept", "application/json");
CompletableFuture<Http2Response> requestFuture =
    networkHandler.http2RequestAsync(connectionHandle, "GET", "/api/data", headers, null);
Http2Response response = requestFuture.get(15, TimeUnit.SECONDS);
```

### WebSocket Usage

```java
// Create WebSocket connection
WasiAdvancedNetworking.WebSocketOptions wsOptions =
    WasiAdvancedNetworking.WebSocketOptions.defaultOptions();

CompletableFuture<Long> wsFuture =
    networkHandler.createWebSocketAsync(
        "wss://example.com/websocket",
        List.of("protocol"),
        Map.of("Authorization", "Bearer token"),
        wsOptions);
Long webSocketHandle = wsFuture.get(15, TimeUnit.SECONDS);

// Send message
ByteBuffer message = ByteBuffer.wrap("Hello WebSocket!".getBytes());
networkHandler.sendWebSocketMessageAsync(
    webSocketHandle, message, WebSocketMessageType.TEXT);

// Receive message
ByteBuffer receiveBuffer = ByteBuffer.allocate(1024);
CompletableFuture<WebSocketMessage> receiveFuture =
    networkHandler.receiveWebSocketMessageAsync(webSocketHandle, receiveBuffer, 10000);
```

## Experimental I/O Operations

### Overview
Experimental I/O provides advanced file operations including async I/O, memory mapping, file locking, and directory watching.

### Key Features
- **Async File I/O**: Non-blocking file operations with callbacks
- **Memory Mapping**: Direct memory access to files
- **File Locking**: Advisory and mandatory locking
- **Directory Watching**: File system event monitoring
- **Vectored I/O**: Scatter-gather I/O operations

### Async I/O Usage

```java
// Create experimental I/O handler
WasiExperimentalIO ioHandler = new WasiExperimentalIO(wasiContext, asyncExecutor);

// Async file read with callback
ByteBuffer readBuffer = ByteBuffer.allocate(1024);
CompletableFuture<Long> readFuture =
    ioHandler.asyncReadFileAsync(
        fileHandle, 0, readBuffer,
        result -> {
            if (result.completed && result.error == null) {
                System.out.println("Read " + result.bytesTransferred + " bytes");
            }
        });

// Memory mapping
CompletableFuture<Long> mappingFuture =
    ioHandler.createMemoryMappingAsync(
        fileHandle, 0, fileSize,
        MemoryProtection.READ_WRITE,
        MappingFlags.SHARED);
Long mappingHandle = mappingFuture.get(10, TimeUnit.SECONDS);

// File locking
CompletableFuture<Long> lockFuture =
    ioHandler.acquireFileLockAsync(
        fileHandle, 0, 0, FileLockType.EXCLUSIVE, false);
Long lockHandle = lockFuture.get(10, TimeUnit.SECONDS);

// Directory watching
CompletableFuture<Long> watcherFuture =
    ioHandler.createDirectoryWatcherAsync(
        "/path/to/watch", true,
        FileSystemEventMask.ALL,
        event -> System.out.println("File event: " + event.eventType));
```

### Vectored I/O

```java
// Vectored read with multiple buffers
List<ByteBuffer> readBuffers = List.of(
    ByteBuffer.allocate(100),
    ByteBuffer.allocate(200),
    ByteBuffer.allocate(150)
);

CompletableFuture<Integer> vectoredReadFuture =
    ioHandler.vectoredReadAsync(fileHandle, 0, readBuffers);
Integer totalBytesRead = vectoredReadFuture.get(10, TimeUnit.SECONDS);
```

## Process Management and Sandboxing

### Overview
Experimental process management provides enhanced sandboxing, resource monitoring, inter-process communication, and system service integration.

### Key Features
- **Enhanced Sandboxing**: Capability-based security model
- **Resource Monitoring**: CPU, memory, I/O, and network monitoring
- **IPC Channels**: Pipes, shared memory, message queues
- **System Services**: Service registration and discovery
- **Container Support**: Namespace and container integration

### Sandboxed Process Creation

```java
// Create experimental process handler
WasiExperimentalProcess processHandler = new WasiExperimentalProcess(wasiContext, asyncExecutor);

// Configure sandbox
SandboxConfig sandboxConfig = new SandboxConfig(
    SandboxType.STRICT,
    SandboxCapabilities.FILE_READ.combine(SandboxCapabilities.NETWORK_CLIENT),
    true,   // allowNetworking
    true,   // allowFileSystemAccess
    false   // allowProcessControl
);

// Set resource limits
ProcessResourceLimits resourceLimits = new ProcessResourceLimits(
    512 * 1024 * 1024,  // 512MB memory
    50,                 // 50% CPU
    256,                // 256 file descriptors
    10,                 // 10 processes
    300                 // 300 second timeout
);

// Create sandboxed process
CompletableFuture<Long> processFuture =
    processHandler.createSandboxedProcessAsync(
        "/usr/bin/wasm-module",
        List.of("--arg1", "value1"),
        Map.of("ENV_VAR", "value"),
        sandboxConfig,
        resourceLimits);
Long processHandle = processFuture.get(15, TimeUnit.SECONDS);
```

### Resource Monitoring

```java
// Configure resource monitoring
ResourceMonitoringConfig monitoringConfig = new ResourceMonitoringConfig(
    5,                  // 5 second interval
    256 * 1024 * 1024, // 256MB memory threshold
    75,                 // 75% CPU threshold
    10 * 1024 * 1024,  // 10MB/s I/O threshold
    5 * 1024 * 1024,   // 5MB/s network threshold
    true                // detailed stats
);

// Create resource monitor
CompletableFuture<Long> monitorFuture =
    processHandler.createResourceMonitorAsync(
        processHandle,
        monitoringConfig,
        alert -> {
            System.out.println("Resource alert: " + alert.alertType);
            System.out.println("Current: " + alert.currentValue +
                              ", Threshold: " + alert.thresholdValue);
        });
```

### System Service Registration

```java
// Define service metadata
SystemServiceMetadata metadata = new SystemServiceMetadata(
    "1.0.0",
    "Example service",
    List.of("data-processing", "analytics"),
    List.of("/api/process", "/api/status"),
    8080,
    true  // secure
);

// Register system service
CompletableFuture<String> registerFuture =
    processHandler.registerSystemServiceAsync(
        "example-service",
        metadata,
        request -> {
            // Handle service request
            System.out.println("Service request: " + request.operation);
            // Process request and return response
        });
String serviceId = registerFuture.get(10, TimeUnit.SECONDS);
```

### IPC Channel Creation

```java
// Configure IPC channel
IPCChannelConfig channelConfig = new IPCChannelConfig(
    64 * 1024,  // 64KB buffer
    1000,       // max messages
    true,       // bidirectional
    false       // not persistent
);

// Create IPC channel between processes
CompletableFuture<Long> ipcFuture =
    processHandler.createIPCChannelAsync(
        sourceProcessHandle,
        targetProcessHandle,
        IPCChannelType.SHARED_MEMORY,
        channelConfig);
Long ipcHandle = ipcFuture.get(10, TimeUnit.SECONDS);
```

## Error Handling

### Exception Hierarchy
All experimental features use the standard WASI exception hierarchy:

- `WasiException`: Base exception for WASI operations
- `WasiFileSystemException`: Filesystem-specific errors
- `WasiPermissionException`: Permission and security errors
- `WasiNetworkException`: Network operation errors

### Error Codes
Experimental features use standard WASI error codes:

- `ENOENT`: Resource not found
- `EPERM`: Operation not permitted
- `EINVAL`: Invalid argument
- `ENOMEM`: Insufficient memory
- `EIO`: I/O error
- `ENOTCONN`: Not connected
- `ETIMEDOUT`: Operation timed out

### Defensive Error Handling

```java
try {
    Long snapshotHandle = snapshotHandler
        .createFullSnapshotAsync("/path", options)
        .get(30, TimeUnit.SECONDS);
} catch (WasiException e) {
    switch (e.getErrorCode()) {
        case ENOENT -> System.err.println("Directory not found");
        case EPERM -> System.err.println("Permission denied");
        case ENOMEM -> System.err.println("Insufficient memory");
        default -> System.err.println("Snapshot failed: " + e.getMessage());
    }
} catch (ExecutionException e) {
    if (e.getCause() instanceof WasiException wasiError) {
        // Handle WASI-specific error
    } else {
        // Handle other execution errors
    }
} catch (TimeoutException e) {
    System.err.println("Snapshot creation timed out");
}
```

## Performance Considerations

### Resource Limits
- **Filesystem Snapshots**: 1GB maximum size, 100 concurrent snapshots
- **Network Connections**: 100 concurrent connections per pool
- **WebSocket Frames**: 1MB maximum frame size
- **Memory Mappings**: 2GB maximum mapping size
- **Vectored I/O**: 1024 maximum buffers per operation
- **Process Monitoring**: 3600 second maximum interval

### Performance Optimization
1. **Use Async Operations**: Leverage async APIs for better concurrency
2. **Batch Operations**: Use vectored I/O for multiple small operations
3. **Connection Pooling**: Reuse HTTP/2 connections when possible
4. **Incremental Snapshots**: Use incremental snapshots for large filesystems
5. **Resource Monitoring**: Set appropriate monitoring intervals
6. **Memory Management**: Close resources promptly to prevent leaks

### Monitoring and Observability

```java
// Enable detailed performance monitoring
ResourceMonitoringConfig config = new ResourceMonitoringConfig(
    1,    // frequent monitoring
    0,    // no memory threshold
    0,    // no CPU threshold
    0,    // no I/O threshold
    0,    // no network threshold
    true  // enable detailed stats
);

processHandler.createResourceMonitorAsync(processHandle, config, alert -> {
    // Log all resource usage for analysis
    logger.info("Resource usage: CPU={}%, Memory={}MB",
                alert.currentValue, alert.thresholdValue);
});
```

## Best Practices

### 1. Feature Detection

```java
// Check if experimental features are available
public static boolean areExperimentalFeaturesAvailable() {
    try {
        // Attempt to create handlers to test availability
        WasiFilesystemSnapshot snapshot = new WasiFilesystemSnapshot(context, executor);
        snapshot.close();
        return true;
    } catch (UnsupportedOperationException e) {
        return false;
    }
}
```

### 2. Graceful Degradation

```java
// Provide fallbacks for experimental features
public CompletableFuture<Void> backupDirectory(String path) {
    if (areExperimentalFeaturesAvailable()) {
        // Use experimental snapshot feature
        return snapshotHandler.createFullSnapshotAsync(path, options)
            .thenApply(handle -> null);
    } else {
        // Fall back to traditional backup method
        return CompletableFuture.runAsync(() -> traditionalBackup(path));
    }
}
```

### 3. Resource Management

```java
// Always use try-with-resources pattern
try (WasiFilesystemSnapshot snapshotHandler =
         new WasiFilesystemSnapshot(context, executor)) {

    Long handle = snapshotHandler
        .createFullSnapshotAsync("/data", options)
        .get(30, TimeUnit.SECONDS);

    // Use snapshot...

} // Automatically closed
```

### 4. Testing Strategy

```java
@Test
@EnabledIf("areExperimentalFeaturesAvailable")
void testExperimentalFeature() {
    // Test only runs if experimental features are available
    assertThat(snapshotHandler.listSnapshots()).isEmpty();
}

@Test
void testFallbackBehavior() {
    // Test fallback behavior when experimental features unavailable
    assumeFalse(areExperimentalFeaturesAvailable());
    // Test traditional implementation
}
```

## Migration Guide

### From Traditional I/O to Experimental I/O

```java
// Traditional synchronous I/O
byte[] data = Files.readAllBytes(path);

// Experimental async I/O
ByteBuffer buffer = ByteBuffer.allocate(data.length);
ioHandler.asyncReadFileAsync(fileHandle, 0, buffer, result -> {
    if (result.completed) {
        // Process data from buffer
    }
}).get();
```

### From Standard HTTP to HTTP/2

```java
// Traditional HTTP client
HttpURLConnection connection = (HttpURLConnection) url.openConnection();
int responseCode = connection.getResponseCode();

// Experimental HTTP/2
Long connectionHandle = networkHandler
    .createHttp2ConnectionAsync(host, port, true, options)
    .get();

Http2Response response = networkHandler
    .http2RequestAsync(connectionHandle, "GET", path, headers, null)
    .get();
```

## Troubleshooting

### Common Issues

1. **UnsupportedOperationException**: Experimental features not available
   - Check Java version (Panama requires Java 23+)
   - Verify native library includes experimental support
   - Enable experimental features in WASI context

2. **WasiException with ENOMEM**: Resource limits exceeded
   - Check active snapshot/connection counts
   - Increase system resource limits
   - Implement resource pooling

3. **TimeoutException**: Operations taking too long
   - Increase timeout values
   - Check system performance
   - Use smaller operation sizes

4. **WasiException with EPERM**: Permission denied
   - Check sandbox configuration
   - Verify file system permissions
   - Review capability settings

### Debug Logging

```java
// Enable debug logging for experimental features
Logger.getLogger("ai.tegmentum.wasmtime4j.jni.wasi").setLevel(Level.FINE);

// Log experimental feature usage
logger.info("Creating snapshot with {} compression", options.compressionLevel);
```

## Future Roadmap

### WASI 0.3 Features (2025)
- Native async I/O support in Component Model
- Enhanced stream operations
- Improved error handling
- Better resource management

### Planned Improvements
- **Q1 2025**: HTTP/2 and WebSocket stabilization
- **Q2 2025**: Async I/O and snapshot stabilization
- **Q3 2025**: HTTP/3 and memory mapping stabilization
- **Q4 2025**: Process management stabilization
- **2026**: System services and full WASI 1.0 support

### API Evolution
- Gradual stabilization of experimental APIs
- Backwards compatibility where possible
- Migration guides for breaking changes
- Deprecation warnings before removal

## Conclusion

The experimental WASI Preview 2 features in wasmtime4j provide cutting-edge capabilities for modern WebAssembly applications. While these features are still evolving, they offer significant advantages for applications requiring advanced filesystem operations, modern networking protocols, high-performance I/O, and sophisticated process management.

When using experimental features:
- Plan for API changes
- Implement fallback strategies
- Monitor stability announcements
- Participate in community feedback
- Test thoroughly in development environments

For the latest updates on experimental features, check the [wasmtime4j releases](https://github.com/tegmentum-ai/wasmtime4j/releases) and [WASI specification updates](https://github.com/WebAssembly/WASI).
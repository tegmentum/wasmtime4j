# UDP Datagram Implementation Roadmap

**Project**: wasmtime4j
**Target**: Complete UDP socket datagram send/receive operations
**Created**: 2025-11-25

## Current Status

UDP sockets are **95% complete**:
- ✅ Socket creation, binding, and configuration (12/14 methods)
- ❌ Datagram send/receive operations (2/14 methods)

**Blocker**: Native Rust layer has only MVP stub implementations

---

## Problem Analysis

### Missing Operations

Two critical methods are unimplemented:

```java
// wasmtime4j/src/main/java/ai/tegmentum/wasmtime4j/wasi/sockets/WasiUdpSocket.java
IncomingDatagram[] receive(long maxResults) throws WasmException;
long send(OutgoingDatagram[] datagrams) throws WasmException;
```

### Root Cause

The native Rust helper functions exist but contain only MVP stubs:

**File**: `wasmtime4j-native/src/wasi_sockets_helpers.rs:385-401`

```rust
pub fn udp_socket_receive(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
    _max_results: u64,
) -> WasmtimeResult<Vec<(Vec<u8>, IpSocketAddress)>> {
    // MVP: Return empty datagram list
    Ok(Vec::new())
}

pub fn udp_socket_send(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
    _datagrams: &[(Vec<u8>, Option<IpSocketAddress>)],
) -> WasmtimeResult<u64> {
    // MVP: Return count of datagrams "sent"
    Ok(_datagrams.len() as u64)
}
```

### Impact

- UDP sockets can be created, configured, and bound
- Cannot actually send or receive UDP datagrams
- Java layer correctly marks these as TODO/UnsupportedOperationException

### Architecture Context

The implementation stack has three layers:

```
[Java Layer - wasmtime4j-jni/panama]
           ↓ (JNI/Panama FFI calls)
[Native Bindings - jni_wasi_sockets_bindings.rs / panama_wasi_sockets_ffi.rs]
           ↓ (calls helper functions)
[Helper Functions - wasi_sockets_helpers.rs] ← **NEEDS IMPLEMENTATION**
```

---

## Implementation Plan

### Phase 1: Native Rust Helper Functions

**Goal**: Implement actual UDP datagram operations in `wasi_sockets_helpers.rs`

**Files to Modify**:
1. `wasmtime4j-native/src/wasi_sockets_helpers.rs`

**Tasks**:

#### Task 1.1: Implement `udp_socket_receive()`

**Function Signature**:
```rust
pub fn udp_socket_receive(
    context: &WasiPreview2Context,
    socket_handle: u64,
    max_results: u64,
) -> WasmtimeResult<Vec<(Vec<u8>, IpSocketAddress)>>
```

**Implementation Requirements**:
1. Retrieve UDP socket from context by handle
2. Perform non-blocking receive operation (respecting `max_results` limit)
3. For each datagram received:
   - Extract data buffer (`Vec<u8>`)
   - Extract source address (`IpSocketAddress`)
   - Add tuple to results vector
4. Handle socket errors (EWOULDBLOCK, EAGAIN, etc.)
5. Convert OS errors to WasmtimeResult
6. Return vector of (data, source_address) tuples

**Error Handling**:
- Socket not found → `WasmtimeError::InvalidHandle`
- Socket not bound → `WasmtimeError::InvalidState`
- OS errors → `WasmtimeError::IoError`

**Reference Implementation**:
- Look at `tcp_socket_accept()` for socket retrieval pattern
- Look at networking.rs for actual socket operations

#### Task 1.2: Implement `udp_socket_send()`

**Function Signature**:
```rust
pub fn udp_socket_send(
    context: &WasiPreview2Context,
    socket_handle: u64,
    datagrams: &[(Vec<u8>, Option<IpSocketAddress>)],
) -> WasmtimeResult<u64>
```

**Implementation Requirements**:
1. Retrieve UDP socket from context by handle
2. For each datagram in input array:
   - Extract data buffer
   - Determine destination address:
     - If provided: use explicit address
     - If None: use address from `stream()` call
   - Perform non-blocking send operation
   - Count successful sends
   - Stop on first error or EWOULDBLOCK
3. Return count of successfully sent datagrams
4. Handle partial sends appropriately

**Error Handling**:
- Socket not found → `WasmtimeError::InvalidHandle`
- Socket not bound and no address → `WasmtimeError::InvalidState`
- OS errors → `WasmtimeError::IoError`
- Datagram too large → `WasmtimeError::InvalidArgument`

**Important**: Follow WASI Preview 2 specification:
- Must respect socket buffer limits
- Return partial success count if some datagrams sent
- Non-blocking behavior (return immediately if would block)

### Phase 2: JNI Bindings

**Goal**: Add JNI native method implementations

**Files to Modify**:
1. `wasmtime4j-native/src/jni_wasi_sockets_bindings.rs`

**Tasks**:

#### Task 2.1: Implement `nativeReceive()`

Add JNI function:
```rust
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiUdpSocket_nativeReceive(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
    max_results: jlong,
) -> jobjectArray
```

**Implementation**:
1. Convert handles to internal types
2. Call `udp_socket_receive()`
3. Convert Rust Vec to Java array:
   - Create `IncomingDatagram[]` array
   - For each (data, address):
     - Create byte array for data
     - Encode address using `encode_ip_socket_address()`
     - Create IncomingDatagram object
     - Add to array
4. Handle errors and convert to WasmException
5. Return Java array

#### Task 2.2: Implement `nativeSend()`

Add JNI function:
```rust
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiUdpSocket_nativeSend(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
    datagrams: jobjectArray,
) -> jlong
```

**Implementation**:
1. Convert Java OutgoingDatagram[] to Rust vector:
   - Iterate over Java array
   - Extract data (byte[])
   - Extract optional remote address
   - Decode address if present
   - Build Rust tuple
2. Call `udp_socket_send()`
3. Return count of sent datagrams
4. Handle errors and convert to WasmException

### Phase 3: Panama FFI Bindings

**Goal**: Add Panama FFI native function implementations

**Files to Modify**:
1. `wasmtime4j-native/src/panama_wasi_sockets_ffi.rs`

**Tasks**:

#### Task 3.1: Implement `wasmtime4j_panama_wasi_udp_socket_receive()`

Add Panama FFI function:
```rust
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_udp_socket_receive(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    max_results: c_longlong,
    out_count: *mut c_longlong,
    out_datagrams: *mut c_void, // pointer to array
) -> c_int
```

**Implementation**:
1. Validate pointers
2. Call `udp_socket_receive()`
3. Write results to output parameters:
   - Set `*out_count` to number of datagrams
   - For each datagram:
     - Write data length and data bytes
     - Write address components
4. Return error code (0 = success)

**Memory Management**:
- Caller allocates output buffer
- Function writes to provided buffer
- Return INSUFFICIENT_BUFFER if too small

#### Task 3.2: Implement `wasmtime4j_panama_wasi_udp_socket_send()`

Add Panama FFI function:
```rust
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_udp_socket_send(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    datagram_count: c_longlong,
    datagram_data: *const c_void, // array of data pointers
    datagram_lengths: *const c_longlong, // array of lengths
    datagram_addresses: *const c_void, // array of addresses (optional)
    out_sent_count: *mut c_longlong,
) -> c_int
```

**Implementation**:
1. Validate pointers
2. Build Rust vector from input arrays
3. Call `udp_socket_send()`
4. Write sent count to `*out_sent_count`
5. Return error code

### Phase 4: Java Layer Completion

**Goal**: Remove TODO/UnsupportedOperationException and implement actual calls

**Files to Modify**:
1. `wasmtime4j-jni/src/main/java/ai/tegmentum/wasmtime4j/jni/wasi/sockets/JniWasiUdpSocket.java`
2. `wasmtime4j-panama/src/main/java/ai/tegmentum/wasmtime4j/panama/wasi/sockets/PanamaWasiUdpSocket.java`

**Tasks**:

#### Task 4.1: Implement JniWasiUdpSocket.receive()

Replace lines 280-287:
```java
@Override
public IncomingDatagram[] receive(final long maxResults) throws WasmException {
    if (closed) {
        throw new WasmException("Socket is closed");
    }
    if (maxResults < 0) {
        throw new IllegalArgumentException("maxResults must be non-negative");
    }

    return nativeReceive(contextHandle, socketHandle, maxResults);
}

// Add native method declaration
private static native IncomingDatagram[] nativeReceive(
    long contextHandle, long socketHandle, long maxResults);
```

#### Task 4.2: Implement JniWasiUdpSocket.send()

Replace lines 290-297:
```java
@Override
public long send(final OutgoingDatagram[] datagrams) throws WasmException {
    if (closed) {
        throw new WasmException("Socket is closed");
    }
    if (datagrams == null || datagrams.length == 0) {
        return 0;
    }

    return nativeSend(contextHandle, socketHandle, datagrams);
}

// Add native method declaration
private static native long nativeSend(
    long contextHandle, long socketHandle, OutgoingDatagram[] datagrams);
```

#### Task 4.3: Implement PanamaWasiUdpSocket.receive()

Add method handle and implementation:
```java
private static final MethodHandle RECEIVE_HANDLE;

static {
    // In static initialization block
    RECEIVE_HANDLE = linker.downcallHandle(
        nativeLib.find("wasmtime4j_panama_wasi_udp_socket_receive").orElseThrow(),
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, // context_handle
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.JAVA_LONG, // max_results
            ValueLayout.ADDRESS, // out_count
            ValueLayout.ADDRESS)); // out_datagrams
}

@Override
public IncomingDatagram[] receive(final long maxResults) throws WasmException {
    if (closed) {
        throw new WasmException("Socket is closed");
    }

    try (final Arena arena = Arena.ofConfined()) {
        final MemorySegment outCount = arena.allocate(ValueLayout.JAVA_LONG);
        final MemorySegment outDatagrams = arena.allocate(/* size calculation */);

        final int result = (int) RECEIVE_HANDLE.invoke(
            contextHandle, socketHandle, maxResults, outCount, outDatagrams);

        if (result != 0) {
            throw new WasmException("Failed to receive datagrams");
        }

        final long count = outCount.get(ValueLayout.JAVA_LONG, 0);
        // Parse datagrams from outDatagrams
        // Return IncomingDatagram array
    }
}
```

#### Task 4.4: Implement PanamaWasiUdpSocket.send()

Similar pattern for send operation with proper memory marshalling.

---

## Testing Strategy

### Unit Tests

Create test class: `UdpDatagramTest.java`

**Test Cases**:
1. **testSendReceiveLocalhost** - Send and receive on localhost
2. **testSendWithExplicitAddress** - Send with address in datagram
3. **testSendAfterStream** - Send without address after `stream()` call
4. **testReceiveEmpty** - Receive when no data available
5. **testReceivePartial** - Receive with maxResults < available
6. **testSendPartial** - Send when buffer full (partial success)
7. **testLargeDatagrams** - Test maximum datagram size
8. **testMultipleDatagrams** - Send/receive multiple in one call
9. **testInvalidSocket** - Error handling for invalid handles
10. **testAfterClose** - Error handling for closed sockets

### Integration Tests

1. **UDP Echo Server Test** - Full round-trip test
2. **IPv4/IPv6 Tests** - Test both address families
3. **Non-blocking Behavior** - Verify EWOULDBLOCK handling
4. **Concurrent Operations** - Multiple threads sending/receiving

---

## Dependencies and Prerequisites

### Required Knowledge
- WASI Preview 2 UDP socket specification
- Rust async I/O (if using tokio)
- JNI object creation and array marshalling
- Panama FFI memory management

### External Dependencies
None - all required infrastructure exists

### Rust Crates
- `wasmtime` (already included)
- `socket2` or `tokio::net` for actual UDP operations (may already be included)

---

## Risks and Mitigation

### Risk 1: Non-blocking I/O Complexity
**Issue**: UDP operations must be non-blocking
**Mitigation**: Use Rust async/await with proper timeout handling

### Risk 2: Memory Management
**Issue**: JNI/Panama memory marshalling can be error-prone
**Mitigation**: Extensive testing, careful bounds checking

### Risk 3: Platform Differences
**Issue**: UDP behavior varies across Windows/Linux/macOS
**Mitigation**: Test on all platforms, handle platform-specific errors

### Risk 4: Datagram Size Limits
**Issue**: Maximum UDP datagram size varies
**Mitigation**: Document limits, validate sizes, handle EMSGSIZE

---

## Estimated Effort

| Phase | Tasks | Estimated Time | Priority |
|-------|-------|---------------|----------|
| Phase 1: Rust Helpers | 2 | 2-3 days | HIGH |
| Phase 2: JNI Bindings | 2 | 1-2 days | HIGH |
| Phase 3: Panama FFI | 2 | 1-2 days | HIGH |
| Phase 4: Java Layer | 4 | 1 day | HIGH |
| Testing | 15+ tests | 2-3 days | HIGH |
| **Total** | **25+** | **7-11 days** | **HIGH** |

---

## Success Criteria

1. ✅ UDP datagram send/receive operations work correctly
2. ✅ All unit tests pass on JNI and Panama implementations
3. ✅ Integration tests pass on Linux, macOS, and Windows
4. ✅ Non-blocking behavior verified
5. ✅ Error handling comprehensive and correct
6. ✅ Memory leaks tested (valgrind/AddressSanitizer)
7. ✅ Documentation updated
8. ✅ Performance acceptable (>10K datagrams/sec)

---

## References

### WASI Preview 2 Specification
- [wasi:sockets/udp@0.2.0](https://github.com/WebAssembly/wasi-sockets/blob/main/wit/udp.wit)

### Related Files
- Helper Functions: `wasmtime4j-native/src/wasi_sockets_helpers.rs:385-401`
- JNI Bindings: `wasmtime4j-native/src/jni_wasi_sockets_bindings.rs`
- Panama FFI: `wasmtime4j-native/src/panama_wasi_sockets_ffi.rs`
- Java API: `wasmtime4j/src/main/java/ai/tegmentum/wasmtime4j/wasi/sockets/WasiUdpSocket.java`
- JNI Impl: `wasmtime4j-jni/src/main/java/ai/tegmentum/wasmtime4j/jni/wasi/sockets/JniWasiUdpSocket.java:280-297`
- Panama Impl: `wasmtime4j-panama/src/main/java/ai/tegmentum/wasmtime4j/panama/wasi/sockets/PanamaWasiUdpSocket.java`

### Similar Implementations
- TCP socket implementation (for patterns)
- Existing socket address encoding/decoding helpers

---

## Next Steps

1. **Review** this roadmap with team
2. **Schedule** implementation work
3. **Set up** development environment with test UDP servers
4. **Begin** Phase 1 (Rust helper functions)
5. **Iterate** through phases with testing at each step

---

## Appendix: Data Structures

### IncomingDatagram
```java
public static final class IncomingDatagram {
    private final byte[] data;
    private final IpSocketAddress remoteAddress;

    public byte[] getData()
    public IpSocketAddress getRemoteAddress()
}
```

### OutgoingDatagram
```java
public static final class OutgoingDatagram {
    private final byte[] data;
    private final IpSocketAddress remoteAddress; // optional

    public byte[] getData()
    public IpSocketAddress getRemoteAddress()
    public boolean hasRemoteAddress()
}
```

### IpSocketAddress
Union type holding either Ipv4SocketAddress or Ipv6SocketAddress.

---

**Document Version**: 1.0
**Last Updated**: 2025-11-25
**Status**: Ready for Review

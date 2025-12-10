/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.jni.wasi.sockets;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.sockets.IpAddress;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv4Address;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv6Address;
import ai.tegmentum.wasmtime4j.wasi.sockets.ResolveAddressStream;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * JNI implementation of the ResolveAddressStream interface.
 *
 * <p>This class provides access to resolved IP addresses from a DNS lookup through JNI calls to the
 * native Wasmtime library. The stream returns IP addresses one at a time and can be iterated until
 * exhausted.
 *
 * <p>WASI Preview 2 specification: wasi:sockets/ip-name-lookup@0.2.0
 *
 * @since 1.0.0
 */
public final class JniResolveAddressStream implements ResolveAddressStream {

  private static final Logger LOGGER = Logger.getLogger(JniResolveAddressStream.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniResolveAddressStream: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final long contextHandle;

  /** The native stream handle. */
  private final long streamHandle;

  /** Whether this stream has been closed. */
  private volatile boolean closed = false;

  /**
   * Creates a new JNI resolve address stream with the given handles.
   *
   * @param contextHandle the native context handle
   * @param streamHandle the native stream handle
   * @throws IllegalArgumentException if either handle is invalid
   */
  JniResolveAddressStream(final long contextHandle, final long streamHandle) {
    if (contextHandle == 0) {
      throw new IllegalArgumentException("Context handle cannot be 0");
    }
    if (streamHandle <= 0) {
      throw new IllegalArgumentException("Stream handle must be positive: " + streamHandle);
    }
    this.contextHandle = contextHandle;
    this.streamHandle = streamHandle;
    LOGGER.fine(
        "Created JNI resolve address stream with context handle: "
            + contextHandle
            + ", stream handle: "
            + streamHandle);
  }

  @Override
  public Optional<IpAddress> resolveNextAddress() throws WasmException {
    if (closed) {
      throw new IllegalStateException("Stream has been closed");
    }

    // Native returns: [hasAddress, isIpv4, ipv4_b0, ipv4_b1, ipv4_b2, ipv4_b3,
    //                  ipv6_s0, ipv6_s1, ipv6_s2, ipv6_s3, ipv6_s4, ipv6_s5, ipv6_s6, ipv6_s7]
    final int[] result = nativeGetNextAddress(contextHandle, streamHandle);

    if (result == null || result[0] == 0) {
      // No more addresses
      return Optional.empty();
    }

    // result[1] indicates if IPv4 (1) or IPv6 (0)
    final boolean isIpv4 = result[1] == 1;

    if (isIpv4) {
      final byte[] ipv4Bytes = new byte[4];
      ipv4Bytes[0] = (byte) result[2];
      ipv4Bytes[1] = (byte) result[3];
      ipv4Bytes[2] = (byte) result[4];
      ipv4Bytes[3] = (byte) result[5];
      LOGGER.fine(
          "Resolved IPv4 address: "
              + (ipv4Bytes[0] & 0xFF)
              + "."
              + (ipv4Bytes[1] & 0xFF)
              + "."
              + (ipv4Bytes[2] & 0xFF)
              + "."
              + (ipv4Bytes[3] & 0xFF));
      return Optional.of(IpAddress.ipv4(new Ipv4Address(ipv4Bytes)));
    } else {
      final short[] ipv6Segments = new short[8];
      for (int i = 0; i < 8; i++) {
        ipv6Segments[i] = (short) result[6 + i];
      }
      LOGGER.fine("Resolved IPv6 address with first segment: " + (ipv6Segments[0] & 0xFFFF));
      return Optional.of(IpAddress.ipv6(new Ipv6Address(ipv6Segments)));
    }
  }

  @Override
  public void subscribe() throws WasmException {
    if (closed) {
      throw new IllegalStateException("Stream has been closed");
    }
    // In synchronous mode, this is a no-op as all addresses are already resolved
    nativeSubscribe(contextHandle, streamHandle);
  }

  @Override
  public boolean isClosed() {
    if (closed) {
      return true;
    }
    return nativeIsClosed(contextHandle, streamHandle);
  }

  @Override
  public void close() throws WasmException {
    if (closed) {
      return;
    }
    closed = true;
    nativeClose(contextHandle, streamHandle);
    LOGGER.fine("Closed JNI resolve address stream with handle: " + streamHandle);
  }

  /**
   * Native method to get the next resolved address.
   *
   * @param contextHandle the native context handle
   * @param streamHandle the native stream handle
   * @return array containing: [hasAddress, isIpv4, ipv4_bytes[4], ipv6_segments[8]]
   */
  private static native int[] nativeGetNextAddress(long contextHandle, long streamHandle);

  /**
   * Native method to subscribe to the stream for async notification.
   *
   * @param contextHandle the native context handle
   * @param streamHandle the native stream handle
   */
  private static native void nativeSubscribe(long contextHandle, long streamHandle);

  /**
   * Native method to check if the stream is closed.
   *
   * @param contextHandle the native context handle
   * @param streamHandle the native stream handle
   * @return true if closed
   */
  private static native boolean nativeIsClosed(long contextHandle, long streamHandle);

  /**
   * Native method to close the stream.
   *
   * @param contextHandle the native context handle
   * @param streamHandle the native stream handle
   */
  private static native void nativeClose(long contextHandle, long streamHandle);
}

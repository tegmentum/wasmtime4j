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
package ai.tegmentum.wasmtime4j.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class for stream operations.
 *
 * <p>Provides a Java 8 compatible implementation of {@code InputStream.readAllBytes()} which was
 * introduced in Java 9. This ensures both JNI (Java 8+) and Panama (Java 23+) modules can share a
 * single implementation.
 *
 * @since 1.0.0
 */
public final class StreamUtils {

  /** Buffer size for reading streams. */
  private static final int BUFFER_SIZE = 8192;

  /** Private constructor to prevent instantiation of utility class. */
  private StreamUtils() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Reads all remaining bytes from an input stream.
   *
   * <p>This is a Java 8 compatible equivalent of {@code InputStream.readAllBytes()}.
   *
   * @param stream the input stream to read from
   * @return a byte array containing all bytes read from the stream
   * @throws IllegalArgumentException if stream is null
   * @throws IOException if an I/O error occurs while reading
   */
  public static byte[] readAllBytes(final InputStream stream) throws IOException {
    Validation.requireNonNull(stream, "stream");
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    final byte[] data = new byte[BUFFER_SIZE];
    int bytesRead;
    while ((bytesRead = stream.read(data, 0, data.length)) != -1) {
      buffer.write(data, 0, bytesRead);
    }
    return buffer.toByteArray();
  }
}

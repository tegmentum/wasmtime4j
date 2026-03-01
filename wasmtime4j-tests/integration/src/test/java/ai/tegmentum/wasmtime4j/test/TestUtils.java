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
package ai.tegmentum.wasmtime4j.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * Shared test utilities for integration tests.
 *
 * @since 1.0.0
 */
public final class TestUtils {

  private static final Logger LOGGER = Logger.getLogger(TestUtils.class.getName());

  /** Private constructor to prevent instantiation. */
  private TestUtils() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Clears global handle registries in native code.
   *
   * <p>Uses reflection to call the appropriate clearHandleRegistries method based on the runtime
   * implementation (JNI or Panama). This prevents test isolation issues when running the full test
   * suite.
   */
  public static void clearHandleRegistries() {
    // Try JNI first
    try {
      final Class<?> jniEngineClass = Class.forName("ai.tegmentum.wasmtime4j.jni.JniEngine");
      final java.lang.reflect.Method method = jniEngineClass.getMethod("clearHandleRegistries");
      final int result = (int) method.invoke(null);
      if (result == 0) {
        LOGGER.info("Cleared JNI handle registries");
        return;
      }
    } catch (final Exception e) {
      LOGGER.fine("JNI clearHandleRegistries not available: " + e.getMessage());
    }

    // Try Panama
    try {
      final Class<?> panamaBindingsClass =
          Class.forName("ai.tegmentum.wasmtime4j.panama.NativeMemoryBindings");
      final java.lang.reflect.Method getInstanceMethod =
          panamaBindingsClass.getMethod("getInstance");
      final Object instance = getInstanceMethod.invoke(null);
      final java.lang.reflect.Method method =
          panamaBindingsClass.getMethod("memoryClearHandleRegistries");
      final int result = (int) method.invoke(instance);
      if (result == 0) {
        LOGGER.info("Cleared Panama handle registries");
        return;
      }
    } catch (final Exception e) {
      LOGGER.fine("Panama clearHandleRegistries not available: " + e.getMessage());
    }

    LOGGER.warning("Could not clear handle registries - no implementation available");
  }

  /**
   * Reads all bytes from an {@link InputStream} into a byte array.
   *
   * <p>This is a Java 8-compatible alternative to {@code InputStream.readAllBytes()} (Java 9+).
   *
   * @param inputStream the input stream to read from
   * @return the byte array containing all bytes from the stream
   * @throws IOException if an I/O error occurs
   */
  public static byte[] readAllBytes(final InputStream inputStream) throws IOException {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    final byte[] tempBuffer = new byte[1024];
    int bytesRead;
    while ((bytesRead = inputStream.read(tempBuffer)) != -1) {
      buffer.write(tempBuffer, 0, bytesRead);
    }
    return buffer.toByteArray();
  }
}

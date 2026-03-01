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
package ai.tegmentum.wasmtime4j.panama.util;

import ai.tegmentum.wasmtime4j.exception.ErrorMapper;
import ai.tegmentum.wasmtime4j.exception.WasmErrorCode;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.NativeMemoryBindings;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for mapping native error codes to appropriate Java exceptions in Panama.
 *
 * <p>This class translates native error codes (as defined by {@link WasmErrorCode}) into the
 * appropriate exception types from the public API exception hierarchy. It ensures that Panama
 * callers produce meaningful, categorized exceptions instead of raw error code integers.
 */
public final class PanamaErrorMapper {

  private static final Logger LOGGER = Logger.getLogger(PanamaErrorMapper.class.getName());

  /** Private constructor to prevent instantiation of utility class. */
  private PanamaErrorMapper() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Retrieves the last error message from the native library and clears it.
   *
   * @return the error message, or null if no error
   */
  public static String retrieveNativeErrorMessage() {
    try {
      final MemorySegment errorPtr = NativeMemoryBindings.getInstance().getLastErrorMessage();
      if (errorPtr == null || errorPtr.equals(MemorySegment.NULL)) {
        return null;
      }
      try {
        return errorPtr.reinterpret(Long.MAX_VALUE).getString(0);
      } finally {
        NativeMemoryBindings.getInstance().freeErrorMessage(errorPtr);
      }
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to retrieve native error message", e);
      return null;
    }
  }

  /**
   * Maps a native error code and context message to an appropriate Java exception.
   *
   * <p>Delegates to the shared {@link ErrorMapper} for consistent error categorization across both
   * JNI and Panama implementations.
   *
   * @param errorCode the native error code
   * @param context a description of the operation that failed
   * @return the appropriate Java exception
   */
  public static WasmException mapNativeError(final int errorCode, final String context) {
    return ErrorMapper.mapErrorCode(errorCode, context);
  }

  /**
   * Maps a native error code to an appropriate Java exception with no additional context.
   *
   * @param errorCode the native error code
   * @return the appropriate Java exception
   */
  public static WasmException mapNativeError(final int errorCode) {
    return mapNativeError(errorCode, null);
  }

  /**
   * Writes an error message to a native error buffer.
   *
   * <p>Truncates the message if it exceeds the buffer size, and always null-terminates.
   *
   * @param errorMsgPtr pointer to the error message buffer
   * @param errorMsgLen size of the error message buffer
   * @param message the error message to write
   */
  public static void writeErrorMessage(
      final MemorySegment errorMsgPtr, final int errorMsgLen, final String message) {
    if (errorMsgPtr == null || errorMsgPtr.equals(MemorySegment.NULL) || errorMsgLen <= 0) {
      return;
    }
    if (message == null || message.isEmpty()) {
      return;
    }

    try {
      final MemorySegment buffer = errorMsgPtr.reinterpret(errorMsgLen);
      final byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
      final int copyLen = Math.min(msgBytes.length, errorMsgLen - 1);
      MemorySegment.copy(msgBytes, 0, buffer, ValueLayout.JAVA_BYTE, 0, copyLen);
      buffer.set(ValueLayout.JAVA_BYTE, copyLen, (byte) 0);
    } catch (final Exception e) {
      LOGGER.warning("Failed to write error message to native buffer: " + e.getMessage());
    }
  }

  /**
   * Returns a human-readable description for a native error code.
   *
   * @param errorCode the native error code
   * @return the error description, or a default message for unknown codes
   */
  public static String getErrorDescription(final int errorCode) {
    final WasmErrorCode wasmErrorCode = WasmErrorCode.fromCode(errorCode);
    if (wasmErrorCode != null) {
      return wasmErrorCode.getDescription();
    }
    return "Unknown native error (code " + errorCode + ")";
  }
}

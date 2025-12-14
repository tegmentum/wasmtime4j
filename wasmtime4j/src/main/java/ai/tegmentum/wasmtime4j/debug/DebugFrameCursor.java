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

package ai.tegmentum.wasmtime4j.debug;

import ai.tegmentum.wasmtime4j.DebugFrame;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.List;
import java.util.Optional;

/**
 * Provides cursor-based navigation through WebAssembly debug frames.
 *
 * <p>DebugFrameCursor allows step-by-step navigation through the call stack during debugging,
 * enabling inspection and modification of local variables, examination of function parameters, and
 * controlled execution.
 *
 * <p>This is a low-level debugging API intended for debugger implementations and advanced
 * diagnostic tools.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * DebugFrameCursor cursor = debugSession.createCursor();
 *
 * // Navigate to the top of the stack
 * cursor.moveToTop();
 *
 * // Inspect the current frame
 * DebugFrame frame = cursor.getCurrentFrame().orElseThrow();
 * System.out.println("Function: " + frame.getFunctionName());
 *
 * // Read local variables
 * List<WasmValue> locals = cursor.getLocals();
 *
 * // Move down the stack
 * while (cursor.moveDown()) {
 *     frame = cursor.getCurrentFrame().orElseThrow();
 *     System.out.println("Caller: " + frame.getFunctionName());
 * }
 * }</pre>
 *
 * @since 1.1.0
 */
public interface DebugFrameCursor extends AutoCloseable {

  /**
   * Gets the current frame at the cursor position.
   *
   * @return an Optional containing the current frame, or empty if cursor is invalid
   */
  Optional<DebugFrame> getCurrentFrame();

  /**
   * Gets the current frame index in the call stack.
   *
   * <p>Index 0 is the top of the stack (most recent call).
   *
   * @return the current frame index, or -1 if cursor is invalid
   */
  int getCurrentIndex();

  /**
   * Gets the total number of frames in the call stack.
   *
   * @return the frame count
   */
  int getFrameCount();

  /**
   * Moves the cursor to the top of the call stack.
   *
   * @return true if the move was successful
   */
  boolean moveToTop();

  /**
   * Moves the cursor to the bottom of the call stack.
   *
   * @return true if the move was successful
   */
  boolean moveToBottom();

  /**
   * Moves the cursor up one frame (towards the top of the stack).
   *
   * @return true if the move was successful, false if already at the top
   */
  boolean moveUp();

  /**
   * Moves the cursor down one frame (towards the bottom of the stack).
   *
   * @return true if the move was successful, false if already at the bottom
   */
  boolean moveDown();

  /**
   * Moves the cursor to a specific frame index.
   *
   * @param index the frame index (0 = top of stack)
   * @return true if the move was successful
   * @throws IndexOutOfBoundsException if index is invalid
   */
  boolean moveTo(int index);

  /**
   * Gets the local variables for the current frame.
   *
   * @return the list of local variable values
   * @throws WasmException if locals cannot be read
   * @throws IllegalStateException if cursor position is invalid
   */
  List<WasmValue> getLocals() throws WasmException;

  /**
   * Gets a specific local variable by index.
   *
   * @param index the local variable index
   * @return the local variable value
   * @throws WasmException if the local cannot be read
   * @throws IndexOutOfBoundsException if index is invalid
   */
  WasmValue getLocal(int index) throws WasmException;

  /**
   * Sets a local variable value.
   *
   * <p><b>Warning:</b> Modifying local variables can cause undefined behavior if the new value is
   * incompatible with the expected type.
   *
   * @param index the local variable index
   * @param value the new value
   * @throws WasmException if the local cannot be modified
   * @throws IndexOutOfBoundsException if index is invalid
   */
  void setLocal(int index, WasmValue value) throws WasmException;

  /**
   * Gets the values on the operand stack for the current frame.
   *
   * @return the list of operand stack values
   * @throws WasmException if the stack cannot be read
   */
  List<WasmValue> getOperandStack() throws WasmException;

  /**
   * Gets the current instruction offset within the function.
   *
   * @return the instruction offset
   */
  long getInstructionOffset();

  /**
   * Checks if the cursor is at a valid position.
   *
   * @return true if the cursor points to a valid frame
   */
  boolean isValid();

  /**
   * Checks if the cursor is at the top of the stack.
   *
   * @return true if at the top
   */
  default boolean isAtTop() {
    return getCurrentIndex() == 0;
  }

  /**
   * Checks if the cursor is at the bottom of the stack.
   *
   * @return true if at the bottom
   */
  default boolean isAtBottom() {
    return getCurrentIndex() == getFrameCount() - 1;
  }

  /** Resets the cursor to the top of the stack. */
  default void reset() {
    moveToTop();
  }

  @Override
  void close();

  /**
   * Creates a snapshot of the current cursor state.
   *
   * @return a snapshot that can be used to restore the cursor position
   */
  CursorSnapshot snapshot();

  /**
   * Restores the cursor to a previously saved snapshot.
   *
   * @param snapshot the snapshot to restore
   * @throws IllegalArgumentException if the snapshot is from a different cursor
   */
  void restore(CursorSnapshot snapshot);

  /** A snapshot of cursor state for save/restore operations. */
  interface CursorSnapshot {
    /**
     * Gets the frame index at the time of the snapshot.
     *
     * @return the frame index
     */
    int getFrameIndex();

    /**
     * Gets the cursor ID this snapshot belongs to.
     *
     * @return the cursor ID
     */
    long getCursorId();
  }
}

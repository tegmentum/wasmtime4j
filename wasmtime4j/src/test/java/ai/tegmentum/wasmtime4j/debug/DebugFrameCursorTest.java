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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.DebugFrame;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.debug.DebugFrameCursor.CursorSnapshot;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DebugFrameCursor} interface.
 *
 * <p>DebugFrameCursor provides cursor-based navigation through WebAssembly debug frames, enabling
 * inspection of call stacks, local variables, and operand stacks during debugging.
 */
@DisplayName("DebugFrameCursor Tests")
class DebugFrameCursorTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(DebugFrameCursor.class.isInterface(), "DebugFrameCursor should be an interface");
    }

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(DebugFrameCursor.class),
          "DebugFrameCursor should extend AutoCloseable");
    }

    @Test
    @DisplayName("should have getCurrentFrame method")
    void shouldHaveGetCurrentFrameMethod() throws NoSuchMethodException {
      final Method method = DebugFrameCursor.class.getMethod("getCurrentFrame");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getCurrentIndex method")
    void shouldHaveGetCurrentIndexMethod() throws NoSuchMethodException {
      final Method method = DebugFrameCursor.class.getMethod("getCurrentIndex");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getFrameCount method")
    void shouldHaveGetFrameCountMethod() throws NoSuchMethodException {
      final Method method = DebugFrameCursor.class.getMethod("getFrameCount");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have moveToTop method")
    void shouldHaveMoveToTopMethod() throws NoSuchMethodException {
      final Method method = DebugFrameCursor.class.getMethod("moveToTop");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have moveToBottom method")
    void shouldHaveMoveToBottomMethod() throws NoSuchMethodException {
      final Method method = DebugFrameCursor.class.getMethod("moveToBottom");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have moveUp method")
    void shouldHaveMoveUpMethod() throws NoSuchMethodException {
      final Method method = DebugFrameCursor.class.getMethod("moveUp");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have moveDown method")
    void shouldHaveMoveDownMethod() throws NoSuchMethodException {
      final Method method = DebugFrameCursor.class.getMethod("moveDown");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have moveTo method")
    void shouldHaveMoveToMethod() throws NoSuchMethodException {
      final Method method = DebugFrameCursor.class.getMethod("moveTo", int.class);
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getLocals method")
    void shouldHaveGetLocalsMethod() throws NoSuchMethodException {
      final Method method = DebugFrameCursor.class.getMethod("getLocals");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getLocal method")
    void shouldHaveGetLocalMethod() throws NoSuchMethodException {
      final Method method = DebugFrameCursor.class.getMethod("getLocal", int.class);
      assertEquals(WasmValue.class, method.getReturnType(), "Should return WasmValue");
    }

    @Test
    @DisplayName("should have setLocal method")
    void shouldHaveSetLocalMethod() throws NoSuchMethodException {
      final Method method =
          DebugFrameCursor.class.getMethod("setLocal", int.class, WasmValue.class);
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getOperandStack method")
    void shouldHaveGetOperandStackMethod() throws NoSuchMethodException {
      final Method method = DebugFrameCursor.class.getMethod("getOperandStack");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getInstructionOffset method")
    void shouldHaveGetInstructionOffsetMethod() throws NoSuchMethodException {
      final Method method = DebugFrameCursor.class.getMethod("getInstructionOffset");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = DebugFrameCursor.class.getMethod("isValid");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have snapshot method")
    void shouldHaveSnapshotMethod() throws NoSuchMethodException {
      final Method method = DebugFrameCursor.class.getMethod("snapshot");
      assertEquals(CursorSnapshot.class, method.getReturnType(), "Should return CursorSnapshot");
    }

    @Test
    @DisplayName("should have restore method")
    void shouldHaveRestoreMethod() throws NoSuchMethodException {
      final Method method = DebugFrameCursor.class.getMethod("restore", CursorSnapshot.class);
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = DebugFrameCursor.class.getMethod("close");
      assertNotNull(method, "close method should exist");
    }
  }

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("should have isAtTop default method")
    void shouldHaveIsAtTopDefaultMethod() throws NoSuchMethodException {
      final Method method = DebugFrameCursor.class.getMethod("isAtTop");
      assertTrue(method.isDefault(), "isAtTop should be a default method");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isAtBottom default method")
    void shouldHaveIsAtBottomDefaultMethod() throws NoSuchMethodException {
      final Method method = DebugFrameCursor.class.getMethod("isAtBottom");
      assertTrue(method.isDefault(), "isAtBottom should be a default method");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have reset default method")
    void shouldHaveResetDefaultMethod() throws NoSuchMethodException {
      final Method method = DebugFrameCursor.class.getMethod("reset");
      assertTrue(method.isDefault(), "reset should be a default method");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("CursorSnapshot Nested Interface Tests")
  class CursorSnapshotInterfaceTests {

    @Test
    @DisplayName("should have CursorSnapshot nested interface")
    void shouldHaveCursorSnapshotNestedInterface() {
      boolean found = false;
      for (final Class<?> inner : DebugFrameCursor.class.getDeclaredClasses()) {
        if ("CursorSnapshot".equals(inner.getSimpleName()) && inner.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have CursorSnapshot nested interface");
    }

    @Test
    @DisplayName("CursorSnapshot should have getFrameIndex method")
    void cursorSnapshotShouldHaveGetFrameIndexMethod() throws NoSuchMethodException {
      final Method method = CursorSnapshot.class.getMethod("getFrameIndex");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("CursorSnapshot should have getCursorId method")
    void cursorSnapshotShouldHaveGetCursorIdMethod() throws NoSuchMethodException {
      final Method method = CursorSnapshot.class.getMethod("getCursorId");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Mock Implementation Tests")
  class MockImplementationTests {

    @Test
    @DisplayName("mock cursor should navigate to top")
    void mockCursorShouldNavigateToTop() {
      final MockDebugFrameCursor cursor = new MockDebugFrameCursor(3);
      cursor.moveTo(2);

      assertTrue(cursor.moveToTop(), "moveToTop should return true");
      assertEquals(0, cursor.getCurrentIndex(), "Should be at index 0");
      assertTrue(cursor.isAtTop(), "Should be at top");
    }

    @Test
    @DisplayName("mock cursor should navigate to bottom")
    void mockCursorShouldNavigateToBottom() {
      final MockDebugFrameCursor cursor = new MockDebugFrameCursor(3);

      assertTrue(cursor.moveToBottom(), "moveToBottom should return true");
      assertEquals(2, cursor.getCurrentIndex(), "Should be at index 2");
      assertTrue(cursor.isAtBottom(), "Should be at bottom");
    }

    @Test
    @DisplayName("mock cursor should move up and down")
    void mockCursorShouldMoveUpAndDown() {
      final MockDebugFrameCursor cursor = new MockDebugFrameCursor(3);
      cursor.moveTo(1);

      assertTrue(cursor.moveUp(), "Should be able to move up");
      assertEquals(0, cursor.getCurrentIndex(), "Should be at index 0");
      assertFalse(cursor.moveUp(), "Should not be able to move up from top");

      assertTrue(cursor.moveDown(), "Should be able to move down");
      assertEquals(1, cursor.getCurrentIndex(), "Should be at index 1");
      assertTrue(cursor.moveDown(), "Should be able to move down again");
      assertEquals(2, cursor.getCurrentIndex(), "Should be at index 2");
      assertFalse(cursor.moveDown(), "Should not be able to move down from bottom");
    }

    @Test
    @DisplayName("mock cursor should return frame count")
    void mockCursorShouldReturnFrameCount() {
      final MockDebugFrameCursor cursor = new MockDebugFrameCursor(5);

      assertEquals(5, cursor.getFrameCount(), "Frame count should be 5");
    }

    @Test
    @DisplayName("mock cursor should track validity")
    void mockCursorShouldTrackValidity() {
      final MockDebugFrameCursor cursor = new MockDebugFrameCursor(3);

      assertTrue(cursor.isValid(), "Cursor should be valid");
      cursor.close();
      assertFalse(cursor.isValid(), "Cursor should be invalid after close");
    }

    @Test
    @DisplayName("mock cursor should create and restore snapshots")
    void mockCursorShouldCreateAndRestoreSnapshots() {
      final MockDebugFrameCursor cursor = new MockDebugFrameCursor(5);
      cursor.moveTo(2);

      final CursorSnapshot snapshot = cursor.snapshot();
      assertEquals(2, snapshot.getFrameIndex(), "Snapshot frame index should match");

      cursor.moveTo(4);
      assertEquals(4, cursor.getCurrentIndex(), "Should be at new position");

      cursor.restore(snapshot);
      assertEquals(2, cursor.getCurrentIndex(), "Should be restored to snapshot position");
    }

    @Test
    @DisplayName("mock cursor should return instruction offset")
    void mockCursorShouldReturnInstructionOffset() {
      final MockDebugFrameCursor cursor = new MockDebugFrameCursor(3);
      cursor.setInstructionOffset(0x1234L);

      assertEquals(0x1234L, cursor.getInstructionOffset(), "Instruction offset should match");
    }

    @Test
    @DisplayName("mock cursor reset should move to top")
    void mockCursorResetShouldMoveToTop() {
      final MockDebugFrameCursor cursor = new MockDebugFrameCursor(3);
      cursor.moveToBottom();
      assertEquals(2, cursor.getCurrentIndex(), "Should be at bottom");

      cursor.reset();
      assertEquals(0, cursor.getCurrentIndex(), "Should be at top after reset");
    }

    @Test
    @DisplayName("mock cursor should return current frame")
    void mockCursorShouldReturnCurrentFrame() {
      final MockDebugFrameCursor cursor = new MockDebugFrameCursor(3);

      final Optional<DebugFrame> frame = cursor.getCurrentFrame();
      assertTrue(frame.isPresent(), "Frame should be present");
    }
  }

  /** Mock implementation of DebugFrameCursor for testing. */
  private static class MockDebugFrameCursor implements DebugFrameCursor {
    private final int frameCount;
    private final long cursorId;
    private int currentIndex;
    private boolean closed;
    private long instructionOffset;
    private final List<WasmValue> locals = new ArrayList<>();
    private final List<WasmValue> operandStack = new ArrayList<>();

    MockDebugFrameCursor(final int frameCount) {
      this.frameCount = frameCount;
      this.cursorId = System.nanoTime();
      this.currentIndex = 0;
      this.closed = false;
      this.instructionOffset = 0;
    }

    @Override
    public Optional<DebugFrame> getCurrentFrame() {
      if (closed || currentIndex < 0 || currentIndex >= frameCount) {
        return Optional.empty();
      }
      // Create a DebugFrame using the public constructor
      return Optional.of(new DebugFrame(currentIndex, currentIndex * 0x100L));
    }

    @Override
    public int getCurrentIndex() {
      return currentIndex;
    }

    @Override
    public int getFrameCount() {
      return frameCount;
    }

    @Override
    public boolean moveToTop() {
      currentIndex = 0;
      return true;
    }

    @Override
    public boolean moveToBottom() {
      currentIndex = frameCount - 1;
      return true;
    }

    @Override
    public boolean moveUp() {
      if (currentIndex > 0) {
        currentIndex--;
        return true;
      }
      return false;
    }

    @Override
    public boolean moveDown() {
      if (currentIndex < frameCount - 1) {
        currentIndex++;
        return true;
      }
      return false;
    }

    @Override
    public boolean moveTo(final int index) {
      if (index >= 0 && index < frameCount) {
        currentIndex = index;
        return true;
      }
      return false;
    }

    @Override
    public List<WasmValue> getLocals() throws WasmException {
      return new ArrayList<>(locals);
    }

    @Override
    public WasmValue getLocal(final int index) throws WasmException {
      return locals.get(index);
    }

    @Override
    public void setLocal(final int index, final WasmValue value) throws WasmException {
      while (locals.size() <= index) {
        locals.add(null);
      }
      locals.set(index, value);
    }

    @Override
    public List<WasmValue> getOperandStack() throws WasmException {
      return new ArrayList<>(operandStack);
    }

    @Override
    public long getInstructionOffset() {
      return instructionOffset;
    }

    public void setInstructionOffset(final long offset) {
      this.instructionOffset = offset;
    }

    @Override
    public boolean isValid() {
      return !closed && currentIndex >= 0 && currentIndex < frameCount;
    }

    @Override
    public void close() {
      closed = true;
    }

    @Override
    public CursorSnapshot snapshot() {
      return new MockCursorSnapshot(currentIndex, cursorId);
    }

    @Override
    public void restore(final CursorSnapshot snapshot) {
      if (snapshot.getCursorId() != cursorId) {
        throw new IllegalArgumentException("Snapshot is from a different cursor");
      }
      currentIndex = snapshot.getFrameIndex();
    }
  }

  /** Mock implementation of CursorSnapshot for testing. */
  private static class MockCursorSnapshot implements CursorSnapshot {
    private final int frameIndex;
    private final long cursorId;

    MockCursorSnapshot(final int frameIndex, final long cursorId) {
      this.frameIndex = frameIndex;
      this.cursorId = cursorId;
    }

    @Override
    public int getFrameIndex() {
      return frameIndex;
    }

    @Override
    public long getCursorId() {
      return cursorId;
    }
  }
}

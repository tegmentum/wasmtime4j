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
package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.debug.FrameInfo;
import ai.tegmentum.wasmtime4j.debug.FrameSymbol;
import ai.tegmentum.wasmtime4j.debug.WasmBacktrace;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link WasmBacktrace} call stack backtrace. */
@DisplayName("WasmBacktrace")
final class WasmBacktraceTest {

  private FrameInfo createFrame(final int funcIndex, final String funcName) {
    return new FrameInfo(funcIndex, null, funcName, null, null, null);
  }

  @Nested
  @DisplayName("constructor")
  final class ConstructorTests {

    @Test
    @DisplayName("should create backtrace with frames")
    void shouldCreateWithFrames() {
      final List<FrameInfo> frames = List.of(createFrame(0, "add"), createFrame(1, "main"));
      final WasmBacktrace backtrace = new WasmBacktrace(frames, false);
      assertNotNull(backtrace, "Backtrace should not be null");
      assertEquals(2, backtrace.getFrameCount(), "Should have 2 frames");
      assertFalse(backtrace.isForceCapture(), "Force capture should be false");
    }

    @Test
    @DisplayName("should create backtrace with force capture")
    void shouldCreateWithForceCapture() {
      final WasmBacktrace backtrace = new WasmBacktrace(List.of(), true);
      assertTrue(backtrace.isForceCapture(), "Force capture should be true");
    }

    @Test
    @DisplayName("should handle null frames gracefully")
    void shouldHandleNullFrames() {
      final WasmBacktrace backtrace = new WasmBacktrace(null, false);
      assertTrue(backtrace.isEmpty(), "Backtrace with null frames should be empty");
      assertEquals(0, backtrace.getFrameCount(), "Frame count should be 0 for null frames");
    }
  }

  @Nested
  @DisplayName("getFrames")
  final class GetFramesTests {

    @Test
    @DisplayName("should return frames in order")
    void shouldReturnFramesInOrder() {
      final List<FrameInfo> frames = List.of(createFrame(0, "inner"), createFrame(1, "outer"));
      final WasmBacktrace backtrace = new WasmBacktrace(frames, false);
      final List<FrameInfo> returned = backtrace.getFrames();
      assertEquals(2, returned.size(), "Should return 2 frames");
      assertEquals(0, returned.get(0).getFuncIndex(), "First frame should have funcIndex 0");
      assertEquals(1, returned.get(1).getFuncIndex(), "Second frame should have funcIndex 1");
    }

    @Test
    @DisplayName("should return defensive copy of frames")
    void shouldReturnDefensiveCopy() {
      final List<FrameInfo> frames = List.of(createFrame(0, "func"));
      final WasmBacktrace backtrace = new WasmBacktrace(frames, false);
      final List<FrameInfo> returned1 = backtrace.getFrames();
      final List<FrameInfo> returned2 = backtrace.getFrames();
      assertEquals(returned1, returned2, "Two calls should return equal lists");
      assertFalse(
          returned1 == returned2,
          "Two calls should return different list instances (defensive copy)");
    }
  }

  @Nested
  @DisplayName("getFrameCount")
  final class GetFrameCountTests {

    @Test
    @DisplayName("should return 0 for empty backtrace")
    void shouldReturnZeroForEmpty() {
      final WasmBacktrace backtrace = new WasmBacktrace(Collections.emptyList(), false);
      assertEquals(0, backtrace.getFrameCount(), "Empty backtrace should have 0 frames");
    }

    @Test
    @DisplayName("should return correct count for non-empty backtrace")
    void shouldReturnCorrectCount() {
      final List<FrameInfo> frames =
          List.of(createFrame(0, "a"), createFrame(1, "b"), createFrame(2, "c"));
      final WasmBacktrace backtrace = new WasmBacktrace(frames, false);
      assertEquals(3, backtrace.getFrameCount(), "Should have 3 frames");
    }
  }

  @Nested
  @DisplayName("isEmpty")
  final class IsEmptyTests {

    @Test
    @DisplayName("should be empty for no frames")
    void shouldBeEmptyForNoFrames() {
      final WasmBacktrace backtrace = new WasmBacktrace(Collections.emptyList(), false);
      assertTrue(backtrace.isEmpty(), "Backtrace with no frames should be empty");
    }

    @Test
    @DisplayName("should not be empty when has frames")
    void shouldNotBeEmptyWithFrames() {
      final WasmBacktrace backtrace = new WasmBacktrace(List.of(createFrame(0, "func")), false);
      assertFalse(backtrace.isEmpty(), "Backtrace with frames should not be empty");
    }
  }

  @Nested
  @DisplayName("equals and hashCode")
  final class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should be equal for same frames and force capture")
    void shouldBeEqualForSameContent() {
      final List<FrameInfo> frames = List.of(createFrame(0, "func"));
      final WasmBacktrace bt1 = new WasmBacktrace(frames, true);
      final WasmBacktrace bt2 = new WasmBacktrace(frames, true);
      assertEquals(bt1, bt2, "Backtraces with same frames and forceCapture should be equal");
    }

    @Test
    @DisplayName("should not be equal for different force capture")
    void shouldNotBeEqualForDifferentForceCapture() {
      final List<FrameInfo> frames = List.of(createFrame(0, "func"));
      final WasmBacktrace bt1 = new WasmBacktrace(frames, true);
      final WasmBacktrace bt2 = new WasmBacktrace(frames, false);
      assertFalse(bt1.equals(bt2), "Backtraces with different forceCapture should not be equal");
    }

    @Test
    @DisplayName("should not be equal for different frames")
    void shouldNotBeEqualForDifferentFrames() {
      final WasmBacktrace bt1 = new WasmBacktrace(List.of(createFrame(0, "a")), false);
      final WasmBacktrace bt2 = new WasmBacktrace(List.of(createFrame(1, "b")), false);
      assertFalse(bt1.equals(bt2), "Backtraces with different frames should not be equal");
    }

    @Test
    @DisplayName("should have same hashCode for equal backtraces")
    void shouldHaveSameHashCodeForEqual() {
      final List<FrameInfo> frames = List.of(createFrame(0, "func"));
      final WasmBacktrace bt1 = new WasmBacktrace(frames, false);
      final WasmBacktrace bt2 = new WasmBacktrace(frames, false);
      assertEquals(bt1.hashCode(), bt2.hashCode(), "Equal backtraces should have same hashCode");
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      final WasmBacktrace bt = new WasmBacktrace(Collections.emptyList(), false);
      assertFalse(bt.equals(null), "Backtrace should not be equal to null");
    }

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      final WasmBacktrace bt = new WasmBacktrace(Collections.emptyList(), false);
      assertTrue(bt.equals(bt), "Backtrace should be equal to itself");
    }
  }

  @Nested
  @DisplayName("toString")
  final class ToStringTests {

    @Test
    @DisplayName("should show empty for no frames")
    void shouldShowEmptyForNoFrames() {
      final WasmBacktrace backtrace = new WasmBacktrace(Collections.emptyList(), false);
      final String str = backtrace.toString();
      assertTrue(str.contains("empty"), "toString for empty backtrace should contain 'empty'");
    }

    @Test
    @DisplayName("should include frame info for non-empty backtrace")
    void shouldIncludeFrameInfo() {
      final WasmBacktrace backtrace = new WasmBacktrace(List.of(createFrame(0, "myFunc")), false);
      final String str = backtrace.toString();
      assertTrue(str.contains("WasmBacktrace"), "toString should contain 'WasmBacktrace'");
      assertTrue(str.contains("0:"), "toString should contain frame index '0:'");
    }
  }

  @Nested
  @DisplayName("fromErrorMessage")
  final class FromErrorMessageTests {

    @Test
    @DisplayName("should return empty backtrace for null message")
    void shouldReturnEmptyForNull() {
      final WasmBacktrace bt = WasmBacktrace.fromErrorMessage(null);
      assertNotNull(bt, "Should not return null");
      assertTrue(bt.isEmpty(), "Backtrace should be empty for null input");
    }

    @Test
    @DisplayName("should return empty backtrace for empty message")
    void shouldReturnEmptyForEmpty() {
      final WasmBacktrace bt = WasmBacktrace.fromErrorMessage("");
      assertTrue(bt.isEmpty(), "Backtrace should be empty for empty input");
    }

    @Test
    @DisplayName("should return empty backtrace for message without backtrace section")
    void shouldReturnEmptyForNoBacktraceSection() {
      final WasmBacktrace bt =
          WasmBacktrace.fromErrorMessage("wasm trap: unreachable\nno backtrace here");
      assertTrue(bt.isEmpty(), "Backtrace should be empty when no 'wasm backtrace:' present");
    }

    @Test
    @DisplayName("should parse single frame without source info")
    void shouldParseSingleFrame() {
      final String msg = "wasm trap: unreachable\nwasm backtrace:\n    0:   0x1a2b - myFunc\n";
      final WasmBacktrace bt = WasmBacktrace.fromErrorMessage(msg);
      assertFalse(bt.isEmpty(), "Backtrace should not be empty");
      assertEquals(1, bt.getFrameCount(), "Should have 1 frame");
      final FrameInfo frame = bt.getFrames().get(0);
      assertEquals(0, frame.getFuncIndex(), "Frame index should be 0");
      assertTrue(frame.getFuncName().isPresent(), "Function name should be present");
      assertEquals("myFunc", frame.getFuncName().get(), "Function name should match");
      assertTrue(frame.getModuleOffset().isPresent(), "Module offset should be present");
      assertEquals(0x1a2b, frame.getModuleOffset().get().intValue(), "Offset should be 0x1a2b");
      assertTrue(frame.getSymbols().isEmpty(), "Symbols should be empty without source info");
    }

    @Test
    @DisplayName("should parse multiple frames")
    void shouldParseMultipleFrames() {
      final String msg =
          "wasm trap: unreachable\n"
              + "wasm backtrace:\n"
              + "    0:   0x100 - funcA\n"
              + "    1:   0x200 - funcB\n"
              + "    2:   0x300 - funcC\n";
      final WasmBacktrace bt = WasmBacktrace.fromErrorMessage(msg);
      assertEquals(3, bt.getFrameCount(), "Should have 3 frames");
      assertEquals("funcA", bt.getFrames().get(0).getFuncName().orElse(null), "First frame: funcA");
      assertEquals(
          "funcB", bt.getFrames().get(1).getFuncName().orElse(null), "Second frame: funcB");
      assertEquals("funcC", bt.getFrames().get(2).getFuncName().orElse(null), "Third frame: funcC");
      assertEquals(2, bt.getFrames().get(2).getFuncIndex(), "Third frame index should be 2");
    }

    @Test
    @DisplayName("should parse frame with DWARF source location")
    void shouldParseFrameWithSourceLocation() {
      final String msg =
          "wasm trap: unreachable\n"
              + "wasm backtrace:\n"
              + "    0:   0xabc - myFunc\n"
              + "                    at src/main.rs:42:10\n";
      final WasmBacktrace bt = WasmBacktrace.fromErrorMessage(msg);
      assertEquals(1, bt.getFrameCount(), "Should have 1 frame");
      final FrameInfo frame = bt.getFrames().get(0);
      assertEquals("myFunc", frame.getFuncName().orElse(null), "Function name should match");
      assertFalse(frame.getSymbols().isEmpty(), "Symbols should not be empty");
      final FrameSymbol sym = frame.getSymbols().get(0);
      assertEquals("src/main.rs", sym.getFile().orElse(null), "Source file should match");
      assertEquals(42, sym.getLine().orElse(-1).intValue(), "Line number should be 42");
      assertEquals(10, sym.getColumn().orElse(-1).intValue(), "Column should be 10");
    }

    @Test
    @DisplayName("should parse frame with source location without column")
    void shouldParseFrameWithSourceLocationNoColumn() {
      final String msg =
          "wasm trap: unreachable\n"
              + "wasm backtrace:\n"
              + "    0:   0xdef - compute\n"
              + "                    at lib/math.c:99\n";
      final WasmBacktrace bt = WasmBacktrace.fromErrorMessage(msg);
      assertEquals(1, bt.getFrameCount(), "Should have 1 frame");
      final FrameSymbol sym = bt.getFrames().get(0).getSymbols().get(0);
      assertEquals("lib/math.c", sym.getFile().orElse(null), "File should match");
      assertEquals(99, sym.getLine().orElse(-1).intValue(), "Line should be 99");
      assertFalse(sym.getColumn().isPresent(), "Column should not be present");
    }

    @Test
    @DisplayName("should strip angle brackets from function names")
    void shouldStripAngleBrackets() {
      final String msg =
          "wasm trap: unreachable\n" + "wasm backtrace:\n" + "    0:   0x100 - <wasm_func>\n";
      final WasmBacktrace bt = WasmBacktrace.fromErrorMessage(msg);
      assertEquals(1, bt.getFrameCount(), "Should have 1 frame");
      assertEquals(
          "wasm_func",
          bt.getFrames().get(0).getFuncName().orElse(null),
          "Angle brackets should be stripped");
    }

    @Test
    @DisplayName("should parse mixed frames with and without source info")
    void shouldParseMixedFrames() {
      final String msg =
          "wasm trap: unreachable\n"
              + "wasm backtrace:\n"
              + "    0:   0x100 - funcWithSource\n"
              + "                    at src/mod.rs:10:5\n"
              + "    1:   0x200 - funcWithoutSource\n"
              + "    2:   0x300 - anotherWithSource\n"
              + "                    at src/lib.rs:20\n";
      final WasmBacktrace bt = WasmBacktrace.fromErrorMessage(msg);
      assertEquals(3, bt.getFrameCount(), "Should have 3 frames");
      assertEquals(1, bt.getFrames().get(0).getSymbols().size(), "Frame 0 should have source info");
      assertTrue(bt.getFrames().get(1).getSymbols().isEmpty(), "Frame 1 should have no source");
      assertEquals(1, bt.getFrames().get(2).getSymbols().size(), "Frame 2 should have source info");
    }

    @Test
    @DisplayName("should not be a force-captured backtrace")
    void shouldNotBeForceCapture() {
      final String msg = "wasm trap: unreachable\nwasm backtrace:\n    0:   0x100 - func\n";
      final WasmBacktrace bt = WasmBacktrace.fromErrorMessage(msg);
      assertFalse(bt.isForceCapture(), "Parsed backtrace should not be force-captured");
    }
  }
}

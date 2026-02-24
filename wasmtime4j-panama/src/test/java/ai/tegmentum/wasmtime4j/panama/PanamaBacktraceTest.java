package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.debug.FrameInfo;
import ai.tegmentum.wasmtime4j.debug.FrameSymbol;
import ai.tegmentum.wasmtime4j.debug.WasmBacktrace;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.*;

/**
 * Unit tests for WebAssembly backtrace functionality.
 *
 * <p>Tests verify the backtrace API classes (WasmBacktrace, FrameInfo, FrameSymbol) and their
 * behavior. These classes are part of the common wasmtime4j API and work identically across JNI and
 * Panama implementations.
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class PanamaBacktraceTest {

  @Test
  @DisplayName("Should create WasmBacktrace with frames")
  void shouldCreateWasmBacktraceWithFrames() {
    final FrameInfo frame =
        new FrameInfo(0, null, "test_function", 100, 50, Collections.emptyList());
    final WasmBacktrace backtrace = new WasmBacktrace(Collections.singletonList(frame), false);

    assertNotNull(backtrace, "Backtrace should not be null");
    assertFalse(backtrace.isForceCapture(), "Should not be force capture");
    assertEquals(1, backtrace.getFrameCount(), "Should have one frame");
    assertFalse(backtrace.isEmpty(), "Should not be empty");
  }

  @Test
  @DisplayName("Should create empty WasmBacktrace")
  void shouldCreateEmptyWasmBacktrace() {
    final WasmBacktrace backtrace = new WasmBacktrace(null, false);

    assertNotNull(backtrace, "Backtrace should not be null");
    assertEquals(0, backtrace.getFrameCount(), "Should have zero frames");
    assertTrue(backtrace.isEmpty(), "Should be empty");
    assertNotNull(backtrace.getFrames(), "Frames list should not be null");
  }

  @Test
  @DisplayName("Should create force-captured WasmBacktrace")
  void shouldCreateForceCapturedWasmBacktrace() {
    final WasmBacktrace backtrace = new WasmBacktrace(Collections.emptyList(), true);

    assertTrue(backtrace.isForceCapture(), "Should be force capture");
  }

  @Test
  @DisplayName("Should create FrameInfo with all fields")
  void shouldCreateFrameInfoWithAllFields() {
    final FrameSymbol symbol = new FrameSymbol("func", "test.wasm", 42, 10);
    final FrameInfo frame =
        new FrameInfo(5, null, "my_function", 1000, 200, Collections.singletonList(symbol));

    assertEquals(5, frame.getFuncIndex(), "Function index should match");
    assertTrue(frame.getFuncName().isPresent(), "Function name should be present");
    assertEquals("my_function", frame.getFuncName().get(), "Function name should match");
    assertTrue(frame.getModuleOffset().isPresent(), "Module offset should be present");
    assertEquals(1000, frame.getModuleOffset().get(), "Module offset should match");
    assertTrue(frame.getFuncOffset().isPresent(), "Function offset should be present");
    assertEquals(200, frame.getFuncOffset().get(), "Function offset should match");
    assertEquals(1, frame.getSymbols().size(), "Should have one symbol");
  }

  @Test
  @DisplayName("Should create FrameInfo with null optional fields")
  void shouldCreateFrameInfoWithNullOptionalFields() {
    final FrameInfo frame = new FrameInfo(0, null, null, null, null, null);

    assertEquals(0, frame.getFuncIndex(), "Function index should be 0");
    assertFalse(frame.getFuncName().isPresent(), "Function name should not be present");
    assertFalse(frame.getModuleOffset().isPresent(), "Module offset should not be present");
    assertFalse(frame.getFuncOffset().isPresent(), "Function offset should not be present");
    assertEquals(0, frame.getSymbols().size(), "Symbols should be empty");
  }

  @Test
  @DisplayName("Should create FrameSymbol with all fields")
  void shouldCreateFrameSymbolWithAllFields() {
    final FrameSymbol symbol = new FrameSymbol("test_func", "main.wasm", 100, 25);

    assertTrue(symbol.getName().isPresent(), "Name should be present");
    assertEquals("test_func", symbol.getName().get(), "Name should match");
    assertTrue(symbol.getFile().isPresent(), "File should be present");
    assertEquals("main.wasm", symbol.getFile().get(), "File should match");
    assertTrue(symbol.getLine().isPresent(), "Line should be present");
    assertEquals(100, symbol.getLine().get(), "Line should match");
    assertTrue(symbol.getColumn().isPresent(), "Column should be present");
    assertEquals(25, symbol.getColumn().get(), "Column should match");
  }

  @Test
  @DisplayName("Should create FrameSymbol with null optional fields")
  void shouldCreateFrameSymbolWithNullOptionalFields() {
    final FrameSymbol symbol = new FrameSymbol(null, null, null, null);

    assertFalse(symbol.getName().isPresent(), "Name should not be present");
    assertFalse(symbol.getFile().isPresent(), "File should not be present");
    assertFalse(symbol.getLine().isPresent(), "Line should not be present");
    assertFalse(symbol.getColumn().isPresent(), "Column should not be present");
  }

  @Test
  @DisplayName("Should verify WasmBacktrace toString format")
  void shouldVerifyBacktraceToStringFormat() {
    final FrameInfo frame = new FrameInfo(0, null, "test", 100, 50, Collections.emptyList());
    final WasmBacktrace backtrace = new WasmBacktrace(Collections.singletonList(frame), false);

    final String str = backtrace.toString();
    assertNotNull(str, "toString should not be null");
    assertTrue(str.contains("WasmBacktrace"), "toString should contain class name");
  }

  @Test
  @DisplayName("Should verify FrameInfo toString format")
  void shouldVerifyFrameInfoToStringFormat() {
    final FrameInfo frame = new FrameInfo(5, null, "my_func", 1000, 200, Collections.emptyList());

    final String str = frame.toString();
    assertNotNull(str, "toString should not be null");
    assertTrue(str.contains("FrameInfo"), "toString should contain class name");
    assertTrue(str.contains("funcIndex=5"), "toString should contain function index");
  }

  @Test
  @DisplayName("Should verify FrameSymbol toString format")
  void shouldVerifyFrameSymbolToStringFormat() {
    final FrameSymbol symbol = new FrameSymbol("test", "file.wasm", 42, 10);

    final String str = symbol.toString();
    assertNotNull(str, "toString should not be null");
    assertTrue(str.contains("test"), "toString should contain symbol name");
    assertTrue(str.contains("file.wasm"), "toString should contain file name");
    assertTrue(str.contains("42"), "toString should contain line number");
  }

  @Test
  @DisplayName("Should verify WasmBacktrace equals and hashCode")
  void shouldVerifyBacktraceEqualsAndHashCode() {
    final FrameInfo frame = new FrameInfo(0, null, "test", 100, 50, Collections.emptyList());
    final WasmBacktrace bt1 = new WasmBacktrace(Collections.singletonList(frame), false);
    final WasmBacktrace bt2 = new WasmBacktrace(Collections.singletonList(frame), false);

    assertEquals(bt1, bt2, "Equal backtraces should be equal");
    assertEquals(bt1.hashCode(), bt2.hashCode(), "Equal backtraces should have same hashCode");
  }

  @Test
  @DisplayName("Should verify FrameInfo equals and hashCode")
  void shouldVerifyFrameInfoEqualsAndHashCode() {
    final FrameInfo frame1 = new FrameInfo(5, null, "func", 100, 50, Collections.emptyList());
    final FrameInfo frame2 = new FrameInfo(5, null, "func", 100, 50, Collections.emptyList());

    assertEquals(frame1, frame2, "Equal frames should be equal");
    assertEquals(frame1.hashCode(), frame2.hashCode(), "Equal frames should have same hashCode");
  }

  @Test
  @DisplayName("Should verify FrameSymbol equals and hashCode")
  void shouldVerifyFrameSymbolEqualsAndHashCode() {
    final FrameSymbol sym1 = new FrameSymbol("test", "file.wasm", 42, 10);
    final FrameSymbol sym2 = new FrameSymbol("test", "file.wasm", 42, 10);

    assertEquals(sym1, sym2, "Equal symbols should be equal");
    assertEquals(sym1.hashCode(), sym2.hashCode(), "Equal symbols should have same hashCode");
  }

  @Test
  @DisplayName("Should handle FrameInfo with defensive copied symbols list")
  void shouldHandleFrameInfoWithDefensiveCopiedSymbolsList() {
    final FrameSymbol symbol = new FrameSymbol("test", "file.wasm", 42, 10);
    final FrameInfo frame =
        new FrameInfo(0, null, "test", 100, 50, Collections.singletonList(symbol));

    // Verify list is not null and has correct size
    assertNotNull(frame.getSymbols(), "Symbols list should not be null");
    assertEquals(1, frame.getSymbols().size(), "Should have one symbol");

    // Implementation returns defensive copy (mutable ArrayList) to protect internal state
    // Verify modifying returned list doesn't affect original
    final List<FrameSymbol> copy = frame.getSymbols();
    copy.clear();
    assertEquals(1, frame.getSymbols().size(), "Original should be unchanged after clearing copy");
  }

  @Test
  @DisplayName("Should handle WasmBacktrace with defensive copied frames list")
  void shouldHandleBacktraceWithDefensiveCopiedFramesList() {
    final FrameInfo frame = new FrameInfo(0, null, "test", 100, 50, Collections.emptyList());
    final WasmBacktrace backtrace = new WasmBacktrace(Collections.singletonList(frame), false);

    // Verify list is not null and has correct size
    assertNotNull(backtrace.getFrames(), "Frames list should not be null");
    assertEquals(1, backtrace.getFrames().size(), "Should have one frame");

    // Implementation returns defensive copy (mutable ArrayList) to protect internal state
    // Verify modifying returned list doesn't affect original
    final List<FrameInfo> copy = backtrace.getFrames();
    copy.clear();
    assertEquals(
        1, backtrace.getFrames().size(), "Original should be unchanged after clearing copy");
  }
}

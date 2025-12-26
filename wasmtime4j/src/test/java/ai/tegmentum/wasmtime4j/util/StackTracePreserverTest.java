package ai.tegmentum.wasmtime4j.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.util.StackTracePreserver.EnhancedWasmException;
import ai.tegmentum.wasmtime4j.util.StackTracePreserver.WasmStackFrame;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StackTracePreserver} utility class.
 *
 * <p>StackTracePreserver provides mechanisms to capture and preserve stack trace information when
 * errors occur during WebAssembly execution, bridging native WebAssembly stack traces and Java
 * stack traces.
 */
@DisplayName("StackTracePreserver Tests")
class StackTracePreserverTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(StackTracePreserver.class.getModifiers()),
          "StackTracePreserver should be a final class");
    }

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = StackTracePreserver.class.getDeclaredConstructor();
      assertTrue(
          Modifier.isPrivate(constructor.getModifiers()),
          "Constructor should be private to prevent instantiation");
    }

    @Test
    @DisplayName("should throw AssertionError when constructor is invoked via reflection")
    void shouldThrowAssertionErrorWhenConstructorInvoked() throws NoSuchMethodException {
      final Constructor<?> constructor = StackTracePreserver.class.getDeclaredConstructor();
      constructor.setAccessible(true);

      final InvocationTargetException exception =
          assertThrows(
              InvocationTargetException.class,
              () -> constructor.newInstance(),
              "Constructor should throw exception when invoked");

      assertTrue(
          exception.getCause() instanceof AssertionError,
          "Cause should be AssertionError for utility class");
    }
  }

  @Nested
  @DisplayName("WasmStackFrame Tests")
  class WasmStackFrameTests {

    @Test
    @DisplayName("should create stack frame with full information")
    void shouldCreateStackFrameWithFullInfo() {
      final WasmStackFrame frame =
          new WasmStackFrame("myFunction", "myModule", "source.wat", 42, 10, 100L);

      assertEquals("myFunction", frame.getFunctionName(), "Function name should match");
      assertTrue(frame.getModuleName().isPresent(), "Module name should be present");
      assertEquals("myModule", frame.getModuleName().get(), "Module name should match");
      assertTrue(frame.getSourceFile().isPresent(), "Source file should be present");
      assertEquals("source.wat", frame.getSourceFile().get(), "Source file should match");
      assertTrue(frame.getLineNumber().isPresent(), "Line number should be present");
      assertEquals(Integer.valueOf(42), frame.getLineNumber().get(), "Line number should match");
      assertTrue(frame.getColumnNumber().isPresent(), "Column number should be present");
      assertEquals(Integer.valueOf(10), frame.getColumnNumber().get(), "Column should match");
      assertEquals(100L, frame.getInstructionOffset(), "Instruction offset should match");
    }

    @Test
    @DisplayName("should create simple stack frame")
    void shouldCreateSimpleStackFrame() {
      final WasmStackFrame frame = WasmStackFrame.simple("simpleFunc", 50L);

      assertEquals("simpleFunc", frame.getFunctionName(), "Function name should match");
      assertFalse(frame.getModuleName().isPresent(), "Module name should be empty");
      assertFalse(frame.getSourceFile().isPresent(), "Source file should be empty");
      assertFalse(frame.getLineNumber().isPresent(), "Line number should be empty");
      assertFalse(frame.getColumnNumber().isPresent(), "Column number should be empty");
      assertEquals(50L, frame.getInstructionOffset(), "Instruction offset should match");
    }

    @Test
    @DisplayName("should handle null function name")
    void shouldHandleNullFunctionName() {
      final WasmStackFrame frame = new WasmStackFrame(null, null, null, null, null, 0L);
      assertEquals("<unknown>", frame.getFunctionName(), "Null function name should be unknown");
    }

    @Test
    @DisplayName("should handle null optional fields")
    void shouldHandleNullOptionalFields() {
      final WasmStackFrame frame = new WasmStackFrame("func", null, null, null, null, 0L);

      assertFalse(frame.getModuleName().isPresent(), "Null module should be empty Optional");
      assertFalse(frame.getSourceFile().isPresent(), "Null source should be empty Optional");
      assertFalse(frame.getLineNumber().isPresent(), "Null line should be empty Optional");
      assertFalse(frame.getColumnNumber().isPresent(), "Null column should be empty Optional");
    }

    @Test
    @DisplayName("should convert to StackTraceElement")
    void shouldConvertToStackTraceElement() {
      final WasmStackFrame frame =
          new WasmStackFrame("testFunc", "testModule", "test.wat", 25, 5, 200L);

      final StackTraceElement element = frame.toStackTraceElement();

      assertNotNull(element, "Stack trace element should not be null");
      assertTrue(
          element.getClassName().contains("[WASM]"), "Class name should contain WASM prefix");
      assertEquals("testFunc", element.getMethodName(), "Method name should be function name");
      assertEquals("test.wat", element.getFileName(), "File name should be source file");
      assertEquals(25, element.getLineNumber(), "Line number should match");
    }

    @Test
    @DisplayName("should convert to StackTraceElement without source info")
    void shouldConvertToStackTraceElementWithoutSourceInfo() {
      final WasmStackFrame frame = WasmStackFrame.simple("noSourceFunc", 300L);

      final StackTraceElement element = frame.toStackTraceElement();

      assertNotNull(element, "Stack trace element should not be null");
      assertEquals("noSourceFunc", element.getMethodName(), "Method name should be function name");
      assertEquals(-1, element.getLineNumber(), "Line number should be -1 when unknown");
    }

    @Test
    @DisplayName("toString should contain function name")
    void toStringShouldContainFunctionName() {
      final WasmStackFrame frame = new WasmStackFrame("myFunc", "myMod", "file.wat", 10, 5, 100L);

      final String str = frame.toString();

      assertTrue(str.contains("[WASM]"), "String should contain WASM prefix");
      assertTrue(str.contains("myFunc"), "String should contain function name");
      assertTrue(str.contains("myMod"), "String should contain module name");
    }

    @Test
    @DisplayName("toString without source should show offset")
    void toStringWithoutSourceShouldShowOffset() {
      final WasmStackFrame frame = WasmStackFrame.simple("func", 500L);

      final String str = frame.toString();

      assertTrue(str.contains("offset"), "String should contain offset");
      assertTrue(str.contains("500"), "String should contain offset value");
    }
  }

  @Nested
  @DisplayName("parseNativeStackTrace Tests")
  class ParseNativeStackTraceTests {

    @Test
    @DisplayName("should return empty list for null input")
    void shouldReturnEmptyListForNull() {
      final List<WasmStackFrame> frames = StackTracePreserver.parseNativeStackTrace(null);
      assertNotNull(frames, "Should return non-null list");
      assertTrue(frames.isEmpty(), "Should return empty list for null input");
    }

    @Test
    @DisplayName("should return empty list for empty string")
    void shouldReturnEmptyListForEmptyString() {
      final List<WasmStackFrame> frames = StackTracePreserver.parseNativeStackTrace("");
      assertTrue(frames.isEmpty(), "Should return empty list for empty string");
    }

    @Test
    @DisplayName("should return empty list for whitespace-only input")
    void shouldReturnEmptyListForWhitespace() {
      final List<WasmStackFrame> frames = StackTracePreserver.parseNativeStackTrace("   \n\t  ");
      assertTrue(frames.isEmpty(), "Should return empty list for whitespace input");
    }

    @Test
    @DisplayName("should parse parentheses format")
    void shouldParseParenthesesFormat() {
      final String stackTrace = "myFunction (module.wasm:42:10)";
      final List<WasmStackFrame> frames = StackTracePreserver.parseNativeStackTrace(stackTrace);

      assertFalse(frames.isEmpty(), "Should parse at least one frame");
      final WasmStackFrame frame = frames.get(0);
      assertEquals("myFunction", frame.getFunctionName(), "Function name should match");
    }

    @Test
    @DisplayName("should parse 'at' format")
    void shouldParseAtFormat() {
      // When line contains parentheses, parseParenthesesFormat is called first
      // This extracts function name as everything before '(' - including "at " prefix
      final String stackTrace = "at someFunction (file.wasm:100)";
      final List<WasmStackFrame> frames = StackTracePreserver.parseNativeStackTrace(stackTrace);

      assertFalse(frames.isEmpty(), "Should parse at least one frame");
      final WasmStackFrame frame = frames.get(0);
      // The "at " prefix is included because parseParenthesesFormat extracts
      // everything before the opening parenthesis
      assertEquals("at someFunction", frame.getFunctionName(), "Function name should match");
    }

    @Test
    @DisplayName("should parse @ sign format")
    void shouldParseAtSignFormat() {
      final String stackTrace = "callFunc@module.wasm:50";
      final List<WasmStackFrame> frames = StackTracePreserver.parseNativeStackTrace(stackTrace);

      assertFalse(frames.isEmpty(), "Should parse at least one frame");
      final WasmStackFrame frame = frames.get(0);
      assertEquals("callFunc", frame.getFunctionName(), "Function name should match");
    }

    @Test
    @DisplayName("should parse multiple lines")
    void shouldParseMultipleLines() {
      final String stackTrace = "func1 (mod.wasm:10)\nfunc2 (mod.wasm:20)\nfunc3 (mod.wasm:30)";
      final List<WasmStackFrame> frames = StackTracePreserver.parseNativeStackTrace(stackTrace);

      assertEquals(3, frames.size(), "Should parse all three frames");
    }

    @Test
    @DisplayName("should skip empty lines")
    void shouldSkipEmptyLines() {
      final String stackTrace = "func1 (mod.wasm:10)\n\n\nfunc2 (mod.wasm:20)";
      final List<WasmStackFrame> frames = StackTracePreserver.parseNativeStackTrace(stackTrace);

      assertEquals(2, frames.size(), "Should skip empty lines");
    }

    @Test
    @DisplayName("should parse simple function name")
    void shouldParseSimpleFunctionName() {
      final String stackTrace = "$main";
      final List<WasmStackFrame> frames = StackTracePreserver.parseNativeStackTrace(stackTrace);

      assertFalse(frames.isEmpty(), "Should parse simple function name");
      assertEquals("$main", frames.get(0).getFunctionName(), "Function name should match");
    }
  }

  @Nested
  @DisplayName("EnhancedWasmException Tests")
  class EnhancedWasmExceptionTests {

    @Test
    @DisplayName("should create with stack frames and trap message")
    void shouldCreateWithStackFramesAndTrapMessage() {
      final List<WasmStackFrame> frames = new ArrayList<>();
      frames.add(WasmStackFrame.simple("testFunc", 100L));

      final EnhancedWasmException exception =
          new EnhancedWasmException("test error", frames, "trap: unreachable", null);

      assertNotNull(exception, "Exception should not be null");
      assertEquals(1, exception.getWasmStackFrames().size(), "Should have one frame");
      assertTrue(exception.getWasmTrapMessage().isPresent(), "Trap message should be present");
      assertEquals(
          "trap: unreachable", exception.getWasmTrapMessage().get(), "Trap message should match");
    }

    @Test
    @DisplayName("should create from existing WasmException")
    void shouldCreateFromExistingWasmException() {
      final WasmException original = new WasmException("original error");
      final List<WasmStackFrame> frames = new ArrayList<>();
      frames.add(WasmStackFrame.simple("func1", 50L));
      frames.add(WasmStackFrame.simple("func2", 100L));

      final EnhancedWasmException enhanced =
          new EnhancedWasmException(original, frames, "trap message");

      assertNotNull(enhanced, "Enhanced exception should not be null");
      assertEquals(2, enhanced.getWasmStackFrames().size(), "Should have two frames");
      assertTrue(enhanced.getMessage().contains("original error"), "Should contain original");
    }

    @Test
    @DisplayName("stack frames should be unmodifiable")
    void stackFramesShouldBeUnmodifiable() {
      final List<WasmStackFrame> frames = new ArrayList<>();
      frames.add(WasmStackFrame.simple("func", 0L));

      final EnhancedWasmException exception =
          new EnhancedWasmException("error", frames, null, null);

      assertThrows(
          UnsupportedOperationException.class,
          () -> exception.getWasmStackFrames().add(WasmStackFrame.simple("new", 0L)),
          "Stack frames list should be unmodifiable");
    }

    @Test
    @DisplayName("getMessage should include trap message when present")
    void getMessageShouldIncludeTrapMessage() {
      final List<WasmStackFrame> frames = new ArrayList<>();
      final EnhancedWasmException exception =
          new EnhancedWasmException("base error", frames, "memory access trap", null);

      final String message = exception.getMessage();

      assertTrue(message.contains("memory access trap"), "Message should include trap message");
    }

    @Test
    @DisplayName("getMessage should include stack trace when frames present")
    void getMessageShouldIncludeStackTraceWhenFramesPresent() {
      final List<WasmStackFrame> frames = new ArrayList<>();
      frames.add(new WasmStackFrame("func1", "mod", "file.wasm", 10, null, 0L));

      final EnhancedWasmException exception =
          new EnhancedWasmException("error", frames, null, null);

      final String message = exception.getMessage();

      assertTrue(
          message.contains("WebAssembly stack trace"),
          "Message should include stack trace section");
      assertTrue(message.contains("func1"), "Message should include function name");
    }

    @Test
    @DisplayName("should handle empty frames list")
    void shouldHandleEmptyFramesList() {
      final List<WasmStackFrame> frames = new ArrayList<>();

      final EnhancedWasmException exception =
          new EnhancedWasmException("error", frames, null, null);

      assertTrue(exception.getWasmStackFrames().isEmpty(), "Frames list should be empty");
      assertFalse(exception.getWasmTrapMessage().isPresent(), "Trap message should be empty");
    }
  }

  @Nested
  @DisplayName("enhanceException Tests")
  class EnhanceExceptionTests {

    @Test
    @DisplayName("should enhance exception with parsed stack trace")
    void shouldEnhanceExceptionWithParsedStackTrace() {
      final WasmException original = new WasmException("test");
      final String stackTrace = "func1 (mod.wasm:10)\nfunc2 (mod.wasm:20)";

      final EnhancedWasmException enhanced =
          StackTracePreserver.enhanceException(original, stackTrace, "trap");

      assertNotNull(enhanced, "Enhanced exception should not be null");
      assertEquals(2, enhanced.getWasmStackFrames().size(), "Should have two frames");
    }

    @Test
    @DisplayName("should handle null stack trace")
    void shouldHandleNullStackTrace() {
      final WasmException original = new WasmException("test");

      final EnhancedWasmException enhanced =
          StackTracePreserver.enhanceException(original, null, null);

      assertNotNull(enhanced, "Should handle null stack trace");
      assertTrue(
          enhanced.getWasmStackFrames().isEmpty(), "Frames should be empty for null stack trace");
    }
  }

  @Nested
  @DisplayName("createEnhancedException Tests")
  class CreateEnhancedExceptionTests {

    @Test
    @DisplayName("should create enhanced exception from scratch")
    void shouldCreateEnhancedExceptionFromScratch() {
      final String stackTrace = "main (module.wasm:1)";

      final EnhancedWasmException exception =
          StackTracePreserver.createEnhancedException(
              "error message", stackTrace, "trap: div by zero", null);

      assertNotNull(exception, "Exception should not be null");
      assertTrue(exception.getMessage().contains("error message"), "Should contain message");
      assertFalse(exception.getWasmStackFrames().isEmpty(), "Should have stack frames");
    }
  }

  @Nested
  @DisplayName("combineStackTraces Tests")
  class CombineStackTracesTests {

    @Test
    @DisplayName("should combine multiple stack traces")
    void shouldCombineMultipleStackTraces() {
      final WasmException original = new WasmException("error");
      final String trace1 = "func1 (mod1.wasm:10)";
      final String trace2 = "func2 (mod2.wasm:20)";

      final EnhancedWasmException combined =
          StackTracePreserver.combineStackTraces(original, trace1, trace2);

      assertNotNull(combined, "Combined exception should not be null");
      assertEquals(2, combined.getWasmStackFrames().size(), "Should have frames from both traces");
    }

    @Test
    @DisplayName("should handle empty varargs")
    void shouldHandleEmptyVarargs() {
      final WasmException original = new WasmException("error");

      final EnhancedWasmException combined = StackTracePreserver.combineStackTraces(original);

      assertNotNull(combined, "Should handle no additional traces");
      assertTrue(combined.getWasmStackFrames().isEmpty(), "Should have no frames");
    }
  }

  @Nested
  @DisplayName("extractDebuggingInfo Tests")
  class ExtractDebuggingInfoTests {

    @Test
    @DisplayName("should extract debugging info from exception")
    void shouldExtractDebuggingInfoFromException() {
      final WasmException exception = new WasmException("test error");

      final String info = StackTracePreserver.extractDebuggingInfo(exception);

      assertNotNull(info, "Debugging info should not be null");
      assertTrue(info.contains("WasmException"), "Should contain exception type");
      assertTrue(info.contains("test error"), "Should contain message");
    }

    @Test
    @DisplayName("should extract info from enhanced exception")
    void shouldExtractInfoFromEnhancedException() {
      final List<WasmStackFrame> frames = new ArrayList<>();
      frames.add(WasmStackFrame.simple("func", 0L));
      final EnhancedWasmException enhanced =
          new EnhancedWasmException("error", frames, "trap", null);

      final String info = StackTracePreserver.extractDebuggingInfo(enhanced);

      assertTrue(info.contains("EnhancedWasmException"), "Should identify enhanced exception");
      assertTrue(info.contains("WebAssembly frames"), "Should mention WASM frames");
    }

    @Test
    @DisplayName("should handle exception chain")
    void shouldHandleExceptionChain() {
      final Exception inner = new Exception("inner");
      final WasmException outer = new WasmException("outer", inner);

      final String info = StackTracePreserver.extractDebuggingInfo(outer);

      assertTrue(info.contains("outer"), "Should contain outer message");
      assertTrue(info.contains("inner"), "Should contain inner message");
    }
  }

  @Nested
  @DisplayName("Public Methods Tests")
  class PublicMethodsTests {

    @Test
    @DisplayName("should have parseNativeStackTrace method")
    void shouldHaveParseNativeStackTraceMethod() throws NoSuchMethodException {
      final Method method =
          StackTracePreserver.class.getMethod("parseNativeStackTrace", String.class);
      assertNotNull(method, "parseNativeStackTrace method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Method should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Method should be public");
    }

    @Test
    @DisplayName("should have enhanceException method")
    void shouldHaveEnhanceExceptionMethod() throws NoSuchMethodException {
      final Method method =
          StackTracePreserver.class.getMethod(
              "enhanceException", WasmException.class, String.class, String.class);
      assertNotNull(method, "enhanceException method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Method should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Method should be public");
    }

    @Test
    @DisplayName("should have createEnhancedException method")
    void shouldHaveCreateEnhancedExceptionMethod() throws NoSuchMethodException {
      final Method method =
          StackTracePreserver.class.getMethod(
              "createEnhancedException", String.class, String.class, String.class, Throwable.class);
      assertNotNull(method, "createEnhancedException method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Method should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Method should be public");
    }

    @Test
    @DisplayName("should have combineStackTraces method")
    void shouldHaveCombineStackTracesMethod() throws NoSuchMethodException {
      final Method method =
          StackTracePreserver.class.getMethod(
              "combineStackTraces", WasmException.class, String[].class);
      assertNotNull(method, "combineStackTraces method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Method should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Method should be public");
    }

    @Test
    @DisplayName("should have extractDebuggingInfo method")
    void shouldHaveExtractDebuggingInfoMethod() throws NoSuchMethodException {
      final Method method =
          StackTracePreserver.class.getMethod("extractDebuggingInfo", Throwable.class);
      assertNotNull(method, "extractDebuggingInfo method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Method should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Method should be public");
    }
  }
}

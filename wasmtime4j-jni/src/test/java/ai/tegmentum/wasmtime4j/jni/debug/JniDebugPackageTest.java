/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 */

package ai.tegmentum.wasmtime4j.jni.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.debug.Breakpoint;
import ai.tegmentum.wasmtime4j.debug.DebugCapabilities;
import ai.tegmentum.wasmtime4j.debug.DebugConfig;
import ai.tegmentum.wasmtime4j.debug.DebugSession;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the JNI debug package.
 *
 * <p>This test covers all debug classes in the ai.tegmentum.wasmtime4j.jni.debug package including
 * JniBreakpoint, JniDebugCapabilities, JniDebugConfig, JniDebugSession, JniStackFrame, JniVariable,
 * and JniVariableValue.
 */
@DisplayName("JNI Debug Package Tests")
class JniDebugPackageTest {

  private static final Logger LOGGER = Logger.getLogger(JniDebugPackageTest.class.getName());

  // ========================================================================
  // JniBreakpoint Tests
  // ========================================================================

  @Nested
  @DisplayName("JniBreakpoint Tests")
  class JniBreakpointTests {

    @Test
    @DisplayName("JniBreakpoint should be a final class")
    void jniBreakpointShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniBreakpoint.class.getModifiers()), "JniBreakpoint should be final");
    }

    @Test
    @DisplayName("JniBreakpoint should implement Breakpoint interface")
    void jniBreakpointShouldImplementBreakpointInterface() {
      assertTrue(
          Breakpoint.class.isAssignableFrom(JniBreakpoint.class),
          "JniBreakpoint should implement Breakpoint");
    }

    @Test
    @DisplayName("Create JniBreakpoint with constructor")
    void testCreateJniBreakpoint() {
      LOGGER.info("Testing JniBreakpoint creation with constructor");

      JniBreakpoint breakpoint = new JniBreakpoint("bp-1", "test_func", 10, 5, 0x1000L);

      assertNotNull(breakpoint, "Breakpoint should not be null");
      assertEquals("bp-1", breakpoint.getBreakpointId(), "Breakpoint ID should match");
      assertEquals("test_func", breakpoint.getFunctionName(), "Function name should match");
      assertEquals(10, breakpoint.getLineNumber(), "Line number should match");
      assertEquals(5, breakpoint.getColumnNumber(), "Column number should match");
      assertEquals(0x1000L, breakpoint.getInstructionOffset(), "Instruction offset should match");
      assertTrue(breakpoint.isEnabled(), "Breakpoint should be enabled by default");
      assertEquals(0, breakpoint.getHitCount(), "Hit count should be 0 initially");
      assertNull(breakpoint.getCondition(), "Condition should be null by default");

      LOGGER.info("JniBreakpoint created successfully: " + breakpoint);
    }

    @Test
    @DisplayName("Create JniBreakpoint using fromNative factory method")
    void testCreateJniBreakpointFromNative() {
      LOGGER.info("Testing JniBreakpoint creation using fromNative");

      JniBreakpoint breakpoint =
          JniBreakpoint.fromNative(123L, "native_func", 42, 3, 0x2000L);

      assertNotNull(breakpoint, "Breakpoint should not be null");
      assertEquals("bp-123", breakpoint.getBreakpointId(), "Breakpoint ID should be bp-123");
      assertEquals("native_func", breakpoint.getFunctionName(), "Function name should match");
      assertEquals(42, breakpoint.getLineNumber(), "Line number should match");
      assertEquals(3, breakpoint.getColumnNumber(), "Column number should match");
      assertEquals(0x2000L, breakpoint.getInstructionOffset(), "Instruction offset should match");

      LOGGER.info("JniBreakpoint from native created successfully: " + breakpoint);
    }

    @Test
    @DisplayName("JniBreakpoint constructor should reject null breakpointId")
    void jniBreakpointConstructorShouldRejectNullId() {
      assertThrows(
          NullPointerException.class,
          () -> new JniBreakpoint(null, "test_func", 1, 1, 0L),
          "Constructor should throw NullPointerException for null breakpointId");
    }

    @Test
    @DisplayName("JniBreakpoint should support enable/disable")
    void testBreakpointEnableDisable() {
      JniBreakpoint breakpoint = new JniBreakpoint("bp-1", "test_func", 10, 5, 0x1000L);

      assertTrue(breakpoint.isEnabled(), "Breakpoint should be enabled initially");

      breakpoint.setEnabled(false);
      assertFalse(breakpoint.isEnabled(), "Breakpoint should be disabled");

      breakpoint.setEnabled(true);
      assertTrue(breakpoint.isEnabled(), "Breakpoint should be enabled again");
    }

    @Test
    @DisplayName("JniBreakpoint should support condition")
    void testBreakpointCondition() {
      JniBreakpoint breakpoint = new JniBreakpoint("bp-1", "test_func", 10, 5, 0x1000L);

      assertNull(breakpoint.getCondition(), "Condition should be null initially");

      breakpoint.setCondition("x > 10");
      assertEquals("x > 10", breakpoint.getCondition(), "Condition should match");

      breakpoint.setCondition(null);
      assertNull(breakpoint.getCondition(), "Condition should be null after clearing");
    }

    @Test
    @DisplayName("JniBreakpoint should support hit count")
    void testBreakpointHitCount() {
      JniBreakpoint breakpoint = new JniBreakpoint("bp-1", "test_func", 10, 5, 0x1000L);

      assertEquals(0, breakpoint.getHitCount(), "Hit count should be 0 initially");

      breakpoint.incrementHitCount();
      assertEquals(1, breakpoint.getHitCount(), "Hit count should be 1");

      breakpoint.incrementHitCount();
      assertEquals(2, breakpoint.getHitCount(), "Hit count should be 2");

      breakpoint.resetHitCount();
      assertEquals(0, breakpoint.getHitCount(), "Hit count should be 0 after reset");
    }

    @Test
    @DisplayName("JniBreakpoint should have correct package")
    void jniBreakpointShouldBeInCorrectPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.jni.debug",
          JniBreakpoint.class.getPackage().getName(),
          "JniBreakpoint should be in ai.tegmentum.wasmtime4j.jni.debug package");
    }
  }

  // ========================================================================
  // JniDebugCapabilities Tests
  // ========================================================================

  @Nested
  @DisplayName("JniDebugCapabilities Tests")
  class JniDebugCapabilitiesTests {

    @Test
    @DisplayName("JniDebugCapabilities should be a final class")
    void jniDebugCapabilitiesShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniDebugCapabilities.class.getModifiers()),
          "JniDebugCapabilities should be final");
    }

    @Test
    @DisplayName("JniDebugCapabilities should implement DebugCapabilities interface")
    void jniDebugCapabilitiesShouldImplementInterface() {
      assertTrue(
          DebugCapabilities.class.isAssignableFrom(JniDebugCapabilities.class),
          "JniDebugCapabilities should implement DebugCapabilities");
    }

    @Test
    @DisplayName("JniDebugCapabilities should use builder pattern")
    void jniDebugCapabilitiesShouldUseBuilderPattern() throws NoSuchMethodException {
      // Verify builder() static method exists
      Method builderMethod = JniDebugCapabilities.class.getMethod("builder");
      assertNotNull(builderMethod, "builder() method should exist");
      assertTrue(
          Modifier.isStatic(builderMethod.getModifiers()), "builder() should be static");

      // Verify getDefault() static method exists
      Method getDefaultMethod = JniDebugCapabilities.class.getMethod("getDefault");
      assertNotNull(getDefaultMethod, "getDefault() method should exist");
      assertTrue(
          Modifier.isStatic(getDefaultMethod.getModifiers()), "getDefault() should be static");
    }

    @Test
    @DisplayName("JniDebugCapabilities can be created via builder")
    void jniDebugCapabilitiesCanBeCreatedViaBuilder() {
      JniDebugCapabilities capabilities = JniDebugCapabilities.builder().build();
      assertNotNull(capabilities, "Capabilities should be created via builder");
    }

    @Test
    @DisplayName("JniDebugCapabilities can be created via getDefault")
    void jniDebugCapabilitiesCanBeCreatedViaGetDefault() {
      JniDebugCapabilities capabilities = JniDebugCapabilities.getDefault();
      assertNotNull(capabilities, "Capabilities should be created via getDefault");
      assertTrue(capabilities.supportsBreakpoints(), "Default should support breakpoints");
    }

    @Test
    @DisplayName("JniDebugCapabilities should be in correct package")
    void jniDebugCapabilitiesShouldBeInCorrectPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.jni.debug",
          JniDebugCapabilities.class.getPackage().getName(),
          "JniDebugCapabilities should be in ai.tegmentum.wasmtime4j.jni.debug package");
    }
  }

  // ========================================================================
  // JniDebugConfig Tests
  // ========================================================================

  @Nested
  @DisplayName("JniDebugConfig Tests")
  class JniDebugConfigTests {

    @Test
    @DisplayName("JniDebugConfig should be a final class")
    void jniDebugConfigShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniDebugConfig.class.getModifiers()),
          "JniDebugConfig should be final");
    }

    @Test
    @DisplayName("JniDebugConfig should implement DebugConfig interface")
    void jniDebugConfigShouldImplementInterface() {
      assertTrue(
          DebugConfig.class.isAssignableFrom(JniDebugConfig.class),
          "JniDebugConfig should implement DebugConfig");
    }

    @Test
    @DisplayName("JniDebugConfig should use builder pattern")
    void jniDebugConfigShouldUseBuilderPattern() throws NoSuchMethodException {
      // Verify builder() static method exists
      Method builderMethod = JniDebugConfig.class.getMethod("builder");
      assertNotNull(builderMethod, "builder() method should exist");
      assertTrue(
          Modifier.isStatic(builderMethod.getModifiers()), "builder() should be static");

      // Verify getDefault() static method exists
      Method getDefaultMethod = JniDebugConfig.class.getMethod("getDefault");
      assertNotNull(getDefaultMethod, "getDefault() method should exist");
      assertTrue(
          Modifier.isStatic(getDefaultMethod.getModifiers()), "getDefault() should be static");
    }

    @Test
    @DisplayName("JniDebugConfig can be created via builder")
    void jniDebugConfigCanBeCreatedViaBuilder() {
      JniDebugConfig config = JniDebugConfig.builder().build();
      assertNotNull(config, "Config should be created via builder");
    }

    @Test
    @DisplayName("JniDebugConfig can be created via getDefault")
    void jniDebugConfigCanBeCreatedViaGetDefault() {
      JniDebugConfig config = JniDebugConfig.getDefault();
      assertNotNull(config, "Config should be created via getDefault");
      assertEquals(
          JniDebugConfig.DEFAULT_DEBUG_PORT, config.getDebugPort(), "Should use default port");
    }

    @Test
    @DisplayName("JniDebugConfig should be in correct package")
    void jniDebugConfigShouldBeInCorrectPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.jni.debug",
          JniDebugConfig.class.getPackage().getName(),
          "JniDebugConfig should be in ai.tegmentum.wasmtime4j.jni.debug package");
    }
  }

  // ========================================================================
  // JniDebugSession Tests
  // ========================================================================

  @Nested
  @DisplayName("JniDebugSession Tests")
  class JniDebugSessionTests {

    @Test
    @DisplayName("JniDebugSession should be a final class")
    void jniDebugSessionShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniDebugSession.class.getModifiers()),
          "JniDebugSession should be final");
    }

    @Test
    @DisplayName("JniDebugSession should implement DebugSession interface")
    void jniDebugSessionShouldImplementInterface() {
      assertTrue(
          DebugSession.class.isAssignableFrom(JniDebugSession.class),
          "JniDebugSession should implement DebugSession");
    }

    @Test
    @DisplayName("JniDebugSession should be in correct package")
    void jniDebugSessionShouldBeInCorrectPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.jni.debug",
          JniDebugSession.class.getPackage().getName(),
          "JniDebugSession should be in ai.tegmentum.wasmtime4j.jni.debug package");
    }
  }

  // ========================================================================
  // JniStackFrame Tests
  // ========================================================================

  @Nested
  @DisplayName("JniStackFrame Tests")
  class JniStackFrameTests {

    @Test
    @DisplayName("JniStackFrame should be a final class")
    void jniStackFrameShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniStackFrame.class.getModifiers()), "JniStackFrame should be final");
    }

    @Test
    @DisplayName("JniStackFrame should be a standalone class")
    void jniStackFrameShouldBeStandaloneClass() {
      // JniStackFrame does not implement an interface - it's a value class
      Class<?>[] interfaces = JniStackFrame.class.getInterfaces();
      assertEquals(0, interfaces.length, "JniStackFrame should not implement any interfaces");
    }

    @Test
    @DisplayName("JniStackFrame should have public constructor")
    void jniStackFrameShouldHavePublicConstructor() {
      Constructor<?>[] constructors = JniStackFrame.class.getConstructors();
      assertTrue(constructors.length > 0, "JniStackFrame should have public constructor");
    }

    @Test
    @DisplayName("JniStackFrame should have fromNative factory method")
    void jniStackFrameShouldHaveFromNativeMethod() throws NoSuchMethodException {
      Method fromNative = JniStackFrame.class.getMethod(
          "fromNative", int.class, String.class, long.class, int.class, int.class, String.class);
      assertNotNull(fromNative, "fromNative method should exist");
      assertTrue(Modifier.isStatic(fromNative.getModifiers()), "fromNative should be static");
    }

    @Test
    @DisplayName("JniStackFrame should have builder method")
    void jniStackFrameShouldHaveBuilderMethod() throws NoSuchMethodException {
      Method builder = JniStackFrame.class.getMethod("builder");
      assertNotNull(builder, "builder() method should exist");
      assertTrue(Modifier.isStatic(builder.getModifiers()), "builder() should be static");
    }

    @Test
    @DisplayName("JniStackFrame can be created via constructor")
    void jniStackFrameCanBeCreatedViaConstructor() {
      JniStackFrame frame = new JniStackFrame(
          0, "test_func", 0x1000L, 10, 5, "test.wasm", Collections.emptyList());
      assertNotNull(frame, "Frame should be created via constructor");
      assertEquals("test_func", frame.getFunctionName(), "Function name should match");
    }

    @Test
    @DisplayName("JniStackFrame can be created via fromNative")
    void jniStackFrameCanBeCreatedViaFromNative() {
      JniStackFrame frame = JniStackFrame.fromNative(0, "native_func", 0x2000L, 20, 3, "module.wasm");
      assertNotNull(frame, "Frame should be created via fromNative");
      assertEquals("native_func", frame.getFunctionName(), "Function name should match");
      assertEquals(20, frame.getLineNumber(), "Line number should match");
    }

    @Test
    @DisplayName("JniStackFrame should be in correct package")
    void jniStackFrameShouldBeInCorrectPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.jni.debug",
          JniStackFrame.class.getPackage().getName(),
          "JniStackFrame should be in ai.tegmentum.wasmtime4j.jni.debug package");
    }
  }

  // ========================================================================
  // JniVariable Tests
  // ========================================================================

  @Nested
  @DisplayName("JniVariable Tests")
  class JniVariableTests {

    @Test
    @DisplayName("JniVariable should be a final class")
    void jniVariableShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniVariable.class.getModifiers()), "JniVariable should be final");
    }

    @Test
    @DisplayName("JniVariable should be a standalone class")
    void jniVariableShouldBeStandaloneClass() {
      // JniVariable does not implement an interface - it's a value class
      Class<?>[] interfaces = JniVariable.class.getInterfaces();
      assertEquals(0, interfaces.length, "JniVariable should not implement any interfaces");
    }

    @Test
    @DisplayName("JniVariable should have public constructor")
    void jniVariableShouldHavePublicConstructor() {
      Constructor<?>[] constructors = JniVariable.class.getConstructors();
      assertTrue(constructors.length > 0, "JniVariable should have public constructor");
    }

    @Test
    @DisplayName("JniVariable should have static factory methods")
    void jniVariableShouldHaveStaticFactoryMethods() throws NoSuchMethodException {
      // local factory
      Method local = JniVariable.class.getMethod(
          "local", String.class, String.class, JniVariableValue.class, int.class);
      assertNotNull(local, "local() factory should exist");
      assertTrue(Modifier.isStatic(local.getModifiers()), "local() should be static");

      // parameter factory
      Method parameter = JniVariable.class.getMethod(
          "parameter", String.class, String.class, JniVariableValue.class, int.class);
      assertNotNull(parameter, "parameter() factory should exist");
      assertTrue(Modifier.isStatic(parameter.getModifiers()), "parameter() should be static");

      // global factory
      Method global = JniVariable.class.getMethod(
          "global", String.class, String.class, JniVariableValue.class, int.class, boolean.class);
      assertNotNull(global, "global() factory should exist");
      assertTrue(Modifier.isStatic(global.getModifiers()), "global() should be static");
    }

    @Test
    @DisplayName("JniVariable can be created via constructor")
    void jniVariableCanBeCreatedViaConstructor() {
      JniVariableValue value = JniVariableValue.i32(42);
      JniVariable variable = new JniVariable(
          "x", "i32", value, JniVariable.VariableScope.LOCAL, 0, true, "test variable");
      assertNotNull(variable, "Variable should be created via constructor");
      assertEquals("x", variable.getName(), "Name should match");
      assertEquals("i32", variable.getVarType(), "Type should match");
    }

    @Test
    @DisplayName("JniVariable can be created via local factory")
    void jniVariableCanBeCreatedViaLocalFactory() {
      JniVariableValue value = JniVariableValue.i32(100);
      JniVariable variable = JniVariable.local("count", "i32", value, 0);
      assertNotNull(variable, "Variable should be created via local()");
      assertEquals("count", variable.getName(), "Name should match");
      assertEquals(JniVariable.VariableScope.LOCAL, variable.getScope(), "Scope should be LOCAL");
    }

    @Test
    @DisplayName("JniVariable should be in correct package")
    void jniVariableShouldBeInCorrectPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.jni.debug",
          JniVariable.class.getPackage().getName(),
          "JniVariable should be in ai.tegmentum.wasmtime4j.jni.debug package");
    }
  }

  // ========================================================================
  // JniVariableValue Tests
  // ========================================================================

  @Nested
  @DisplayName("JniVariableValue Tests")
  class JniVariableValueTests {

    @Test
    @DisplayName("JniVariableValue should be a final class")
    void jniVariableValueShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniVariableValue.class.getModifiers()),
          "JniVariableValue should be final");
    }

    @Test
    @DisplayName("JniVariableValue should use static factory methods")
    void jniVariableValueShouldUseStaticFactoryMethods() throws NoSuchMethodException {
      // Private constructor - uses static factory methods instead
      Constructor<?>[] publicConstructors = JniVariableValue.class.getConstructors();
      assertEquals(0, publicConstructors.length, "JniVariableValue should have no public constructor");

      // Verify static factory methods exist
      Method i32 = JniVariableValue.class.getMethod("i32", int.class);
      assertNotNull(i32, "i32() factory should exist");
      assertTrue(Modifier.isStatic(i32.getModifiers()), "i32() should be static");

      Method i64 = JniVariableValue.class.getMethod("i64", long.class);
      assertNotNull(i64, "i64() factory should exist");
      assertTrue(Modifier.isStatic(i64.getModifiers()), "i64() should be static");

      Method f32 = JniVariableValue.class.getMethod("f32", float.class);
      assertNotNull(f32, "f32() factory should exist");
      assertTrue(Modifier.isStatic(f32.getModifiers()), "f32() should be static");

      Method f64 = JniVariableValue.class.getMethod("f64", double.class);
      assertNotNull(f64, "f64() factory should exist");
      assertTrue(Modifier.isStatic(f64.getModifiers()), "f64() should be static");
    }

    @Test
    @DisplayName("JniVariableValue can be created via factory methods")
    void jniVariableValueCanBeCreatedViaFactoryMethods() {
      JniVariableValue i32Value = JniVariableValue.i32(42);
      assertNotNull(i32Value, "i32 value should be created");
      assertEquals(42, i32Value.asI32(), "i32 value should match");

      JniVariableValue i64Value = JniVariableValue.i64(1000L);
      assertNotNull(i64Value, "i64 value should be created");
      assertEquals(1000L, i64Value.asI64(), "i64 value should match");

      JniVariableValue f32Value = JniVariableValue.f32(3.14f);
      assertNotNull(f32Value, "f32 value should be created");
      assertEquals(3.14f, f32Value.asF32(), 0.001f, "f32 value should match");

      JniVariableValue f64Value = JniVariableValue.f64(2.718);
      assertNotNull(f64Value, "f64 value should be created");
      assertEquals(2.718, f64Value.asF64(), 0.001, "f64 value should match");
    }

    @Test
    @DisplayName("JniVariableValue should be in correct package")
    void jniVariableValueShouldBeInCorrectPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.jni.debug",
          JniVariableValue.class.getPackage().getName(),
          "JniVariableValue should be in ai.tegmentum.wasmtime4j.jni.debug package");
    }
  }

  // ========================================================================
  // Package-Level Tests
  // ========================================================================

  @Nested
  @DisplayName("Package-Level Tests")
  class PackageLevelTests {

    @Test
    @DisplayName("All debug classes should be in correct package")
    void allDebugClassesShouldBeInCorrectPackage() {
      Class<?>[] debugClasses = {
        JniBreakpoint.class,
        JniDebugCapabilities.class,
        JniDebugConfig.class,
        JniDebugSession.class,
        JniStackFrame.class,
        JniVariable.class,
        JniVariableValue.class
      };

      String expectedPackage = "ai.tegmentum.wasmtime4j.jni.debug";
      for (Class<?> clazz : debugClasses) {
        assertEquals(
            expectedPackage,
            clazz.getPackage().getName(),
            clazz.getSimpleName() + " should be in " + expectedPackage);
      }
    }

    @Test
    @DisplayName("All debug classes should be final")
    void allDebugClassesShouldBeFinal() {
      Class<?>[] debugClasses = {
        JniBreakpoint.class,
        JniDebugCapabilities.class,
        JniDebugConfig.class,
        JniDebugSession.class,
        JniStackFrame.class,
        JniVariable.class,
        JniVariableValue.class
      };

      for (Class<?> clazz : debugClasses) {
        assertTrue(
            Modifier.isFinal(clazz.getModifiers()), clazz.getSimpleName() + " should be final");
      }
    }

    @Test
    @DisplayName("All debug classes should not be interfaces")
    void allDebugClassesShouldNotBeInterfaces() {
      Class<?>[] debugClasses = {
        JniBreakpoint.class,
        JniDebugCapabilities.class,
        JniDebugConfig.class,
        JniDebugSession.class,
        JniStackFrame.class,
        JniVariable.class,
        JniVariableValue.class
      };

      for (Class<?> clazz : debugClasses) {
        assertFalse(clazz.isInterface(), clazz.getSimpleName() + " should not be an interface");
      }
    }
  }
}

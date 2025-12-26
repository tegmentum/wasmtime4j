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

import ai.tegmentum.wasmtime4j.debug.Variable.VariableScope;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Variable} interface.
 *
 * <p>Variable provides variable inspection for WebAssembly debugging including name, type, value,
 * and scope information.
 */
@DisplayName("Variable Tests")
class VariableTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(Variable.class.isInterface(), "Variable should be an interface");
    }

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = Variable.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = Variable.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getValue method")
    void shouldHaveGetValueMethod() throws NoSuchMethodException {
      final Method method = Variable.class.getMethod("getValue");
      assertNotNull(method, "getValue method should exist");
      assertEquals(VariableValue.class, method.getReturnType(), "Should return VariableValue");
    }

    @Test
    @DisplayName("should have getScope method")
    void shouldHaveGetScopeMethod() throws NoSuchMethodException {
      final Method method = Variable.class.getMethod("getScope");
      assertNotNull(method, "getScope method should exist");
      assertEquals(VariableScope.class, method.getReturnType(), "Should return VariableScope");
    }

    @Test
    @DisplayName("should have isMutable method")
    void shouldHaveIsMutableMethod() throws NoSuchMethodException {
      final Method method = Variable.class.getMethod("isMutable");
      assertNotNull(method, "isMutable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("VariableScope Enum Tests")
  class VariableScopeEnumTests {

    @Test
    @DisplayName("should have LOCAL value")
    void shouldHaveLocalValue() {
      assertNotNull(VariableScope.valueOf("LOCAL"), "LOCAL should exist");
    }

    @Test
    @DisplayName("should have GLOBAL value")
    void shouldHaveGlobalValue() {
      assertNotNull(VariableScope.valueOf("GLOBAL"), "GLOBAL should exist");
    }

    @Test
    @DisplayName("should have PARAMETER value")
    void shouldHaveParameterValue() {
      assertNotNull(VariableScope.valueOf("PARAMETER"), "PARAMETER should exist");
    }

    @Test
    @DisplayName("should have RETURN_VALUE value")
    void shouldHaveReturnValueValue() {
      assertNotNull(VariableScope.valueOf("RETURN_VALUE"), "RETURN_VALUE should exist");
    }

    @Test
    @DisplayName("should have exactly four values")
    void shouldHaveExactlyFourValues() {
      assertEquals(4, VariableScope.values().length, "Should have exactly 4 VariableScope values");
    }
  }

  @Nested
  @DisplayName("Mock Implementation Tests")
  class MockImplementationTests {

    @Test
    @DisplayName("mock variable should return name")
    void mockVariableShouldReturnName() {
      final MockVariable variable = new MockVariable("counter", "i32", VariableScope.LOCAL, true);

      assertEquals("counter", variable.getName(), "Variable name should match");
    }

    @Test
    @DisplayName("mock variable should return type")
    void mockVariableShouldReturnType() {
      final MockVariable variable = new MockVariable("x", "f64", VariableScope.GLOBAL, false);

      assertEquals("f64", variable.getType(), "Variable type should match");
    }

    @Test
    @DisplayName("mock variable should return scope")
    void mockVariableShouldReturnScope() {
      final MockVariable paramVar = new MockVariable("arg", "i32", VariableScope.PARAMETER, false);
      final MockVariable globalVar = new MockVariable("g", "i64", VariableScope.GLOBAL, false);
      final MockVariable localVar = new MockVariable("l", "f32", VariableScope.LOCAL, true);
      final MockVariable returnVar =
          new MockVariable("r", "i32", VariableScope.RETURN_VALUE, false);

      assertEquals(VariableScope.PARAMETER, paramVar.getScope(), "Should be parameter scope");
      assertEquals(VariableScope.GLOBAL, globalVar.getScope(), "Should be global scope");
      assertEquals(VariableScope.LOCAL, localVar.getScope(), "Should be local scope");
      assertEquals(
          VariableScope.RETURN_VALUE, returnVar.getScope(), "Should be return value scope");
    }

    @Test
    @DisplayName("mock variable should report mutability")
    void mockVariableShouldReportMutability() {
      final MockVariable mutableVar = new MockVariable("x", "i32", VariableScope.LOCAL, true);
      final MockVariable immutableVar = new MockVariable("y", "i32", VariableScope.LOCAL, false);

      assertTrue(mutableVar.isMutable(), "Should be mutable");
      assertFalse(immutableVar.isMutable(), "Should be immutable");
    }

    @Test
    @DisplayName("mock variable should return value")
    void mockVariableShouldReturnValue() {
      final MockVariable variable = new MockVariable("x", "i32", VariableScope.LOCAL, true);
      variable.setValue(new MockVariableValue(42));

      assertNotNull(variable.getValue(), "Value should not be null");
      assertEquals(42, variable.getValue().asInt(), "Value should match");
    }
  }

  /** Mock implementation of Variable for testing. */
  private static class MockVariable implements Variable {
    private final String name;
    private final String type;
    private final VariableScope scope;
    private final boolean mutable;
    private VariableValue value;

    MockVariable(
        final String name, final String type, final VariableScope scope, final boolean mutable) {
      this.name = name;
      this.type = type;
      this.scope = scope;
      this.mutable = mutable;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getType() {
      return type;
    }

    @Override
    public VariableValue getValue() {
      return value;
    }

    @Override
    public VariableScope getScope() {
      return scope;
    }

    @Override
    public boolean isMutable() {
      return mutable;
    }

    public void setValue(final VariableValue value) {
      this.value = value;
    }
  }

  /** Mock implementation of VariableValue for testing. */
  private static class MockVariableValue implements VariableValue {
    private final Object value;

    MockVariableValue(final Object value) {
      this.value = value;
    }

    @Override
    public String asString() {
      return String.valueOf(value);
    }

    @Override
    public int asInt() {
      return ((Number) value).intValue();
    }

    @Override
    public long asLong() {
      return ((Number) value).longValue();
    }

    @Override
    public float asFloat() {
      return ((Number) value).floatValue();
    }

    @Override
    public double asDouble() {
      return ((Number) value).doubleValue();
    }

    @Override
    public boolean asBoolean() {
      return (Boolean) value;
    }

    @Override
    public Object getRawValue() {
      return value;
    }

    @Override
    public ValueType getValueType() {
      if (value instanceof Integer) {
        return ValueType.INTEGER;
      }
      if (value instanceof Long) {
        return ValueType.LONG;
      }
      if (value instanceof Float) {
        return ValueType.FLOAT;
      }
      if (value instanceof Double) {
        return ValueType.DOUBLE;
      }
      if (value instanceof Boolean) {
        return ValueType.BOOLEAN;
      }
      if (value instanceof String) {
        return ValueType.STRING;
      }
      return ValueType.OBJECT;
    }
  }
}

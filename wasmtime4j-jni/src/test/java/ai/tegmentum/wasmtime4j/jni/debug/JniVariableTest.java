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

package ai.tegmentum.wasmtime4j.jni.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.debug.JniVariable.VariableScope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link JniVariable}.
 */
@DisplayName("JniVariable Tests")
class JniVariableTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("JniVariable should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(JniVariable.class.getModifiers()),
          "JniVariable should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should create variable with all fields")
    void constructorShouldCreateVariableWithAllFields() {
      final JniVariableValue value = JniVariableValue.i32(42);
      final JniVariable var = new JniVariable(
          "myVar", "i32", value, VariableScope.LOCAL, 5, true, "Test variable");

      assertEquals("myVar", var.getName(), "Name should match");
      assertEquals("i32", var.getVarType(), "Type should match");
      assertEquals(value, var.getValue(), "Value should match");
      assertEquals(VariableScope.LOCAL, var.getScope(), "Scope should match");
      assertEquals(5, var.getIndex(), "Index should match");
      assertTrue(var.isMutable(), "Should be mutable");
      assertEquals("Test variable", var.getDescription(), "Description should match");
    }

    @Test
    @DisplayName("Constructor should throw on null name")
    void constructorShouldThrowOnNullName() {
      assertThrows(
          NullPointerException.class,
          () -> new JniVariable(
              null, "i32", JniVariableValue.i32(0), VariableScope.LOCAL, 0, true, null),
          "Should throw on null name");
    }

    @Test
    @DisplayName("Constructor should throw on null varType")
    void constructorShouldThrowOnNullVarType() {
      assertThrows(
          NullPointerException.class,
          () -> new JniVariable(
              "var", null, JniVariableValue.i32(0), VariableScope.LOCAL, 0, true, null),
          "Should throw on null varType");
    }

    @Test
    @DisplayName("Constructor should throw on null value")
    void constructorShouldThrowOnNullValue() {
      assertThrows(
          NullPointerException.class,
          () -> new JniVariable("var", "i32", null, VariableScope.LOCAL, 0, true, null),
          "Should throw on null value");
    }

    @Test
    @DisplayName("Constructor should throw on null scope")
    void constructorShouldThrowOnNullScope() {
      assertThrows(
          NullPointerException.class,
          () -> new JniVariable(
              "var", "i32", JniVariableValue.i32(0), null, 0, true, null),
          "Should throw on null scope");
    }

    @Test
    @DisplayName("Constructor should accept null description")
    void constructorShouldAcceptNullDescription() {
      final JniVariable var = new JniVariable(
          "var", "i32", JniVariableValue.i32(0), VariableScope.LOCAL, 0, true, null);

      assertNull(var.getDescription(), "Description should be null");
    }

    @Test
    @DisplayName("Constructor should accept negative index")
    void constructorShouldAcceptNegativeIndex() {
      final JniVariable var = new JniVariable(
          "var", "i32", JniVariableValue.i32(0), VariableScope.LOCAL, -1, true, null);

      assertEquals(-1, var.getIndex(), "Index should be -1");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests - local()")
  class LocalFactoryTests {

    @Test
    @DisplayName("local() should create local variable with correct scope")
    void localShouldCreateLocalVariableWithCorrectScope() {
      final JniVariable var = JniVariable.local("x", "i32", JniVariableValue.i32(42), 0);

      assertEquals("x", var.getName(), "Name should match");
      assertEquals("i32", var.getVarType(), "Type should match");
      assertEquals(VariableScope.LOCAL, var.getScope(), "Scope should be LOCAL");
      assertEquals(0, var.getIndex(), "Index should match");
      assertTrue(var.isMutable(), "Local variables should be mutable");
      assertNull(var.getDescription(), "Description should be null");
    }

    @Test
    @DisplayName("local() should handle different indices")
    void localShouldHandleDifferentIndices() {
      final JniVariable var0 = JniVariable.local("a", "i32", JniVariableValue.i32(0), 0);
      final JniVariable var1 = JniVariable.local("b", "i32", JniVariableValue.i32(1), 1);
      final JniVariable var2 = JniVariable.local("c", "i32", JniVariableValue.i32(2), 100);

      assertEquals(0, var0.getIndex(), "Index 0 should match");
      assertEquals(1, var1.getIndex(), "Index 1 should match");
      assertEquals(100, var2.getIndex(), "Index 100 should match");
    }

    @Test
    @DisplayName("local() should handle different value types")
    void localShouldHandleDifferentValueTypes() {
      final JniVariable i32Var = JniVariable.local("a", "i32", JniVariableValue.i32(42), 0);
      final JniVariable i64Var = JniVariable.local("b", "i64", JniVariableValue.i64(100L), 1);
      final JniVariable f32Var = JniVariable.local("c", "f32", JniVariableValue.f32(3.14f), 2);
      final JniVariable f64Var = JniVariable.local("d", "f64", JniVariableValue.f64(2.718), 3);

      assertEquals(42, i32Var.getValue().asI32(), "i32 value should match");
      assertEquals(100L, i64Var.getValue().asI64(), "i64 value should match");
      assertEquals(3.14f, f32Var.getValue().asF32(), "f32 value should match");
      assertEquals(2.718, f64Var.getValue().asF64(), "f64 value should match");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests - parameter()")
  class ParameterFactoryTests {

    @Test
    @DisplayName("parameter() should create parameter variable with correct scope")
    void parameterShouldCreateParameterVariableWithCorrectScope() {
      final JniVariable var = JniVariable.parameter("p0", "i64", JniVariableValue.i64(100L), 0);

      assertEquals("p0", var.getName(), "Name should match");
      assertEquals("i64", var.getVarType(), "Type should match");
      assertEquals(VariableScope.PARAMETER, var.getScope(), "Scope should be PARAMETER");
      assertEquals(0, var.getIndex(), "Index should match");
      assertFalse(var.isMutable(), "Parameters should be immutable");
      assertNull(var.getDescription(), "Description should be null");
    }

    @Test
    @DisplayName("parameter() should handle multiple parameters")
    void parameterShouldHandleMultipleParameters() {
      final JniVariable p0 = JniVariable.parameter("arg0", "i32", JniVariableValue.i32(1), 0);
      final JniVariable p1 = JniVariable.parameter("arg1", "i32", JniVariableValue.i32(2), 1);
      final JniVariable p2 = JniVariable.parameter("arg2", "i32", JniVariableValue.i32(3), 2);

      assertEquals(0, p0.getIndex(), "Parameter 0 index should match");
      assertEquals(1, p1.getIndex(), "Parameter 1 index should match");
      assertEquals(2, p2.getIndex(), "Parameter 2 index should match");

      assertFalse(p0.isMutable(), "Parameter 0 should be immutable");
      assertFalse(p1.isMutable(), "Parameter 1 should be immutable");
      assertFalse(p2.isMutable(), "Parameter 2 should be immutable");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests - global()")
  class GlobalFactoryTests {

    @Test
    @DisplayName("global() should create mutable global variable")
    void globalShouldCreateMutableGlobalVariable() {
      final JniVariable var = JniVariable.global(
          "counter", "i32", JniVariableValue.i32(0), 0, true);

      assertEquals("counter", var.getName(), "Name should match");
      assertEquals("i32", var.getVarType(), "Type should match");
      assertEquals(VariableScope.GLOBAL, var.getScope(), "Scope should be GLOBAL");
      assertEquals(0, var.getIndex(), "Index should match");
      assertTrue(var.isMutable(), "Should be mutable");
      assertNull(var.getDescription(), "Description should be null");
    }

    @Test
    @DisplayName("global() should create immutable global variable")
    void globalShouldCreateImmutableGlobalVariable() {
      final JniVariable var = JniVariable.global(
          "constant", "f64", JniVariableValue.f64(Math.PI), 0, false);

      assertEquals("constant", var.getName(), "Name should match");
      assertEquals(VariableScope.GLOBAL, var.getScope(), "Scope should be GLOBAL");
      assertFalse(var.isMutable(), "Should be immutable");
    }

    @Test
    @DisplayName("global() should handle different indices")
    void globalShouldHandleDifferentIndices() {
      final JniVariable g0 = JniVariable.global("g0", "i32", JniVariableValue.i32(0), 0, true);
      final JniVariable g1 = JniVariable.global("g1", "i32", JniVariableValue.i32(1), 5, false);
      final JniVariable g2 = JniVariable.global("g2", "i32", JniVariableValue.i32(2), 10, true);

      assertEquals(0, g0.getIndex(), "Global 0 index should match");
      assertEquals(5, g1.getIndex(), "Global 1 index should match");
      assertEquals(10, g2.getIndex(), "Global 2 index should match");
    }
  }

  @Nested
  @DisplayName("VariableScope Enum Tests")
  class VariableScopeTests {

    @Test
    @DisplayName("VariableScope should have all expected values")
    void variableScopeShouldHaveAllExpectedValues() {
      final VariableScope[] scopes = VariableScope.values();

      assertEquals(6, scopes.length, "Should have 6 scope values");
      assertNotNull(VariableScope.valueOf("LOCAL"), "LOCAL should exist");
      assertNotNull(VariableScope.valueOf("PARAMETER"), "PARAMETER should exist");
      assertNotNull(VariableScope.valueOf("GLOBAL"), "GLOBAL should exist");
      assertNotNull(VariableScope.valueOf("IMPORTED"), "IMPORTED should exist");
      assertNotNull(VariableScope.valueOf("EXPORTED"), "EXPORTED should exist");
      assertNotNull(VariableScope.valueOf("TEMPORARY"), "TEMPORARY should exist");
    }

    @Test
    @DisplayName("Variables can be created with all scope types")
    void variablesCanBeCreatedWithAllScopeTypes() {
      for (final VariableScope scope : VariableScope.values()) {
        final JniVariable var = new JniVariable(
            "var", "i32", JniVariableValue.i32(0), scope, 0, true, null);

        assertEquals(scope, var.getScope(), "Scope should match: " + scope);
      }
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include all fields")
    void toStringShouldIncludeAllFields() {
      final JniVariable var = new JniVariable(
          "myVar", "i32", JniVariableValue.i32(42), VariableScope.LOCAL, 5, true, null);

      final String str = var.toString();

      assertTrue(str.contains("name='myVar'"), "Should contain name");
      assertTrue(str.contains("type='i32'"), "Should contain type");
      assertTrue(str.contains("scope=LOCAL"), "Should contain scope");
      assertTrue(str.contains("index=5"), "Should contain index");
      assertTrue(str.contains("mutable=true"), "Should contain mutable");
    }

    @Test
    @DisplayName("toString should show different scope values")
    void toStringShouldShowDifferentScopeValues() {
      final JniVariable localVar = JniVariable.local("x", "i32", JniVariableValue.i32(0), 0);
      final JniVariable paramVar = JniVariable.parameter("p", "i32", JniVariableValue.i32(0), 0);
      final JniVariable globalVar = JniVariable.global("g", "i32", JniVariableValue.i32(0), 0, true);

      assertTrue(localVar.toString().contains("scope=LOCAL"), "Local should show LOCAL");
      assertTrue(paramVar.toString().contains("scope=PARAMETER"), "Param should show PARAMETER");
      assertTrue(globalVar.toString().contains("scope=GLOBAL"), "Global should show GLOBAL");
    }
  }

  @Nested
  @DisplayName("equals and hashCode Tests")
  class EqualsHashCodeTests {

    @Test
    @DisplayName("equals should return true for same values")
    void equalsShouldReturnTrueForSameValues() {
      final JniVariable var1 = new JniVariable(
          "x", "i32", JniVariableValue.i32(42), VariableScope.LOCAL, 0, true, null);
      final JniVariable var2 = new JniVariable(
          "x", "i32", JniVariableValue.i32(42), VariableScope.LOCAL, 0, true, null);

      assertEquals(var1, var2, "Variables with same values should be equal");
    }

    @Test
    @DisplayName("equals should ignore value content")
    void equalsShouldIgnoreValueContent() {
      final JniVariable var1 = new JniVariable(
          "x", "i32", JniVariableValue.i32(42), VariableScope.LOCAL, 0, true, null);
      final JniVariable var2 = new JniVariable(
          "x", "i32", JniVariableValue.i32(100), VariableScope.LOCAL, 0, true, null);

      assertEquals(var1, var2, "Variables should be equal regardless of value content");
    }

    @Test
    @DisplayName("equals should ignore description")
    void equalsShouldIgnoreDescription() {
      final JniVariable var1 = new JniVariable(
          "x", "i32", JniVariableValue.i32(0), VariableScope.LOCAL, 0, true, "desc1");
      final JniVariable var2 = new JniVariable(
          "x", "i32", JniVariableValue.i32(0), VariableScope.LOCAL, 0, true, "desc2");

      assertEquals(var1, var2, "Variables should be equal regardless of description");
    }

    @Test
    @DisplayName("equals should return false for different name")
    void equalsShouldReturnFalseForDifferentName() {
      final JniVariable var1 = JniVariable.local("x", "i32", JniVariableValue.i32(0), 0);
      final JniVariable var2 = JniVariable.local("y", "i32", JniVariableValue.i32(0), 0);

      assertNotEquals(var1, var2, "Variables with different names should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different type")
    void equalsShouldReturnFalseForDifferentType() {
      final JniVariable var1 = JniVariable.local("x", "i32", JniVariableValue.i32(0), 0);
      final JniVariable var2 = JniVariable.local("x", "i64", JniVariableValue.i64(0L), 0);

      assertNotEquals(var1, var2, "Variables with different types should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different scope")
    void equalsShouldReturnFalseForDifferentScope() {
      final JniVariable var1 = JniVariable.local("x", "i32", JniVariableValue.i32(0), 0);
      final JniVariable var2 = JniVariable.parameter("x", "i32", JniVariableValue.i32(0), 0);

      assertNotEquals(var1, var2, "Variables with different scopes should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different index")
    void equalsShouldReturnFalseForDifferentIndex() {
      final JniVariable var1 = JniVariable.local("x", "i32", JniVariableValue.i32(0), 0);
      final JniVariable var2 = JniVariable.local("x", "i32", JniVariableValue.i32(0), 1);

      assertNotEquals(var1, var2, "Variables with different indices should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different mutability")
    void equalsShouldReturnFalseForDifferentMutability() {
      final JniVariable var1 = JniVariable.global("g", "i32", JniVariableValue.i32(0), 0, true);
      final JniVariable var2 = JniVariable.global("g", "i32", JniVariableValue.i32(0), 0, false);

      assertNotEquals(var1, var2, "Variables with different mutability should not be equal");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final JniVariable var = JniVariable.local("x", "i32", JniVariableValue.i32(0), 0);
      assertNotEquals(null, var, "Should not be equal to null");
    }

    @Test
    @DisplayName("equals should return false for different type")
    void equalsShouldReturnFalseForDifferentObjectType() {
      final JniVariable var = JniVariable.local("x", "i32", JniVariableValue.i32(0), 0);
      assertNotEquals("x", var, "Should not be equal to String");
    }

    @Test
    @DisplayName("equals should return true for same instance")
    void equalsShouldReturnTrueForSameInstance() {
      final JniVariable var = JniVariable.local("x", "i32", JniVariableValue.i32(0), 0);
      assertEquals(var, var, "Should be equal to itself");
    }

    @Test
    @DisplayName("hashCode should be consistent with equals")
    void hashCodeShouldBeConsistentWithEquals() {
      final JniVariable var1 = new JniVariable(
          "x", "i32", JniVariableValue.i32(42), VariableScope.LOCAL, 0, true, null);
      final JniVariable var2 = new JniVariable(
          "x", "i32", JniVariableValue.i32(100), VariableScope.LOCAL, 0, true, null);

      assertEquals(var1.hashCode(), var2.hashCode(),
          "Equal variables should have same hashCode");
    }

    @Test
    @DisplayName("hashCode should be stable across multiple calls")
    void hashCodeShouldBeStableAcrossMultipleCalls() {
      final JniVariable var = JniVariable.local("x", "i32", JniVariableValue.i32(0), 0);

      final int hash1 = var.hashCode();
      final int hash2 = var.hashCode();
      final int hash3 = var.hashCode();

      assertEquals(hash1, hash2, "Hash should be stable");
      assertEquals(hash2, hash3, "Hash should be stable");
    }
  }
}

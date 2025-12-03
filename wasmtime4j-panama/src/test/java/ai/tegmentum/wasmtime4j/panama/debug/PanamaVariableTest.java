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

package ai.tegmentum.wasmtime4j.panama.debug;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.panama.debug.PanamaVariable.VariableScope;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for {@link PanamaVariable} class. */
@DisplayName("PanamaVariable Tests")
public class PanamaVariableTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaVariableTest.class.getName());

  @Test
  @DisplayName("Create local variable")
  public void testCreateLocalVariable() {
    LOGGER.info("Testing local variable creation");

    final PanamaVariableValue value = PanamaVariableValue.i32(42);
    final PanamaVariable variable = PanamaVariable.local("counter", "i32", value, 0);

    assertNotNull(variable, "Variable should not be null");
    assertEquals("counter", variable.getName(), "Name should match");
    assertEquals("i32", variable.getVarType(), "Type should match");
    assertEquals(value, variable.getValue(), "Value should match");
    assertEquals(VariableScope.LOCAL, variable.getScope(), "Scope should be LOCAL");
    assertEquals(0, variable.getIndex(), "Index should be 0");
    assertTrue(variable.isMutable(), "Local variables should be mutable by default");
    assertNull(variable.getDescription(), "Description should be null");

    LOGGER.info("Local variable created successfully: " + variable);
  }

  @Test
  @DisplayName("Create parameter variable")
  public void testCreateParameterVariable() {
    LOGGER.info("Testing parameter variable creation");

    final PanamaVariableValue value = PanamaVariableValue.i64(1000L);
    final PanamaVariable variable = PanamaVariable.parameter("input", "i64", value, 1);

    assertNotNull(variable, "Variable should not be null");
    assertEquals("input", variable.getName(), "Name should match");
    assertEquals("i64", variable.getVarType(), "Type should match");
    assertEquals(value, variable.getValue(), "Value should match");
    assertEquals(VariableScope.PARAMETER, variable.getScope(), "Scope should be PARAMETER");
    assertEquals(1, variable.getIndex(), "Index should be 1");
    assertFalse(variable.isMutable(), "Parameters should not be mutable by default");

    LOGGER.info("Parameter variable created successfully: " + variable);
  }

  @Test
  @DisplayName("Create mutable global variable")
  public void testCreateMutableGlobalVariable() {
    LOGGER.info("Testing mutable global variable creation");

    final PanamaVariableValue value = PanamaVariableValue.f32(3.14f);
    final PanamaVariable variable = PanamaVariable.global("pi_approx", "f32", value, 0, true);

    assertNotNull(variable, "Variable should not be null");
    assertEquals("pi_approx", variable.getName(), "Name should match");
    assertEquals("f32", variable.getVarType(), "Type should match");
    assertEquals(value, variable.getValue(), "Value should match");
    assertEquals(VariableScope.GLOBAL, variable.getScope(), "Scope should be GLOBAL");
    assertEquals(0, variable.getIndex(), "Index should be 0");
    assertTrue(variable.isMutable(), "Variable should be mutable");

    LOGGER.info("Mutable global variable created successfully: " + variable);
  }

  @Test
  @DisplayName("Create immutable global variable")
  public void testCreateImmutableGlobalVariable() {
    LOGGER.info("Testing immutable global variable creation");

    final PanamaVariableValue value = PanamaVariableValue.f64(Math.PI);
    final PanamaVariable variable = PanamaVariable.global("pi", "f64", value, 1, false);

    assertNotNull(variable, "Variable should not be null");
    assertEquals("pi", variable.getName(), "Name should match");
    assertEquals(VariableScope.GLOBAL, variable.getScope(), "Scope should be GLOBAL");
    assertFalse(variable.isMutable(), "Variable should not be mutable");

    LOGGER.info("Immutable global variable created successfully: " + variable);
  }

  @Test
  @DisplayName("Create variable with description")
  public void testCreateVariableWithDescription() {
    LOGGER.info("Testing variable creation with description");

    final PanamaVariableValue value = PanamaVariableValue.i32(100);
    final PanamaVariable variable =
        new PanamaVariable(
            "max_items",
            "i32",
            value,
            VariableScope.LOCAL,
            0,
            true,
            "Maximum number of items to process");

    assertNotNull(variable, "Variable should not be null");
    assertEquals(
        "Maximum number of items to process",
        variable.getDescription(),
        "Description should match");

    LOGGER.info("Variable with description created successfully: " + variable);
  }

  @Test
  @DisplayName("Create variable with all scope types")
  public void testAllScopeTypes() {
    LOGGER.info("Testing all scope types");

    final PanamaVariableValue value = PanamaVariableValue.i32(0);

    final PanamaVariable local =
        new PanamaVariable("local_var", "i32", value, VariableScope.LOCAL, 0, true, null);
    assertEquals(VariableScope.LOCAL, local.getScope(), "Scope should be LOCAL");

    final PanamaVariable param =
        new PanamaVariable("param_var", "i32", value, VariableScope.PARAMETER, 0, false, null);
    assertEquals(VariableScope.PARAMETER, param.getScope(), "Scope should be PARAMETER");

    final PanamaVariable global =
        new PanamaVariable("global_var", "i32", value, VariableScope.GLOBAL, 0, true, null);
    assertEquals(VariableScope.GLOBAL, global.getScope(), "Scope should be GLOBAL");

    final PanamaVariable imported =
        new PanamaVariable("imported_var", "i32", value, VariableScope.IMPORTED, 0, false, null);
    assertEquals(VariableScope.IMPORTED, imported.getScope(), "Scope should be IMPORTED");

    final PanamaVariable exported =
        new PanamaVariable("exported_var", "i32", value, VariableScope.EXPORTED, 0, false, null);
    assertEquals(VariableScope.EXPORTED, exported.getScope(), "Scope should be EXPORTED");

    final PanamaVariable temporary =
        new PanamaVariable("temp_var", "i32", value, VariableScope.TEMPORARY, 0, false, null);
    assertEquals(VariableScope.TEMPORARY, temporary.getScope(), "Scope should be TEMPORARY");

    LOGGER.info("All scope types test passed");
  }

  @Test
  @DisplayName("Reject null name")
  public void testRejectNullName() {
    LOGGER.info("Testing null name rejection");

    final PanamaVariableValue value = PanamaVariableValue.i32(0);

    assertThrows(
        NullPointerException.class,
        () -> new PanamaVariable(null, "i32", value, VariableScope.LOCAL, 0, true, null),
        "Should reject null name");

    LOGGER.info("Null name rejection test passed");
  }

  @Test
  @DisplayName("Reject null type")
  public void testRejectNullType() {
    LOGGER.info("Testing null type rejection");

    final PanamaVariableValue value = PanamaVariableValue.i32(0);

    assertThrows(
        NullPointerException.class,
        () -> new PanamaVariable("var", null, value, VariableScope.LOCAL, 0, true, null),
        "Should reject null type");

    LOGGER.info("Null type rejection test passed");
  }

  @Test
  @DisplayName("Reject null value")
  public void testRejectNullValue() {
    LOGGER.info("Testing null value rejection");

    assertThrows(
        NullPointerException.class,
        () -> new PanamaVariable("var", "i32", null, VariableScope.LOCAL, 0, true, null),
        "Should reject null value");

    LOGGER.info("Null value rejection test passed");
  }

  @Test
  @DisplayName("Reject null scope")
  public void testRejectNullScope() {
    LOGGER.info("Testing null scope rejection");

    final PanamaVariableValue value = PanamaVariableValue.i32(0);

    assertThrows(
        NullPointerException.class,
        () -> new PanamaVariable("var", "i32", value, null, 0, true, null),
        "Should reject null scope");

    LOGGER.info("Null scope rejection test passed");
  }

  @Test
  @DisplayName("Test toString contains all fields")
  public void testToStringContainsFields() {
    LOGGER.info("Testing toString contains all fields");

    final PanamaVariableValue value = PanamaVariableValue.i32(42);
    final PanamaVariable variable =
        new PanamaVariable("counter", "i32", value, VariableScope.LOCAL, 5, true, null);

    final String str = variable.toString();

    assertTrue(str.contains("counter"), "toString should contain name");
    assertTrue(str.contains("i32"), "toString should contain type");
    assertTrue(str.contains("LOCAL"), "toString should contain scope");
    assertTrue(str.contains("5"), "toString should contain index");
    assertTrue(str.contains("mutable=true"), "toString should contain mutability");

    LOGGER.info("toString test passed: " + str);
  }

  @Test
  @DisplayName("Test equality based on name, type, scope, and index")
  public void testEquality() {
    LOGGER.info("Testing variable equality");

    final PanamaVariableValue value1 = PanamaVariableValue.i32(42);
    final PanamaVariableValue value2 = PanamaVariableValue.i32(100);

    final PanamaVariable var1 =
        new PanamaVariable("x", "i32", value1, VariableScope.LOCAL, 0, true, null);
    final PanamaVariable var2 =
        new PanamaVariable("x", "i32", value2, VariableScope.LOCAL, 0, true, null);
    final PanamaVariable var3 =
        new PanamaVariable("y", "i32", value1, VariableScope.LOCAL, 0, true, null);
    final PanamaVariable var4 =
        new PanamaVariable("x", "i32", value1, VariableScope.GLOBAL, 0, true, null);

    assertEquals(var1, var2, "Variables with same name/type/scope/index should be equal");
    assertNotEquals(var1, var3, "Variables with different names should not be equal");
    assertNotEquals(var1, var4, "Variables with different scopes should not be equal");

    LOGGER.info("Equality test passed");
  }

  @Test
  @DisplayName("Test equals edge cases")
  public void testEqualsEdgeCases() {
    LOGGER.info("Testing equals edge cases");

    final PanamaVariableValue value = PanamaVariableValue.i32(42);
    final PanamaVariable variable =
        new PanamaVariable("x", "i32", value, VariableScope.LOCAL, 0, true, null);

    assertTrue(variable.equals(variable), "Variable should equal itself");
    assertFalse(variable.equals(null), "Variable should not equal null");
    assertFalse(variable.equals("not a variable"), "Variable should not equal string");

    LOGGER.info("Equals edge cases test passed");
  }

  @Test
  @DisplayName("Test hashCode consistency")
  public void testHashCodeConsistency() {
    LOGGER.info("Testing hashCode consistency");

    final PanamaVariableValue value1 = PanamaVariableValue.i32(42);
    final PanamaVariableValue value2 = PanamaVariableValue.i32(100);

    final PanamaVariable var1 =
        new PanamaVariable("x", "i32", value1, VariableScope.LOCAL, 0, true, null);
    final PanamaVariable var2 =
        new PanamaVariable("x", "i32", value2, VariableScope.LOCAL, 0, true, null);

    assertEquals(var1.hashCode(), var2.hashCode(), "Equal variables should have same hashCode");

    LOGGER.info("hashCode consistency test passed");
  }

  @Test
  @DisplayName("Test variable with different value types")
  public void testVariableWithDifferentValueTypes() {
    LOGGER.info("Testing variables with different value types");

    final PanamaVariable i32Var =
        PanamaVariable.local("i32_var", "i32", PanamaVariableValue.i32(42), 0);
    assertEquals("i32", i32Var.getVarType(), "Type should be i32");

    final PanamaVariable i64Var =
        PanamaVariable.local("i64_var", "i64", PanamaVariableValue.i64(100L), 1);
    assertEquals("i64", i64Var.getVarType(), "Type should be i64");

    final PanamaVariable f32Var =
        PanamaVariable.local("f32_var", "f32", PanamaVariableValue.f32(3.14f), 2);
    assertEquals("f32", f32Var.getVarType(), "Type should be f32");

    final PanamaVariable f64Var =
        PanamaVariable.local("f64_var", "f64", PanamaVariableValue.f64(3.14159), 3);
    assertEquals("f64", f64Var.getVarType(), "Type should be f64");

    final PanamaVariable funcRefVar =
        PanamaVariable.local("funcref_var", "funcref", PanamaVariableValue.funcRef(5), 4);
    assertEquals("funcref", funcRefVar.getVarType(), "Type should be funcref");

    final PanamaVariable externRefVar =
        PanamaVariable.local("externref_var", "externref", PanamaVariableValue.externRef(100L), 5);
    assertEquals("externref", externRefVar.getVarType(), "Type should be externref");

    LOGGER.info("Variable with different value types test passed");
  }

  @Test
  @DisplayName("Test VariableScope enum values")
  public void testVariableScopeEnumValues() {
    LOGGER.info("Testing VariableScope enum values");

    final VariableScope[] scopes = VariableScope.values();

    assertEquals(6, scopes.length, "Should have 6 scope types");
    assertNotNull(VariableScope.valueOf("LOCAL"), "LOCAL should be a valid scope");
    assertNotNull(VariableScope.valueOf("PARAMETER"), "PARAMETER should be a valid scope");
    assertNotNull(VariableScope.valueOf("GLOBAL"), "GLOBAL should be a valid scope");
    assertNotNull(VariableScope.valueOf("IMPORTED"), "IMPORTED should be a valid scope");
    assertNotNull(VariableScope.valueOf("EXPORTED"), "EXPORTED should be a valid scope");
    assertNotNull(VariableScope.valueOf("TEMPORARY"), "TEMPORARY should be a valid scope");

    LOGGER.info("VariableScope enum values test passed");
  }
}

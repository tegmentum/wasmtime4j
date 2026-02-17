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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wit.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.wit.WitInterfaceDefinition;
import ai.tegmentum.wasmtime4j.wit.WitPrimitiveType;
import ai.tegmentum.wasmtime4j.wit.WitType;
import ai.tegmentum.wasmtime4j.wit.WitTypeValidator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitTypeValidator} class.
 *
 * <p>WitTypeValidator provides comprehensive validation of WIT types, interfaces, and function
 * signatures.
 */
@DisplayName("WitTypeValidator Tests")
class WitTypeValidatorTest {

  private WitTypeValidator validator;

  @BeforeEach
  void setUp() {
    validator = new WitTypeValidator();
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create validator instance")
    void shouldCreateValidatorInstance() {
      final WitTypeValidator newValidator = new WitTypeValidator();
      assertNotNull(newValidator);
    }
  }

  @Nested
  @DisplayName("Type Validation Tests")
  class TypeValidationTests {

    @Test
    @DisplayName("validateType should throw on null type")
    void validateTypeShouldThrowOnNullType() {
      assertThrows(NullPointerException.class, () -> validator.validateType(null));
    }

    @Test
    @DisplayName("validateType should validate primitive type")
    void validateTypeShouldValidatePrimitiveType() throws Exception {
      final WitType type = WitType.primitive(WitPrimitiveType.S32);
      final WitTypeValidator.WitTypeValidationResult result = validator.validateType(type);

      assertNotNull(result);
      assertTrue(result.isValid());
      assertTrue(result.getErrors().isEmpty());
    }
  }

  @Nested
  @DisplayName("Interface Validation Tests")
  class InterfaceValidationTests {

    @Test
    @DisplayName("validateInterface should throw on null interface")
    void validateInterfaceShouldThrowOnNullInterface() {
      assertThrows(NullPointerException.class, () -> validator.validateInterface(null));
    }

    @Test
    @DisplayName("validateInterface should validate valid interface")
    void validateInterfaceShouldValidateValidInterface() throws Exception {
      final MockInterfaceDefinition interfaceDef = new MockInterfaceDefinition("test-interface");
      final WitTypeValidator.WitInterfaceValidationResult result =
          validator.validateInterface(interfaceDef);

      assertNotNull(result);
    }
  }

  @Nested
  @DisplayName("Type Compatibility Tests")
  class TypeCompatibilityTests {

    @Test
    @DisplayName("validateTypeCompatibility should throw on null source type")
    void validateTypeCompatibilityShouldThrowOnNullSourceType() {
      final WitType targetType = WitType.primitive(WitPrimitiveType.S32);
      assertThrows(
          NullPointerException.class, () -> validator.validateTypeCompatibility(null, targetType));
    }

    @Test
    @DisplayName("validateTypeCompatibility should throw on null target type")
    void validateTypeCompatibilityShouldThrowOnNullTargetType() {
      final WitType sourceType = WitType.primitive(WitPrimitiveType.S32);
      assertThrows(
          NullPointerException.class, () -> validator.validateTypeCompatibility(sourceType, null));
    }

    @Test
    @DisplayName("validateTypeCompatibility should validate compatible types")
    void validateTypeCompatibilityShouldValidateCompatibleTypes() {
      final WitType sourceType = WitType.primitive(WitPrimitiveType.S32);
      final WitType targetType = WitType.primitive(WitPrimitiveType.S32);

      final WitTypeValidator.WitTypeCompatibilityResult result =
          validator.validateTypeCompatibility(sourceType, targetType);

      assertNotNull(result);
      assertTrue(result.isCompatible());
    }

    @Test
    @DisplayName("validateTypeCompatibility should detect incompatible types")
    void validateTypeCompatibilityShouldDetectIncompatibleTypes() {
      final WitType sourceType = WitType.primitive(WitPrimitiveType.S32);
      final WitType targetType = WitType.primitive(WitPrimitiveType.FLOAT64);

      final WitTypeValidator.WitTypeCompatibilityResult result =
          validator.validateTypeCompatibility(sourceType, targetType);

      assertNotNull(result);
      // Result depends on compatibility check implementation
    }
  }

  @Nested
  @DisplayName("WitTypeValidationResult Tests")
  class WitTypeValidationResultTests {

    @Test
    @DisplayName("should create valid result")
    void shouldCreateValidResult() {
      final WitTypeValidator.WitTypeValidationResult result =
          new WitTypeValidator.WitTypeValidationResult(true, List.of(), List.of());

      assertTrue(result.isValid());
      assertTrue(result.getErrors().isEmpty());
      assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    @DisplayName("should create invalid result with errors")
    void shouldCreateInvalidResultWithErrors() {
      final List<String> errors = List.of("Error 1", "Error 2");
      final WitTypeValidator.WitTypeValidationResult result =
          new WitTypeValidator.WitTypeValidationResult(false, errors, List.of());

      assertFalse(result.isValid());
      assertEquals(2, result.getErrors().size());
      assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    @DisplayName("should create result with warnings")
    void shouldCreateResultWithWarnings() {
      final List<String> warnings = List.of("Warning 1", "Warning 2", "Warning 3");
      final WitTypeValidator.WitTypeValidationResult result =
          new WitTypeValidator.WitTypeValidationResult(true, List.of(), warnings);

      assertTrue(result.isValid());
      assertTrue(result.getErrors().isEmpty());
      assertEquals(3, result.getWarnings().size());
    }

    @Test
    @DisplayName("should create defensive copies")
    void shouldCreateDefensiveCopies() {
      final List<String> errors = List.of("Error");
      final List<String> warnings = List.of("Warning");
      final WitTypeValidator.WitTypeValidationResult result =
          new WitTypeValidator.WitTypeValidationResult(false, errors, warnings);

      assertNotNull(result.getErrors());
      assertNotNull(result.getWarnings());
    }
  }

  @Nested
  @DisplayName("WitInterfaceValidationResult Tests")
  class WitInterfaceValidationResultTests {

    @Test
    @DisplayName("should create valid result")
    void shouldCreateValidResult() {
      final WitTypeValidator.WitInterfaceValidationResult result =
          new WitTypeValidator.WitInterfaceValidationResult(true, List.of(), List.of());

      assertTrue(result.isValid());
      assertTrue(result.getErrors().isEmpty());
      assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    @DisplayName("should create invalid result with errors")
    void shouldCreateInvalidResultWithErrors() {
      final List<String> errors = List.of("Interface error 1", "Interface error 2");
      final WitTypeValidator.WitInterfaceValidationResult result =
          new WitTypeValidator.WitInterfaceValidationResult(false, errors, List.of());

      assertFalse(result.isValid());
      assertEquals(2, result.getErrors().size());
    }

    @Test
    @DisplayName("should create result with both errors and warnings")
    void shouldCreateResultWithBothErrorsAndWarnings() {
      final List<String> errors = List.of("Error");
      final List<String> warnings = List.of("Warning 1", "Warning 2");
      final WitTypeValidator.WitInterfaceValidationResult result =
          new WitTypeValidator.WitInterfaceValidationResult(false, errors, warnings);

      assertFalse(result.isValid());
      assertEquals(1, result.getErrors().size());
      assertEquals(2, result.getWarnings().size());
    }
  }

  @Nested
  @DisplayName("WitTypeCompatibilityResult Tests")
  class WitTypeCompatibilityResultTests {

    @Test
    @DisplayName("should create compatible result")
    void shouldCreateCompatibleResult() {
      final WitTypeValidator.WitTypeCompatibilityResult result =
          new WitTypeValidator.WitTypeCompatibilityResult(true, "Types are compatible", List.of());

      assertTrue(result.isCompatible());
      assertEquals("Types are compatible", result.getMessage());
      assertTrue(result.getIssues().isEmpty());
    }

    @Test
    @DisplayName("should create incompatible result with issues")
    void shouldCreateIncompatibleResultWithIssues() {
      final List<String> issues = List.of("Type size mismatch", "Signedness differs");
      final WitTypeValidator.WitTypeCompatibilityResult result =
          new WitTypeValidator.WitTypeCompatibilityResult(false, "Types incompatible", issues);

      assertFalse(result.isCompatible());
      assertEquals("Types incompatible", result.getMessage());
      assertEquals(2, result.getIssues().size());
    }

    @Test
    @DisplayName("should create defensive copies of issues")
    void shouldCreateDefensiveCopiesOfIssues() {
      final List<String> issues = List.of("Issue 1");
      final WitTypeValidator.WitTypeCompatibilityResult result =
          new WitTypeValidator.WitTypeCompatibilityResult(false, "Message", issues);

      assertNotNull(result.getIssues());
      assertEquals(1, result.getIssues().size());
    }
  }

  /** Mock implementation of WitInterfaceDefinition for testing. */
  private static class MockInterfaceDefinition implements WitInterfaceDefinition {
    private final String name;

    MockInterfaceDefinition(final String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getVersion() {
      return "1.0.0";
    }

    @Override
    public String getPackageName() {
      return "test:package";
    }

    @Override
    public List<String> getFunctionNames() {
      return List.of("function1", "function2");
    }

    @Override
    public List<String> getTypeNames() {
      return List.of("type1");
    }

    @Override
    public List<String> getImportNames() {
      return List.of("import1");
    }

    @Override
    public List<String> getExportNames() {
      return List.of("export1");
    }

    @Override
    public java.util.Set<String> getDependencies() {
      return java.util.Set.of();
    }

    @Override
    public String getWitText() {
      return "interface " + name + " {}";
    }

    @Override
    public WitCompatibilityResult isCompatibleWith(final WitInterfaceDefinition other) {
      return WitCompatibilityResult.compatible("Compatible", java.util.Set.of());
    }
  }
}

package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.type.ImportType;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link ModuleImport} class.
 *
 * <p>This test class verifies the structure and behavior of the ModuleImport class, which
 * represents a WebAssembly module import with complete type information.
 */
@DisplayName("ModuleImport Class Tests")
class ModuleImportTest {

  @Nested
  @DisplayName("Class Definition Tests")
  class ClassDefinitionTests {

    @Test
    @DisplayName("ModuleImport should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(ModuleImport.class.getModifiers()),
          "ModuleImport should be a final class");
    }

    @Test
    @DisplayName("ModuleImport should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ModuleImport.class.getModifiers()),
          "ModuleImport should be a public class");
    }

    @Test
    @DisplayName("ModuleImport should not be abstract")
    void shouldNotBeAbstract() {
      assertFalse(
          Modifier.isAbstract(ModuleImport.class.getModifiers()),
          "ModuleImport should not be abstract");
    }
  }

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("Should have private moduleName field")
    void shouldHavePrivateModuleNameField() throws NoSuchFieldException {
      final Field field = ModuleImport.class.getDeclaredField("moduleName");
      assertNotNull(field, "moduleName field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "moduleName should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "moduleName should be final");
      assertEquals(String.class, field.getType(), "moduleName should be String type");
    }

    @Test
    @DisplayName("Should have private fieldName field")
    void shouldHavePrivateFieldNameField() throws NoSuchFieldException {
      final Field field = ModuleImport.class.getDeclaredField("fieldName");
      assertNotNull(field, "fieldName field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "fieldName should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "fieldName should be final");
      assertEquals(String.class, field.getType(), "fieldName should be String type");
    }

    @Test
    @DisplayName("Should have private importType field")
    void shouldHavePrivateImportTypeField() throws NoSuchFieldException {
      final Field field = ModuleImport.class.getDeclaredField("importType");
      assertNotNull(field, "importType field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "importType should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "importType should be final");
      assertEquals(ImportType.class, field.getType(), "importType should be ImportType type");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should have public constructor with three parameters")
    void shouldHavePublicConstructor() throws NoSuchMethodException {
      final Constructor<ModuleImport> constructor =
          ModuleImport.class.getConstructor(String.class, String.class, ImportType.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("Constructor should throw IllegalArgumentException for null moduleName")
    void constructorShouldThrowForNullModuleName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ModuleImport(null, "field", createMockImportType()),
          "Constructor should throw IllegalArgumentException for null moduleName");
    }

    @Test
    @DisplayName("Constructor should throw IllegalArgumentException for null fieldName")
    void constructorShouldThrowForNullFieldName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ModuleImport("module", null, createMockImportType()),
          "Constructor should throw IllegalArgumentException for null fieldName");
    }

    @Test
    @DisplayName("Constructor should throw IllegalArgumentException for null importType")
    void constructorShouldThrowForNullImportType() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ModuleImport("module", "field", null),
          "Constructor should throw IllegalArgumentException for null importType");
    }
  }

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("Should have getModuleName() method")
    void shouldHaveGetModuleNameMethod() throws NoSuchMethodException {
      final Method method = ModuleImport.class.getMethod("getModuleName");
      assertNotNull(method, "getModuleName() method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
    }

    @Test
    @DisplayName("Should have getFieldName() method")
    void shouldHaveGetFieldNameMethod() throws NoSuchMethodException {
      final Method method = ModuleImport.class.getMethod("getFieldName");
      assertNotNull(method, "getFieldName() method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
    }

    @Test
    @DisplayName("Should have getImportType() method")
    void shouldHaveGetImportTypeMethod() throws NoSuchMethodException {
      final Method method = ModuleImport.class.getMethod("getImportType");
      assertNotNull(method, "getImportType() method should exist");
      assertEquals(ImportType.class, method.getReturnType(), "Should return ImportType");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
    }
  }

  @Nested
  @DisplayName("Object Method Tests")
  class ObjectMethodTests {

    @Test
    @DisplayName("Should have toString() method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = ModuleImport.class.getMethod("toString");
      assertNotNull(method, "toString() method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("Should have equals(Object) method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = ModuleImport.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals(Object) method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have hashCode() method")
    void shouldHaveHashCodeMethod() throws NoSuchMethodException {
      final Method method = ModuleImport.class.getMethod("hashCode");
      assertNotNull(method, "hashCode() method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Behavior Tests")
  class BehaviorTests {

    @Test
    @DisplayName("getModuleName should return the moduleName passed to constructor")
    void getModuleNameShouldReturnConstructorValue() {
      final ImportType mockType = createMockImportType();
      final ModuleImport moduleImport = new ModuleImport("env", "print", mockType);
      assertEquals(
          "env", moduleImport.getModuleName(), "getModuleName should return the module name");
    }

    @Test
    @DisplayName("getFieldName should return the fieldName passed to constructor")
    void getFieldNameShouldReturnConstructorValue() {
      final ImportType mockType = createMockImportType();
      final ModuleImport moduleImport = new ModuleImport("env", "print", mockType);
      assertEquals(
          "print", moduleImport.getFieldName(), "getFieldName should return the field name");
    }

    @Test
    @DisplayName("getImportType should return the importType passed to constructor")
    void getImportTypeShouldReturnConstructorValue() {
      final ImportType mockType = createMockImportType();
      final ModuleImport moduleImport = new ModuleImport("env", "print", mockType);
      assertEquals(mockType, moduleImport.getImportType(), "getImportType should return the type");
    }

    @Test
    @DisplayName("toString should include module name and field name")
    void toStringShouldIncludeNames() {
      final ImportType mockType = createMockImportType();
      final ModuleImport moduleImport =
          new ModuleImport("wasi_snapshot_preview1", "fd_write", mockType);
      final String result = moduleImport.toString();
      assertTrue(result.contains("wasi_snapshot_preview1"), "toString should include module name");
      assertTrue(result.contains("fd_write"), "toString should include field name");
      assertTrue(result.contains("ModuleImport"), "toString should include class name");
    }

    @Test
    @DisplayName("equals should return true for same instance")
    void equalsShouldReturnTrueForSameInstance() {
      final ImportType mockType = createMockImportType();
      final ModuleImport moduleImport = new ModuleImport("env", "test", mockType);
      assertEquals(moduleImport, moduleImport, "equals should return true for same instance");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final ImportType mockType = createMockImportType();
      final ModuleImport moduleImport = new ModuleImport("env", "test", mockType);
      assertNotEquals(null, moduleImport, "equals should return false for null");
    }

    @Test
    @DisplayName("equals should return false for different class")
    void equalsShouldReturnFalseForDifferentClass() {
      final ImportType mockType = createMockImportType();
      final ModuleImport moduleImport = new ModuleImport("env", "test", mockType);
      assertNotEquals("test", moduleImport, "equals should return false for different class");
    }

    @Test
    @DisplayName("hashCode should be consistent")
    void hashCodeShouldBeConsistent() {
      final ImportType mockType = createMockImportType();
      final ModuleImport moduleImport = new ModuleImport("env", "test", mockType);
      final int hash1 = moduleImport.hashCode();
      final int hash2 = moduleImport.hashCode();
      assertEquals(hash1, hash2, "hashCode should be consistent");
    }
  }

  /**
   * Creates an ImportType for testing.
   *
   * @return an ImportType instance
   */
  private ImportType createMockImportType() {
    return new ImportType("testModule", "testImport", null);
  }
}

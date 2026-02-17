package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.type.ExportType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link ModuleExport} class.
 *
 * <p>This test class verifies the structure and behavior of the ModuleExport class, which
 * represents a WebAssembly module export with complete type information.
 */
@DisplayName("ModuleExport Class Tests")
@SuppressWarnings("deprecation")
class ModuleExportTest {

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("Should have private name field")
    void shouldHavePrivateNameField() throws NoSuchFieldException {
      final Field field = ModuleExport.class.getDeclaredField("name");
      assertNotNull(field, "name field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "name should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "name should be final");
      assertEquals(String.class, field.getType(), "name should be String type");
    }

    @Test
    @DisplayName("Should have private exportType field")
    void shouldHavePrivateExportTypeField() throws NoSuchFieldException {
      final Field field = ModuleExport.class.getDeclaredField("exportType");
      assertNotNull(field, "exportType field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "exportType should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "exportType should be final");
      assertEquals(ExportType.class, field.getType(), "exportType should be ExportType type");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should have public constructor with name and exportType parameters")
    void shouldHavePublicConstructor() throws NoSuchMethodException {
      final Constructor<ModuleExport> constructor =
          ModuleExport.class.getConstructor(String.class, ExportType.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("Constructor should throw IllegalArgumentException for null name")
    void constructorShouldThrowForNullName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ModuleExport(null, createMockExportType()),
          "Constructor should throw IllegalArgumentException for null name");
    }

    @Test
    @DisplayName("Constructor should throw IllegalArgumentException for null exportType")
    void constructorShouldThrowForNullExportType() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ModuleExport("test", null),
          "Constructor should throw IllegalArgumentException for null exportType");
    }
  }

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("Should have getName() method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = ModuleExport.class.getMethod("getName");
      assertNotNull(method, "getName() method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
    }

    @Test
    @DisplayName("Should have getExportType() method")
    void shouldHaveGetExportTypeMethod() throws NoSuchMethodException {
      final Method method = ModuleExport.class.getMethod("getExportType");
      assertNotNull(method, "getExportType() method should exist");
      assertEquals(ExportType.class, method.getReturnType(), "Should return ExportType");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
    }
  }

  @Nested
  @DisplayName("Object Method Tests")
  class ObjectMethodTests {

    @Test
    @DisplayName("Should have toString() method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = ModuleExport.class.getMethod("toString");
      assertNotNull(method, "toString() method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("Should have equals(Object) method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = ModuleExport.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals(Object) method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have hashCode() method")
    void shouldHaveHashCodeMethod() throws NoSuchMethodException {
      final Method method = ModuleExport.class.getMethod("hashCode");
      assertNotNull(method, "hashCode() method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Behavior Tests")
  class BehaviorTests {

    @Test
    @DisplayName("getName should return the name passed to constructor")
    void getNameShouldReturnConstructorValue() {
      final ExportType mockType = createMockExportType();
      final ModuleExport export = new ModuleExport("testExport", mockType);
      assertEquals("testExport", export.getName(), "getName should return the name");
    }

    @Test
    @DisplayName("getExportType should return the exportType passed to constructor")
    void getExportTypeShouldReturnConstructorValue() {
      final ExportType mockType = createMockExportType();
      final ModuleExport export = new ModuleExport("testExport", mockType);
      assertEquals(mockType, export.getExportType(), "getExportType should return the type");
    }

    @Test
    @DisplayName("toString should include name")
    void toStringShouldIncludeName() {
      final ExportType mockType = createMockExportType();
      final ModuleExport export = new ModuleExport("myFunction", mockType);
      final String result = export.toString();
      assertTrue(result.contains("myFunction"), "toString should include the name");
      assertTrue(result.contains("ModuleExport"), "toString should include class name");
    }

    @Test
    @DisplayName("equals should return true for same instance")
    void equalsShouldReturnTrueForSameInstance() {
      final ExportType mockType = createMockExportType();
      final ModuleExport export = new ModuleExport("test", mockType);
      assertEquals(export, export, "equals should return true for same instance");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final ExportType mockType = createMockExportType();
      final ModuleExport export = new ModuleExport("test", mockType);
      assertNotEquals(null, export, "equals should return false for null");
    }

    @Test
    @DisplayName("equals should return false for different class")
    void equalsShouldReturnFalseForDifferentClass() {
      final ExportType mockType = createMockExportType();
      final ModuleExport export = new ModuleExport("test", mockType);
      assertNotEquals("test", export, "equals should return false for different class");
    }

    @Test
    @DisplayName("hashCode should be consistent")
    void hashCodeShouldBeConsistent() {
      final ExportType mockType = createMockExportType();
      final ModuleExport export = new ModuleExport("test", mockType);
      final int hash1 = export.hashCode();
      final int hash2 = export.hashCode();
      assertEquals(hash1, hash2, "hashCode should be consistent");
    }
  }

  /**
   * Creates an ExportType for testing.
   *
   * @return an ExportType instance
   */
  private ExportType createMockExportType() {
    return new ExportType("testExport", null);
  }
}

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link RuntimeType} enum.
 *
 * <p>This test class verifies the structure and values of the RuntimeType enum, which identifies
 * the underlying WebAssembly runtime implementation.
 */
@DisplayName("RuntimeType Enum Tests")
class RuntimeTypeTest {

  @Nested
  @DisplayName("Enum Definition Tests")
  class EnumDefinitionTests {

    @Test
    @DisplayName("RuntimeType should be an enum")
    void shouldBeAnEnum() {
      assertTrue(RuntimeType.class.isEnum(), "RuntimeType should be an enum");
    }

    @Test
    @DisplayName("RuntimeType should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(RuntimeType.class.getModifiers()),
          "RuntimeType should be a public enum");
    }

    @Test
    @DisplayName("RuntimeType should be final (enums are implicitly final)")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(RuntimeType.class.getModifiers()),
          "RuntimeType should be final (enums are implicitly final)");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("RuntimeType should have JNI value")
    void shouldHaveJniValue() {
      assertNotNull(RuntimeType.valueOf("JNI"), "RuntimeType should have JNI value");
      assertEquals(RuntimeType.JNI, RuntimeType.valueOf("JNI"), "JNI value should be accessible");
    }

    @Test
    @DisplayName("RuntimeType should have PANAMA value")
    void shouldHavePanamaValue() {
      assertNotNull(RuntimeType.valueOf("PANAMA"), "RuntimeType should have PANAMA value");
      assertEquals(
          RuntimeType.PANAMA, RuntimeType.valueOf("PANAMA"), "PANAMA value should be accessible");
    }

    @Test
    @DisplayName("RuntimeType should have exactly 2 values")
    void shouldHaveExactlyTwoValues() {
      assertEquals(2, RuntimeType.values().length, "RuntimeType should have exactly 2 values");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("JNI should have ordinal 0")
    void jniShouldHaveOrdinalZero() {
      assertEquals(0, RuntimeType.JNI.ordinal(), "JNI should have ordinal 0");
    }

    @Test
    @DisplayName("PANAMA should have ordinal 1")
    void panamaShouldHaveOrdinalOne() {
      assertEquals(1, RuntimeType.PANAMA.ordinal(), "PANAMA should have ordinal 1");
    }
  }

  @Nested
  @DisplayName("Enum Name Tests")
  class EnumNameTests {

    @Test
    @DisplayName("JNI name should be 'JNI'")
    void jniNameShouldBeCorrect() {
      assertEquals("JNI", RuntimeType.JNI.name(), "JNI name should be 'JNI'");
    }

    @Test
    @DisplayName("PANAMA name should be 'PANAMA'")
    void panamaNameShouldBeCorrect() {
      assertEquals("PANAMA", RuntimeType.PANAMA.name(), "PANAMA name should be 'PANAMA'");
    }
  }

  @Nested
  @DisplayName("Values Array Tests")
  class ValuesArrayTests {

    @Test
    @DisplayName("values() should return array containing JNI and PANAMA")
    void valuesShouldReturnCorrectArray() {
      final RuntimeType[] values = RuntimeType.values();
      assertEquals(RuntimeType.JNI, values[0], "First value should be JNI");
      assertEquals(RuntimeType.PANAMA, values[1], "Second value should be PANAMA");
    }

    @Test
    @DisplayName("values() should return a new array each time")
    void valuesShouldReturnNewArray() {
      final RuntimeType[] values1 = RuntimeType.values();
      final RuntimeType[] values2 = RuntimeType.values();
      assertTrue(values1 != values2, "values() should return a new array each time");
    }
  }
}

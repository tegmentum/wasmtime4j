package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.ComponentVal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link ComponentVal} interface.
 *
 * <p>Tests verify:
 *
 * <ul>
 *   <li>Interface structure and method signatures
 *   <li>Type checking methods (isBool, isS32, isString, etc.)
 *   <li>Value extraction methods (asBool, asS32, asString, etc.)
 *   <li>Static factory methods (bool, s32, string, list, record, etc.)
 * </ul>
 *
 * <p>Note: These tests focus on the interface contract and expected behavior.
 * Implementation-specific tests are in the JNI/Panama modules.
 */
@DisplayName("ComponentVal Tests")
class ComponentValTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("ComponentVal should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ComponentVal.class.isInterface(), "ComponentVal should be an interface");
    }
  }

  @Nested
  @DisplayName("Type Checking Method Tests")
  class TypeCheckingMethodTests {

    @Test
    @DisplayName("should have isBool method")
    void shouldHaveIsBoolMethod() throws NoSuchMethodException {
      assertNotNull(ComponentVal.class.getMethod("isBool"), "ComponentVal should have isBool()");
    }

    @Test
    @DisplayName("should have isS8 method")
    void shouldHaveIsS8Method() throws NoSuchMethodException {
      assertNotNull(ComponentVal.class.getMethod("isS8"), "ComponentVal should have isS8()");
    }

    @Test
    @DisplayName("should have isS16 method")
    void shouldHaveIsS16Method() throws NoSuchMethodException {
      assertNotNull(ComponentVal.class.getMethod("isS16"), "ComponentVal should have isS16()");
    }

    @Test
    @DisplayName("should have isS32 method")
    void shouldHaveIsS32Method() throws NoSuchMethodException {
      assertNotNull(ComponentVal.class.getMethod("isS32"), "ComponentVal should have isS32()");
    }

    @Test
    @DisplayName("should have isS64 method")
    void shouldHaveIsS64Method() throws NoSuchMethodException {
      assertNotNull(ComponentVal.class.getMethod("isS64"), "ComponentVal should have isS64()");
    }

    @Test
    @DisplayName("should have isU8 method")
    void shouldHaveIsU8Method() throws NoSuchMethodException {
      assertNotNull(ComponentVal.class.getMethod("isU8"), "ComponentVal should have isU8()");
    }

    @Test
    @DisplayName("should have isU16 method")
    void shouldHaveIsU16Method() throws NoSuchMethodException {
      assertNotNull(ComponentVal.class.getMethod("isU16"), "ComponentVal should have isU16()");
    }

    @Test
    @DisplayName("should have isU32 method")
    void shouldHaveIsU32Method() throws NoSuchMethodException {
      assertNotNull(ComponentVal.class.getMethod("isU32"), "ComponentVal should have isU32()");
    }

    @Test
    @DisplayName("should have isU64 method")
    void shouldHaveIsU64Method() throws NoSuchMethodException {
      assertNotNull(ComponentVal.class.getMethod("isU64"), "ComponentVal should have isU64()");
    }

    @Test
    @DisplayName("should have isF32 method")
    void shouldHaveIsF32Method() throws NoSuchMethodException {
      assertNotNull(ComponentVal.class.getMethod("isF32"), "ComponentVal should have isF32()");
    }

    @Test
    @DisplayName("should have isF64 method")
    void shouldHaveIsF64Method() throws NoSuchMethodException {
      assertNotNull(ComponentVal.class.getMethod("isF64"), "ComponentVal should have isF64()");
    }

    @Test
    @DisplayName("should have isChar method")
    void shouldHaveIsCharMethod() throws NoSuchMethodException {
      assertNotNull(ComponentVal.class.getMethod("isChar"), "ComponentVal should have isChar()");
    }

    @Test
    @DisplayName("should have isString method")
    void shouldHaveIsStringMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentVal.class.getMethod("isString"), "ComponentVal should have isString()");
    }

    @Test
    @DisplayName("should have isList method")
    void shouldHaveIsListMethod() throws NoSuchMethodException {
      assertNotNull(ComponentVal.class.getMethod("isList"), "ComponentVal should have isList()");
    }

    @Test
    @DisplayName("should have isRecord method")
    void shouldHaveIsRecordMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentVal.class.getMethod("isRecord"), "ComponentVal should have isRecord()");
    }

    @Test
    @DisplayName("should have isTuple method")
    void shouldHaveIsTupleMethod() throws NoSuchMethodException {
      assertNotNull(ComponentVal.class.getMethod("isTuple"), "ComponentVal should have isTuple()");
    }

    @Test
    @DisplayName("should have isVariant method")
    void shouldHaveIsVariantMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentVal.class.getMethod("isVariant"), "ComponentVal should have isVariant()");
    }

    @Test
    @DisplayName("should have isEnum method")
    void shouldHaveIsEnumMethod() throws NoSuchMethodException {
      assertNotNull(ComponentVal.class.getMethod("isEnum"), "ComponentVal should have isEnum()");
    }

    @Test
    @DisplayName("should have isOption method")
    void shouldHaveIsOptionMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentVal.class.getMethod("isOption"), "ComponentVal should have isOption()");
    }

    @Test
    @DisplayName("should have isResult method")
    void shouldHaveIsResultMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentVal.class.getMethod("isResult"), "ComponentVal should have isResult()");
    }

    @Test
    @DisplayName("should have isFlags method")
    void shouldHaveIsFlagsMethod() throws NoSuchMethodException {
      assertNotNull(ComponentVal.class.getMethod("isFlags"), "ComponentVal should have isFlags()");
    }

    @Test
    @DisplayName("should have isResource method")
    void shouldHaveIsResourceMethod() throws NoSuchMethodException {
      assertNotNull(
          ComponentVal.class.getMethod("isResource"), "ComponentVal should have isResource()");
    }

    @Test
    @DisplayName("all type check methods should return boolean")
    void allTypeCheckMethodsShouldReturnBoolean() throws NoSuchMethodException {
      String[] typeCheckMethods = {
        "isBool",
        "isS8",
        "isS16",
        "isS32",
        "isS64",
        "isU8",
        "isU16",
        "isU32",
        "isU64",
        "isF32",
        "isF64",
        "isChar",
        "isString",
        "isList",
        "isRecord",
        "isTuple",
        "isVariant",
        "isEnum",
        "isOption",
        "isResult",
        "isFlags",
        "isResource"
      };
      for (String methodName : typeCheckMethods) {
        Class<?> returnType = ComponentVal.class.getMethod(methodName).getReturnType();
        assertEquals(boolean.class, returnType, methodName + " should return boolean");
      }
    }
  }

  @Nested
  @DisplayName("Value Extraction Method Tests")
  class ValueExtractionMethodTests {

    @Test
    @DisplayName("should have asBool method")
    void shouldHaveAsBoolMethod() throws NoSuchMethodException {
      assertNotNull(ComponentVal.class.getMethod("asBool"), "ComponentVal should have asBool()");
    }

    @Test
    @DisplayName("asBool should return boolean")
    void asBoolShouldReturnBoolean() throws NoSuchMethodException {
      Class<?> returnType = ComponentVal.class.getMethod("asBool").getReturnType();
      assertEquals(boolean.class, returnType, "asBool should return boolean");
    }

    @Test
    @DisplayName("should have asS8 method returning byte")
    void shouldHaveAsS8Method() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("asS8");
      assertNotNull(method, "ComponentVal should have asS8()");
      assertEquals(byte.class, method.getReturnType(), "asS8 should return byte");
    }

    @Test
    @DisplayName("should have asS16 method returning short")
    void shouldHaveAsS16Method() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("asS16");
      assertNotNull(method, "ComponentVal should have asS16()");
      assertEquals(short.class, method.getReturnType(), "asS16 should return short");
    }

    @Test
    @DisplayName("should have asS32 method returning int")
    void shouldHaveAsS32Method() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("asS32");
      assertNotNull(method, "ComponentVal should have asS32()");
      assertEquals(int.class, method.getReturnType(), "asS32 should return int");
    }

    @Test
    @DisplayName("should have asS64 method returning long")
    void shouldHaveAsS64Method() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("asS64");
      assertNotNull(method, "ComponentVal should have asS64()");
      assertEquals(long.class, method.getReturnType(), "asS64 should return long");
    }

    @Test
    @DisplayName("should have asU8 method returning short")
    void shouldHaveAsU8Method() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("asU8");
      assertNotNull(method, "ComponentVal should have asU8()");
      assertEquals(
          short.class, method.getReturnType(), "asU8 should return short (unsigned 8-bit range)");
    }

    @Test
    @DisplayName("should have asU16 method returning int")
    void shouldHaveAsU16Method() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("asU16");
      assertNotNull(method, "ComponentVal should have asU16()");
      assertEquals(
          int.class, method.getReturnType(), "asU16 should return int (unsigned 16-bit range)");
    }

    @Test
    @DisplayName("should have asU32 method returning long")
    void shouldHaveAsU32Method() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("asU32");
      assertNotNull(method, "ComponentVal should have asU32()");
      assertEquals(
          long.class, method.getReturnType(), "asU32 should return long (unsigned 32-bit range)");
    }

    @Test
    @DisplayName("should have asU64 method returning long")
    void shouldHaveAsU64Method() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("asU64");
      assertNotNull(method, "ComponentVal should have asU64()");
      assertEquals(long.class, method.getReturnType(), "asU64 should return long");
    }

    @Test
    @DisplayName("should have asF32 method returning float")
    void shouldHaveAsF32Method() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("asF32");
      assertNotNull(method, "ComponentVal should have asF32()");
      assertEquals(float.class, method.getReturnType(), "asF32 should return float");
    }

    @Test
    @DisplayName("should have asF64 method returning double")
    void shouldHaveAsF64Method() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("asF64");
      assertNotNull(method, "ComponentVal should have asF64()");
      assertEquals(double.class, method.getReturnType(), "asF64 should return double");
    }

    @Test
    @DisplayName("should have asChar method returning char")
    void shouldHaveAsCharMethod() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("asChar");
      assertNotNull(method, "ComponentVal should have asChar()");
      assertEquals(char.class, method.getReturnType(), "asChar should return char");
    }

    @Test
    @DisplayName("should have asString method returning String")
    void shouldHaveAsStringMethod() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("asString");
      assertNotNull(method, "ComponentVal should have asString()");
      assertEquals(String.class, method.getReturnType(), "asString should return String");
    }

    @Test
    @DisplayName("should have asList method returning List")
    void shouldHaveAsListMethod() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("asList");
      assertNotNull(method, "ComponentVal should have asList()");
      assertEquals(List.class, method.getReturnType(), "asList should return List");
    }

    @Test
    @DisplayName("should have asRecord method returning Map")
    void shouldHaveAsRecordMethod() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("asRecord");
      assertNotNull(method, "ComponentVal should have asRecord()");
      assertEquals(Map.class, method.getReturnType(), "asRecord should return Map");
    }

    @Test
    @DisplayName("should have asTuple method returning List")
    void shouldHaveAsTupleMethod() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("asTuple");
      assertNotNull(method, "ComponentVal should have asTuple()");
      assertEquals(List.class, method.getReturnType(), "asTuple should return List");
    }

    @Test
    @DisplayName("should have asVariant method returning ComponentVariant")
    void shouldHaveAsVariantMethod() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("asVariant");
      assertNotNull(method, "ComponentVal should have asVariant()");
      assertEquals(
          "ComponentVariant",
          method.getReturnType().getSimpleName(),
          "asVariant should return ComponentVariant");
    }

    @Test
    @DisplayName("should have asEnum method returning String")
    void shouldHaveAsEnumMethod() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("asEnum");
      assertNotNull(method, "ComponentVal should have asEnum()");
      assertEquals(String.class, method.getReturnType(), "asEnum should return String");
    }

    @Test
    @DisplayName("should have asSome method returning Optional")
    void shouldHaveAsSomeMethod() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("asSome");
      assertNotNull(method, "ComponentVal should have asSome()");
      assertEquals(Optional.class, method.getReturnType(), "asSome should return Optional");
    }

    @Test
    @DisplayName("should have asResult method returning ComponentResult")
    void shouldHaveAsResultMethod() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("asResult");
      assertNotNull(method, "ComponentVal should have asResult()");
      assertEquals(
          "ComponentResult",
          method.getReturnType().getSimpleName(),
          "asResult should return ComponentResult");
    }

    @Test
    @DisplayName("should have asFlags method returning Set")
    void shouldHaveAsFlagsMethod() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("asFlags");
      assertNotNull(method, "ComponentVal should have asFlags()");
      assertEquals(Set.class, method.getReturnType(), "asFlags should return Set");
    }

    @Test
    @DisplayName("should have asResource method returning ComponentResourceHandle")
    void shouldHaveAsResourceMethod() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("asResource");
      assertNotNull(method, "ComponentVal should have asResource()");
      assertEquals(
          "ComponentResourceHandle",
          method.getReturnType().getSimpleName(),
          "asResource should return ComponentResourceHandle");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have static bool factory method")
    void shouldHaveStaticBoolMethod() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("bool", boolean.class);
      assertNotNull(method, "ComponentVal should have static bool(boolean)");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "bool method should be static");
      assertEquals(ComponentVal.class, method.getReturnType(), "bool should return ComponentVal");
    }

    @Test
    @DisplayName("should have static s8 factory method")
    void shouldHaveStaticS8Method() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("s8", byte.class);
      assertNotNull(method, "ComponentVal should have static s8(byte)");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()), "s8 method should be static");
    }

    @Test
    @DisplayName("should have static s16 factory method")
    void shouldHaveStaticS16Method() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("s16", short.class);
      assertNotNull(method, "ComponentVal should have static s16(short)");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "s16 method should be static");
    }

    @Test
    @DisplayName("should have static s32 factory method")
    void shouldHaveStaticS32Method() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("s32", int.class);
      assertNotNull(method, "ComponentVal should have static s32(int)");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "s32 method should be static");
    }

    @Test
    @DisplayName("should have static s64 factory method")
    void shouldHaveStaticS64Method() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("s64", long.class);
      assertNotNull(method, "ComponentVal should have static s64(long)");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "s64 method should be static");
    }

    @Test
    @DisplayName("should have static u8 factory method")
    void shouldHaveStaticU8Method() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("u8", short.class);
      assertNotNull(method, "ComponentVal should have static u8(short)");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()), "u8 method should be static");
    }

    @Test
    @DisplayName("should have static u16 factory method")
    void shouldHaveStaticU16Method() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("u16", int.class);
      assertNotNull(method, "ComponentVal should have static u16(int)");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "u16 method should be static");
    }

    @Test
    @DisplayName("should have static u32 factory method")
    void shouldHaveStaticU32Method() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("u32", long.class);
      assertNotNull(method, "ComponentVal should have static u32(long)");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "u32 method should be static");
    }

    @Test
    @DisplayName("should have static u64 factory method")
    void shouldHaveStaticU64Method() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("u64", long.class);
      assertNotNull(method, "ComponentVal should have static u64(long)");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "u64 method should be static");
    }

    @Test
    @DisplayName("should have static f32 factory method")
    void shouldHaveStaticF32Method() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("f32", float.class);
      assertNotNull(method, "ComponentVal should have static f32(float)");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "f32 method should be static");
    }

    @Test
    @DisplayName("should have static f64 factory method")
    void shouldHaveStaticF64Method() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("f64", double.class);
      assertNotNull(method, "ComponentVal should have static f64(double)");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "f64 method should be static");
    }

    @Test
    @DisplayName("should have static char_ factory method")
    void shouldHaveStaticCharMethod() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("char_", char.class);
      assertNotNull(method, "ComponentVal should have static char_(char)");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "char_ method should be static");
    }

    @Test
    @DisplayName("should have static string factory method")
    void shouldHaveStaticStringMethod() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("string", String.class);
      assertNotNull(method, "ComponentVal should have static string(String)");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "string method should be static");
    }

    @Test
    @DisplayName("should have static list factory method with varargs")
    void shouldHaveStaticListMethodVarargs() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("list", ComponentVal[].class);
      assertNotNull(method, "ComponentVal should have static list(ComponentVal...)");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "list method should be static");
      assertTrue(method.isVarArgs(), "list method should be varargs");
    }

    @Test
    @DisplayName("should have static list factory method with List")
    void shouldHaveStaticListMethodWithList() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("list", List.class);
      assertNotNull(method, "ComponentVal should have static list(List)");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "list method should be static");
    }

    @Test
    @DisplayName("should have static record factory method")
    void shouldHaveStaticRecordMethod() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("record", Map.class);
      assertNotNull(method, "ComponentVal should have static record(Map)");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "record method should be static");
    }

    @Test
    @DisplayName("should have static tuple factory method")
    void shouldHaveStaticTupleMethod() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("tuple", ComponentVal[].class);
      assertNotNull(method, "ComponentVal should have static tuple(ComponentVal...)");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "tuple method should be static");
    }

    @Test
    @DisplayName("should have static variant factory method with payload")
    void shouldHaveStaticVariantMethodWithPayload() throws NoSuchMethodException {
      java.lang.reflect.Method method =
          ComponentVal.class.getMethod("variant", String.class, ComponentVal.class);
      assertNotNull(method, "ComponentVal should have static variant(String, ComponentVal)");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "variant method should be static");
    }

    @Test
    @DisplayName("should have static variant factory method without payload")
    void shouldHaveStaticVariantMethodWithoutPayload() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("variant", String.class);
      assertNotNull(method, "ComponentVal should have static variant(String)");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "variant method should be static");
    }

    @Test
    @DisplayName("should have static enum_ factory method")
    void shouldHaveStaticEnumMethod() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("enum_", String.class);
      assertNotNull(method, "ComponentVal should have static enum_(String)");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "enum_ method should be static");
    }

    @Test
    @DisplayName("should have static some factory method")
    void shouldHaveStaticSomeMethod() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("some", ComponentVal.class);
      assertNotNull(method, "ComponentVal should have static some(ComponentVal)");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "some method should be static");
    }

    @Test
    @DisplayName("should have static none factory method")
    void shouldHaveStaticNoneMethod() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("none");
      assertNotNull(method, "ComponentVal should have static none()");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "none method should be static");
    }

    @Test
    @DisplayName("should have static ok factory method with value")
    void shouldHaveStaticOkMethodWithValue() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("ok", ComponentVal.class);
      assertNotNull(method, "ComponentVal should have static ok(ComponentVal)");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()), "ok method should be static");
    }

    @Test
    @DisplayName("should have static ok factory method without value")
    void shouldHaveStaticOkMethodWithoutValue() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("ok");
      assertNotNull(method, "ComponentVal should have static ok()");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()), "ok method should be static");
    }

    @Test
    @DisplayName("should have static err factory method with error")
    void shouldHaveStaticErrMethodWithError() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("err", ComponentVal.class);
      assertNotNull(method, "ComponentVal should have static err(ComponentVal)");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "err method should be static");
    }

    @Test
    @DisplayName("should have static err factory method without error")
    void shouldHaveStaticErrMethodWithoutError() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("err");
      assertNotNull(method, "ComponentVal should have static err()");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "err method should be static");
    }

    @Test
    @DisplayName("should have static flags factory method with Set")
    void shouldHaveStaticFlagsMethodWithSet() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("flags", Set.class);
      assertNotNull(method, "ComponentVal should have static flags(Set)");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "flags method should be static");
    }

    @Test
    @DisplayName("should have static flags factory method with varargs")
    void shouldHaveStaticFlagsMethodWithVarargs() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("flags", String[].class);
      assertNotNull(method, "ComponentVal should have static flags(String...)");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "flags method should be static");
      assertTrue(method.isVarArgs(), "flags method should be varargs");
    }
  }

  @Nested
  @DisplayName("Related Type Tests")
  class RelatedTypeTests {

    @Test
    @DisplayName("ComponentType should exist")
    void componentTypeShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.ComponentType");
        assertNotNull(clazz, "ComponentType class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("ComponentType class should exist", e);
      }
    }

    @Test
    @DisplayName("ComponentVariant should exist")
    void componentVariantShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.ComponentVariant");
        assertNotNull(clazz, "ComponentVariant class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("ComponentVariant class should exist", e);
      }
    }

    @Test
    @DisplayName("ComponentResult should exist")
    void componentResultShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.ComponentResult");
        assertNotNull(clazz, "ComponentResult class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("ComponentResult class should exist", e);
      }
    }

    @Test
    @DisplayName("ComponentResourceHandle should exist")
    void componentResourceHandleShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.ComponentResourceHandle");
        assertNotNull(clazz, "ComponentResourceHandle class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("ComponentResourceHandle class should exist", e);
      }
    }

    @Test
    @DisplayName("ComponentValFactory should exist")
    void componentValFactoryShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.ComponentValFactory");
        assertNotNull(clazz, "ComponentValFactory class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("ComponentValFactory class should exist", e);
      }
    }
  }

  @Nested
  @DisplayName("Interface Contract Tests")
  class InterfaceContractTests {

    @Test
    @DisplayName("getType method should exist and return ComponentType")
    void getTypeShouldExist() throws NoSuchMethodException {
      java.lang.reflect.Method method = ComponentVal.class.getMethod("getType");
      assertNotNull(method, "ComponentVal should have getType()");
      assertEquals(
          "ComponentType",
          method.getReturnType().getSimpleName(),
          "getType should return ComponentType");
    }

    @Test
    @DisplayName("all instance methods should be public")
    void allMethodsShouldBePublic() {
      for (java.lang.reflect.Method method : ComponentVal.class.getDeclaredMethods()) {
        // Skip synthetic methods added by instrumentation (e.g., Jacoco's $jacocoInit)
        if (method.isSynthetic()) {
          continue;
        }
        assertTrue(
            java.lang.reflect.Modifier.isPublic(method.getModifiers()),
            "Method " + method.getName() + " should be public");
      }
    }
  }
}

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentVal} interface.
 *
 * <p>ComponentVal represents a Component Model value that can be passed to and from WebAssembly
 * components.
 */
@DisplayName("ComponentVal Tests")
class ComponentValTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentVal.class.getModifiers()), "ComponentVal should be public");
      assertTrue(ComponentVal.class.isInterface(), "ComponentVal should be an interface");
    }
  }

  @Nested
  @DisplayName("Type Method Tests")
  class TypeMethodTests {

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(ComponentType.class, method.getReturnType(), "Should return ComponentType");
    }
  }

  @Nested
  @DisplayName("Type Checking Method Tests")
  class TypeCheckingMethodTests {

    @Test
    @DisplayName("should have isBool method")
    void shouldHaveIsBoolMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("isBool");
      assertNotNull(method, "isBool method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isS8 method")
    void shouldHaveIsS8Method() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("isS8");
      assertNotNull(method, "isS8 method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isS16 method")
    void shouldHaveIsS16Method() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("isS16");
      assertNotNull(method, "isS16 method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isS32 method")
    void shouldHaveIsS32Method() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("isS32");
      assertNotNull(method, "isS32 method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isS64 method")
    void shouldHaveIsS64Method() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("isS64");
      assertNotNull(method, "isS64 method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isU8 method")
    void shouldHaveIsU8Method() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("isU8");
      assertNotNull(method, "isU8 method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isU16 method")
    void shouldHaveIsU16Method() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("isU16");
      assertNotNull(method, "isU16 method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isU32 method")
    void shouldHaveIsU32Method() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("isU32");
      assertNotNull(method, "isU32 method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isU64 method")
    void shouldHaveIsU64Method() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("isU64");
      assertNotNull(method, "isU64 method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isF32 method")
    void shouldHaveIsF32Method() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("isF32");
      assertNotNull(method, "isF32 method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isF64 method")
    void shouldHaveIsF64Method() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("isF64");
      assertNotNull(method, "isF64 method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isChar method")
    void shouldHaveIsCharMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("isChar");
      assertNotNull(method, "isChar method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isString method")
    void shouldHaveIsStringMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("isString");
      assertNotNull(method, "isString method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isList method")
    void shouldHaveIsListMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("isList");
      assertNotNull(method, "isList method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isRecord method")
    void shouldHaveIsRecordMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("isRecord");
      assertNotNull(method, "isRecord method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isTuple method")
    void shouldHaveIsTupleMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("isTuple");
      assertNotNull(method, "isTuple method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isVariant method")
    void shouldHaveIsVariantMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("isVariant");
      assertNotNull(method, "isVariant method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isEnum method")
    void shouldHaveIsEnumMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("isEnum");
      assertNotNull(method, "isEnum method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isOption method")
    void shouldHaveIsOptionMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("isOption");
      assertNotNull(method, "isOption method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isResult method")
    void shouldHaveIsResultMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("isResult");
      assertNotNull(method, "isResult method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isFlags method")
    void shouldHaveIsFlagsMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("isFlags");
      assertNotNull(method, "isFlags method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isResource method")
    void shouldHaveIsResourceMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("isResource");
      assertNotNull(method, "isResource method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Value Extraction Method Tests")
  class ValueExtractionMethodTests {

    @Test
    @DisplayName("should have asBool method")
    void shouldHaveAsBoolMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("asBool");
      assertNotNull(method, "asBool method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have asS8 method")
    void shouldHaveAsS8Method() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("asS8");
      assertNotNull(method, "asS8 method should exist");
      assertEquals(byte.class, method.getReturnType(), "Should return byte");
    }

    @Test
    @DisplayName("should have asS16 method")
    void shouldHaveAsS16Method() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("asS16");
      assertNotNull(method, "asS16 method should exist");
      assertEquals(short.class, method.getReturnType(), "Should return short");
    }

    @Test
    @DisplayName("should have asS32 method")
    void shouldHaveAsS32Method() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("asS32");
      assertNotNull(method, "asS32 method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have asS64 method")
    void shouldHaveAsS64Method() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("asS64");
      assertNotNull(method, "asS64 method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have asU8 method")
    void shouldHaveAsU8Method() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("asU8");
      assertNotNull(method, "asU8 method should exist");
      assertEquals(short.class, method.getReturnType(), "Should return short (unsigned byte)");
    }

    @Test
    @DisplayName("should have asU16 method")
    void shouldHaveAsU16Method() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("asU16");
      assertNotNull(method, "asU16 method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int (unsigned short)");
    }

    @Test
    @DisplayName("should have asU32 method")
    void shouldHaveAsU32Method() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("asU32");
      assertNotNull(method, "asU32 method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long (unsigned int)");
    }

    @Test
    @DisplayName("should have asU64 method")
    void shouldHaveAsU64Method() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("asU64");
      assertNotNull(method, "asU64 method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have asF32 method")
    void shouldHaveAsF32Method() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("asF32");
      assertNotNull(method, "asF32 method should exist");
      assertEquals(float.class, method.getReturnType(), "Should return float");
    }

    @Test
    @DisplayName("should have asF64 method")
    void shouldHaveAsF64Method() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("asF64");
      assertNotNull(method, "asF64 method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }

    @Test
    @DisplayName("should have asChar method")
    void shouldHaveAsCharMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("asChar");
      assertNotNull(method, "asChar method should exist");
      assertEquals(char.class, method.getReturnType(), "Should return char");
    }

    @Test
    @DisplayName("should have asString method")
    void shouldHaveAsStringMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("asString");
      assertNotNull(method, "asString method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have asList method")
    void shouldHaveAsListMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("asList");
      assertNotNull(method, "asList method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have asRecord method")
    void shouldHaveAsRecordMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("asRecord");
      assertNotNull(method, "asRecord method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have asTuple method")
    void shouldHaveAsTupleMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("asTuple");
      assertNotNull(method, "asTuple method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have asVariant method")
    void shouldHaveAsVariantMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("asVariant");
      assertNotNull(method, "asVariant method should exist");
      assertEquals(
          ComponentVariant.class, method.getReturnType(), "Should return ComponentVariant");
    }

    @Test
    @DisplayName("should have asEnum method")
    void shouldHaveAsEnumMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("asEnum");
      assertNotNull(method, "asEnum method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have asSome method")
    void shouldHaveAsSomeMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("asSome");
      assertNotNull(method, "asSome method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have asResult method")
    void shouldHaveAsResultMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("asResult");
      assertNotNull(method, "asResult method should exist");
      assertEquals(ComponentResult.class, method.getReturnType(), "Should return ComponentResult");
    }

    @Test
    @DisplayName("should have asFlags method")
    void shouldHaveAsFlagsMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("asFlags");
      assertNotNull(method, "asFlags method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have asResource method")
    void shouldHaveAsResourceMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("asResource");
      assertNotNull(method, "asResource method should exist");
      assertEquals(
          ComponentResourceHandle.class,
          method.getReturnType(),
          "Should return ComponentResourceHandle");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests - Primitives")
  class StaticPrimitiveFactoryMethodTests {

    @Test
    @DisplayName("should have bool static factory method")
    void shouldHaveBoolStaticMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("bool", boolean.class);
      assertNotNull(method, "bool method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "bool should be static");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have s8 static factory method")
    void shouldHaveS8StaticMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("s8", byte.class);
      assertNotNull(method, "s8 method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "s8 should be static");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have s16 static factory method")
    void shouldHaveS16StaticMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("s16", short.class);
      assertNotNull(method, "s16 method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "s16 should be static");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have s32 static factory method")
    void shouldHaveS32StaticMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("s32", int.class);
      assertNotNull(method, "s32 method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "s32 should be static");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have s64 static factory method")
    void shouldHaveS64StaticMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("s64", long.class);
      assertNotNull(method, "s64 method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "s64 should be static");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have u8 static factory method")
    void shouldHaveU8StaticMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("u8", short.class);
      assertNotNull(method, "u8 method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "u8 should be static");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have u16 static factory method")
    void shouldHaveU16StaticMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("u16", int.class);
      assertNotNull(method, "u16 method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "u16 should be static");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have u32 static factory method")
    void shouldHaveU32StaticMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("u32", long.class);
      assertNotNull(method, "u32 method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "u32 should be static");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have u64 static factory method")
    void shouldHaveU64StaticMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("u64", long.class);
      assertNotNull(method, "u64 method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "u64 should be static");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have f32 static factory method")
    void shouldHaveF32StaticMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("f32", float.class);
      assertNotNull(method, "f32 method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "f32 should be static");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have f64 static factory method")
    void shouldHaveF64StaticMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("f64", double.class);
      assertNotNull(method, "f64 method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "f64 should be static");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have char_ static factory method")
    void shouldHaveCharStaticMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("char_", char.class);
      assertNotNull(method, "char_ method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "char_ should be static");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have string static factory method")
    void shouldHaveStringStaticMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("string", String.class);
      assertNotNull(method, "string method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "string should be static");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests - Compound Types")
  class StaticCompoundFactoryMethodTests {

    @Test
    @DisplayName("should have list static factory method with varargs")
    void shouldHaveListVarargsStaticMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("list", ComponentVal[].class);
      assertNotNull(method, "list method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "list should be static");
      assertTrue(method.isVarArgs(), "list should be varargs");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have list static factory method with List")
    void shouldHaveListStaticMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("list", List.class);
      assertNotNull(method, "list method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "list should be static");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have record static factory method")
    void shouldHaveRecordStaticMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("record", Map.class);
      assertNotNull(method, "record method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "record should be static");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have tuple static factory method")
    void shouldHaveTupleStaticMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("tuple", ComponentVal[].class);
      assertNotNull(method, "tuple method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "tuple should be static");
      assertTrue(method.isVarArgs(), "tuple should be varargs");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have variant static factory method with payload")
    void shouldHaveVariantWithPayloadStaticMethod() throws NoSuchMethodException {
      final Method method =
          ComponentVal.class.getMethod("variant", String.class, ComponentVal.class);
      assertNotNull(method, "variant method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "variant should be static");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have variant static factory method without payload")
    void shouldHaveVariantWithoutPayloadStaticMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("variant", String.class);
      assertNotNull(method, "variant method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "variant should be static");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have enum_ static factory method")
    void shouldHaveEnumStaticMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("enum_", String.class);
      assertNotNull(method, "enum_ method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "enum_ should be static");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have some static factory method")
    void shouldHaveSomeStaticMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("some", ComponentVal.class);
      assertNotNull(method, "some method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "some should be static");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have none static factory method")
    void shouldHaveNoneStaticMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("none");
      assertNotNull(method, "none method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "none should be static");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have ok static factory method with value")
    void shouldHaveOkWithValueStaticMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("ok", ComponentVal.class);
      assertNotNull(method, "ok method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "ok should be static");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have ok static factory method without value")
    void shouldHaveOkWithoutValueStaticMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("ok");
      assertNotNull(method, "ok method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "ok should be static");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have err static factory method with value")
    void shouldHaveErrWithValueStaticMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("err", ComponentVal.class);
      assertNotNull(method, "err method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "err should be static");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have err static factory method without value")
    void shouldHaveErrWithoutValueStaticMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("err");
      assertNotNull(method, "err method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "err should be static");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have flags static factory method with Set")
    void shouldHaveFlagsWithSetStaticMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("flags", Set.class);
      assertNotNull(method, "flags method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "flags should be static");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have flags static factory method with varargs")
    void shouldHaveFlagsVarargsStaticMethod() throws NoSuchMethodException {
      final Method method = ComponentVal.class.getMethod("flags", String[].class);
      assertNotNull(method, "flags method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "flags should be static");
      assertTrue(method.isVarArgs(), "flags should be varargs");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }
  }
}

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

package ai.tegmentum.wasmtime4j.wit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitRecord} class.
 *
 * <p>WitRecord represents a WIT record value with named fields.
 */
@DisplayName("WitRecord Tests")
class WitRecordTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(WitRecord.class.getModifiers()), "WitRecord should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(WitRecord.class.getModifiers()), "WitRecord should be public");
    }

    @Test
    @DisplayName("should extend WitValue")
    void shouldExtendWitValue() {
      assertTrue(
          WitValue.class.isAssignableFrom(WitRecord.class), "WitRecord should extend WitValue");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should have of factory method")
    void shouldHaveOfFactoryMethod() throws NoSuchMethodException {
      final Method method = WitRecord.class.getMethod("of", Map.class);
      assertNotNull(method, "of method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
      assertEquals(WitRecord.class, method.getReturnType(), "of should return WitRecord");
    }

    @Test
    @DisplayName("should have builder factory method")
    void shouldHaveBuilderFactoryMethod() throws NoSuchMethodException {
      final Method method = WitRecord.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
      assertEquals(
          WitRecord.Builder.class, method.getReturnType(), "builder should return Builder");
    }
  }

  @Nested
  @DisplayName("Field Access Method Tests")
  class FieldAccessMethodTests {

    @Test
    @DisplayName("should have getField method")
    void shouldHaveGetFieldMethod() throws NoSuchMethodException {
      final Method method = WitRecord.class.getMethod("getField", String.class);
      assertNotNull(method, "getField method should exist");
      assertEquals(WitValue.class, method.getReturnType(), "getField should return WitValue");
    }

    @Test
    @DisplayName("should have getFields method")
    void shouldHaveGetFieldsMethod() throws NoSuchMethodException {
      final Method method = WitRecord.class.getMethod("getFields");
      assertNotNull(method, "getFields method should exist");
      assertEquals(Map.class, method.getReturnType(), "getFields should return Map");
    }

    @Test
    @DisplayName("should have getFieldCount method")
    void shouldHaveGetFieldCountMethod() throws NoSuchMethodException {
      final Method method = WitRecord.class.getMethod("getFieldCount");
      assertNotNull(method, "getFieldCount method should exist");
      assertEquals(int.class, method.getReturnType(), "getFieldCount should return int");
    }

    @Test
    @DisplayName("should have hasField method")
    void shouldHaveHasFieldMethod() throws NoSuchMethodException {
      final Method method = WitRecord.class.getMethod("hasField", String.class);
      assertNotNull(method, "hasField method should exist");
      assertEquals(boolean.class, method.getReturnType(), "hasField should return boolean");
    }
  }

  @Nested
  @DisplayName("ToJava Method Tests")
  class ToJavaMethodTests {

    @Test
    @DisplayName("should have toJava method returning Map")
    void shouldHaveToJavaMethod() throws NoSuchMethodException {
      final Method method = WitRecord.class.getMethod("toJava");
      assertNotNull(method, "toJava method should exist");
      assertEquals(Map.class, method.getReturnType(), "toJava should return Map");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("Builder should be a nested class")
    void builderShouldBeNestedClass() {
      assertTrue(
          WitRecord.Builder.class.getDeclaringClass() == WitRecord.class,
          "Builder should be nested in WitRecord");
    }

    @Test
    @DisplayName("Builder should be public and final")
    void builderShouldBePublicAndFinal() {
      assertTrue(
          Modifier.isPublic(WitRecord.Builder.class.getModifiers()), "Builder should be public");
      assertTrue(
          Modifier.isFinal(WitRecord.Builder.class.getModifiers()), "Builder should be final");
    }

    @Test
    @DisplayName("Builder should have field method")
    void builderShouldHaveFieldMethod() throws NoSuchMethodException {
      final Method method =
          WitRecord.Builder.class.getMethod("field", String.class, WitValue.class);
      assertNotNull(method, "field method should exist");
      assertEquals(WitRecord.Builder.class, method.getReturnType(), "field should return Builder");
    }

    @Test
    @DisplayName("Builder should have build method")
    void builderShouldHaveBuildMethod() throws NoSuchMethodException {
      final Method method = WitRecord.Builder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(WitRecord.class, method.getReturnType(), "build should return WitRecord");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should have equals method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = WitRecord.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
    }

    @Test
    @DisplayName("should have hashCode method")
    void shouldHaveHashCodeMethod() throws NoSuchMethodException {
      final Method method = WitRecord.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
    }
  }
}

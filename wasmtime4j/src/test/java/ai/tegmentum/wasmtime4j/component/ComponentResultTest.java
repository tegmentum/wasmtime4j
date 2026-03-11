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
package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentResult}.
 *
 * @since 1.1.0
 */
@DisplayName("ComponentResult")
class ComponentResultTest {

  @Nested
  @DisplayName("ok factory")
  class OkFactory {

    @Test
    @DisplayName("creates ok with value")
    void createsOkWithValue() {
      final ComponentResult result = ComponentResult.ok(ComponentVal.s32(42));
      assertTrue(result.isOk());
      assertFalse(result.isErr());
      assertTrue(result.getOk().isPresent());
      assertEquals(42, result.getOk().get().asS32());
      assertFalse(result.getErr().isPresent());
    }

    @Test
    @DisplayName("creates ok without value")
    void createsOkWithoutValue() {
      final ComponentResult result = ComponentResult.ok();
      assertTrue(result.isOk());
      assertFalse(result.getOk().isPresent());
    }

    @Test
    @DisplayName("creates ok with null value")
    void createsOkWithNull() {
      final ComponentResult result = ComponentResult.ok(null);
      assertTrue(result.isOk());
      assertFalse(result.getOk().isPresent());
    }
  }

  @Nested
  @DisplayName("err factory")
  class ErrFactory {

    @Test
    @DisplayName("creates err with value")
    void createsErrWithValue() {
      final ComponentResult result = ComponentResult.err(ComponentVal.string("failure"));
      assertFalse(result.isOk());
      assertTrue(result.isErr());
      assertTrue(result.getErr().isPresent());
      assertEquals("failure", result.getErr().get().asString());
      assertFalse(result.getOk().isPresent());
    }

    @Test
    @DisplayName("creates err without value")
    void createsErrWithoutValue() {
      final ComponentResult result = ComponentResult.err();
      assertTrue(result.isErr());
      assertFalse(result.getErr().isPresent());
    }
  }

  @Nested
  @DisplayName("unwrap")
  class Unwrap {

    @Test
    @DisplayName("unwrap returns ok value")
    void unwrapReturnsOkValue() {
      final ComponentResult result = ComponentResult.ok(ComponentVal.s32(99));
      assertEquals(99, result.unwrap().asS32());
    }

    @Test
    @DisplayName("unwrap returns null for ok without value")
    void unwrapReturnsNullForOkWithoutValue() {
      final ComponentResult result = ComponentResult.ok();
      assertNull(result.unwrap());
    }

    @Test
    @DisplayName("unwrap throws on err")
    void unwrapThrowsOnErr() {
      final ComponentResult result = ComponentResult.err(ComponentVal.string("oops"));
      assertThrows(IllegalStateException.class, result::unwrap);
    }
  }

  @Nested
  @DisplayName("unwrapErr")
  class UnwrapErr {

    @Test
    @DisplayName("unwrapErr returns err value")
    void unwrapErrReturnsValue() {
      final ComponentResult result = ComponentResult.err(ComponentVal.string("bad"));
      assertEquals("bad", result.unwrapErr().asString());
    }

    @Test
    @DisplayName("unwrapErr throws on ok")
    void unwrapErrThrowsOnOk() {
      final ComponentResult result = ComponentResult.ok(ComponentVal.s32(1));
      assertThrows(IllegalStateException.class, result::unwrapErr);
    }
  }

  @Nested
  @DisplayName("map")
  class Map {

    @Test
    @DisplayName("maps ok value")
    void mapsOkValue() {
      final ComponentResult result = ComponentResult.ok(ComponentVal.s32(10));
      final ComponentResult mapped = result.map(v -> ComponentVal.s32(v.asS32() * 2));
      assertTrue(mapped.isOk());
      assertEquals(20, mapped.getOk().get().asS32());
    }

    @Test
    @DisplayName("map returns err unchanged")
    void mapReturnsErrUnchanged() {
      final ComponentResult result = ComponentResult.err(ComponentVal.string("error"));
      final ComponentResult mapped = result.map(v -> ComponentVal.s32(999));
      assertTrue(mapped.isErr());
      assertEquals("error", mapped.getErr().get().asString());
    }
  }

  @Nested
  @DisplayName("mapErr")
  class MapErr {

    @Test
    @DisplayName("maps err value")
    void mapsErrValue() {
      final ComponentResult result = ComponentResult.err(ComponentVal.string("bad"));
      final ComponentResult mapped = result.mapErr(v -> ComponentVal.string("worse"));
      assertTrue(mapped.isErr());
      assertEquals("worse", mapped.getErr().get().asString());
    }

    @Test
    @DisplayName("mapErr returns ok unchanged")
    void mapErrReturnsOkUnchanged() {
      final ComponentResult result = ComponentResult.ok(ComponentVal.s32(42));
      final ComponentResult mapped = result.mapErr(v -> ComponentVal.string("shouldn't happen"));
      assertTrue(mapped.isOk());
      assertEquals(42, mapped.getOk().get().asS32());
    }
  }

  @Nested
  @DisplayName("equals and hashCode")
  class EqualsAndHashCode {

    @Test
    @DisplayName("equal ok results")
    void equalOkResults() {
      final ComponentResult r1 = ComponentResult.ok();
      final ComponentResult r2 = ComponentResult.ok();
      assertEquals(r1, r2);
      assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    @DisplayName("equal err results")
    void equalErrResults() {
      final ComponentResult r1 = ComponentResult.err();
      final ComponentResult r2 = ComponentResult.err();
      assertEquals(r1, r2);
      assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    @DisplayName("ok and err not equal")
    void okAndErrNotEqual() {
      assertNotEquals(ComponentResult.ok(), ComponentResult.err());
    }

    @Test
    @DisplayName("equal to self")
    void equalToSelf() {
      final ComponentResult r1 = ComponentResult.ok();
      assertEquals(r1, r1);
    }

    @Test
    @DisplayName("not equal to null")
    void notEqualToNull() {
      assertNotEquals(null, ComponentResult.ok());
    }
  }

  @Nested
  @DisplayName("toString")
  class ToStringTests {

    @Test
    @DisplayName("ok result toString")
    void okToString() {
      final String str = ComponentResult.ok().toString();
      assertTrue(str.startsWith("ok("));
    }

    @Test
    @DisplayName("err result toString")
    void errToString() {
      final String str = ComponentResult.err().toString();
      assertTrue(str.startsWith("err("));
    }
  }
}

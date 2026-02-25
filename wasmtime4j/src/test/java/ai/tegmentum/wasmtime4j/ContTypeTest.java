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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.type.ContType;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ContType} interface and {@link DefaultContType} implementation.
 *
 * @since 1.0.0
 */
@DisplayName("ContType Tests")
class ContTypeTest {

  private static final Logger LOGGER = Logger.getLogger(ContTypeTest.class.getName());

  private static FunctionType funcTypeOf(
      final WasmValueType[] params, final WasmValueType[] returns) {
    return new FunctionType(params, returns);
  }

  @Nested
  @DisplayName("ContType.create factory")
  class CreateTests {

    @Test
    @DisplayName("create returns non-null ContType")
    void createReturnsNonNull() {
      final FunctionType funcType =
          funcTypeOf(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final ContType contType = ContType.create(funcType);

      assertNotNull(contType, "ContType should not be null");
      assertTrue(
          contType instanceof DefaultContType, "ContType.create should return DefaultContType");
      LOGGER.info("Created ContType: " + contType);
    }

    @Test
    @DisplayName("create throws on null funcType")
    void createThrowsOnNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ContType.create(null),
          "create(null) should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("create with void signature")
    void createWithVoidSignature() {
      final FunctionType funcType = funcTypeOf(new WasmValueType[] {}, new WasmValueType[] {});
      final ContType contType = ContType.create(funcType);

      assertNotNull(contType, "ContType with void signature should not be null");
      assertEquals(funcType, contType.getFunctionType(), "Function type should match");
      LOGGER.info("Void ContType: " + contType);
    }
  }

  @Nested
  @DisplayName("getFunctionType")
  class GetFunctionTypeTests {

    @Test
    @DisplayName("getFunctionType returns the wrapped function type")
    void getFunctionTypeReturnsWrappedType() {
      final FunctionType funcType =
          funcTypeOf(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I64},
              new WasmValueType[] {WasmValueType.F64});
      final ContType contType = ContType.create(funcType);

      assertEquals(funcType, contType.getFunctionType(), "Should return the wrapped FunctionType");
      LOGGER.info(
          "ContType function type params: " + contType.getFunctionType().getParamTypes().length);
    }
  }

  @Nested
  @DisplayName("equals and hashCode")
  class EqualsHashCodeTests {

    @Test
    @DisplayName("equal ContTypes with same function type")
    void equalContTypesWithSameFuncType() {
      final FunctionType funcType =
          funcTypeOf(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final ContType ct1 = ContType.create(funcType);
      final ContType ct2 = ContType.create(funcType);

      assertEquals(ct1, ct2, "ContTypes wrapping same FunctionType should be equal");
      assertEquals(ct1.hashCode(), ct2.hashCode(), "Hash codes should match for equal objects");
      LOGGER.info("ct1: " + ct1 + ", ct2: " + ct2);
    }

    @Test
    @DisplayName("not equal ContTypes with different function types")
    void notEqualWithDifferentFuncTypes() {
      final FunctionType ft1 =
          funcTypeOf(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final FunctionType ft2 =
          funcTypeOf(new WasmValueType[] {WasmValueType.I64}, new WasmValueType[] {});
      final ContType ct1 = ContType.create(ft1);
      final ContType ct2 = ContType.create(ft2);

      assertNotEquals(ct1, ct2, "ContTypes with different FunctionTypes should not be equal");
      LOGGER.info("ct1: " + ct1 + ", ct2: " + ct2);
    }

    @Test
    @DisplayName("equals is reflexive")
    void equalsIsReflexive() {
      final ContType ct =
          ContType.create(funcTypeOf(new WasmValueType[] {}, new WasmValueType[] {}));
      assertEquals(ct, ct, "ContType should equal itself");
    }

    @Test
    @DisplayName("not equal to null")
    void notEqualToNull() {
      final ContType ct =
          ContType.create(funcTypeOf(new WasmValueType[] {}, new WasmValueType[] {}));
      assertFalse(ct.equals(null), "ContType should not equal null");
    }

    @Test
    @DisplayName("not equal to non-ContType object")
    void notEqualToNonContType() {
      final ContType ct =
          ContType.create(funcTypeOf(new WasmValueType[] {}, new WasmValueType[] {}));
      assertFalse(ct.equals("not a cont type"), "ContType should not equal a String");
    }
  }

  @Nested
  @DisplayName("toString")
  class ToStringTests {

    @Test
    @DisplayName("toString contains ContType identifier")
    void toStringContainsIdentifier() {
      final ContType ct =
          ContType.create(
              funcTypeOf(
                  new WasmValueType[] {WasmValueType.I32},
                  new WasmValueType[] {WasmValueType.I64}));
      final String str = ct.toString();

      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("ContType"), "toString should contain 'ContType'");
      LOGGER.info("ContType toString: " + str);
    }
  }
}

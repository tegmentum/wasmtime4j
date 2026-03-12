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
package ai.tegmentum.wasmtime4j.fuzz;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.gc.AnyRef;
import ai.tegmentum.wasmtime4j.gc.ArrayRef;
import ai.tegmentum.wasmtime4j.gc.ArrayType;
import ai.tegmentum.wasmtime4j.gc.EqRef;
import ai.tegmentum.wasmtime4j.gc.FieldType;
import ai.tegmentum.wasmtime4j.gc.I31Type;
import ai.tegmentum.wasmtime4j.gc.StructRef;
import ai.tegmentum.wasmtime4j.gc.StructType;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;

/**
 * Fuzz tests for WebAssembly GC type operations.
 *
 * <p>This fuzzer tests the robustness of GC type APIs by:
 *
 * <ul>
 *   <li>Creating I31 values with fuzzed integers and exercising validation/conversion
 *   <li>Building struct types with random field counts and types
 *   <li>Building array types with random element types
 *   <li>Testing GC reference type casting and null reference handling
 * </ul>
 *
 * @since 1.0.0
 */
public class GcTypeFuzzer {

  /**
   * Creates an EngineConfig with GC features enabled.
   *
   * @return a GC-enabled engine config
   */
  private static EngineConfig gcConfig() {
    return new EngineConfig().wasmGc(true).wasmGcTypes(true);
  }

  /**
   * Selects a FieldType based on a fuzzed index.
   *
   * @param index the index (0-3) to select a field type
   * @return the selected field type
   */
  private static FieldType selectFieldType(final int index) {
    switch (index) {
      case 0:
        return FieldType.i32();
      case 1:
        return FieldType.i64();
      case 2:
        return FieldType.f32();
      case 3:
        return FieldType.f64();
      default:
        return FieldType.i32();
    }
  }

  /**
   * Fuzz test for I31 value creation and operations.
   *
   * <p>Tests creating I31 values with fuzzed integers, validating range checks, clamping, and
   * unsigned conversions.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzI31Creation(final FuzzedDataProvider data) {
    final int value = data.consumeInt();

    try (Engine engine = Engine.create(gcConfig())) {
      engine.isValid();

      // Test I31Value creation - may throw for out-of-range values
      try {
        final I31Type.I31Value i31 = I31Type.I31Value.of(value);
        i31.getValue();
      } catch (IllegalArgumentException e) {
        // Expected for out-of-range values
      }

      // Test validation
      I31Type.isValidValue(value);

      // Test clamping - should never throw
      final int clamped = I31Type.clampValue(value);

      // Test toUnsigned with clamped value - should never throw
      I31Type.toUnsigned(clamped);

      // Test fromUnsigned with fuzzed value
      try {
        I31Type.fromUnsigned(value);
      } catch (IllegalArgumentException e) {
        // Expected for out-of-range values
      }

    } catch (WasmException e) {
      // Expected
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Fuzz test for struct type building and field access.
   *
   * <p>Tests creating struct types with random field counts and types, then exercising field
   * retrieval by index.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzStructOperations(final FuzzedDataProvider data) {
    final int fieldCount = data.consumeInt(0, 16);

    try (Engine engine = Engine.create(gcConfig())) {
      engine.isValid();

      try {
        final StructType.Builder builder = StructType.builder("fuzzed");

        for (int i = 0; i < fieldCount; i++) {
          final FieldType fieldType = selectFieldType(data.consumeInt(0, 3));
          builder.addField(fieldType);
        }

        final StructType structType = builder.build();

        // Exercise getters
        structType.getFields();
        structType.getFieldCount();

        // Fuzz field access by index
        final int accessIndex = data.consumeInt();
        try {
          structType.getField(accessIndex);
        } catch (IndexOutOfBoundsException e) {
          // Expected for out-of-bounds indices
        }
      } catch (IllegalArgumentException | IllegalStateException e) {
        // Expected for invalid configurations (e.g., zero fields)
      }

    } catch (WasmException e) {
      // Expected
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Fuzz test for array type building and property access.
   *
   * <p>Tests creating array types with random element types and exercising property getters.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzArrayOperations(final FuzzedDataProvider data) {
    final int typeIndex = data.consumeInt(0, 3);

    try (Engine engine = Engine.create(gcConfig())) {
      engine.isValid();

      try {
        final FieldType elementType = selectFieldType(typeIndex);
        final ArrayType arrayType =
            ArrayType.builder("fuzzed")
                .elementType(elementType)
                .mutable(data.consumeBoolean())
                .build();

        // Exercise getters
        arrayType.getElementType();
        arrayType.isMutable();
      } catch (IllegalArgumentException | IllegalStateException e) {
        // Expected for invalid configurations
      }

    } catch (WasmException e) {
      // Expected
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Fuzz test for GC reference type casting and null reference handling.
   *
   * <p>Tests null reference creation, type inspection, and i31 reference creation from fuzzed
   * values.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzTypeCasting(final FuzzedDataProvider data) {
    final int i31Value = data.consumeInt();

    try (Engine engine = Engine.create(gcConfig())) {
      engine.isValid();

      // Test null references
      final AnyRef anyNull = AnyRef.nullRef();
      anyNull.isNull();
      anyNull.isI31();
      anyNull.isStruct();
      anyNull.isArray();

      final EqRef eqNull = EqRef.nullRef();
      eqNull.isNull();

      final StructRef structNull = StructRef.nullRef();
      structNull.isNull();

      final ArrayRef arrayNull = ArrayRef.nullRef();
      arrayNull.isNull();

      // Test fromI31 with fuzzed value
      try {
        final AnyRef i31Ref = AnyRef.fromI31(i31Value);
        i31Ref.isNull();
        i31Ref.isI31();
        i31Ref.isStruct();
        i31Ref.isArray();
      } catch (WasmException | IllegalArgumentException | UnsupportedOperationException e) {
        // Expected for invalid i31 values or unsupported operations
      }

    } catch (WasmException
        | ClassCastException
        | IllegalArgumentException
        | UnsupportedOperationException e) {
      // Expected
    } catch (Exception e) {
      throw e;
    }
  }
}

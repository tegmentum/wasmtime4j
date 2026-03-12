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
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Precompiled;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import java.util.Arrays;

/**
 * Fuzz tests for module serialization and deserialization.
 *
 * <p>This fuzzer tests the robustness of serialization by:
 *
 * <ul>
 *   <li>Serializing a valid module, corrupting the bytes, and attempting deserialization
 *   <li>Passing completely random bytes to Module.deserialize
 *   <li>Corrupting precompiled module output and attempting deserialization
 *   <li>Passing fuzzed bytes to Engine.detectPrecompiled
 * </ul>
 *
 * <p>All corruption and random-data scenarios should throw WasmException, never crash the JVM.
 *
 * @since 1.0.0
 */
public class SerializationFuzzer {

  private static final String VALID_MODULE_WAT =
      """
      (module
        (func $add (param i32 i32) (result i32)
          local.get 0
          local.get 1
          i32.add)
        (func $identity (param i32) (result i32)
          local.get 0)
        (memory 1)
        (export "add" (func $add))
        (export "identity" (func $identity))
        (export "memory" (memory 0))
      )
      """;

  /**
   * Compiles a valid WAT module, serializes it, applies random mutations, and attempts
   * deserialization.
   *
   * <p>Mutations include bit flips, byte insertions, truncation, and overwrites. Deserialization of
   * corrupted data should throw WasmException, never crash.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzSerializeCorruptDeserialize(final FuzzedDataProvider data) {
    final int mutationType = data.consumeInt(0, 3);
    final int mutationOffset = data.consumeInt(0, 65535);
    final byte mutationByte = data.consumeByte();

    try (Engine engine = Engine.create();
        Module module = engine.compileWat(VALID_MODULE_WAT)) {
      final byte[] serialized = module.serialize();
      if (serialized == null || serialized.length == 0) {
        return;
      }

      final byte[] corrupted =
          applyMutation(serialized, mutationType, mutationOffset, mutationByte, data);

      try (Module deserialized = Module.deserialize(engine, corrupted)) {
        // If deserialization succeeds on corrupted data, that's fine too
        deserialized.isValid();
      } catch (WasmException e) {
        // Expected: corrupted data should be rejected
      }
    } catch (WasmException e) {
      // Expected: compilation or serialization may fail
    } catch (IllegalArgumentException e) {
      // Expected: null or empty bytes
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Passes completely random bytes to Module.deserialize.
   *
   * <p>Should always throw WasmException or IllegalArgumentException, never crash.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzRawBytesDeserialize(final FuzzedDataProvider data) {
    final byte[] randomBytes = data.consumeRemainingAsBytes();

    try (Engine engine = Engine.create()) {
      try (Module module = Module.deserialize(engine, randomBytes)) {
        // Unexpected success with random data
        module.isValid();
      } catch (WasmException e) {
        // Expected: random bytes are not valid serialized modules
      } catch (IllegalArgumentException e) {
        // Expected: null or empty bytes
      }
    } catch (WasmException e) {
      // Expected: engine creation may fail
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Precompiles a valid WASM module, corrupts the result, and passes to Module.deserialize.
   *
   * <p>Should throw WasmException on corruption, never crash.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzPrecompileCorrupt(final FuzzedDataProvider data) {
    final int mutationType = data.consumeInt(0, 3);
    final int mutationOffset = data.consumeInt(0, 65535);
    final byte mutationByte = data.consumeByte();

    try (Engine engine = Engine.create()) {
      // Create valid WASM bytes from WAT
      final byte[] wasmBytes = watToWasm(VALID_MODULE_WAT);
      if (wasmBytes == null) {
        return;
      }

      final byte[] precompiled = engine.precompileModule(wasmBytes);
      if (precompiled == null || precompiled.length == 0) {
        return;
      }

      final byte[] corrupted =
          applyMutation(precompiled, mutationType, mutationOffset, mutationByte, data);

      try (Module module = Module.deserialize(engine, corrupted)) {
        module.isValid();
      } catch (WasmException e) {
        // Expected
      }
    } catch (WasmException e) {
      // Expected: compilation or precompilation may fail
    } catch (IllegalArgumentException e) {
      // Expected
    } catch (UnsupportedOperationException e) {
      // Expected: precompile may not be supported
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Passes fuzzed bytes to Engine.detectPrecompiled.
   *
   * <p>Should return a Precompiled enum value or null, never crash.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzDetectPrecompiled(final FuzzedDataProvider data) {
    final byte[] fuzzedBytes = data.consumeRemainingAsBytes();

    try (Engine engine = Engine.create()) {
      try {
        final Precompiled result = engine.detectPrecompiled(fuzzedBytes);
        // Result may be null (not precompiled) or a valid enum value
        if (result != null) {
          result.getValue();
        }
      } catch (IllegalArgumentException e) {
        // Expected: null or empty bytes
      }
    } catch (WasmException e) {
      // Expected: engine creation may fail
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Applies a mutation to the given byte array based on the mutation type.
   *
   * @param original the original bytes
   * @param mutationType 0=bit flip, 1=truncate, 2=append garbage, 3=zero out range
   * @param offset the mutation offset (mod original length)
   * @param mutationByte byte value used for mutations
   * @param data fuzz data for additional bytes if needed
   * @return the mutated byte array
   */
  private byte[] applyMutation(
      final byte[] original,
      final int mutationType,
      final int offset,
      final byte mutationByte,
      final FuzzedDataProvider data) {
    switch (mutationType) {
      case 0 -> {
        // Bit flip at offset
        final byte[] result = Arrays.copyOf(original, original.length);
        final int idx = Math.abs(offset) % result.length;
        result[idx] ^= mutationByte == 0 ? (byte) 0xFF : mutationByte;
        return result;
      }
      case 1 -> {
        // Truncate
        final int truncLen = Math.abs(offset) % Math.max(original.length, 1);
        return Arrays.copyOf(original, Math.max(truncLen, 1));
      }
      case 2 -> {
        // Append garbage
        final int garbageLen = Math.min(Math.abs(offset) % 256, 255) + 1;
        final byte[] garbage = data.consumeBytes(garbageLen);
        final byte[] result = Arrays.copyOf(original, original.length + garbage.length);
        System.arraycopy(garbage, 0, result, original.length, garbage.length);
        return result;
      }
      case 3 -> {
        // Zero out range
        final byte[] result = Arrays.copyOf(original, original.length);
        final int start = Math.abs(offset) % result.length;
        final int zeroLen = Math.min(Math.abs(mutationByte) % 32 + 1, result.length - start);
        Arrays.fill(result, start, start + zeroLen, (byte) 0);
        return result;
      }
      default -> {
        return Arrays.copyOf(original, original.length);
      }
    }
  }

  /**
   * Converts WAT text to WASM binary by compiling and getting the raw bytes.
   *
   * <p>Uses a temporary compilation to extract WASM bytes.
   *
   * @param wat the WAT text format
   * @return WASM binary bytes, or null if compilation fails
   */
  private byte[] watToWasm(final String wat) {
    try (Engine engine = Engine.create();
        Module module = engine.compileWat(wat)) {
      return module.serialize();
    } catch (WasmException e) {
      return null;
    } catch (Exception e) {
      return null;
    }
  }
}

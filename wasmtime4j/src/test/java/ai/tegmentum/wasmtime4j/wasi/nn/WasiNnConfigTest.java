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
package ai.tegmentum.wasmtime4j.wasi.nn;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WasiNnConfig}.
 *
 * <p>These tests exercise the pure-Java surface of {@code WasiNnConfig} — defensive-copy semantics,
 * immutability after {@link WasiNnConfig.Builder#build()}, and the {@link WasiNnConfig#defaults()}
 * singleton identity that the JNI path uses to pick between the classic {@code nativeEnableWasiNn}
 * entry point and the named-model {@code nativeEnableWasiNnWithModels} entry point.
 *
 * <p>The end-to-end round trip (empty config vs. registered-model config) through the JNI layer is
 * exercised by the integration suite; those tests are gated on wasi-nn being compiled into the
 * native library, whereas these tests always run.
 *
 * @since 1.4.1
 */
@DisplayName("WasiNnConfig Tests")
final class WasiNnConfigTest {

  @Nested
  @DisplayName("defaults() factory")
  class DefaultsFactory {

    @Test
    void defaultsIsSingleton() {
      assertSame(
          WasiNnConfig.defaults(),
          WasiNnConfig.defaults(),
          "defaults() must return the same instance so JNI can identity-compare");
    }

    @Test
    void defaultsHasNoNamedModels() {
      assertTrue(
          WasiNnConfig.defaults().namedModels().isEmpty(),
          "defaults() must yield an empty registry so JniComponentLinker takes the "
              + "nativeEnableWasiNn path (not nativeEnableWasiNnWithModels)");
    }

    @Test
    void builderEmptyEquivalentToDefaults() {
      // Not identity-equal, but observationally equivalent — both dispatch through
      // nativeEnableWasiNn.
      final WasiNnConfig built = WasiNnConfig.builder().build();
      assertTrue(built.namedModels().isEmpty());
      assertNotSame(
          WasiNnConfig.defaults(),
          built,
          "builder().build() returns a fresh instance; defaults() is the constant");
    }
  }

  @Nested
  @DisplayName("Builder validation")
  class BuilderValidation {

    @Test
    void rejectsNullName() {
      final WasiNnConfig.Builder b = WasiNnConfig.builder();
      assertThrows(
          IllegalArgumentException.class, () -> b.registerModel(null, new byte[] {1, 2, 3}));
    }

    @Test
    void rejectsEmptyName() {
      final WasiNnConfig.Builder b = WasiNnConfig.builder();
      assertThrows(IllegalArgumentException.class, () -> b.registerModel("", new byte[] {1, 2, 3}));
    }

    @Test
    void rejectsNullBytes() {
      final WasiNnConfig.Builder b = WasiNnConfig.builder();
      assertThrows(IllegalArgumentException.class, () -> b.registerModel("m", null));
    }

    @Test
    void allowsEmptyBytes() {
      // Not our business to validate ONNX magic here — that decodes at
      // native-side backend.load() time, and the message operator gets is
      // whichever the backend produces. Empty bytes is a valid registration
      // that will fail at enableWasiNn(config) invocation.
      final WasiNnConfig cfg = WasiNnConfig.builder().registerModel("empty", new byte[0]).build();
      assertEquals(1, cfg.namedModels().size());
      assertEquals(0, cfg.namedModels().get("empty").length);
    }
  }

  @Nested
  @DisplayName("Defensive copy semantics")
  class DefensiveCopy {

    @Test
    void builderCopiesModelBytesOnRegister() {
      final byte[] original = {1, 2, 3, 4};
      final WasiNnConfig cfg = WasiNnConfig.builder().registerModel("m", original).build();

      // Mutate the caller's array — the stored copy must be unaffected.
      original[0] = (byte) 0xFF;

      final byte[] stored = cfg.namedModels().get("m");
      assertArrayEquals(
          new byte[] {1, 2, 3, 4},
          stored,
          "registerModel must clone modelBytes so post-build caller mutation "
              + "doesn't reach the native decoder");
    }

    @Test
    void builderContinuedUseDoesNotAffectBuiltConfig() {
      final WasiNnConfig.Builder b = WasiNnConfig.builder().registerModel("a", new byte[] {1});
      final WasiNnConfig first = b.build();

      // Continue using the builder after build() — must not add "b" to `first`.
      b.registerModel("b", new byte[] {2});

      assertEquals(
          1,
          first.namedModels().size(),
          "post-build() Builder mutation must not leak into a built config");
      assertTrue(first.namedModels().containsKey("a"));
    }

    @Test
    void returnedMapIsUnmodifiable() {
      final WasiNnConfig cfg = WasiNnConfig.builder().registerModel("m", new byte[] {1}).build();

      final Map<String, byte[]> models = cfg.namedModels();
      assertThrows(UnsupportedOperationException.class, () -> models.put("evil", new byte[] {2}));
    }
  }

  @Nested
  @DisplayName("Multi-model round-trip")
  class MultiModelRoundTrip {

    @Test
    void preservesInsertionOrder() {
      // The JNI-side path serialises Map.entrySet() into parallel String[]/byte[][]
      // arrays; preserving insertion order matters for operator debuggability
      // (indexing into the logged registration list should match construction order).
      final WasiNnConfig cfg =
          WasiNnConfig.builder()
              .registerModel("m1", new byte[] {1})
              .registerModel("m2", new byte[] {2})
              .registerModel("m3", new byte[] {3})
              .build();

      final String[] keys = cfg.namedModels().keySet().toArray(new String[0]);
      assertArrayEquals(new String[] {"m1", "m2", "m3"}, keys);
    }

    @Test
    void overwriteReplacesEntry() {
      final WasiNnConfig cfg =
          WasiNnConfig.builder()
              .registerModel("m", new byte[] {1})
              .registerModel("m", new byte[] {9, 9, 9})
              .build();

      assertEquals(1, cfg.namedModels().size());
      assertArrayEquals(new byte[] {9, 9, 9}, cfg.namedModels().get("m"));
    }
  }
}

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.type.HeapType;
import ai.tegmentum.wasmtime4j.type.Ref;
import ai.tegmentum.wasmtime4j.type.RefType;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ContRef} interface and its integration with {@link Ref}.
 *
 * @since 1.0.0
 */
@DisplayName("ContRef Tests")
class ContRefTest {

  private static final Logger LOGGER = Logger.getLogger(ContRefTest.class.getName());

  /** Test implementation of ContRef for unit testing. */
  private static final class TestContRef implements ContRef {
    private final long handle;
    private boolean valid;

    TestContRef(final long handle) {
      this.handle = handle;
      this.valid = true;
    }

    @Override
    public long getNativeHandle() {
      return handle;
    }

    @Override
    public boolean isValid() {
      return valid;
    }

    void invalidate() {
      valid = false;
    }
  }

  @Nested
  @DisplayName("ContRef interface")
  class ContRefInterfaceTests {

    @Test
    @DisplayName("ContRef should report native handle")
    void contRefShouldReportNativeHandle() {
      final ContRef ref = new TestContRef(42L);
      assertEquals(42L, ref.getNativeHandle(), "Native handle should match constructor value");
      LOGGER.info("ContRef native handle: " + ref.getNativeHandle());
    }

    @Test
    @DisplayName("ContRef should report valid status")
    void contRefShouldReportValidStatus() {
      final TestContRef ref = new TestContRef(1L);
      assertTrue(ref.isValid(), "New ContRef should be valid");
      LOGGER.info("ContRef valid before invalidate: " + ref.isValid());

      ref.invalidate();
      assertFalse(ref.isValid(), "Invalidated ContRef should not be valid");
      LOGGER.info("ContRef valid after invalidate: " + ref.isValid());
    }

    @Test
    @DisplayName("ContRef with zero handle is valid")
    void contRefWithZeroHandle() {
      final ContRef ref = new TestContRef(0L);
      assertEquals(0L, ref.getNativeHandle(), "Zero handle should be allowed");
      assertTrue(ref.isValid(), "Zero handle ContRef should be valid");
    }
  }

  @Nested
  @DisplayName("Ref.fromCont factory")
  class RefFromContTests {

    @Test
    @DisplayName("fromCont creates non-null cont ref")
    void fromContCreatesNonNullContRef() {
      final ContRef contRef = new TestContRef(99L);
      final Ref ref = Ref.fromCont(contRef);

      assertNotNull(ref, "Ref should not be null");
      assertTrue(ref.isCont(), "Should be a cont ref");
      assertTrue(ref.isNonNull(), "Should be non-null");
      assertFalse(ref.isNull(), "Should not be null");
      LOGGER.info("fromCont ref: " + ref);
    }

    @Test
    @DisplayName("fromCont throws on null argument")
    void fromContThrowsOnNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Ref.fromCont(null),
          "fromCont(null) should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("fromCont ref is not other kinds")
    void fromContRefIsNotOtherKinds() {
      final Ref ref = Ref.fromCont(new TestContRef(1L));

      assertFalse(ref.isFunc(), "Cont ref should not be func");
      assertFalse(ref.isExtern(), "Cont ref should not be extern");
      assertFalse(ref.isAny(), "Cont ref should not be any");
      assertFalse(ref.isExn(), "Cont ref should not be exn");
      assertTrue(ref.isCont(), "Cont ref should be cont");
    }
  }

  @Nested
  @DisplayName("Ref.asCont extractor")
  class RefAsContTests {

    @Test
    @DisplayName("asCont extracts ContRef from non-null cont ref")
    void asContExtractsContRef() {
      final ContRef contRef = new TestContRef(77L);
      final Ref ref = Ref.fromCont(contRef);

      final Optional<ContRef> extracted = ref.asCont();
      assertTrue(extracted.isPresent(), "asCont should return present Optional");
      assertEquals(77L, extracted.get().getNativeHandle(), "Extracted handle should match");
      LOGGER.info("Extracted ContRef handle: " + extracted.get().getNativeHandle());
    }

    @Test
    @DisplayName("asCont returns empty for null cont ref")
    void asContReturnsEmptyForNullContRef() {
      final Ref ref = Ref.nullContRef();

      final Optional<ContRef> extracted = ref.asCont();
      assertFalse(extracted.isPresent(), "asCont on null contref should return empty");
    }

    @Test
    @DisplayName("asCont returns empty for non-cont ref")
    void asContReturnsEmptyForNonContRef() {
      final Ref ref = Ref.nullFuncRef();

      final Optional<ContRef> extracted = ref.asCont();
      assertFalse(extracted.isPresent(), "asCont on funcref should return empty");
    }
  }

  @Nested
  @DisplayName("Ref.nullContRef factory")
  class NullContRefTests {

    @Test
    @DisplayName("nullContRef creates null cont ref")
    void nullContRefCreatesNullRef() {
      final Ref ref = Ref.nullContRef();

      assertNotNull(ref, "Ref instance should not be null");
      assertTrue(ref.isCont(), "Should be a cont ref");
      assertTrue(ref.isNull(), "Should be null");
      assertFalse(ref.isNonNull(), "Should not be non-null");
      LOGGER.info("nullContRef: " + ref);
    }

    @Test
    @DisplayName("nullContRef has CONT kind")
    void nullContRefHasContKind() {
      final Ref ref = Ref.nullContRef();
      assertEquals(Ref.Kind.CONT, ref.getKind(), "Kind should be CONT");
    }
  }

  @Nested
  @DisplayName("Ref type operations with cont refs")
  class RefTypeOperationsTests {

    @Test
    @DisplayName("non-null cont ref has non-nullable CONT heap type")
    void nonNullContRefType() {
      final Ref ref = Ref.fromCont(new TestContRef(1L));
      final RefType refType = ref.ty();

      assertNotNull(refType, "RefType should not be null");
      assertEquals(HeapType.CONT, refType.getHeapType(), "Heap type should be CONT");
      assertFalse(refType.isNullable(), "Non-null ref should have non-nullable type");
      LOGGER.info("Non-null cont ref type: " + refType);
    }

    @Test
    @DisplayName("null cont ref has nullable CONT heap type")
    void nullContRefType() {
      final Ref ref = Ref.nullContRef();
      final RefType refType = ref.ty();

      assertNotNull(refType, "RefType should not be null");
      assertEquals(HeapType.CONT, refType.getHeapType(), "Heap type should be CONT");
      assertTrue(refType.isNullable(), "Null ref should have nullable type");
      LOGGER.info("Null cont ref type: " + refType);
    }
  }

  @Nested
  @DisplayName("Ref equality with cont refs")
  class RefEqualityTests {

    @Test
    @DisplayName("null cont refs are equal")
    void nullContRefsAreEqual() {
      final Ref ref1 = Ref.nullContRef();
      final Ref ref2 = Ref.nullContRef();

      assertEquals(ref1, ref2, "Two null cont refs should be equal");
      assertEquals(ref1.hashCode(), ref2.hashCode(), "Hash codes should match");
    }

    @Test
    @DisplayName("non-null cont ref with same value is equal")
    void nonNullContRefsSameValueAreEqual() {
      final TestContRef contRef = new TestContRef(42L);
      final Ref ref1 = Ref.fromCont(contRef);
      final Ref ref2 = Ref.fromCont(contRef);

      assertEquals(ref1, ref2, "Refs wrapping same ContRef should be equal");
    }

    @Test
    @DisplayName("null cont ref not equal to null func ref")
    void nullContRefNotEqualToNullFuncRef() {
      final Ref contRef = Ref.nullContRef();
      final Ref funcRef = Ref.nullFuncRef();

      assertFalse(contRef.equals(funcRef), "Null cont ref should not equal null func ref");
    }
  }
}

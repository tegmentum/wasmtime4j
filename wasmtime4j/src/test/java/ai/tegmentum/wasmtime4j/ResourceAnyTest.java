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

import ai.tegmentum.wasmtime4j.component.ResourceAny;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ResourceAny} interface and its {@link ResourceAny.DefaultResourceAny} inner
 * class.
 *
 * <p>ResourceAny represents an opaque resource handle in the WebAssembly Component Model. The
 * DefaultResourceAny provides a host-side implementation for resource management with type tracking,
 * ownership semantics, and drop lifecycle.
 */
@DisplayName("ResourceAny Tests")
class ResourceAnyTest {

  /** Creates a no-op Store proxy for tests that need a non-null Store reference. */
  private static Store createStubStore() {
    return (Store)
        Proxy.newProxyInstance(
            Store.class.getClassLoader(),
            new Class<?>[] {Store.class},
            (proxy, method, args) -> {
              if ("close".equals(method.getName())) {
                return null;
              }
              return null;
            });
  }

  private static final Store STUB_STORE = createStubStore();

  @Nested
  @DisplayName("resourceNew Factory Tests")
  class ResourceNewFactoryTests {

    @Test
    @DisplayName("should create owned resource with given typeId and rep")
    void shouldCreateOwnedResource() throws WasmException {
      final ResourceAny resource = ResourceAny.resourceNew(STUB_STORE, 42, 100);

      assertNotNull(resource, "Resource should be created");
      assertEquals(42, resource.getTypeId(), "Type ID should be 42");
      assertTrue(resource.isOwned(), "Resource created via resourceNew should be owned");
      assertFalse(resource.isBorrowed(), "Owned resource should not be borrowed");
    }

    @Test
    @DisplayName("should create resource with zero typeId and rep")
    void shouldCreateResourceWithZeroValues() throws WasmException {
      final ResourceAny resource = ResourceAny.resourceNew(STUB_STORE, 0, 0);

      assertEquals(0, resource.getTypeId(), "Type ID should be 0");
      assertEquals(0, resource.resourceRep(STUB_STORE), "Rep should be 0");
    }

    @Test
    @DisplayName("should create resources with different typeIds")
    void shouldCreateResourcesWithDifferentTypeIds() throws WasmException {
      final ResourceAny resource1 = ResourceAny.resourceNew(STUB_STORE, 1, 10);
      final ResourceAny resource2 = ResourceAny.resourceNew(STUB_STORE, 2, 20);

      assertEquals(1, resource1.getTypeId(), "First resource type ID should be 1");
      assertEquals(2, resource2.getTypeId(), "Second resource type ID should be 2");
      assertEquals(10, resource1.resourceRep(STUB_STORE), "First resource rep should be 10");
      assertEquals(20, resource2.resourceRep(STUB_STORE), "Second resource rep should be 20");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when store is null")
    void shouldThrowWhenStoreNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ResourceAny.resourceNew(null, 1, 1),
          "Should throw IllegalArgumentException for null store");
    }
  }

  @Nested
  @DisplayName("DefaultResourceAny Lifecycle Tests")
  class DefaultResourceAnyLifecycleTests {

    @Test
    @DisplayName("resourceRep should return the representation value")
    void resourceRepShouldReturnRepValue() throws WasmException {
      final ResourceAny resource = ResourceAny.resourceNew(STUB_STORE, 1, 777);

      assertEquals(
          777,
          resource.resourceRep(STUB_STORE),
          "resourceRep should return the u32 representation value");
    }

    @Test
    @DisplayName("getNativeHandle should return the rep value")
    void getNativeHandleShouldReturnRep() throws WasmException {
      final ResourceAny resource = ResourceAny.resourceNew(STUB_STORE, 1, 42);

      assertEquals(42, resource.getNativeHandle(), "getNativeHandle should return rep value");
    }

    @Test
    @DisplayName("resourceDrop should succeed on first call")
    void resourceDropShouldSucceedOnFirstCall() throws WasmException {
      final ResourceAny resource = ResourceAny.resourceNew(STUB_STORE, 1, 1);

      // Should not throw
      resource.resourceDrop(STUB_STORE);
    }

    @Test
    @DisplayName("resourceDrop should throw WasmException on second call (double-drop)")
    void resourceDropShouldThrowOnDoubleDrop() throws WasmException {
      final ResourceAny resource = ResourceAny.resourceNew(STUB_STORE, 1, 1);
      resource.resourceDrop(STUB_STORE);

      final WasmException exception =
          assertThrows(
              WasmException.class,
              () -> resource.resourceDrop(STUB_STORE),
              "Should throw WasmException on double-drop");
      assertTrue(
          exception.getMessage().contains("already been dropped"),
          "Exception message should indicate resource was already dropped, got: "
              + exception.getMessage());
    }

    @Test
    @DisplayName("resourceRep should throw WasmException after drop")
    void resourceRepShouldThrowAfterDrop() throws WasmException {
      final ResourceAny resource = ResourceAny.resourceNew(STUB_STORE, 1, 42);
      resource.resourceDrop(STUB_STORE);

      final WasmException exception =
          assertThrows(
              WasmException.class,
              () -> resource.resourceRep(STUB_STORE),
              "Should throw WasmException when accessing rep after drop");
      assertTrue(
          exception.getMessage().contains("dropped"),
          "Exception message should indicate resource was dropped, got: "
              + exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Ownership Tests")
  class OwnershipTests {

    @Test
    @DisplayName("resources from resourceNew should always be owned")
    void resourceNewShouldAlwaysBeOwned() throws WasmException {
      final ResourceAny resource = ResourceAny.resourceNew(STUB_STORE, 5, 10);

      assertTrue(resource.isOwned(), "Resource from resourceNew should be owned");
      assertFalse(resource.isBorrowed(), "Resource from resourceNew should not be borrowed");
    }

    @Test
    @DisplayName("isOwned and isBorrowed should be mutually exclusive")
    void isOwnedAndIsBorrowedShouldBeMutuallyExclusive() throws WasmException {
      final ResourceAny resource = ResourceAny.resourceNew(STUB_STORE, 1, 1);

      assertTrue(
          resource.isOwned() != resource.isBorrowed(),
          "isOwned() and isBorrowed() should return opposite values");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain ResourceAny prefix")
    void toStringShouldContainPrefix() throws WasmException {
      final ResourceAny resource = ResourceAny.resourceNew(STUB_STORE, 42, 100);

      assertTrue(
          resource.toString().startsWith("ResourceAny{"),
          "toString should start with 'ResourceAny{'");
    }

    @Test
    @DisplayName("toString should contain typeId, owned, and rep values")
    void toStringShouldContainFields() throws WasmException {
      final ResourceAny resource = ResourceAny.resourceNew(STUB_STORE, 42, 100);

      final String str = resource.toString();

      assertTrue(str.contains("typeId=42"), "toString should contain 'typeId=42'");
      assertTrue(str.contains("owned=true"), "toString should contain 'owned=true'");
      assertTrue(str.contains("rep=100"), "toString should contain 'rep=100'");
    }
  }
}

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
package ai.tegmentum.wasmtime4j.wasmtime.generated.epoch;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;

/** Debug test for native library loading — only runs on Java 22+ where Panama is available. */
public class SymbolLookupDebugTest {

  @Test
  void testSymbolsExist() throws Exception {
    assumeTrue(
        Runtime.version().feature() >= 22, "Requires Java 22+ for Panama Foreign Function API");

    Class<?> loaderClass = Class.forName("ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader");
    Object loader = loaderClass.getMethod("getInstance").invoke(null);
    boolean isLoaded = (boolean) loaderClass.getMethod("isLoaded").invoke(loader);
    assertTrue(isLoaded, "Native library should be loaded");

    Object lookup = loaderClass.getMethod("getSymbolLookup").invoke(loader);
    assertNotNull(lookup, "SymbolLookup should not be null");
  }
}

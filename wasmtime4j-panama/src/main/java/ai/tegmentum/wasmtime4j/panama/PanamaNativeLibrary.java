/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.panama;

import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;

/**
 * Helper class for finding native functions.
 *
 * <p>This class provides a simple static interface for looking up native function method handles,
 * delegating to the NativeFunctionBindings singleton.
 */
final class PanamaNativeLibrary {

  private PanamaNativeLibrary() {
    // Utility class
  }

  /**
   * Finds a native function by name and descriptor.
   *
   * @param functionName the name of the function
   * @param descriptor the function descriptor
   * @return the method handle for the function
   * @throws RuntimeException if the function cannot be found
   */
  static MethodHandle findFunction(final String functionName, final FunctionDescriptor descriptor) {
    NativeFunctionBindings bindings = NativeFunctionBindings.getInstance();
    MethodHandle handle = bindings.getFunction(functionName, descriptor);
    if (handle == null) {
      throw new RuntimeException("Native function not found: " + functionName);
    }
    return handle;
  }
}

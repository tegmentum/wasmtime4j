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
package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.component.ComponentExportIndex;
import java.util.logging.Logger;

/**
 * JNI implementation of {@link ComponentExportIndex}.
 *
 * <p>Wraps a native pointer to a boxed {@code wasmtime::component::ComponentExportIndex}. The
 * native memory is freed when {@link #close()} is called.
 *
 * @since 1.0.0
 */
final class JniComponentExportIndex implements ComponentExportIndex {

  private static final Logger LOGGER = Logger.getLogger(JniComponentExportIndex.class.getName());

  private long nativeHandle;

  /**
   * Creates a new JNI component export index.
   *
   * @param nativeHandle the native pointer to the boxed ComponentExportIndex
   */
  JniComponentExportIndex(final long nativeHandle) {
    if (nativeHandle == 0) {
      throw new IllegalArgumentException("nativeHandle cannot be 0");
    }
    this.nativeHandle = nativeHandle;
  }

  @Override
  public long getNativeHandle() {
    return nativeHandle;
  }

  @Override
  public boolean isValid() {
    return nativeHandle != 0;
  }

  @Override
  public void close() {
    if (nativeHandle != 0) {
      JniComponent.nativeDestroyExportIndex(nativeHandle);
      nativeHandle = 0;
      LOGGER.fine("Closed component export index");
    }
  }
}

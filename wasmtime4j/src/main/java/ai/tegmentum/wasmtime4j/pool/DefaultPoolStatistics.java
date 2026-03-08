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
package ai.tegmentum.wasmtime4j.pool;

/**
 * Default implementation of {@link PoolStatistics}.
 *
 * <p>This class provides statistics for monitoring pooling allocator usage and performance. It is
 * shared across all runtime implementations (JNI and Panama) since the statistics structure is
 * identical.
 *
 * @since 1.0.0
 */
public final class DefaultPoolStatistics extends AbstractPoolStatistics {

  /**
   * Creates a new DefaultPoolStatistics from a metrics array returned by native code.
   *
   * @param metrics the 12-element metrics array from the native call
   */
  public DefaultPoolStatistics(final long[] metrics) {
    super(metrics);
  }

  /** Creates empty statistics with all values set to zero. */
  public DefaultPoolStatistics() {
    super();
  }
}

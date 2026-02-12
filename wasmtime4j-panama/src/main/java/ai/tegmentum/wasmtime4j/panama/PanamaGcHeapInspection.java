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

import ai.tegmentum.wasmtime4j.gc.GcHeapInspection;
import ai.tegmentum.wasmtime4j.gc.GcStats;
import ai.tegmentum.wasmtime4j.gc.ReferenceGraph;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Panama implementation of GC heap inspection.
 *
 * @since 1.0.0
 */
final class PanamaGcHeapInspection implements GcHeapInspection {

  @Override
  public long getTotalObjectCount() {
    return 0;
  }

  @Override
  public long getTotalHeapSize() {
    return 0;
  }

  @Override
  public long getUsedHeapSize() {
    return 0;
  }

  @Override
  public long getFreeHeapSize() {
    return 0;
  }

  @Override
  public Map<String, Long> getObjectTypeDistribution() {
    return Collections.emptyMap();
  }

  @Override
  public Map<String, Long> getMemoryUsageByType() {
    return Collections.emptyMap();
  }

  @Override
  public List<StructInstanceInfo> getStructInstances() {
    return Collections.emptyList();
  }

  @Override
  public List<ArrayInstanceInfo> getArrayInstances() {
    return Collections.emptyList();
  }

  @Override
  public List<I31InstanceInfo> getI31Instances() {
    return Collections.emptyList();
  }

  @Override
  public ReferenceGraph getReferenceGraph() {
    return null;
  }

  @Override
  public GcStats getGcStats() {
    return null;
  }

  @Override
  public HeapFragmentation getFragmentationInfo() {
    return new HeapFragmentation() {
      @Override
      public double getFragmentationRatio() {
        return 0.0;
      }

      @Override
      public int getFreeBlockCount() {
        return 0;
      }

      @Override
      public long getLargestFreeBlock() {
        return 0;
      }

      @Override
      public long getAverageFreeBlockSize() {
        return 0;
      }
    };
  }

  @Override
  public List<RootObjectInfo> getRootObjects() {
    return Collections.emptyList();
  }
}

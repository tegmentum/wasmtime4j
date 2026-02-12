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

import ai.tegmentum.wasmtime4j.gc.MemoryLeakAnalysis;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Panama implementation of memory leak analysis.
 *
 * @since 1.0.0
 */
final class PanamaMemoryLeakAnalysis implements MemoryLeakAnalysis {

  private final Instant analysisTime = Instant.now();

  @Override
  public Instant getAnalysisTime() {
    return analysisTime;
  }

  @Override
  public long getTotalObjectCount() {
    return 0;
  }

  @Override
  public int getPotentialLeakCount() {
    return 0;
  }

  @Override
  public List<PotentialLeak> getPotentialLeaks() {
    return Collections.emptyList();
  }

  @Override
  public List<CircularReference> getCircularReferences() {
    return Collections.emptyList();
  }

  @Override
  public List<LongLivedObject> getLongLivedObjects() {
    return Collections.emptyList();
  }

  @Override
  public List<HighlyReferencedObject> getHighlyReferencedObjects() {
    return Collections.emptyList();
  }

  @Override
  public MemoryUsageTrend getMemoryUsageTrend() {
    return new MemoryUsageTrend() {
      @Override
      public boolean isIncreasing() {
        return false;
      }

      @Override
      public double getGrowthRate() {
        return 0.0;
      }

      @Override
      public double getCorrelation() {
        return 0.0;
      }

      @Override
      public long getTimeToExhaustionMillis() {
        return Long.MAX_VALUE;
      }

      @Override
      public boolean isLeakPattern() {
        return false;
      }
    };
  }

  @Override
  public LeakSeverity getLeakSeverity() {
    return LeakSeverity.LOW;
  }

  @Override
  public List<LeakRecommendation> getRecommendations() {
    return Collections.emptyList();
  }
}

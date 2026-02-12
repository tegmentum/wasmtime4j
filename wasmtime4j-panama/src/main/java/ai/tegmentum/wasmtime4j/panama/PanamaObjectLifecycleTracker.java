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

import ai.tegmentum.wasmtime4j.gc.GcObject;
import ai.tegmentum.wasmtime4j.gc.ObjectLifecycleTracker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Panama implementation of object lifecycle tracking.
 *
 * @since 1.0.0
 */
final class PanamaObjectLifecycleTracker implements ObjectLifecycleTracker {

  private final List<Long> trackedObjectIds;
  private final long startTime;

  PanamaObjectLifecycleTracker(final long[] objectIds) {
    this.trackedObjectIds = new ArrayList<>();
    for (final long id : objectIds) {
      this.trackedObjectIds.add(id);
    }
    this.startTime = System.currentTimeMillis();
  }

  @Override
  public List<Long> getTrackedObjects() {
    return Collections.unmodifiableList(trackedObjectIds);
  }

  @Override
  public List<LifecycleEvent> getLifecycleEvents(final long objectId) {
    return Collections.emptyList();
  }

  @Override
  public Map<Long, ObjectStatus> getObjectStatuses() {
    return Collections.emptyMap();
  }

  @Override
  public Map<Long, AccessStatistics> getAccessStatistics() {
    return Collections.emptyMap();
  }

  @Override
  public Map<Long, List<ReferenceChange>> getReferenceHistory() {
    return Collections.emptyMap();
  }

  @Override
  public LifecycleTrackingSummary stopTracking() {
    final long duration = System.currentTimeMillis() - startTime;
    final int count = trackedObjectIds.size();
    return new LifecycleTrackingSummary() {
      @Override
      public long getTrackingDurationMillis() {
        return duration;
      }

      @Override
      public int getTrackedObjectCount() {
        return count;
      }

      @Override
      public int getCollectedObjectCount() {
        return 0;
      }

      @Override
      public long getTotalEventCount() {
        return 0;
      }

      @Override
      public List<Long> getMostAccessedObjects() {
        return Collections.emptyList();
      }

      @Override
      public List<Long> getLongestLivedObjects() {
        return Collections.emptyList();
      }

      @Override
      public List<Long> getPotentialLeaks() {
        return Collections.emptyList();
      }
    };
  }

  @Override
  public void trackAdditionalObjects(final List<GcObject> objects) {
    if (objects != null) {
      for (final GcObject obj : objects) {
        trackedObjectIds.add(obj.getObjectId());
      }
    }
  }

  @Override
  public void stopTrackingObjects(final List<Long> objectIds) {
    if (objectIds != null) {
      trackedObjectIds.removeAll(objectIds);
    }
  }
}

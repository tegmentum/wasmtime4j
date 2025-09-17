package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.time.Instant;
import java.util.Objects;

/**
 * Filter for time ranges.
 *
 * @since 1.0.0
 */
public final class TimeRangeFilter {
  private final Instant startTime;
  private final Instant endTime;

  public TimeRangeFilter(final Instant startTime, final Instant endTime) {
    this.startTime = startTime;
    this.endTime = endTime;

    if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
      throw new IllegalArgumentException("Start time cannot be after end time");
    }
  }

  public Instant getStartTime() {
    return startTime;
  }

  public Instant getEndTime() {
    return endTime;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final TimeRangeFilter that = (TimeRangeFilter) obj;
    return Objects.equals(startTime, that.startTime) && Objects.equals(endTime, that.endTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(startTime, endTime);
  }

  @Override
  public String toString() {
    return "TimeRangeFilter{" + "start=" + startTime + ", end=" + endTime + '}';
  }
}

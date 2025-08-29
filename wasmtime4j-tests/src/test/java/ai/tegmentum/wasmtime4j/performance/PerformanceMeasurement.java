package ai.tegmentum.wasmtime4j.performance;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class PerformanceMeasurement {
    private final String testName;
    private final RuntimeType runtimeType;
    private final Duration duration;
    private final Instant measurementTime;
    private final boolean successful;
    
    public PerformanceMeasurement(final String testName, final RuntimeType runtimeType, 
                                 final Duration duration, final Instant measurementTime, 
                                 final boolean successful) {
        this.testName = Objects.requireNonNull(testName);
        this.runtimeType = Objects.requireNonNull(runtimeType);
        this.duration = Objects.requireNonNull(duration);
        this.measurementTime = Objects.requireNonNull(measurementTime);
        this.successful = successful;
    }
    
    public String getTestName() { return testName; }
    public RuntimeType getRuntimeType() { return runtimeType; }
    public Duration getDuration() { return duration; }
    public Instant getMeasurementTime() { return measurementTime; }
    public boolean isSuccessful() { return successful; }
    
    @Override
    public String toString() {
        return "PerformanceMeasurement{testName='" + testName + "', runtimeType=" + runtimeType + 
               ", duration=" + duration.toMillis() + "ms, successful=" + successful + '}';
    }
}
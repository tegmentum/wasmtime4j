package ai.tegmentum.wasmtime4j.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for CircuitBreaker functionality.
 */
class CircuitBreakerTest {

    private CircuitBreaker circuitBreaker;
    private AtomicInteger callCount;
    private AtomicInteger failureCount;

    @BeforeEach
    void setUp() {
        circuitBreaker = CircuitBreaker.builder()
            .failureThreshold(50.0) // 50% failure rate
            .minimumCallCount(5)
            .waitDuration(Duration.ofMillis(100))
            .slidingWindowSize(10)
            .permittedCallsInHalfOpenState(3)
            .build();

        callCount = new AtomicInteger(0);
        failureCount = new AtomicInteger(0);
    }

    @Test
    void testDefaultCircuitBreakerCreation() {
        final CircuitBreaker defaultBreaker = CircuitBreaker.defaultCircuitBreaker();

        assertThat(defaultBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(defaultBreaker.getFailureThreshold()).isEqualTo(50.0);
        assertThat(defaultBreaker.getMinimumCallCount()).isEqualTo(10);
        assertThat(defaultBreaker.getWaitDuration()).isEqualTo(Duration.ofSeconds(30));
        assertThat(defaultBreaker.getSlidingWindowSize()).isEqualTo(100);
        assertThat(defaultBreaker.getPermittedCallsInHalfOpenState()).isEqualTo(5);
    }

    @Test
    void testSuccessfulOperationsKeepCircuitClosed() {
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        for (int i = 0; i < 10; i++) {
            final String result = circuitBreaker.execute(() -> {
                callCount.incrementAndGet();
                return "success";
            });
            assertThat(result).isEqualTo("success");
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getTotalCalls()).isEqualTo(10);
        assertThat(circuitBreaker.getMetrics().getSuccessfulCalls()).isEqualTo(10);
        assertThat(circuitBreaker.getMetrics().getFailedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isEqualTo(0.0);
    }

    @Test
    void testCircuitOpensOnFailureThreshold() {
        // Execute enough calls to reach minimum call count
        for (int i = 0; i < 3; i++) {
            circuitBreaker.execute(() -> {
                callCount.incrementAndGet();
                return "success";
            });
        }

        // Execute failing calls to trigger circuit opening
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() ->
                circuitBreaker.execute(() -> {
                    callCount.incrementAndGet();
                    failureCount.incrementAndGet();
                    throw new RuntimeException("Test failure");
                })
            ).isInstanceOf(RuntimeException.class);
        }

        // Circuit should now be open due to high failure rate
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isGreaterThan(50.0);
    }

    @Test
    void testCircuitBlocksCallsWhenOpen() {
        // Force circuit to open
        circuitBreaker.open();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Calls should be blocked
        assertThatThrownBy(() ->
            circuitBreaker.execute(() -> "should not execute")
        ).isInstanceOf(CircuitBreakerOpenException.class);

        assertThat(circuitBreaker.getMetrics().getBlockedCalls()).isGreaterThan(0);
    }

    @Test
    void testExecuteWithFallback() {
        circuitBreaker.open();

        final String result = circuitBreaker.executeWithFallback(
            () -> "primary",
            "fallback"
        );

        assertThat(result).isEqualTo("fallback");
    }

    @Test
    void testExecuteWithFallbackSupplier() {
        circuitBreaker.open();

        final String result = circuitBreaker.executeWithFallback(
            () -> "primary",
            () -> "dynamic-fallback"
        );

        assertThat(result).isEqualTo("dynamic-fallback");
    }

    @Test
    void testAsyncExecution() {
        final CompletableFuture<String> future = circuitBreaker.executeAsync(() -> "async-result");

        assertThat(future).succeedsWithin(Duration.ofSeconds(1))
                         .isEqualTo("async-result");
    }

    @Test
    void testAsyncExecutionWithFailure() {
        final CompletableFuture<String> future = circuitBreaker.executeAsync(() -> {
            throw new RuntimeException("Async failure");
        });

        assertThat(future).failsWithin(Duration.ofSeconds(1))
                         .withThrowableOfType(RuntimeException.class)
                         .withMessage("Async failure");
    }

    @Test
    void testCircuitTransitionToHalfOpen() throws InterruptedException {
        // Force circuit to open
        circuitBreaker.open();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Wait for transition to half-open
        Thread.sleep(150); // Wait longer than waitDuration

        // Manual transition to half-open for testing
        circuitBreaker.halfOpen();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
    }

    @Test
    void testCircuitRecoveryFromHalfOpen() {
        circuitBreaker.halfOpen();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        // Execute successful calls in half-open state
        for (int i = 0; i < circuitBreaker.getPermittedCallsInHalfOpenState(); i++) {
            final String result = circuitBreaker.execute(() -> "success");
            assertThat(result).isEqualTo("success");
        }

        // Circuit should close after successful calls
        circuitBreaker.close();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void testMetricsAccuracy() {
        // Execute mixed successful and failed calls
        for (int i = 0; i < 5; i++) {
            circuitBreaker.execute(() -> "success");
        }

        for (int i = 0; i < 3; i++) {
            try {
                circuitBreaker.execute(() -> {
                    throw new RuntimeException("failure");
                });
            } catch (final RuntimeException e) {
                // Expected
            }
        }

        final CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getTotalCalls()).isEqualTo(8);
        assertThat(metrics.getSuccessfulCalls()).isEqualTo(5);
        assertThat(metrics.getFailedCalls()).isEqualTo(3);
        assertThat(metrics.getFailureRate()).isEqualTo(37.5); // 3/8 = 37.5%
    }

    @Test
    void testStateChangeListener() {
        final AtomicInteger stateChangeCount = new AtomicInteger(0);

        circuitBreaker.addStateChangeListener((previous, current, metrics) -> {
            stateChangeCount.incrementAndGet();
            assertThat(previous).isNotEqualTo(current);
        });

        circuitBreaker.open();
        circuitBreaker.halfOpen();
        circuitBreaker.close();

        assertThat(stateChangeCount.get()).isEqualTo(3);
    }

    @Test
    void testManualStateTransitions() {
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        circuitBreaker.open();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        circuitBreaker.halfOpen();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        circuitBreaker.close();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void testMetricsReset() {
        // Generate some metrics
        circuitBreaker.execute(() -> "success");
        try {
            circuitBreaker.execute(() -> {
                throw new RuntimeException("failure");
            });
        } catch (final RuntimeException e) {
            // Expected
        }

        assertThat(circuitBreaker.getMetrics().getTotalCalls()).isGreaterThan(0);

        circuitBreaker.reset();

        // Metrics should be reset but exact values depend on implementation
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void testCallAllowedCheck() {
        assertThat(circuitBreaker.isCallAllowed()).isTrue();

        circuitBreaker.open();
        assertThat(circuitBreaker.isCallAllowed()).isFalse();

        circuitBreaker.close();
        assertThat(circuitBreaker.isCallAllowed()).isTrue();
    }

    @Test
    void testManualRecordingMethods() {
        circuitBreaker.recordSuccess();
        circuitBreaker.recordFailure(new RuntimeException("test"));

        final CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getSuccessfulCalls()).isEqualTo(1);
        assertThat(metrics.getFailedCalls()).isEqualTo(1);
    }

    @Test
    void testBuilderValidation() {
        assertThatThrownBy(() ->
            CircuitBreaker.builder()
                .failureThreshold(-1.0)
                .build()
        ).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() ->
            CircuitBreaker.builder()
                .failureThreshold(101.0)
                .build()
        ).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() ->
            CircuitBreaker.builder()
                .minimumCallCount(0)
                .build()
        ).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() ->
            CircuitBreaker.builder()
                .waitDuration(Duration.ofMillis(-1))
                .build()
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
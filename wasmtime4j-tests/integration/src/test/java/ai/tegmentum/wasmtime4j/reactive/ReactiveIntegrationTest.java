/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for reactive package interfaces.
 *
 * <p>This test class validates the Publisher, Subscriber, and Subscription interfaces.
 */
@DisplayName("Reactive Integration Tests")
public class ReactiveIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(ReactiveIntegrationTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting Reactive Integration Tests");
  }

  @Nested
  @DisplayName("Publisher Interface Tests")
  class PublisherInterfaceTests {

    @Test
    @DisplayName("Should verify Publisher interface exists")
    void shouldVerifyPublisherInterfaceExists() {
      LOGGER.info("Testing Publisher interface existence");

      assertNotNull(Publisher.class, "Publisher interface should exist");
      assertTrue(Publisher.class.isInterface(), "Publisher should be an interface");

      LOGGER.info("Publisher interface verified");
    }

    @Test
    @DisplayName("Should implement Publisher with subscribe method")
    void shouldImplementPublisherWithSubscribeMethod() {
      LOGGER.info("Testing Publisher subscribe method");

      // Create a simple publisher implementation
      Publisher<String> publisher =
          new Publisher<String>() {
            @Override
            public void subscribe(final Subscriber<? super String> subscriber) {
              // Simple implementation for test
              subscriber.onSubscribe(
                  new Subscription() {
                    @Override
                    public void request(final long n) {
                      for (long i = 0; i < n; i++) {
                        subscriber.onNext("item" + i);
                      }
                      subscriber.onComplete();
                    }

                    @Override
                    public void cancel() {
                      // No-op for test
                    }
                  });
            }
          };

      assertNotNull(publisher, "Publisher implementation should not be null");

      LOGGER.info("Publisher subscribe method verified");
    }

    @Test
    @DisplayName("Should call subscriber methods correctly")
    void shouldCallSubscriberMethodsCorrectly() {
      LOGGER.info("Testing Publisher-Subscriber interaction");

      AtomicBoolean subscribed = new AtomicBoolean(false);
      AtomicInteger itemCount = new AtomicInteger(0);
      AtomicBoolean completed = new AtomicBoolean(false);

      Publisher<Integer> publisher =
          subscriber -> {
            subscriber.onSubscribe(
                new Subscription() {
                  @Override
                  public void request(final long n) {
                    for (long i = 0; i < Math.min(n, 5); i++) {
                      subscriber.onNext((int) i);
                    }
                    subscriber.onComplete();
                  }

                  @Override
                  public void cancel() {}
                });
          };

      Subscriber<Integer> subscriber =
          new Subscriber<Integer>() {
            private Subscription subscription;

            @Override
            public void onSubscribe(final Subscription s) {
              subscribed.set(true);
              this.subscription = s;
              s.request(5);
            }

            @Override
            public void onNext(final Integer item) {
              itemCount.incrementAndGet();
            }

            @Override
            public void onError(final Throwable t) {}

            @Override
            public void onComplete() {
              completed.set(true);
            }
          };

      publisher.subscribe(subscriber);

      assertTrue(subscribed.get(), "Subscriber should have received onSubscribe");
      assertEquals(5, itemCount.get(), "Should have received 5 items");
      assertTrue(completed.get(), "Subscriber should have received onComplete");

      LOGGER.info("Publisher-Subscriber interaction verified");
    }
  }

  @Nested
  @DisplayName("Subscriber Interface Tests")
  class SubscriberInterfaceTests {

    @Test
    @DisplayName("Should verify Subscriber interface exists")
    void shouldVerifySubscriberInterfaceExists() {
      LOGGER.info("Testing Subscriber interface existence");

      assertNotNull(Subscriber.class, "Subscriber interface should exist");
      assertTrue(Subscriber.class.isInterface(), "Subscriber should be an interface");

      LOGGER.info("Subscriber interface verified");
    }

    @Test
    @DisplayName("Should implement Subscriber with all methods")
    void shouldImplementSubscriberWithAllMethods() {
      LOGGER.info("Testing Subscriber implementation");

      List<String> received = new ArrayList<>();
      AtomicReference<Throwable> error = new AtomicReference<>();
      AtomicBoolean completed = new AtomicBoolean(false);

      Subscriber<String> subscriber =
          new Subscriber<String>() {
            @Override
            public void onSubscribe(final Subscription subscription) {
              LOGGER.info("Subscriber received subscription");
            }

            @Override
            public void onNext(final String item) {
              received.add(item);
            }

            @Override
            public void onError(final Throwable throwable) {
              error.set(throwable);
            }

            @Override
            public void onComplete() {
              completed.set(true);
            }
          };

      assertNotNull(subscriber, "Subscriber implementation should not be null");

      // Test onNext
      subscriber.onNext("item1");
      subscriber.onNext("item2");
      assertEquals(2, received.size(), "Should have received 2 items");

      // Test onComplete
      subscriber.onComplete();
      assertTrue(completed.get(), "Should be completed");

      LOGGER.info("Subscriber implementation verified");
    }

    @Test
    @DisplayName("Should handle onError")
    void shouldHandleOnError() {
      LOGGER.info("Testing Subscriber onError handling");

      AtomicReference<Throwable> capturedError = new AtomicReference<>();

      Subscriber<String> subscriber =
          new Subscriber<String>() {
            @Override
            public void onSubscribe(final Subscription subscription) {}

            @Override
            public void onNext(final String item) {}

            @Override
            public void onError(final Throwable throwable) {
              capturedError.set(throwable);
            }

            @Override
            public void onComplete() {}
          };

      RuntimeException testError = new RuntimeException("Test error");
      subscriber.onError(testError);

      assertNotNull(capturedError.get(), "Error should be captured");
      assertEquals("Test error", capturedError.get().getMessage(), "Error message should match");

      LOGGER.info("Subscriber onError handling verified");
    }
  }

  @Nested
  @DisplayName("Subscription Interface Tests")
  class SubscriptionInterfaceTests {

    @Test
    @DisplayName("Should verify Subscription interface exists")
    void shouldVerifySubscriptionInterfaceExists() {
      LOGGER.info("Testing Subscription interface existence");

      assertNotNull(Subscription.class, "Subscription interface should exist");
      assertTrue(Subscription.class.isInterface(), "Subscription should be an interface");

      LOGGER.info("Subscription interface verified");
    }

    @Test
    @DisplayName("Should implement Subscription with request method")
    void shouldImplementSubscriptionWithRequestMethod() {
      LOGGER.info("Testing Subscription request method");

      AtomicInteger requestedCount = new AtomicInteger(0);

      Subscription subscription =
          new Subscription() {
            @Override
            public void request(final long n) {
              requestedCount.addAndGet((int) n);
            }

            @Override
            public void cancel() {}
          };

      subscription.request(10);
      assertEquals(10, requestedCount.get(), "Should have requested 10");

      subscription.request(5);
      assertEquals(15, requestedCount.get(), "Should have requested 15 total");

      LOGGER.info("Subscription request method verified");
    }

    @Test
    @DisplayName("Should implement Subscription with cancel method")
    void shouldImplementSubscriptionWithCancelMethod() {
      LOGGER.info("Testing Subscription cancel method");

      AtomicBoolean cancelled = new AtomicBoolean(false);

      Subscription subscription =
          new Subscription() {
            @Override
            public void request(final long n) {}

            @Override
            public void cancel() {
              cancelled.set(true);
            }
          };

      assertNotNull(subscription, "Subscription should not be null");

      subscription.cancel();
      assertTrue(cancelled.get(), "Subscription should be cancelled");

      LOGGER.info("Subscription cancel method verified");
    }
  }

  @Nested
  @DisplayName("Full Reactive Flow Tests")
  class FullReactiveFlowTests {

    @Test
    @DisplayName("Should complete full reactive flow")
    void shouldCompleteFullReactiveFlow() {
      LOGGER.info("Testing full reactive flow");

      List<Integer> results = new ArrayList<>();
      AtomicBoolean completed = new AtomicBoolean(false);

      // Create a publisher that emits 1-10
      Publisher<Integer> publisher =
          subscriber -> {
            subscriber.onSubscribe(
                new Subscription() {
                  private int current = 1;
                  private boolean cancelled = false;

                  @Override
                  public void request(final long n) {
                    for (long i = 0; i < n && current <= 10 && !cancelled; i++) {
                      subscriber.onNext(current++);
                    }
                    if (current > 10 && !cancelled) {
                      subscriber.onComplete();
                    }
                  }

                  @Override
                  public void cancel() {
                    cancelled = true;
                  }
                });
          };

      // Create a subscriber that collects items
      Subscriber<Integer> subscriber =
          new Subscriber<Integer>() {
            @Override
            public void onSubscribe(final Subscription s) {
              s.request(10);
            }

            @Override
            public void onNext(final Integer item) {
              results.add(item);
            }

            @Override
            public void onError(final Throwable t) {}

            @Override
            public void onComplete() {
              completed.set(true);
            }
          };

      publisher.subscribe(subscriber);

      assertEquals(10, results.size(), "Should have received 10 items");
      assertTrue(completed.get(), "Flow should be completed");
      assertEquals(1, results.get(0), "First item should be 1");
      assertEquals(10, results.get(9), "Last item should be 10");

      LOGGER.info("Full reactive flow verified");
    }

    @Test
    @DisplayName("Should handle back pressure")
    void shouldHandleBackPressure() {
      LOGGER.info("Testing back pressure handling");

      List<Integer> results = new ArrayList<>();
      AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();

      Publisher<Integer> publisher =
          subscriber -> {
            subscriber.onSubscribe(
                new Subscription() {
                  private int current = 1;

                  @Override
                  public void request(final long n) {
                    for (long i = 0; i < n && current <= 100; i++) {
                      subscriber.onNext(current++);
                    }
                    if (current > 100) {
                      subscriber.onComplete();
                    }
                  }

                  @Override
                  public void cancel() {}
                });
          };

      Subscriber<Integer> subscriber =
          new Subscriber<Integer>() {
            @Override
            public void onSubscribe(final Subscription s) {
              subscriptionRef.set(s);
              // Request only 5 initially
              s.request(5);
            }

            @Override
            public void onNext(final Integer item) {
              results.add(item);
            }

            @Override
            public void onError(final Throwable t) {}

            @Override
            public void onComplete() {}
          };

      publisher.subscribe(subscriber);

      // Initially should only have 5 items
      assertEquals(5, results.size(), "Should have 5 items after initial request");

      // Request 5 more
      subscriptionRef.get().request(5);
      assertEquals(10, results.size(), "Should have 10 items after second request");

      LOGGER.info("Back pressure handling verified");
    }

    @Test
    @DisplayName("Should handle cancellation")
    void shouldHandleCancellation() {
      LOGGER.info("Testing cancellation handling");

      List<Integer> results = new ArrayList<>();
      AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();
      AtomicBoolean completed = new AtomicBoolean(false);

      Publisher<Integer> publisher =
          subscriber -> {
            subscriber.onSubscribe(
                new Subscription() {
                  private int current = 1;
                  private boolean cancelled = false;

                  @Override
                  public void request(final long n) {
                    for (long i = 0; i < n && !cancelled; i++) {
                      subscriber.onNext(current++);
                    }
                    if (!cancelled) {
                      subscriber.onComplete();
                    }
                  }

                  @Override
                  public void cancel() {
                    cancelled = true;
                  }
                });
          };

      Subscriber<Integer> subscriber =
          new Subscriber<Integer>() {
            @Override
            public void onSubscribe(final Subscription s) {
              subscriptionRef.set(s);
            }

            @Override
            public void onNext(final Integer item) {
              results.add(item);
              if (results.size() >= 5) {
                subscriptionRef.get().cancel();
              }
            }

            @Override
            public void onError(final Throwable t) {}

            @Override
            public void onComplete() {
              completed.set(true);
            }
          };

      publisher.subscribe(subscriber);
      subscriptionRef.get().request(100);

      // Should have stopped at 5 due to cancellation
      assertEquals(5, results.size(), "Should have stopped at 5 items");

      LOGGER.info("Cancellation handling verified");
    }

    @Test
    @DisplayName("Should handle error propagation")
    void shouldHandleErrorPropagation() {
      LOGGER.info("Testing error propagation");

      AtomicReference<Throwable> capturedError = new AtomicReference<>();
      AtomicBoolean completed = new AtomicBoolean(false);

      Publisher<Integer> publisher =
          subscriber -> {
            subscriber.onSubscribe(
                new Subscription() {
                  @Override
                  public void request(final long n) {
                    subscriber.onNext(1);
                    subscriber.onError(new RuntimeException("Simulated error"));
                  }

                  @Override
                  public void cancel() {}
                });
          };

      Subscriber<Integer> subscriber =
          new Subscriber<Integer>() {
            @Override
            public void onSubscribe(final Subscription s) {
              s.request(10);
            }

            @Override
            public void onNext(final Integer item) {}

            @Override
            public void onError(final Throwable t) {
              capturedError.set(t);
            }

            @Override
            public void onComplete() {
              completed.set(true);
            }
          };

      publisher.subscribe(subscriber);

      assertNotNull(capturedError.get(), "Error should be captured");
      assertEquals(
          "Simulated error", capturedError.get().getMessage(), "Error message should match");
      assertTrue(!completed.get(), "Should not complete after error");

      LOGGER.info("Error propagation verified");
    }

    @Test
    @DisplayName("Should support concurrent subscription")
    void shouldSupportConcurrentSubscription() throws InterruptedException {
      LOGGER.info("Testing concurrent subscription");

      CountDownLatch latch = new CountDownLatch(2);
      List<Integer> results1 = new ArrayList<>();
      List<Integer> results2 = new ArrayList<>();

      Publisher<Integer> publisher =
          subscriber -> {
            new Thread(
                    () -> {
                      subscriber.onSubscribe(
                          new Subscription() {
                            @Override
                            public void request(final long n) {
                              for (long i = 0; i < n; i++) {
                                subscriber.onNext((int) i);
                              }
                              subscriber.onComplete();
                            }

                            @Override
                            public void cancel() {}
                          });
                    })
                .start();
          };

      publisher.subscribe(
          new Subscriber<Integer>() {
            @Override
            public void onSubscribe(final Subscription s) {
              s.request(5);
            }

            @Override
            public void onNext(final Integer item) {
              synchronized (results1) {
                results1.add(item);
              }
            }

            @Override
            public void onError(final Throwable t) {}

            @Override
            public void onComplete() {
              latch.countDown();
            }
          });

      publisher.subscribe(
          new Subscriber<Integer>() {
            @Override
            public void onSubscribe(final Subscription s) {
              s.request(3);
            }

            @Override
            public void onNext(final Integer item) {
              synchronized (results2) {
                results2.add(item);
              }
            }

            @Override
            public void onError(final Throwable t) {}

            @Override
            public void onComplete() {
              latch.countDown();
            }
          });

      boolean completed = latch.await(5, TimeUnit.SECONDS);
      assertTrue(completed, "Both subscriptions should complete");

      assertEquals(5, results1.size(), "First subscription should have 5 items");
      assertEquals(3, results2.size(), "Second subscription should have 3 items");

      LOGGER.info("Concurrent subscription verified");
    }
  }
}

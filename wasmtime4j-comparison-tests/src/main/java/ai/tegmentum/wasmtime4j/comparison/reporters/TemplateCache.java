package ai.tegmentum.wasmtime4j.comparison.reporters;

import freemarker.template.Template;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Template cache implementation for caching FreeMarker templates.
 *
 * @since 1.0.0
 */
final class TemplateCache {
  private final Map<String, CacheEntry> cache;
  private final long expirationTimeMs;
  private final ScheduledExecutorService cleanupExecutor;
  private final AtomicLong hits = new AtomicLong(0);
  private final AtomicLong misses = new AtomicLong(0);

  public TemplateCache(final int maxSize, final long expirationMinutes) {
    if (maxSize <= 0) {
      throw new IllegalArgumentException("maxSize must be positive");
    }
    if (expirationMinutes < 0) {
      throw new IllegalArgumentException("expirationMinutes cannot be negative");
    }

    this.expirationTimeMs = expirationMinutes * 60 * 1000;
    this.cache = new ConcurrentHashMap<>(maxSize);
    this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
      final Thread t = new Thread(r, "TemplateCache-Cleanup");
      t.setDaemon(true);
      return t;
    });

    // Schedule cleanup every minute
    this.cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries, 1, 1, TimeUnit.MINUTES);
  }

  public Template get(final String key) {
    Objects.requireNonNull(key, "key cannot be null");

    final CacheEntry entry = cache.get(key);
    if (entry == null) {
      misses.incrementAndGet();
      return null;
    }

    if (isExpired(entry)) {
      cache.remove(key);
      misses.incrementAndGet();
      return null;
    }

    hits.incrementAndGet();
    return entry.template;
  }

  public void put(final String key, final Template template) {
    Objects.requireNonNull(key, "key cannot be null");
    Objects.requireNonNull(template, "template cannot be null");

    cache.put(key, new CacheEntry(template, System.currentTimeMillis()));
  }

  public void clear() {
    cache.clear();
    hits.set(0);
    misses.set(0);
  }

  public CacheStatistics getStatistics() {
    return new CacheStatistics(cache.size(), hits.get(), misses.get());
  }

  private boolean isExpired(final CacheEntry entry) {
    if (expirationTimeMs == 0) {
      return false; // No expiration
    }
    return (System.currentTimeMillis() - entry.timestamp) > expirationTimeMs;
  }

  private void cleanupExpiredEntries() {
    if (expirationTimeMs == 0) {
      return; // No expiration
    }

    final long currentTime = System.currentTimeMillis();
    cache.entrySet().removeIf(entry ->
        (currentTime - entry.getValue().timestamp) > expirationTimeMs);
  }

  private static final class CacheEntry {
    final Template template;
    final long timestamp;

    CacheEntry(final Template template, final long timestamp) {
      this.template = template;
      this.timestamp = timestamp;
    }
  }
}
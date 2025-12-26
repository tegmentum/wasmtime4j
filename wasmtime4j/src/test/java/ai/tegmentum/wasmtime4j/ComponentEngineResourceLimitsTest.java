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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentEngineResourceLimits} interface.
 *
 * <p>ComponentEngineResourceLimits provides resource limits for the component engine.
 */
@DisplayName("ComponentEngineResourceLimits Tests")
class ComponentEngineResourceLimitsTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentEngineResourceLimits.class.getModifiers()),
          "ComponentEngineResourceLimits should be public");
      assertTrue(
          ComponentEngineResourceLimits.class.isInterface(),
          "ComponentEngineResourceLimits should be an interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getMemoryLimit method")
    void shouldHaveGetMemoryLimitMethod() throws NoSuchMethodException {
      final Method method = ComponentEngineResourceLimits.class.getMethod("getMemoryLimit");
      assertNotNull(method, "getMemoryLimit method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getTimeLimit method")
    void shouldHaveGetTimeLimitMethod() throws NoSuchMethodException {
      final Method method = ComponentEngineResourceLimits.class.getMethod("getTimeLimit");
      assertNotNull(method, "getTimeLimit method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getMaxInstances method")
    void shouldHaveGetMaxInstancesMethod() throws NoSuchMethodException {
      final Method method = ComponentEngineResourceLimits.class.getMethod("getMaxInstances");
      assertNotNull(method, "getMaxInstances method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have isResourceLimitingEnabled method")
    void shouldHaveIsResourceLimitingEnabledMethod() throws NoSuchMethodException {
      final Method method =
          ComponentEngineResourceLimits.class.getMethod("isResourceLimitingEnabled");
      assertNotNull(method, "isResourceLimitingEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Stub Implementation Tests")
  class StubImplementationTests {

    @Test
    @DisplayName("should support enabled resource limits implementation")
    void shouldSupportEnabledResourceLimitsImplementation() {
      final ComponentEngineResourceLimits limits = createEnabledLimits();

      assertTrue(limits.isResourceLimitingEnabled(), "Should be enabled");
      assertEquals(1024L * 1024 * 1024, limits.getMemoryLimit(), "Memory limit should be 1GB");
      assertEquals(60000L, limits.getTimeLimit(), "Time limit should be 60 seconds");
      assertEquals(100, limits.getMaxInstances(), "Max instances should be 100");
    }

    @Test
    @DisplayName("should support disabled resource limits implementation")
    void shouldSupportDisabledResourceLimitsImplementation() {
      final ComponentEngineResourceLimits limits = createDisabledLimits();

      assertFalse(limits.isResourceLimitingEnabled(), "Should be disabled");
      assertEquals(Long.MAX_VALUE, limits.getMemoryLimit(), "Memory limit should be MAX");
      assertEquals(Long.MAX_VALUE, limits.getTimeLimit(), "Time limit should be MAX");
      assertEquals(Integer.MAX_VALUE, limits.getMaxInstances(), "Max instances should be MAX");
    }
  }

  @Nested
  @DisplayName("Memory Limit Tests")
  class MemoryLimitTests {

    @Test
    @DisplayName("should support various memory limits")
    void shouldSupportVariousMemoryLimits() {
      // 256MB
      final ComponentEngineResourceLimits limits256 = createLimitsWithMemory(256L * 1024 * 1024);
      assertEquals(256L * 1024 * 1024, limits256.getMemoryLimit(), "Should be 256MB");

      // 1GB
      final ComponentEngineResourceLimits limits1g = createLimitsWithMemory(1024L * 1024 * 1024);
      assertEquals(1024L * 1024 * 1024, limits1g.getMemoryLimit(), "Should be 1GB");

      // 4GB
      final ComponentEngineResourceLimits limits4g =
          createLimitsWithMemory(4L * 1024 * 1024 * 1024);
      assertEquals(4L * 1024 * 1024 * 1024, limits4g.getMemoryLimit(), "Should be 4GB");
    }

    @Test
    @DisplayName("should handle max long memory limit")
    void shouldHandleMaxLongMemoryLimit() {
      final ComponentEngineResourceLimits limits = createLimitsWithMemory(Long.MAX_VALUE);

      assertEquals(Long.MAX_VALUE, limits.getMemoryLimit(), "Should handle max long");
    }

    @Test
    @DisplayName("should handle zero memory limit")
    void shouldHandleZeroMemoryLimit() {
      final ComponentEngineResourceLimits limits = createLimitsWithMemory(0L);

      assertEquals(0L, limits.getMemoryLimit(), "Should handle zero");
    }
  }

  @Nested
  @DisplayName("Time Limit Tests")
  class TimeLimitTests {

    @Test
    @DisplayName("should support various time limits")
    void shouldSupportVariousTimeLimits() {
      // 1 second
      final ComponentEngineResourceLimits limits1s = createLimitsWithTime(1000L);
      assertEquals(1000L, limits1s.getTimeLimit(), "Should be 1 second");

      // 1 minute
      final ComponentEngineResourceLimits limits1m = createLimitsWithTime(60000L);
      assertEquals(60000L, limits1m.getTimeLimit(), "Should be 1 minute");

      // 1 hour
      final ComponentEngineResourceLimits limits1h = createLimitsWithTime(3600000L);
      assertEquals(3600000L, limits1h.getTimeLimit(), "Should be 1 hour");
    }

    @Test
    @DisplayName("should handle max long time limit")
    void shouldHandleMaxLongTimeLimit() {
      final ComponentEngineResourceLimits limits = createLimitsWithTime(Long.MAX_VALUE);

      assertEquals(Long.MAX_VALUE, limits.getTimeLimit(), "Should handle max long");
    }
  }

  @Nested
  @DisplayName("Max Instances Tests")
  class MaxInstancesTests {

    @Test
    @DisplayName("should support various instance limits")
    void shouldSupportVariousInstanceLimits() {
      // Single instance
      final ComponentEngineResourceLimits limits1 = createLimitsWithInstances(1);
      assertEquals(1, limits1.getMaxInstances(), "Should be 1");

      // 100 instances
      final ComponentEngineResourceLimits limits100 = createLimitsWithInstances(100);
      assertEquals(100, limits100.getMaxInstances(), "Should be 100");

      // 10000 instances
      final ComponentEngineResourceLimits limits10k = createLimitsWithInstances(10000);
      assertEquals(10000, limits10k.getMaxInstances(), "Should be 10000");
    }

    @Test
    @DisplayName("should handle max int instances")
    void shouldHandleMaxIntInstances() {
      final ComponentEngineResourceLimits limits = createLimitsWithInstances(Integer.MAX_VALUE);

      assertEquals(Integer.MAX_VALUE, limits.getMaxInstances(), "Should handle max int");
    }

    @Test
    @DisplayName("should handle zero instances")
    void shouldHandleZeroInstances() {
      final ComponentEngineResourceLimits limits = createLimitsWithInstances(0);

      assertEquals(0, limits.getMaxInstances(), "Should handle zero");
    }
  }

  @Nested
  @DisplayName("Resource Limiting Enabled Tests")
  class ResourceLimitingEnabledTests {

    @Test
    @DisplayName("when enabled all limits should be enforced")
    void whenEnabledAllLimitsShouldBeEnforced() {
      final ComponentEngineResourceLimits limits = createEnabledLimits();

      assertTrue(limits.isResourceLimitingEnabled(), "Should be enabled");
      assertTrue(limits.getMemoryLimit() < Long.MAX_VALUE, "Memory should have limit");
      assertTrue(limits.getTimeLimit() < Long.MAX_VALUE, "Time should have limit");
      assertTrue(limits.getMaxInstances() < Integer.MAX_VALUE, "Instances should have limit");
    }

    @Test
    @DisplayName("when disabled limits should be maximum")
    void whenDisabledLimitsShouldBeMaximum() {
      final ComponentEngineResourceLimits limits = createDisabledLimits();

      assertFalse(limits.isResourceLimitingEnabled(), "Should be disabled");
      assertEquals(Long.MAX_VALUE, limits.getMemoryLimit(), "Memory should be unlimited");
      assertEquals(Long.MAX_VALUE, limits.getTimeLimit(), "Time should be unlimited");
      assertEquals(Integer.MAX_VALUE, limits.getMaxInstances(), "Instances should be unlimited");
    }
  }

  /**
   * Creates enabled resource limits.
   *
   * @return enabled limits
   */
  private ComponentEngineResourceLimits createEnabledLimits() {
    return new ComponentEngineResourceLimits() {
      @Override
      public long getMemoryLimit() {
        return 1024L * 1024 * 1024; // 1GB
      }

      @Override
      public long getTimeLimit() {
        return 60000L; // 60 seconds
      }

      @Override
      public int getMaxInstances() {
        return 100;
      }

      @Override
      public boolean isResourceLimitingEnabled() {
        return true;
      }
    };
  }

  /**
   * Creates disabled resource limits.
   *
   * @return disabled limits
   */
  private ComponentEngineResourceLimits createDisabledLimits() {
    return new ComponentEngineResourceLimits() {
      @Override
      public long getMemoryLimit() {
        return Long.MAX_VALUE;
      }

      @Override
      public long getTimeLimit() {
        return Long.MAX_VALUE;
      }

      @Override
      public int getMaxInstances() {
        return Integer.MAX_VALUE;
      }

      @Override
      public boolean isResourceLimitingEnabled() {
        return false;
      }
    };
  }

  /**
   * Creates limits with specified memory limit.
   *
   * @param memoryLimit the memory limit
   * @return limits with specified memory
   */
  private ComponentEngineResourceLimits createLimitsWithMemory(final long memoryLimit) {
    return new ComponentEngineResourceLimits() {
      @Override
      public long getMemoryLimit() {
        return memoryLimit;
      }

      @Override
      public long getTimeLimit() {
        return 60000L;
      }

      @Override
      public int getMaxInstances() {
        return 100;
      }

      @Override
      public boolean isResourceLimitingEnabled() {
        return true;
      }
    };
  }

  /**
   * Creates limits with specified time limit.
   *
   * @param timeLimit the time limit
   * @return limits with specified time
   */
  private ComponentEngineResourceLimits createLimitsWithTime(final long timeLimit) {
    return new ComponentEngineResourceLimits() {
      @Override
      public long getMemoryLimit() {
        return 1024L * 1024 * 1024;
      }

      @Override
      public long getTimeLimit() {
        return timeLimit;
      }

      @Override
      public int getMaxInstances() {
        return 100;
      }

      @Override
      public boolean isResourceLimitingEnabled() {
        return true;
      }
    };
  }

  /**
   * Creates limits with specified max instances.
   *
   * @param maxInstances the max instances
   * @return limits with specified instances
   */
  private ComponentEngineResourceLimits createLimitsWithInstances(final int maxInstances) {
    return new ComponentEngineResourceLimits() {
      @Override
      public long getMemoryLimit() {
        return 1024L * 1024 * 1024;
      }

      @Override
      public long getTimeLimit() {
        return 60000L;
      }

      @Override
      public int getMaxInstances() {
        return maxInstances;
      }

      @Override
      public boolean isResourceLimitingEnabled() {
        return true;
      }
    };
  }
}

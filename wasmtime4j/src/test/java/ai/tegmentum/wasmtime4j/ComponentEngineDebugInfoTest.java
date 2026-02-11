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

import ai.tegmentum.wasmtime4j.component.ComponentEngineDebugInfo;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentEngineDebugInfo} interface.
 *
 * <p>ComponentEngineDebugInfo provides debug information for the component engine.
 */
@DisplayName("ComponentEngineDebugInfo Tests")
class ComponentEngineDebugInfoTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentEngineDebugInfo.class.getModifiers()),
          "ComponentEngineDebugInfo should be public");
      assertTrue(
          ComponentEngineDebugInfo.class.isInterface(),
          "ComponentEngineDebugInfo should be an interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getDebugLevel method")
    void shouldHaveGetDebugLevelMethod() throws NoSuchMethodException {
      final Method method = ComponentEngineDebugInfo.class.getMethod("getDebugLevel");
      assertNotNull(method, "getDebugLevel method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have isDebugEnabled method")
    void shouldHaveIsDebugEnabledMethod() throws NoSuchMethodException {
      final Method method = ComponentEngineDebugInfo.class.getMethod("isDebugEnabled");
      assertNotNull(method, "isDebugEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getDebugStatistics method")
    void shouldHaveGetDebugStatisticsMethod() throws NoSuchMethodException {
      final Method method = ComponentEngineDebugInfo.class.getMethod("getDebugStatistics");
      assertNotNull(method, "getDebugStatistics method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("Stub Implementation Tests")
  class StubImplementationTests {

    @Test
    @DisplayName("should support enabled debug implementation")
    void shouldSupportEnabledDebugImplementation() {
      final ComponentEngineDebugInfo debugInfo = createEnabledDebugInfo();

      assertTrue(debugInfo.isDebugEnabled(), "Debug should be enabled");
      assertNotNull(debugInfo.getDebugLevel(), "Debug level should not be null");
      assertNotNull(debugInfo.getDebugStatistics(), "Debug statistics should not be null");
    }

    @Test
    @DisplayName("should support disabled debug implementation")
    void shouldSupportDisabledDebugImplementation() {
      final ComponentEngineDebugInfo debugInfo = createDisabledDebugInfo();

      assertFalse(debugInfo.isDebugEnabled(), "Debug should be disabled");
      assertEquals("NONE", debugInfo.getDebugLevel(), "Debug level should be NONE");
    }
  }

  @Nested
  @DisplayName("Debug Level Tests")
  class DebugLevelTests {

    @Test
    @DisplayName("should support NONE level")
    void shouldSupportNoneLevel() {
      final ComponentEngineDebugInfo debugInfo = createDebugInfoWithLevel("NONE");

      assertEquals("NONE", debugInfo.getDebugLevel(), "Level should be NONE");
    }

    @Test
    @DisplayName("should support ERROR level")
    void shouldSupportErrorLevel() {
      final ComponentEngineDebugInfo debugInfo = createDebugInfoWithLevel("ERROR");

      assertEquals("ERROR", debugInfo.getDebugLevel(), "Level should be ERROR");
    }

    @Test
    @DisplayName("should support WARN level")
    void shouldSupportWarnLevel() {
      final ComponentEngineDebugInfo debugInfo = createDebugInfoWithLevel("WARN");

      assertEquals("WARN", debugInfo.getDebugLevel(), "Level should be WARN");
    }

    @Test
    @DisplayName("should support INFO level")
    void shouldSupportInfoLevel() {
      final ComponentEngineDebugInfo debugInfo = createDebugInfoWithLevel("INFO");

      assertEquals("INFO", debugInfo.getDebugLevel(), "Level should be INFO");
    }

    @Test
    @DisplayName("should support DEBUG level")
    void shouldSupportDebugLevel() {
      final ComponentEngineDebugInfo debugInfo = createDebugInfoWithLevel("DEBUG");

      assertEquals("DEBUG", debugInfo.getDebugLevel(), "Level should be DEBUG");
    }

    @Test
    @DisplayName("should support TRACE level")
    void shouldSupportTraceLevel() {
      final ComponentEngineDebugInfo debugInfo = createDebugInfoWithLevel("TRACE");

      assertEquals("TRACE", debugInfo.getDebugLevel(), "Level should be TRACE");
    }
  }

  @Nested
  @DisplayName("Debug Statistics Tests")
  class DebugStatisticsTests {

    @Test
    @DisplayName("should return statistics string")
    void shouldReturnStatisticsString() {
      final ComponentEngineDebugInfo debugInfo = createEnabledDebugInfo();

      final String stats = debugInfo.getDebugStatistics();
      assertNotNull(stats, "Statistics should not be null");
      assertTrue(stats.length() > 0, "Statistics should not be empty");
    }

    @Test
    @DisplayName("should include relevant information in statistics")
    void shouldIncludeRelevantInformationInStatistics() {
      final ComponentEngineDebugInfo debugInfo = createDetailedDebugInfo();

      final String stats = debugInfo.getDebugStatistics();
      assertTrue(stats.contains("components"), "Should contain component info");
      assertTrue(stats.contains("instances"), "Should contain instance info");
    }
  }

  @Nested
  @DisplayName("Debug Enabled Tests")
  class DebugEnabledTests {

    @Test
    @DisplayName("should return true when enabled")
    void shouldReturnTrueWhenEnabled() {
      final ComponentEngineDebugInfo debugInfo = createEnabledDebugInfo();

      assertTrue(debugInfo.isDebugEnabled(), "Should be true");
    }

    @Test
    @DisplayName("should return false when disabled")
    void shouldReturnFalseWhenDisabled() {
      final ComponentEngineDebugInfo debugInfo = createDisabledDebugInfo();

      assertFalse(debugInfo.isDebugEnabled(), "Should be false");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle empty statistics")
    void shouldHandleEmptyStatistics() {
      final ComponentEngineDebugInfo debugInfo = createDebugInfoWithStats("");

      assertEquals("", debugInfo.getDebugStatistics(), "Empty stats should work");
    }

    @Test
    @DisplayName("should handle long statistics string")
    void shouldHandleLongStatisticsString() {
      final String longStats = "X".repeat(10000);
      final ComponentEngineDebugInfo debugInfo = createDebugInfoWithStats(longStats);

      assertEquals(10000, debugInfo.getDebugStatistics().length(), "Should handle long stats");
    }

    @Test
    @DisplayName("should handle custom debug levels")
    void shouldHandleCustomDebugLevels() {
      final ComponentEngineDebugInfo debugInfo = createDebugInfoWithLevel("CUSTOM_LEVEL");

      assertEquals("CUSTOM_LEVEL", debugInfo.getDebugLevel(), "Should handle custom level");
    }
  }

  /**
   * Creates an enabled debug info.
   *
   * @return enabled debug info
   */
  private ComponentEngineDebugInfo createEnabledDebugInfo() {
    return new ComponentEngineDebugInfo() {
      @Override
      public String getDebugLevel() {
        return "DEBUG";
      }

      @Override
      public boolean isDebugEnabled() {
        return true;
      }

      @Override
      public String getDebugStatistics() {
        return "Debug enabled, 10 components loaded";
      }
    };
  }

  /**
   * Creates a disabled debug info.
   *
   * @return disabled debug info
   */
  private ComponentEngineDebugInfo createDisabledDebugInfo() {
    return new ComponentEngineDebugInfo() {
      @Override
      public String getDebugLevel() {
        return "NONE";
      }

      @Override
      public boolean isDebugEnabled() {
        return false;
      }

      @Override
      public String getDebugStatistics() {
        return "Debug disabled";
      }
    };
  }

  /**
   * Creates debug info with specified level.
   *
   * @param level the debug level
   * @return debug info with specified level
   */
  private ComponentEngineDebugInfo createDebugInfoWithLevel(final String level) {
    return new ComponentEngineDebugInfo() {
      @Override
      public String getDebugLevel() {
        return level;
      }

      @Override
      public boolean isDebugEnabled() {
        return !"NONE".equals(level);
      }

      @Override
      public String getDebugStatistics() {
        return "Level: " + level;
      }
    };
  }

  /**
   * Creates detailed debug info.
   *
   * @return detailed debug info
   */
  private ComponentEngineDebugInfo createDetailedDebugInfo() {
    return new ComponentEngineDebugInfo() {
      @Override
      public String getDebugLevel() {
        return "TRACE";
      }

      @Override
      public boolean isDebugEnabled() {
        return true;
      }

      @Override
      public String getDebugStatistics() {
        return "components=10, instances=25, memory=512MB";
      }
    };
  }

  /**
   * Creates debug info with specified statistics.
   *
   * @param stats the statistics string
   * @return debug info with specified stats
   */
  private ComponentEngineDebugInfo createDebugInfoWithStats(final String stats) {
    return new ComponentEngineDebugInfo() {
      @Override
      public String getDebugLevel() {
        return "DEBUG";
      }

      @Override
      public boolean isDebugEnabled() {
        return true;
      }

      @Override
      public String getDebugStatistics() {
        return stats;
      }
    };
  }
}

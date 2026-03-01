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
package ai.tegmentum.wasmtime4j.testing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Annotation to mark tests that require the WebAssembly runtime to be available.
 *
 * <p>Tests annotated with {@code @RequiresWasmRuntime} will be automatically skipped if neither JNI
 * nor Panama runtime implementations are available in the test environment. This is useful for
 * distinguishing integration tests that need the native Wasmtime library from pure unit tests that
 * can run without native dependencies.
 *
 * <p>This annotation can be applied to:
 *
 * <ul>
 *   <li>Test classes - all tests in the class will require the runtime
 *   <li>Test methods - only the specific test method will require the runtime
 *   <li>Nested test classes - all tests in the nested class will require the runtime
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @RequiresWasmRuntime
 * class ModuleIntegrationTest {
 *     @Test
 *     void shouldCompileModule() {
 *         // This test will be skipped if runtime is not available
 *     }
 * }
 *
 * class MixedTest {
 *     @Test
 *     void pureUnitTest() {
 *         // Always runs
 *     }
 *
 *     @Test
 *     @RequiresWasmRuntime
 *     void integrationTest() {
 *         // Only runs if runtime is available
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @see WasmRuntimeCondition
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Tag("integration")
@ExtendWith(WasmRuntimeCondition.class)
public @interface RequiresWasmRuntime {
  // Marker annotation - no additional elements needed
}

/*
 * Copyright 2024 Tegmentum Technology, Inc.
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

package ai.tegmentum.wasmtime4j.experimental;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks APIs as experimental and subject to change.
 *
 * <p>APIs marked with this annotation are experimental implementations of cutting-edge WebAssembly
 * proposals and may change or be removed in future versions without notice. They should not be used
 * in production environments without thorough testing and understanding of the risks involved.
 *
 * <p>Experimental APIs require explicit enablement through feature flags and are disabled by
 * default to ensure stability of production code.
 *
 * @since 1.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({
  ElementType.TYPE,
  ElementType.METHOD,
  ElementType.FIELD,
  ElementType.CONSTRUCTOR,
  ElementType.PACKAGE,
  ElementType.ANNOTATION_TYPE
})
public @interface ExperimentalApi {

  /**
   * The experimental feature this API belongs to.
   *
   * @return the experimental feature
   */
  ExperimentalFeatures.Feature feature();

  /**
   * Description of the experimental nature of this API.
   *
   * @return description of experimental status
   */
  String description() default "This API is experimental and subject to change";

  /**
   * Version when this API was introduced as experimental.
   *
   * @return the version when introduced
   */
  String since() default "1.0.0";

  /**
   * Expected timeline for stabilization or removal.
   *
   * @return expected timeline
   */
  String timeline() default "Unknown";
}

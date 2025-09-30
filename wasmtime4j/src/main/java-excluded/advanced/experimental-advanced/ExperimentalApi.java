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
 * Annotation for experimental WebAssembly APIs.
 *
 * <p>This annotation marks classes, methods, or fields that are part of experimental WebAssembly
 * features. These APIs are subject to change and may be removed or modified in future releases
 * without notice.
 *
 * <p><strong>WARNING:</strong> Code using experimental APIs should be carefully reviewed and
 * tested, as behavior may change between releases.
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
  ElementType.PACKAGE
})
public @interface ExperimentalApi {

  /**
   * The experimental feature that this API is part of.
   *
   * @return the feature
   */
  ExperimentalFeatures.Feature feature();

  /**
   * Additional notes or warnings about this experimental API.
   *
   * @return notes about the API
   */
  String notes() default "";

  /**
   * The version when this experimental API was introduced.
   *
   * @return the version
   */
  String since() default "1.0.0";

  /**
   * Whether this experimental API is expected to be stable in the future.
   *
   * @return true if expected to be stable, false if likely to change
   */
  boolean unstable() default true;
}

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
package ai.tegmentum.wasmtime4j.component;

/**
 * A {@link TaskExit} implementation representing an already-completed task.
 *
 * <p>This is used when concurrent calls complete synchronously and no further waiting is needed.
 *
 * @since 1.1.0
 */
final class CompletedTaskExit implements TaskExit {

  /** Singleton instance since completed tasks are stateless. */
  static final CompletedTaskExit INSTANCE = new CompletedTaskExit();

  private CompletedTaskExit() {}

  @Override
  public void block() {
    // Already completed; nothing to wait for.
  }

  @Override
  public boolean isCompleted() {
    return true;
  }

  @Override
  public String toString() {
    return "TaskExit[completed]";
  }
}

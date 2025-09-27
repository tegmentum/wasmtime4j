/*
 * Copyright 2024 Tegmentum AI
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

import java.util.Collections;
import java.util.List;

/**
 * Information about a component link.
 *
 * <p>This class provides information about linked components, including
 * the source components, resulting linked component, and link status.
 *
 * @since 1.0.0
 */
public final class ComponentLinkInfo {

  private final String linkId;
  private final List<ComponentSimple> sourceComponents;
  private final ComponentSimple linkedComponent;
  private final boolean active;

  /**
   * Creates new component link information.
   *
   * @param linkId unique identifier for the link
   * @param sourceComponents the components that were linked
   * @param linkedComponent the resulting linked component
   * @param active whether the link is currently active
   */
  public ComponentLinkInfo(final String linkId,
      final List<ComponentSimple> sourceComponents,
      final ComponentSimple linkedComponent,
      final boolean active) {
    this.linkId = linkId != null ? linkId : "unknown";
    this.sourceComponents = Collections.unmodifiableList(sourceComponents);
    this.linkedComponent = linkedComponent;
    this.active = active;
  }

  /**
   * Gets the link identifier.
   *
   * @return the link ID
   */
  public String getLinkId() {
    return linkId;
  }

  /**
   * Gets the source components that were linked.
   *
   * @return unmodifiable list of source components
   */
  public List<ComponentSimple> getSourceComponents() {
    return sourceComponents;
  }

  /**
   * Gets the resulting linked component.
   *
   * @return the linked component
   */
  public ComponentSimple getLinkedComponent() {
    return linkedComponent;
  }

  /**
   * Checks if the link is currently active.
   *
   * @return true if active, false otherwise
   */
  public boolean isActive() {
    return active && linkedComponent != null && linkedComponent.isValid();
  }

  /**
   * Gets the number of source components in this link.
   *
   * @return number of source components
   */
  public int getSourceComponentCount() {
    return sourceComponents.size();
  }

  @Override
  public String toString() {
    return "ComponentLinkInfo{" +
        "linkId='" + linkId + '\'' +
        ", sourceComponentCount=" + sourceComponents.size() +
        ", active=" + active +
        '}';
  }
}
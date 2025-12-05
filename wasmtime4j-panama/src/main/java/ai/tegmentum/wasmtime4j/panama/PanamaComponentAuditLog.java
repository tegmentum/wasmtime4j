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

package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.ComponentAuditLog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Panama FFI implementation of ComponentAuditLog.
 *
 * <p>This class provides audit logging functionality for WebAssembly components through the Panama
 * Foreign Function API. It maintains a thread-safe in-memory log of audit entries.
 *
 * @since 1.0.0
 */
final class PanamaComponentAuditLog implements ComponentAuditLog {

  private final String componentId;
  private final List<AuditEntry> entries;

  /**
   * Creates a new Panama component audit log instance.
   *
   * @param componentId the component ID
   */
  PanamaComponentAuditLog(final String componentId) {
    if (componentId == null) {
      throw new IllegalArgumentException("componentId cannot be null");
    }
    this.componentId = componentId;
    this.entries = new CopyOnWriteArrayList<>();
  }

  @Override
  public String getComponentId() {
    return componentId;
  }

  @Override
  public List<AuditEntry> getEntries() {
    return Collections.unmodifiableList(new ArrayList<>(entries));
  }

  @Override
  public void addEntry(final AuditEntry entry) {
    if (entry != null) {
      entries.add(entry);
    }
  }

  @Override
  public List<AuditEntry> getEntriesByType(final AuditEntryType type) {
    if (type == null) {
      return Collections.emptyList();
    }
    return entries.stream()
        .filter(e -> e.getType() == type)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<AuditEntry> getEntriesInRange(final long startTime, final long endTime) {
    return entries.stream()
        .filter(e -> e.getTimestamp() >= startTime && e.getTimestamp() <= endTime)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public void clear() {
    entries.clear();
  }

  @Override
  public int size() {
    return entries.size();
  }

  @Override
  public boolean isEmpty() {
    return entries.isEmpty();
  }

  @Override
  public byte[] export(final ExportFormat format) {
    if (format == null || entries.isEmpty()) {
      return new byte[0];
    }

    final StringBuilder sb = new StringBuilder();
    switch (format) {
      case JSON:
        sb.append("[");
        for (int i = 0; i < entries.size(); i++) {
          if (i > 0) {
            sb.append(",");
          }
          final AuditEntry e = entries.get(i);
          sb.append("{\"id\":\"").append(escapeJson(e.getId())).append("\",");
          sb.append("\"timestamp\":").append(e.getTimestamp()).append(",");
          sb.append("\"type\":\"").append(e.getType()).append("\",");
          sb.append("\"message\":\"").append(escapeJson(e.getMessage())).append("\",");
          sb.append("\"user\":\"").append(escapeJson(e.getUser())).append("\",");
          sb.append("\"action\":\"").append(escapeJson(e.getAction())).append("\",");
          sb.append("\"resource\":\"").append(escapeJson(e.getResource())).append("\",");
          sb.append("\"severity\":\"").append(e.getSeverity()).append("\"}");
        }
        sb.append("]");
        break;

      case CSV:
        sb.append("id,timestamp,type,message,user,action,resource,severity\n");
        for (final AuditEntry e : entries) {
          sb.append(escapeCsv(e.getId())).append(",");
          sb.append(e.getTimestamp()).append(",");
          sb.append(e.getType()).append(",");
          sb.append(escapeCsv(e.getMessage())).append(",");
          sb.append(escapeCsv(e.getUser())).append(",");
          sb.append(escapeCsv(e.getAction())).append(",");
          sb.append(escapeCsv(e.getResource())).append(",");
          sb.append(e.getSeverity()).append("\n");
        }
        break;

      case XML:
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<auditLog>\n");
        for (final AuditEntry e : entries) {
          sb.append("  <entry>\n");
          sb.append("    <id>").append(escapeXml(e.getId())).append("</id>\n");
          sb.append("    <timestamp>").append(e.getTimestamp()).append("</timestamp>\n");
          sb.append("    <type>").append(e.getType()).append("</type>\n");
          sb.append("    <message>").append(escapeXml(e.getMessage())).append("</message>\n");
          sb.append("    <user>").append(escapeXml(e.getUser())).append("</user>\n");
          sb.append("    <action>").append(escapeXml(e.getAction())).append("</action>\n");
          sb.append("    <resource>").append(escapeXml(e.getResource())).append("</resource>\n");
          sb.append("    <severity>").append(e.getSeverity()).append("</severity>\n");
          sb.append("  </entry>\n");
        }
        sb.append("</auditLog>");
        break;

      case TEXT:
      default:
        for (final AuditEntry e : entries) {
          sb.append("[")
              .append(e.getTimestamp())
              .append("] ")
              .append(e.getSeverity())
              .append(" - ")
              .append(e.getType())
              .append(": ")
              .append(e.getMessage())
              .append(" (user=")
              .append(e.getUser())
              .append(", action=")
              .append(e.getAction())
              .append(", resource=")
              .append(e.getResource())
              .append(")\n");
        }
        break;
    }

    return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
  }

  private static String escapeJson(final String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }

  private static String escapeCsv(final String value) {
    if (value == null) {
      return "";
    }
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }

  private static String escapeXml(final String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }

  /** Default implementation of AuditEntry. */
  static final class DefaultAuditEntry implements AuditEntry {

    private final String id;
    private final long timestamp;
    private final AuditEntryType type;
    private final String message;
    private final String user;
    private final String action;
    private final String resource;
    private final Map<String, Object> metadata;
    private final SeverityLevel severity;

    DefaultAuditEntry(
        final String id,
        final long timestamp,
        final AuditEntryType type,
        final String message,
        final String user,
        final String action,
        final String resource,
        final Map<String, Object> metadata,
        final SeverityLevel severity) {
      this.id = id != null ? id : "";
      this.timestamp = timestamp;
      this.type = type != null ? type : AuditEntryType.SYSTEM_EVENT;
      this.message = message != null ? message : "";
      this.user = user != null ? user : "";
      this.action = action != null ? action : "";
      this.resource = resource != null ? resource : "";
      this.metadata = metadata != null ? Map.copyOf(metadata) : Collections.emptyMap();
      this.severity = severity != null ? severity : SeverityLevel.INFO;
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public long getTimestamp() {
      return timestamp;
    }

    @Override
    public AuditEntryType getType() {
      return type;
    }

    @Override
    public String getMessage() {
      return message;
    }

    @Override
    public String getUser() {
      return user;
    }

    @Override
    public String getAction() {
      return action;
    }

    @Override
    public String getResource() {
      return resource;
    }

    @Override
    public Map<String, Object> getMetadata() {
      return metadata;
    }

    @Override
    public SeverityLevel getSeverity() {
      return severity;
    }
  }
}

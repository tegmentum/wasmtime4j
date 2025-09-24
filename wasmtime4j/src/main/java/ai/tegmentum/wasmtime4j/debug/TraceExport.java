package ai.tegmentum.wasmtime4j.debug;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Exportable trace data for external analysis tools.
 * Provides a standardized format for trace data that can be imported
 * into profiling and analysis tools.
 */
public final class TraceExport {
    private final List<ExportEvent> events;
    private final TraceStatistics statistics;
    private final Map<String, FunctionProfile> functionProfiles;
    private final Instant exportTimestamp;

    public TraceExport(final List<ExportEvent> events,
                      final TraceStatistics statistics,
                      final Map<String, FunctionProfile> functionProfiles) {
        this.events = Collections.unmodifiableList(List.copyOf(events));
        this.statistics = Objects.requireNonNull(statistics);
        this.functionProfiles = Collections.unmodifiableMap(Map.copyOf(functionProfiles));
        this.exportTimestamp = Instant.now();
    }

    // Getters
    public List<ExportEvent> getEvents() { return events; }
    public TraceStatistics getStatistics() { return statistics; }
    public Map<String, FunctionProfile> getFunctionProfiles() { return functionProfiles; }
    public Instant getExportTimestamp() { return exportTimestamp; }

    /**
     * Exports to Chrome DevTools tracing format.
     */
    public String toChromeTraceFormat() {
        final StringBuilder json = new StringBuilder();
        json.append("{\"traceEvents\":[");

        boolean first = true;
        for (final ExportEvent event : events) {
            if (!first) {
                json.append(",");
            }
            first = false;

            json.append("{");
            json.append("\"name\":\"").append(escape(event.getDescription())).append("\",");
            json.append("\"cat\":\"").append(event.getType()).append("\",");
            json.append("\"ph\":\"B\","); // Begin event
            json.append("\"ts\":").append(event.getTimestamp().toEpochMilli() * 1000).append(",");
            json.append("\"pid\":1,\"tid\":1");
            if (!event.getData().isEmpty()) {
                json.append(",\"args\":{");
                boolean firstArg = true;
                for (final Map.Entry<String, Object> entry : event.getData().entrySet()) {
                    if (!firstArg) {
                        json.append(",");
                    }
                    firstArg = false;
                    json.append("\"").append(escape(entry.getKey())).append("\":");
                    json.append("\"").append(escape(String.valueOf(entry.getValue()))).append("\"");
                }
                json.append("}");
            }
            json.append("}");
        }

        json.append("]}");
        return json.toString();
    }

    private String escape(final String str) {
        return str.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    @Override
    public String toString() {
        return String.format("TraceExport{events=%d, exportTime=%s}",
                           events.size(), exportTimestamp);
    }

    /**
     * Exportable event data.
     */
    public static final class ExportEvent {
        private final Instant timestamp;
        private final ExecutionTracer.TraceEventType type;
        private final String description;
        private final Map<String, Object> data;

        public ExportEvent(final Instant timestamp,
                          final ExecutionTracer.TraceEventType type,
                          final String description,
                          final Map<String, Object> data) {
            this.timestamp = Objects.requireNonNull(timestamp);
            this.type = Objects.requireNonNull(type);
            this.description = Objects.requireNonNull(description);
            this.data = Collections.unmodifiableMap(Map.copyOf(data));
        }

        public Instant getTimestamp() { return timestamp; }
        public ExecutionTracer.TraceEventType getType() { return type; }
        public String getDescription() { return description; }
        public Map<String, Object> getData() { return data; }

        @Override
        public String toString() {
            return String.format("ExportEvent{type=%s, desc='%s', time=%s}",
                               type, description, timestamp);
        }
    }
}
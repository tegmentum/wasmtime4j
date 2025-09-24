package ai.tegmentum.wasmtime4j.ide;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.dev.ModuleInspector;
import ai.tegmentum.wasmtime4j.dev.DebuggingSession;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

/**
 * Language Server Protocol (LSP) implementation for WebAssembly development.
 * Provides IDE integration features including syntax highlighting, validation,
 * code completion, and debugging support.
 */
public final class WasmLanguageServer {

    private final Engine engine;
    private final Map<URI, DocumentInfo> openDocuments;
    private final Map<URI, ModuleInspector> moduleInspectors;
    private final Map<String, DebuggingSession> debugSessions;
    private final LanguageServerCapabilities capabilities;
    private volatile boolean isInitialized;

    /**
     * Creates a new WebAssembly Language Server.
     */
    public WasmLanguageServer() {
        this.engine = Engine.newEngine();
        this.openDocuments = new ConcurrentHashMap<>();
        this.moduleInspectors = new ConcurrentHashMap<>();
        this.debugSessions = new ConcurrentHashMap<>();
        this.capabilities = createCapabilities();
        this.isInitialized = false;
    }

    /**
     * Initializes the language server with client capabilities.
     *
     * @param clientCapabilities The client's capabilities
     * @return Server initialization result
     */
    public CompletableFuture<InitializeResult> initialize(final ClientCapabilities clientCapabilities) {
        return CompletableFuture.supplyAsync(() -> {
            isInitialized = true;
            return new InitializeResult(capabilities);
        });
    }

    /**
     * Called when the client has finished initialization.
     */
    public void initialized() {
        // Perform any post-initialization setup
    }

    /**
     * Shuts down the language server.
     *
     * @return Shutdown completion future
     */
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            // Close all debug sessions
            for (final DebuggingSession session : debugSessions.values()) {
                try {
                    session.close();
                } catch (final Exception e) {
                    // Log error but continue shutdown
                }
            }
            debugSessions.clear();

            // Clear document information
            openDocuments.clear();
            moduleInspectors.clear();

            isInitialized = false;
        });
    }

    /**
     * Handles document open notifications.
     *
     * @param params Document open parameters
     */
    public void didOpen(final DidOpenTextDocumentParams params) {
        if (!isInitialized) {
            return;
        }

        final TextDocumentItem document = params.getTextDocument();
        final URI uri = URI.create(document.getUri());

        final DocumentInfo docInfo = new DocumentInfo(
            uri,
            document.getText(),
            document.getVersion(),
            document.getLanguageId()
        );

        openDocuments.put(uri, docInfo);

        // Perform initial validation
        validateDocument(uri, document.getText());
    }

    /**
     * Handles document change notifications.
     *
     * @param params Document change parameters
     */
    public void didChange(final DidChangeTextDocumentParams params) {
        if (!isInitialized) {
            return;
        }

        final VersionedTextDocumentIdentifier document = params.getTextDocument();
        final URI uri = URI.create(document.getUri());
        final List<TextDocumentContentChangeEvent> changes = params.getContentChanges();

        final DocumentInfo docInfo = openDocuments.get(uri);
        if (docInfo == null) {
            return;
        }

        // Apply changes to document
        String newText = docInfo.getText();
        for (final TextDocumentContentChangeEvent change : changes) {
            newText = applyTextChange(newText, change);
        }

        // Update document info
        final DocumentInfo updatedInfo = new DocumentInfo(
            uri,
            newText,
            document.getVersion(),
            docInfo.getLanguageId()
        );

        openDocuments.put(uri, updatedInfo);

        // Validate updated document
        validateDocument(uri, newText);
    }

    /**
     * Handles document close notifications.
     *
     * @param params Document close parameters
     */
    public void didClose(final DidCloseTextDocumentParams params) {
        if (!isInitialized) {
            return;
        }

        final TextDocumentIdentifier document = params.getTextDocument();
        final URI uri = URI.create(document.getUri());

        openDocuments.remove(uri);
        moduleInspectors.remove(uri);
    }

    /**
     * Provides hover information for a position in a document.
     *
     * @param params Hover request parameters
     * @return Hover information future
     */
    public CompletableFuture<Hover> hover(final HoverParams params) {
        return CompletableFuture.supplyAsync(() -> {
            final URI uri = URI.create(params.getTextDocument().getUri());
            final Position position = params.getPosition();

            final DocumentInfo docInfo = openDocuments.get(uri);
            if (docInfo == null) {
                return null;
            }

            // Analyze the symbol at the position
            final String symbolInfo = analyzeSymbolAtPosition(docInfo.getText(), position);
            if (symbolInfo == null) {
                return null;
            }

            return new Hover(new MarkupContent(MarkupKind.MARKDOWN, symbolInfo));
        });
    }

    /**
     * Provides code completion suggestions.
     *
     * @param params Completion request parameters
     * @return Completion list future
     */
    public CompletableFuture<CompletionList> completion(final CompletionParams params) {
        return CompletableFuture.supplyAsync(() -> {
            final URI uri = URI.create(params.getTextDocument().getUri());
            final Position position = params.getPosition();

            final DocumentInfo docInfo = openDocuments.get(uri);
            if (docInfo == null) {
                return new CompletionList(false, Collections.emptyList());
            }

            final List<CompletionItem> items = generateCompletionItems(docInfo.getText(), position);
            return new CompletionList(false, items);
        });
    }

    /**
     * Provides diagnostic information for a document.
     *
     * @param params Diagnostic request parameters
     * @return Diagnostic list future
     */
    public CompletableFuture<List<Diagnostic>> diagnostic(final DocumentDiagnosticParams params) {
        return CompletableFuture.supplyAsync(() -> {
            final URI uri = URI.create(params.getTextDocument().getUri());
            final DocumentInfo docInfo = openDocuments.get(uri);

            if (docInfo == null) {
                return Collections.emptyList();
            }

            return validateDocumentAndGetDiagnostics(uri, docInfo.getText());
        });
    }

    /**
     * Provides definition location for a symbol.
     *
     * @param params Definition request parameters
     * @return Definition location future
     */
    public CompletableFuture<List<LocationLink>> definition(final DefinitionParams params) {
        return CompletableFuture.supplyAsync(() -> {
            final URI uri = URI.create(params.getTextDocument().getUri());
            final Position position = params.getPosition();

            // Find definition of symbol at position
            return findSymbolDefinition(uri, position);
        });
    }

    /**
     * Provides symbol references.
     *
     * @param params Reference request parameters
     * @return Reference locations future
     */
    public CompletableFuture<List<Location>> references(final ReferencesParams params) {
        return CompletableFuture.supplyAsync(() -> {
            final URI uri = URI.create(params.getTextDocument().getUri());
            final Position position = params.getPosition();

            // Find all references to symbol at position
            return findSymbolReferences(uri, position);
        });
    }

    /**
     * Provides document formatting.
     *
     * @param params Formatting request parameters
     * @return Text edits future
     */
    public CompletableFuture<List<TextEdit>> formatting(final DocumentFormattingParams params) {
        return CompletableFuture.supplyAsync(() -> {
            final URI uri = URI.create(params.getTextDocument().getUri());
            final DocumentInfo docInfo = openDocuments.get(uri);

            if (docInfo == null) {
                return Collections.emptyList();
            }

            // Format WebAssembly text format
            return formatWatDocument(docInfo.getText(), params.getOptions());
        });
    }

    private void validateDocument(final URI uri, final String text) {
        try {
            // Check if this is a WebAssembly binary or text file
            if (uri.getPath().endsWith(".wasm")) {
                validateWasmBinary(uri, text);
            } else if (uri.getPath().endsWith(".wat")) {
                validateWatText(uri, text);
            }
        } catch (final Exception e) {
            // Error during validation - create diagnostic
            publishDiagnostics(uri, List.of(new Diagnostic(
                new Range(new Position(0, 0), new Position(0, 0)),
                "Validation error: " + e.getMessage(),
                DiagnosticSeverity.ERROR,
                "wasmtime4j"
            )));
        }
    }

    private void validateWasmBinary(final URI uri, final String text) throws Exception {
        // Binary WebAssembly validation would require reading the file as bytes
        final Path path = Paths.get(uri);
        if (Files.exists(path)) {
            final byte[] bytes = Files.readAllBytes(path);
            final Module module = Module.fromBinary(engine, bytes);

            final ModuleInspector inspector = new ModuleInspector(module);
            moduleInspectors.put(uri, inspector);

            final ModuleInspector.ModuleValidationReport validation = inspector.validate();
            final List<Diagnostic> diagnostics = convertValidationToDiagnostics(validation);

            publishDiagnostics(uri, diagnostics);
        }
    }

    private void validateWatText(final URI uri, final String text) throws Exception {
        // WebAssembly text format validation
        // This would require a WAT parser - for now, basic syntax checking
        final List<Diagnostic> diagnostics = new ArrayList<>();

        final String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            final String line = lines[i].trim();

            // Basic syntax validation
            if (line.startsWith("(") && !line.endsWith(")") && !line.contains(";;")) {
                diagnostics.add(new Diagnostic(
                    new Range(new Position(i, 0), new Position(i, line.length())),
                    "Unclosed parenthesis",
                    DiagnosticSeverity.ERROR,
                    "wasmtime4j"
                ));
            }
        }

        publishDiagnostics(uri, diagnostics);
    }

    private List<Diagnostic> validateDocumentAndGetDiagnostics(final URI uri, final String text) {
        try {
            validateDocument(uri, text);
            return Collections.emptyList(); // Diagnostics published separately
        } catch (final Exception e) {
            return List.of(new Diagnostic(
                new Range(new Position(0, 0), new Position(0, 0)),
                "Validation error: " + e.getMessage(),
                DiagnosticSeverity.ERROR,
                "wasmtime4j"
            ));
        }
    }

    private List<Diagnostic> convertValidationToDiagnostics(final ModuleInspector.ModuleValidationReport validation) {
        final List<Diagnostic> diagnostics = new ArrayList<>();

        for (final ModuleInspector.ValidationIssue issue : validation.getIssues()) {
            final DiagnosticSeverity severity = convertSeverity(issue.getSeverity());
            diagnostics.add(new Diagnostic(
                new Range(new Position(0, 0), new Position(0, 0)), // Position would need source mapping
                issue.getMessage(),
                severity,
                "wasmtime4j"
            ));
        }

        return diagnostics;
    }

    private DiagnosticSeverity convertSeverity(final ModuleInspector.IssueSeverity severity) {
        switch (severity) {
            case CRITICAL:
            case HIGH:
                return DiagnosticSeverity.ERROR;
            case MEDIUM:
                return DiagnosticSeverity.WARNING;
            case LOW:
                return DiagnosticSeverity.INFORMATION;
            default:
                return DiagnosticSeverity.HINT;
        }
    }

    private String analyzeSymbolAtPosition(final String text, final Position position) {
        // Extract symbol at position and provide information
        final String[] lines = text.split("\n");
        if (position.getLine() >= lines.length) {
            return null;
        }

        final String line = lines[position.getLine()];
        if (position.getCharacter() >= line.length()) {
            return null;
        }

        // Simple symbol extraction (would need more sophisticated parsing)
        final String word = extractWordAtPosition(line, position.getCharacter());
        if (word == null) {
            return null;
        }

        // Provide information about WebAssembly keywords/instructions
        return getWasmSymbolInfo(word);
    }

    private String extractWordAtPosition(final String line, final int character) {
        int start = character;
        int end = character;

        // Find word boundaries
        while (start > 0 && Character.isLetterOrDigit(line.charAt(start - 1))) {
            start--;
        }
        while (end < line.length() && Character.isLetterOrDigit(line.charAt(end))) {
            end++;
        }

        if (start == end) {
            return null;
        }

        return line.substring(start, end);
    }

    private String getWasmSymbolInfo(final String symbol) {
        // Provide documentation for WebAssembly symbols
        switch (symbol) {
            case "i32":
                return "**i32** - 32-bit integer type";
            case "i64":
                return "**i64** - 64-bit integer type";
            case "f32":
                return "**f32** - 32-bit floating point type";
            case "f64":
                return "**f64** - 64-bit floating point type";
            case "func":
                return "**func** - Function declaration";
            case "module":
                return "**module** - WebAssembly module";
            case "memory":
                return "**memory** - Linear memory declaration";
            case "table":
                return "**table** - Table declaration";
            case "global":
                return "**global** - Global variable declaration";
            case "export":
                return "**export** - Export declaration";
            case "import":
                return "**import** - Import declaration";
            default:
                return null;
        }
    }

    private List<CompletionItem> generateCompletionItems(final String text, final Position position) {
        final List<CompletionItem> items = new ArrayList<>();

        // Add WebAssembly keywords
        items.add(new CompletionItem("i32", CompletionItemKind.KEYWORD, "32-bit integer type"));
        items.add(new CompletionItem("i64", CompletionItemKind.KEYWORD, "64-bit integer type"));
        items.add(new CompletionItem("f32", CompletionItemKind.KEYWORD, "32-bit floating point type"));
        items.add(new CompletionItem("f64", CompletionItemKind.KEYWORD, "64-bit floating point type"));
        items.add(new CompletionItem("func", CompletionItemKind.KEYWORD, "Function declaration"));
        items.add(new CompletionItem("module", CompletionItemKind.KEYWORD, "WebAssembly module"));
        items.add(new CompletionItem("memory", CompletionItemKind.KEYWORD, "Linear memory declaration"));
        items.add(new CompletionItem("table", CompletionItemKind.KEYWORD, "Table declaration"));
        items.add(new CompletionItem("global", CompletionItemKind.KEYWORD, "Global variable declaration"));
        items.add(new CompletionItem("export", CompletionItemKind.KEYWORD, "Export declaration"));
        items.add(new CompletionItem("import", CompletionItemKind.KEYWORD, "Import declaration"));

        // Add common instructions
        items.add(new CompletionItem("i32.add", CompletionItemKind.FUNCTION, "Add two i32 values"));
        items.add(new CompletionItem("i32.sub", CompletionItemKind.FUNCTION, "Subtract two i32 values"));
        items.add(new CompletionItem("i32.mul", CompletionItemKind.FUNCTION, "Multiply two i32 values"));
        items.add(new CompletionItem("i32.load", CompletionItemKind.FUNCTION, "Load i32 from memory"));
        items.add(new CompletionItem("i32.store", CompletionItemKind.FUNCTION, "Store i32 to memory"));

        return items;
    }

    private List<LocationLink> findSymbolDefinition(final URI uri, final Position position) {
        // Find where a symbol is defined
        // This would require symbol table and cross-reference analysis
        return Collections.emptyList();
    }

    private List<Location> findSymbolReferences(final URI uri, final Position position) {
        // Find all references to a symbol
        // This would require symbol table and cross-reference analysis
        return Collections.emptyList();
    }

    private List<TextEdit> formatWatDocument(final String text, final FormattingOptions options) {
        // Format WebAssembly text format
        // This would require a WAT formatter
        return Collections.emptyList();
    }

    private String applyTextChange(final String text, final TextDocumentContentChangeEvent change) {
        if (change.getRange() == null) {
            // Full document change
            return change.getText();
        }

        // Apply incremental change
        final Range range = change.getRange();
        final String[] lines = text.split("\n");

        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i < range.getStart().getLine() || i > range.getEnd().getLine()) {
                result.append(lines[i]);
                if (i < lines.length - 1) {
                    result.append("\n");
                }
            } else if (i == range.getStart().getLine()) {
                if (i == range.getEnd().getLine()) {
                    // Single line change
                    final String line = lines[i];
                    result.append(line.substring(0, range.getStart().getCharacter()));
                    result.append(change.getText());
                    result.append(line.substring(range.getEnd().getCharacter()));
                    if (i < lines.length - 1) {
                        result.append("\n");
                    }
                } else {
                    // Multi-line change start
                    final String line = lines[i];
                    result.append(line.substring(0, range.getStart().getCharacter()));
                    result.append(change.getText());
                }
            } else if (i == range.getEnd().getLine()) {
                // Multi-line change end
                final String line = lines[i];
                result.append(line.substring(range.getEnd().getCharacter()));
                if (i < lines.length - 1) {
                    result.append("\n");
                }
            }
            // Skip lines in the middle of a multi-line change
        }

        return result.toString();
    }

    private void publishDiagnostics(final URI uri, final List<Diagnostic> diagnostics) {
        // In a real implementation, this would send diagnostics to the client
        // For now, just store them
    }

    private LanguageServerCapabilities createCapabilities() {
        final LanguageServerCapabilities capabilities = new LanguageServerCapabilities();

        // Text document sync
        capabilities.setTextDocumentSync(TextDocumentSyncKind.INCREMENTAL);

        // Hover support
        capabilities.setHoverProvider(true);

        // Completion support
        final CompletionOptions completionOptions = new CompletionOptions();
        completionOptions.setResolveProvider(false);
        completionOptions.setTriggerCharacters(List.of(".", "(", " "));
        capabilities.setCompletionProvider(completionOptions);

        // Diagnostic support
        final DiagnosticOptions diagnosticOptions = new DiagnosticOptions();
        diagnosticOptions.setInterFileDependencies(false);
        diagnosticOptions.setWorkspaceDiagnostics(false);
        capabilities.setDiagnosticProvider(diagnosticOptions);

        // Definition support
        capabilities.setDefinitionProvider(true);

        // References support
        capabilities.setReferencesProvider(true);

        // Formatting support
        capabilities.setDocumentFormattingProvider(true);

        return capabilities;
    }

    // LSP data structures (simplified representations)

    public static final class InitializeResult {
        private final LanguageServerCapabilities capabilities;

        public InitializeResult(final LanguageServerCapabilities capabilities) {
            this.capabilities = capabilities;
        }

        public LanguageServerCapabilities getCapabilities() { return capabilities; }
    }

    public static final class LanguageServerCapabilities {
        private TextDocumentSyncKind textDocumentSync;
        private boolean hoverProvider;
        private CompletionOptions completionProvider;
        private DiagnosticOptions diagnosticProvider;
        private boolean definitionProvider;
        private boolean referencesProvider;
        private boolean documentFormattingProvider;

        public TextDocumentSyncKind getTextDocumentSync() { return textDocumentSync; }
        public void setTextDocumentSync(final TextDocumentSyncKind textDocumentSync) { this.textDocumentSync = textDocumentSync; }
        public boolean isHoverProvider() { return hoverProvider; }
        public void setHoverProvider(final boolean hoverProvider) { this.hoverProvider = hoverProvider; }
        public CompletionOptions getCompletionProvider() { return completionProvider; }
        public void setCompletionProvider(final CompletionOptions completionProvider) { this.completionProvider = completionProvider; }
        public DiagnosticOptions getDiagnosticProvider() { return diagnosticProvider; }
        public void setDiagnosticProvider(final DiagnosticOptions diagnosticProvider) { this.diagnosticProvider = diagnosticProvider; }
        public boolean isDefinitionProvider() { return definitionProvider; }
        public void setDefinitionProvider(final boolean definitionProvider) { this.definitionProvider = definitionProvider; }
        public boolean isReferencesProvider() { return referencesProvider; }
        public void setReferencesProvider(final boolean referencesProvider) { this.referencesProvider = referencesProvider; }
        public boolean isDocumentFormattingProvider() { return documentFormattingProvider; }
        public void setDocumentFormattingProvider(final boolean documentFormattingProvider) { this.documentFormattingProvider = documentFormattingProvider; }
    }

    public static final class CompletionOptions {
        private boolean resolveProvider;
        private List<String> triggerCharacters;

        public boolean isResolveProvider() { return resolveProvider; }
        public void setResolveProvider(final boolean resolveProvider) { this.resolveProvider = resolveProvider; }
        public List<String> getTriggerCharacters() { return triggerCharacters; }
        public void setTriggerCharacters(final List<String> triggerCharacters) { this.triggerCharacters = triggerCharacters; }
    }

    public static final class DiagnosticOptions {
        private boolean interFileDependencies;
        private boolean workspaceDiagnostics;

        public boolean isInterFileDependencies() { return interFileDependencies; }
        public void setInterFileDependencies(final boolean interFileDependencies) { this.interFileDependencies = interFileDependencies; }
        public boolean isWorkspaceDiagnostics() { return workspaceDiagnostics; }
        public void setWorkspaceDiagnostics(final boolean workspaceDiagnostics) { this.workspaceDiagnostics = workspaceDiagnostics; }
    }

    public static final class DocumentInfo {
        private final URI uri;
        private final String text;
        private final int version;
        private final String languageId;

        public DocumentInfo(final URI uri, final String text, final int version, final String languageId) {
            this.uri = uri;
            this.text = text;
            this.version = version;
            this.languageId = languageId;
        }

        public URI getUri() { return uri; }
        public String getText() { return text; }
        public int getVersion() { return version; }
        public String getLanguageId() { return languageId; }
    }

    // Additional LSP data structures would be defined here
    // (Position, Range, Diagnostic, CompletionItem, etc.)

    public enum TextDocumentSyncKind {
        NONE, FULL, INCREMENTAL
    }

    public enum DiagnosticSeverity {
        ERROR, WARNING, INFORMATION, HINT
    }

    public enum CompletionItemKind {
        TEXT, METHOD, FUNCTION, CONSTRUCTOR, FIELD, VARIABLE, CLASS, INTERFACE,
        MODULE, PROPERTY, UNIT, VALUE, ENUM, KEYWORD, SNIPPET, COLOR, FILE,
        REFERENCE, FOLDER, ENUM_MEMBER, CONSTANT, STRUCT, EVENT, OPERATOR, TYPE_PARAMETER
    }

    public enum MarkupKind {
        PLAINTEXT, MARKDOWN
    }

    // Simplified LSP data structures
    public static final class ClientCapabilities {
        // Client capability information
    }

    public static final class Position {
        private final int line;
        private final int character;

        public Position(final int line, final int character) {
            this.line = line;
            this.character = character;
        }

        public int getLine() { return line; }
        public int getCharacter() { return character; }
    }

    public static final class Range {
        private final Position start;
        private final Position end;

        public Range(final Position start, final Position end) {
            this.start = start;
            this.end = end;
        }

        public Position getStart() { return start; }
        public Position getEnd() { return end; }
    }

    public static final class Diagnostic {
        private final Range range;
        private final String message;
        private final DiagnosticSeverity severity;
        private final String source;

        public Diagnostic(final Range range, final String message, final DiagnosticSeverity severity, final String source) {
            this.range = range;
            this.message = message;
            this.severity = severity;
            this.source = source;
        }

        public Range getRange() { return range; }
        public String getMessage() { return message; }
        public DiagnosticSeverity getSeverity() { return severity; }
        public String getSource() { return source; }
    }

    public static final class CompletionItem {
        private final String label;
        private final CompletionItemKind kind;
        private final String detail;

        public CompletionItem(final String label, final CompletionItemKind kind, final String detail) {
            this.label = label;
            this.kind = kind;
            this.detail = detail;
        }

        public String getLabel() { return label; }
        public CompletionItemKind getKind() { return kind; }
        public String getDetail() { return detail; }
    }

    public static final class CompletionList {
        private final boolean isIncomplete;
        private final List<CompletionItem> items;

        public CompletionList(final boolean isIncomplete, final List<CompletionItem> items) {
            this.isIncomplete = isIncomplete;
            this.items = items;
        }

        public boolean isIncomplete() { return isIncomplete; }
        public List<CompletionItem> getItems() { return items; }
    }

    public static final class Hover {
        private final MarkupContent contents;

        public Hover(final MarkupContent contents) {
            this.contents = contents;
        }

        public MarkupContent getContents() { return contents; }
    }

    public static final class MarkupContent {
        private final MarkupKind kind;
        private final String value;

        public MarkupContent(final MarkupKind kind, final String value) {
            this.kind = kind;
            this.value = value;
        }

        public MarkupKind getKind() { return kind; }
        public String getValue() { return value; }
    }

    public static final class Location {
        private final URI uri;
        private final Range range;

        public Location(final URI uri, final Range range) {
            this.uri = uri;
            this.range = range;
        }

        public URI getUri() { return uri; }
        public Range getRange() { return range; }
    }

    public static final class LocationLink {
        private final URI targetUri;
        private final Range targetRange;
        private final Range targetSelectionRange;

        public LocationLink(final URI targetUri, final Range targetRange, final Range targetSelectionRange) {
            this.targetUri = targetUri;
            this.targetRange = targetRange;
            this.targetSelectionRange = targetSelectionRange;
        }

        public URI getTargetUri() { return targetUri; }
        public Range getTargetRange() { return targetRange; }
        public Range getTargetSelectionRange() { return targetSelectionRange; }
    }

    public static final class TextEdit {
        private final Range range;
        private final String newText;

        public TextEdit(final Range range, final String newText) {
            this.range = range;
            this.newText = newText;
        }

        public Range getRange() { return range; }
        public String getNewText() { return newText; }
    }

    public static final class FormattingOptions {
        private final int tabSize;
        private final boolean insertSpaces;

        public FormattingOptions(final int tabSize, final boolean insertSpaces) {
            this.tabSize = tabSize;
            this.insertSpaces = insertSpaces;
        }

        public int getTabSize() { return tabSize; }
        public boolean isInsertSpaces() { return insertSpaces; }
    }

    // LSP request parameter classes
    public static final class TextDocumentItem {
        private final String uri;
        private final String languageId;
        private final int version;
        private final String text;

        public TextDocumentItem(final String uri, final String languageId, final int version, final String text) {
            this.uri = uri;
            this.languageId = languageId;
            this.version = version;
            this.text = text;
        }

        public String getUri() { return uri; }
        public String getLanguageId() { return languageId; }
        public int getVersion() { return version; }
        public String getText() { return text; }
    }

    public static final class TextDocumentIdentifier {
        private final String uri;

        public TextDocumentIdentifier(final String uri) {
            this.uri = uri;
        }

        public String getUri() { return uri; }
    }

    public static final class VersionedTextDocumentIdentifier extends TextDocumentIdentifier {
        private final int version;

        public VersionedTextDocumentIdentifier(final String uri, final int version) {
            super(uri);
            this.version = version;
        }

        public int getVersion() { return version; }
    }

    public static final class TextDocumentContentChangeEvent {
        private final Range range;
        private final String text;

        public TextDocumentContentChangeEvent(final Range range, final String text) {
            this.range = range;
            this.text = text;
        }

        public Range getRange() { return range; }
        public String getText() { return text; }
    }

    public static final class DidOpenTextDocumentParams {
        private final TextDocumentItem textDocument;

        public DidOpenTextDocumentParams(final TextDocumentItem textDocument) {
            this.textDocument = textDocument;
        }

        public TextDocumentItem getTextDocument() { return textDocument; }
    }

    public static final class DidChangeTextDocumentParams {
        private final VersionedTextDocumentIdentifier textDocument;
        private final List<TextDocumentContentChangeEvent> contentChanges;

        public DidChangeTextDocumentParams(final VersionedTextDocumentIdentifier textDocument,
                                         final List<TextDocumentContentChangeEvent> contentChanges) {
            this.textDocument = textDocument;
            this.contentChanges = contentChanges;
        }

        public VersionedTextDocumentIdentifier getTextDocument() { return textDocument; }
        public List<TextDocumentContentChangeEvent> getContentChanges() { return contentChanges; }
    }

    public static final class DidCloseTextDocumentParams {
        private final TextDocumentIdentifier textDocument;

        public DidCloseTextDocumentParams(final TextDocumentIdentifier textDocument) {
            this.textDocument = textDocument;
        }

        public TextDocumentIdentifier getTextDocument() { return textDocument; }
    }

    public static final class HoverParams {
        private final TextDocumentIdentifier textDocument;
        private final Position position;

        public HoverParams(final TextDocumentIdentifier textDocument, final Position position) {
            this.textDocument = textDocument;
            this.position = position;
        }

        public TextDocumentIdentifier getTextDocument() { return textDocument; }
        public Position getPosition() { return position; }
    }

    public static final class CompletionParams {
        private final TextDocumentIdentifier textDocument;
        private final Position position;

        public CompletionParams(final TextDocumentIdentifier textDocument, final Position position) {
            this.textDocument = textDocument;
            this.position = position;
        }

        public TextDocumentIdentifier getTextDocument() { return textDocument; }
        public Position getPosition() { return position; }
    }

    public static final class DocumentDiagnosticParams {
        private final TextDocumentIdentifier textDocument;

        public DocumentDiagnosticParams(final TextDocumentIdentifier textDocument) {
            this.textDocument = textDocument;
        }

        public TextDocumentIdentifier getTextDocument() { return textDocument; }
    }

    public static final class DefinitionParams {
        private final TextDocumentIdentifier textDocument;
        private final Position position;

        public DefinitionParams(final TextDocumentIdentifier textDocument, final Position position) {
            this.textDocument = textDocument;
            this.position = position;
        }

        public TextDocumentIdentifier getTextDocument() { return textDocument; }
        public Position getPosition() { return position; }
    }

    public static final class ReferencesParams {
        private final TextDocumentIdentifier textDocument;
        private final Position position;

        public ReferencesParams(final TextDocumentIdentifier textDocument, final Position position) {
            this.textDocument = textDocument;
            this.position = position;
        }

        public TextDocumentIdentifier getTextDocument() { return textDocument; }
        public Position getPosition() { return position; }
    }

    public static final class DocumentFormattingParams {
        private final TextDocumentIdentifier textDocument;
        private final FormattingOptions options;

        public DocumentFormattingParams(final TextDocumentIdentifier textDocument, final FormattingOptions options) {
            this.textDocument = textDocument;
            this.options = options;
        }

        public TextDocumentIdentifier getTextDocument() { return textDocument; }
        public FormattingOptions getOptions() { return options; }
    }
}
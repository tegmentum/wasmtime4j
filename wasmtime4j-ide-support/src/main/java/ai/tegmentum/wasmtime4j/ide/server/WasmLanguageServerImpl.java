package ai.tegmentum.wasmtime4j.ide.server;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.dev.ModuleInspector;
import ai.tegmentum.wasmtime4j.dev.DebuggingSession;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Language Server Protocol implementation for WebAssembly development.
 * Provides comprehensive IDE integration including syntax validation,
 * code completion, hover information, and debugging support.
 */
public final class WasmLanguageServerImpl implements LanguageServer, LanguageClientAware {

    private static final Logger LOGGER = Logger.getLogger(WasmLanguageServerImpl.class.getName());

    private LanguageClient client;
    private final Engine engine;
    private final Map<String, TextDocumentItem> openDocuments;
    private final Map<String, ModuleInspector> moduleInspectors;
    private final Map<String, DebuggingSession> debugSessions;
    private final WasmTextDocumentService textDocumentService;
    private final WasmWorkspaceService workspaceService;
    private ClientCapabilities clientCapabilities;
    private boolean isInitialized = false;

    /**
     * Creates a new WebAssembly Language Server implementation.
     */
    public WasmLanguageServerImpl() {
        this.engine = Engine.newEngine();
        this.openDocuments = new ConcurrentHashMap<>();
        this.moduleInspectors = new ConcurrentHashMap<>();
        this.debugSessions = new ConcurrentHashMap<>();
        this.textDocumentService = new WasmTextDocumentService(this);
        this.workspaceService = new WasmWorkspaceService(this);
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(final InitializeParams params) {
        LOGGER.info("Initializing WebAssembly Language Server");

        this.clientCapabilities = params.getCapabilities();

        final ServerCapabilities capabilities = createServerCapabilities();
        final InitializeResult result = new InitializeResult(capabilities);

        // Set server info
        final ServerInfo serverInfo = new ServerInfo();
        serverInfo.setName("WebAssembly Language Server");
        serverInfo.setVersion("1.0.0");
        result.setServerInfo(serverInfo);

        isInitialized = true;

        LOGGER.info("WebAssembly Language Server initialized");
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public void initialized(final InitializedParams params) {
        LOGGER.info("WebAssembly Language Server initialization complete");

        // Perform post-initialization setup
        if (client != null) {
            // Send initial notifications to client if needed
            client.logMessage(new MessageParams(MessageType.Info,
                "WebAssembly Language Server is ready"));
        }
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        LOGGER.info("Shutting down WebAssembly Language Server");

        return CompletableFuture.runAsync(() -> {
            // Close all debug sessions
            for (final DebuggingSession session : debugSessions.values()) {
                try {
                    session.close();
                } catch (final Exception e) {
                    LOGGER.log(Level.WARNING, "Error closing debug session", e);
                }
            }
            debugSessions.clear();

            // Clear document information
            openDocuments.clear();
            moduleInspectors.clear();

            isInitialized = false;
            LOGGER.info("WebAssembly Language Server shutdown complete");
        }).thenApply(v -> new Object());
    }

    @Override
    public void exit() {
        LOGGER.info("WebAssembly Language Server exiting");
        System.exit(isInitialized ? 1 : 0);
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }

    @Override
    public void connect(final LanguageClient client) {
        this.client = client;
        LOGGER.info("Connected to language client");
    }

    /**
     * Gets the language client for communication.
     *
     * @return Language client
     */
    public LanguageClient getClient() {
        return client;
    }

    /**
     * Gets the WebAssembly engine.
     *
     * @return WebAssembly engine
     */
    public Engine getEngine() {
        return engine;
    }

    /**
     * Gets all open documents.
     *
     * @return Map of document URI to document item
     */
    public Map<String, TextDocumentItem> getOpenDocuments() {
        return Collections.unmodifiableMap(openDocuments);
    }

    /**
     * Adds an open document.
     *
     * @param document Document to add
     */
    public void addOpenDocument(final TextDocumentItem document) {
        openDocuments.put(document.getUri(), document);
        analyzeDocument(document);
    }

    /**
     * Updates an open document.
     *
     * @param uri Document URI
     * @param newText New document text
     * @param version New document version
     */
    public void updateOpenDocument(final String uri, final String newText, final int version) {
        final TextDocumentItem existing = openDocuments.get(uri);
        if (existing != null) {
            final TextDocumentItem updated = new TextDocumentItem(
                uri,
                existing.getLanguageId(),
                version,
                newText
            );
            openDocuments.put(uri, updated);
            analyzeDocument(updated);
        }
    }

    /**
     * Removes an open document.
     *
     * @param uri Document URI to remove
     */
    public void removeOpenDocument(final String uri) {
        openDocuments.remove(uri);
        moduleInspectors.remove(uri);
    }

    /**
     * Gets the module inspector for a document.
     *
     * @param uri Document URI
     * @return Module inspector or null if not available
     */
    public ModuleInspector getModuleInspector(final String uri) {
        return moduleInspectors.get(uri);
    }

    /**
     * Creates a debugging session for a document.
     *
     * @param uri Document URI
     * @return Debugging session ID or null if failed
     */
    public String createDebugSession(final String uri) {
        final ModuleInspector inspector = moduleInspectors.get(uri);
        if (inspector == null) {
            return null;
        }

        try {
            // In a real implementation, this would create an instance
            // For now, we'll simulate it
            final DebuggingSession.DebuggerConfig config = new DebuggingSession.DebuggerConfig(
                true, true, true, 1000
            );

            // This would need actual Store and Instance objects
            // final DebuggingSession session = new DebuggingSession(engine, store, module, instance, config);
            // debugSessions.put(session.getSessionId(), session);
            // return session.getSessionId();

            return null; // Disabled until we have proper instance creation
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to create debug session for " + uri, e);
            return null;
        }
    }

    private ServerCapabilities createServerCapabilities() {
        final ServerCapabilities capabilities = new ServerCapabilities();

        // Text document synchronization
        final TextDocumentSyncOptions syncOptions = new TextDocumentSyncOptions();
        syncOptions.setOpenClose(true);
        syncOptions.setChange(TextDocumentSyncKind.Incremental);
        capabilities.setTextDocumentSync(syncOptions);

        // Hover support
        capabilities.setHoverProvider(Either.forLeft(true));

        // Completion support
        final CompletionOptions completionOptions = new CompletionOptions();
        completionOptions.setResolveProvider(false);
        completionOptions.setTriggerCharacters(Arrays.asList(".", "(", " "));
        capabilities.setCompletionProvider(completionOptions);

        // Diagnostic support
        final DiagnosticOptions diagnosticOptions = new DiagnosticOptions();
        diagnosticOptions.setInterFileDependencies(false);
        diagnosticOptions.setWorkspaceDiagnostics(false);
        capabilities.setDiagnosticProvider(Either.forRight(diagnosticOptions));

        // Definition support
        capabilities.setDefinitionProvider(Either.forLeft(true));

        // References support
        capabilities.setReferencesProvider(Either.forLeft(true));

        // Document formatting support
        capabilities.setDocumentFormattingProvider(Either.forLeft(true));

        // Document symbols support
        capabilities.setDocumentSymbolProvider(Either.forLeft(true));

        // Workspace symbols support
        capabilities.setWorkspaceSymbolProvider(Either.forLeft(true));

        // Code action support
        final CodeActionOptions codeActionOptions = new CodeActionOptions();
        codeActionOptions.setCodeActionKinds(Arrays.asList(
            CodeActionKind.QuickFix,
            CodeActionKind.Refactor,
            CodeActionKind.Source
        ));
        capabilities.setCodeActionProvider(Either.forRight(codeActionOptions));

        return capabilities;
    }

    private void analyzeDocument(final TextDocumentItem document) {
        try {
            final String uri = document.getUri();
            final String text = document.getText();

            // Analyze WebAssembly modules
            if (uri.endsWith(".wasm")) {
                analyzeBinaryModule(uri, text);
            } else if (uri.endsWith(".wat")) {
                analyzeTextModule(uri, text);
            }
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Error analyzing document: " + document.getUri(), e);

            // Send error diagnostic to client
            if (client != null) {
                final List<Diagnostic> diagnostics = Arrays.asList(
                    new Diagnostic(
                        new Range(new Position(0, 0), new Position(0, 0)),
                        "Analysis error: " + e.getMessage(),
                        DiagnosticSeverity.Error,
                        "wasmtime4j"
                    )
                );

                client.publishDiagnostics(new PublishDiagnosticsParams(
                    document.getUri(),
                    diagnostics
                ));
            }
        }
    }

    private void analyzeBinaryModule(final String uri, final String text) throws Exception {
        // For binary files, we need to read the actual file content
        final Path path = Paths.get(URI.create(uri));
        if (Files.exists(path)) {
            final byte[] bytes = Files.readAllBytes(path);
            final Module module = Module.fromBinary(engine, bytes);
            final ModuleInspector inspector = new ModuleInspector(module);

            moduleInspectors.put(uri, inspector);

            // Validate and send diagnostics
            validateAndPublishDiagnostics(uri, inspector);
        }
    }

    private void analyzeTextModule(final String uri, final String text) throws Exception {
        // For WAT files, we would need to parse the text format
        // For now, perform basic syntax validation
        final List<Diagnostic> diagnostics = new ArrayList<>();

        final String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            final String line = lines[i].trim();

            // Basic syntax validation
            if (line.startsWith("(") && !line.endsWith(")") && !line.contains(";;")) {
                diagnostics.add(new Diagnostic(
                    new Range(new Position(i, 0), new Position(i, line.length())),
                    "Unclosed parenthesis",
                    DiagnosticSeverity.Error,
                    "wasmtime4j"
                ));
            }

            // Check for common WAT keywords
            if (line.contains("module") && !line.startsWith("(module")) {
                diagnostics.add(new Diagnostic(
                    new Range(new Position(i, 0), new Position(i, line.length())),
                    "Module declaration should start with '(module'",
                    DiagnosticSeverity.Warning,
                    "wasmtime4j"
                ));
            }
        }

        // Publish diagnostics to client
        if (client != null) {
            client.publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics));
        }
    }

    private void validateAndPublishDiagnostics(final String uri, final ModuleInspector inspector) {
        final ModuleInspector.ModuleValidationReport validation = inspector.validate();
        final List<Diagnostic> diagnostics = new ArrayList<>();

        for (final ModuleInspector.ValidationIssue issue : validation.getIssues()) {
            final DiagnosticSeverity severity;
            switch (issue.getSeverity()) {
                case CRITICAL:
                case HIGH:
                    severity = DiagnosticSeverity.Error;
                    break;
                case MEDIUM:
                    severity = DiagnosticSeverity.Warning;
                    break;
                case LOW:
                    severity = DiagnosticSeverity.Information;
                    break;
                default:
                    severity = DiagnosticSeverity.Hint;
                    break;
            }

            diagnostics.add(new Diagnostic(
                new Range(
                    new Position(issue.getLine(), issue.getColumn()),
                    new Position(issue.getLine(), issue.getColumn() + 1)
                ),
                issue.getMessage(),
                severity,
                "wasmtime4j"
            ));
        }

        // Publish diagnostics to client
        if (client != null) {
            client.publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics));
        }
    }
}
package ai.tegmentum.wasmtime4j.ide.server;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.WorkspaceService;

/**
 * Workspace service implementation for WebAssembly language server. Handles workspace-level
 * operations like workspace symbols, file watching, etc.
 */
public final class WasmWorkspaceService implements WorkspaceService {

  private static final Logger LOGGER = Logger.getLogger(WasmWorkspaceService.class.getName());

  private final WasmLanguageServerImpl server;

  /**
   * Creates a new WebAssembly workspace service.
   *
   * @param server Parent language server
   */
  public WasmWorkspaceService(final WasmLanguageServerImpl server) {
    this.server = Objects.requireNonNull(server, "Server cannot be null");
  }

  @Override
  public CompletableFuture<List<? extends SymbolInformation>> symbol(
      final WorkspaceSymbolParams params) {
    return CompletableFuture.supplyAsync(
        () -> {
          final String query = params.getQuery();
          LOGGER.fine("Workspace symbols request for query: " + query);

          final List<SymbolInformation> symbols = new ArrayList<>();

          // Search for symbols across all open documents
          for (final TextDocumentItem document : server.getOpenDocuments().values()) {
            symbols.addAll(extractWorkspaceSymbols(document, query));
          }

          return symbols;
        });
  }

  @Override
  public void didChangeConfiguration(final DidChangeConfigurationParams params) {
    LOGGER.info("Configuration changed");

    // Handle configuration changes
    final Object settings = params.getSettings();
    if (settings instanceof Map) {
      @SuppressWarnings("unchecked")
      final Map<String, Object> settingsMap = (Map<String, Object>) settings;
      processConfigurationSettings(settingsMap);
    }
  }

  @Override
  public void didChangeWatchedFiles(final DidChangeWatchedFilesParams params) {
    LOGGER.fine("Watched files changed");

    for (final FileEvent change : params.getChanges()) {
      final String uri = change.getUri();
      final FileChangeType changeType = change.getType();

      LOGGER.fine("File change: " + uri + " (" + changeType + ")");

      switch (changeType) {
        case Created:
          handleFileCreated(uri);
          break;
        case Changed:
          handleFileChanged(uri);
          break;
        case Deleted:
          handleFileDeleted(uri);
          break;
        default:
          LOGGER.warning("Unknown file change type: " + changeType);
          break;
      }
    }
  }

  @Override
  public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(
      final CodeActionParams params) {
    return CompletableFuture.supplyAsync(
        () -> {
          final String uri = params.getTextDocument().getUri();
          final Range range = params.getRange();
          final CodeActionContext context = params.getContext();

          LOGGER.fine("Workspace code action request for " + uri);

          final List<Either<Command, CodeAction>> actions = new ArrayList<>();

          // Generate workspace-level code actions
          actions.addAll(generateWorkspaceCodeActions(uri, range, context));

          return actions;
        });
  }

  @Override
  public CompletableFuture<Object> executeCommand(final ExecuteCommandParams params) {
    return CompletableFuture.supplyAsync(
        () -> {
          final String command = params.getCommand();
          final List<Object> arguments = params.getArguments();

          LOGGER.info("Execute command: " + command);

          switch (command) {
            case "wasmtime4j.validate.module":
              return executeValidateModule(arguments);

            case "wasmtime4j.debug.start":
              return executeStartDebug(arguments);

            case "wasmtime4j.debug.stop":
              return executeStopDebug(arguments);

            case "wasmtime4j.analyze.performance":
              return executeAnalyzePerformance(arguments);

            case "wasmtime4j.generate.bindings":
              return executeGenerateBindings(arguments);

            default:
              LOGGER.warning("Unknown command: " + command);
              return null;
          }
        });
  }

  private List<SymbolInformation> extractWorkspaceSymbols(
      final TextDocumentItem document, final String query) {
    final List<SymbolInformation> symbols = new ArrayList<>();
    final String uri = document.getUri();
    final String text = document.getText();

    // Skip if document doesn't match query
    if (!query.isEmpty() && !text.toLowerCase().contains(query.toLowerCase())) {
      return symbols;
    }

    final String[] lines = text.split("\n");
    for (int i = 0; i < lines.length; i++) {
      final String line = lines[i].trim();

      // Extract function symbols
      if (line.startsWith("(func")) {
        final String functionName = extractFunctionName(line);
        if (functionName != null && matchesQuery(functionName, query)) {
          final SymbolInformation symbol = new SymbolInformation();
          symbol.setName(functionName);
          symbol.setKind(SymbolKind.Function);
          symbol.setLocation(
              new Location(uri, new Range(new Position(i, 0), new Position(i, line.length()))));
          symbols.add(symbol);
        }
      }

      // Extract global symbols
      if (line.startsWith("(global")) {
        final String globalName = extractGlobalName(line);
        if (globalName != null && matchesQuery(globalName, query)) {
          final SymbolInformation symbol = new SymbolInformation();
          symbol.setName(globalName);
          symbol.setKind(SymbolKind.Variable);
          symbol.setLocation(
              new Location(uri, new Range(new Position(i, 0), new Position(i, line.length()))));
          symbols.add(symbol);
        }
      }

      // Extract memory symbols
      if (line.startsWith("(memory")) {
        final String memoryName = extractMemoryName(line);
        if (memoryName != null && matchesQuery(memoryName, query)) {
          final SymbolInformation symbol = new SymbolInformation();
          symbol.setName(memoryName);
          symbol.setKind(SymbolKind.Array);
          symbol.setLocation(
              new Location(uri, new Range(new Position(i, 0), new Position(i, line.length()))));
          symbols.add(symbol);
        }
      }

      // Extract table symbols
      if (line.startsWith("(table")) {
        final String tableName = extractTableName(line);
        if (tableName != null && matchesQuery(tableName, query)) {
          final SymbolInformation symbol = new SymbolInformation();
          symbol.setName(tableName);
          symbol.setKind(SymbolKind.Array);
          symbol.setLocation(
              new Location(uri, new Range(new Position(i, 0), new Position(i, line.length()))));
          symbols.add(symbol);
        }
      }
    }

    return symbols;
  }

  private String extractFunctionName(final String line) {
    // Extract function name from WAT syntax: (func $name ...)
    final int dollarIndex = line.indexOf('$');
    if (dollarIndex == -1) {
      return null;
    }

    final int spaceIndex = line.indexOf(' ', dollarIndex);
    if (spaceIndex == -1) {
      final int parenIndex = line.indexOf(')', dollarIndex);
      if (parenIndex == -1) {
        return line.substring(dollarIndex + 1);
      }
      return line.substring(dollarIndex + 1, parenIndex);
    }

    return line.substring(dollarIndex + 1, spaceIndex);
  }

  private String extractGlobalName(final String line) {
    // Similar extraction logic for globals
    return extractNameFromLine(line);
  }

  private String extractMemoryName(final String line) {
    // Similar extraction logic for memory
    return extractNameFromLine(line);
  }

  private String extractTableName(final String line) {
    // Similar extraction logic for tables
    return extractNameFromLine(line);
  }

  private String extractNameFromLine(final String line) {
    final int dollarIndex = line.indexOf('$');
    if (dollarIndex == -1) {
      return null;
    }

    final int spaceIndex = line.indexOf(' ', dollarIndex);
    final int parenIndex = line.indexOf(')', dollarIndex);

    int endIndex = line.length();
    if (spaceIndex != -1 && (parenIndex == -1 || spaceIndex < parenIndex)) {
      endIndex = spaceIndex;
    } else if (parenIndex != -1) {
      endIndex = parenIndex;
    }

    return line.substring(dollarIndex + 1, endIndex);
  }

  private boolean matchesQuery(final String name, final String query) {
    if (query.isEmpty()) {
      return true;
    }
    return name.toLowerCase().contains(query.toLowerCase());
  }

  private void processConfigurationSettings(final Map<String, Object> settings) {
    // Process WebAssembly-specific configuration settings
    final Object wasmSettings = settings.get("wasmtime4j");
    if (wasmSettings instanceof Map) {
      @SuppressWarnings("unchecked")
      final Map<String, Object> wasmConfig = (Map<String, Object>) wasmSettings;

      // Process validation settings
      final Object validation = wasmConfig.get("validation");
      if (validation instanceof Map) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> validationConfig = (Map<String, Object>) validation;
        processValidationSettings(validationConfig);
      }

      // Process debugging settings
      final Object debugging = wasmConfig.get("debugging");
      if (debugging instanceof Map) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> debugConfig = (Map<String, Object>) debugging;
        processDebuggingSettings(debugConfig);
      }

      // Process formatting settings
      final Object formatting = wasmConfig.get("formatting");
      if (formatting instanceof Map) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> formatConfig = (Map<String, Object>) formatting;
        processFormattingSettings(formatConfig);
      }
    }
  }

  private void processValidationSettings(final Map<String, Object> settings) {
    // Process validation-specific settings
    LOGGER.info("Updated validation settings");
  }

  private void processDebuggingSettings(final Map<String, Object> settings) {
    // Process debugging-specific settings
    LOGGER.info("Updated debugging settings");
  }

  private void processFormattingSettings(final Map<String, Object> settings) {
    // Process formatting-specific settings
    LOGGER.info("Updated formatting settings");
  }

  private void handleFileCreated(final String uri) {
    LOGGER.fine("File created: " + uri);

    // If it's a WebAssembly file, we might want to analyze it
    if (uri.endsWith(".wasm") || uri.endsWith(".wat")) {
      // Could trigger analysis or update workspace symbols
    }
  }

  private void handleFileChanged(final String uri) {
    LOGGER.fine("File changed: " + uri);

    // Re-analyze changed files that might affect open documents
    if (uri.endsWith(".wasm") || uri.endsWith(".wat")) {
      // Could trigger re-analysis
    }
  }

  private void handleFileDeleted(final String uri) {
    LOGGER.fine("File deleted: " + uri);

    // Clean up any cached information for deleted files
    server.removeOpenDocument(uri);
  }

  private List<Either<Command, CodeAction>> generateWorkspaceCodeActions(
      final String uri, final Range range, final CodeActionContext context) {
    final List<Either<Command, CodeAction>> actions = new ArrayList<>();

    // Add WebAssembly-specific workspace actions
    actions.add(
        Either.forLeft(
            new Command(
                "Validate WebAssembly Module", "wasmtime4j.validate.module", Arrays.asList(uri))));

    actions.add(
        Either.forLeft(
            new Command("Start Debug Session", "wasmtime4j.debug.start", Arrays.asList(uri))));

    actions.add(
        Either.forLeft(
            new Command(
                "Analyze Performance", "wasmtime4j.analyze.performance", Arrays.asList(uri))));

    actions.add(
        Either.forLeft(
            new Command(
                "Generate Language Bindings", "wasmtime4j.generate.bindings", Arrays.asList(uri))));

    return actions;
  }

  private Object executeValidateModule(final List<Object> arguments) {
    if (arguments.isEmpty()) {
      return "No module URI provided";
    }

    final String uri = arguments.get(0).toString();
    LOGGER.info("Validating module: " + uri);

    try {
      // Validate the WebAssembly module
      final TextDocumentItem document = server.getOpenDocuments().get(uri);
      if (document != null) {
        // Re-trigger analysis which will publish diagnostics
        server.updateOpenDocument(uri, document.getText(), document.getVersion());
        return "Module validation completed";
      } else {
        return "Module not found or not open";
      }
    } catch (final Exception e) {
      LOGGER.severe("Module validation failed: " + e.getMessage());
      return "Module validation failed: " + e.getMessage();
    }
  }

  private Object executeStartDebug(final List<Object> arguments) {
    if (arguments.isEmpty()) {
      return "No module URI provided";
    }

    final String uri = arguments.get(0).toString();
    LOGGER.info("Starting debug session for: " + uri);

    try {
      final String sessionId = server.createDebugSession(uri);
      if (sessionId != null) {
        return "Debug session started: " + sessionId;
      } else {
        return "Failed to start debug session";
      }
    } catch (final Exception e) {
      LOGGER.severe("Failed to start debug session: " + e.getMessage());
      return "Failed to start debug session: " + e.getMessage();
    }
  }

  private Object executeStopDebug(final List<Object> arguments) {
    if (arguments.isEmpty()) {
      return "No session ID provided";
    }

    final String sessionId = arguments.get(0).toString();
    LOGGER.info("Stopping debug session: " + sessionId);

    // In a real implementation, this would stop the debug session
    return "Debug session stopped: " + sessionId;
  }

  private Object executeAnalyzePerformance(final List<Object> arguments) {
    if (arguments.isEmpty()) {
      return "No module URI provided";
    }

    final String uri = arguments.get(0).toString();
    LOGGER.info("Analyzing performance for: " + uri);

    // In a real implementation, this would analyze module performance
    return "Performance analysis completed for: " + uri;
  }

  private Object executeGenerateBindings(final List<Object> arguments) {
    if (arguments.isEmpty()) {
      return "No module URI provided";
    }

    final String uri = arguments.get(0).toString();
    LOGGER.info("Generating bindings for: " + uri);

    // In a real implementation, this would generate language bindings
    return "Language bindings generated for: " + uri;
  }
}

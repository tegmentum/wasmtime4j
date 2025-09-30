package ai.tegmentum.wasmtime4j.ide.server;

import ai.tegmentum.wasmtime4j.dev.ModuleInspector;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.TextDocumentService;

/**
 * Text document service implementation for WebAssembly language server. Handles document-specific
 * operations like completion, hover, diagnostics, etc.
 */
public final class WasmTextDocumentService implements TextDocumentService {

  private static final Logger LOGGER = Logger.getLogger(WasmTextDocumentService.class.getName());

  private final WasmLanguageServerImpl server;

  /**
   * Creates a new WebAssembly text document service.
   *
   * @param server Parent language server
   */
  public WasmTextDocumentService(final WasmLanguageServerImpl server) {
    this.server = Objects.requireNonNull(server, "Server cannot be null");
  }

  @Override
  public void didOpen(final DidOpenTextDocumentParams params) {
    LOGGER.info("Document opened: " + params.getTextDocument().getUri());
    server.addOpenDocument(params.getTextDocument());
  }

  @Override
  public void didChange(final DidChangeTextDocumentParams params) {
    final VersionedTextDocumentIdentifier document = params.getTextDocument();
    final List<TextDocumentContentChangeEvent> changes = params.getContentChanges();

    LOGGER.fine("Document changed: " + document.getUri());

    // Apply all changes to get the new document text
    final TextDocumentItem existingDoc = server.getOpenDocuments().get(document.getUri());
    if (existingDoc == null) {
      LOGGER.warning("Received change for unopened document: " + document.getUri());
      return;
    }

    String newText = existingDoc.getText();
    for (final TextDocumentContentChangeEvent change : changes) {
      newText = applyChange(newText, change);
    }

    server.updateOpenDocument(document.getUri(), newText, document.getVersion());
  }

  @Override
  public void didClose(final DidCloseTextDocumentParams params) {
    LOGGER.info("Document closed: " + params.getTextDocument().getUri());
    server.removeOpenDocument(params.getTextDocument().getUri());
  }

  @Override
  public void didSave(final DidSaveTextDocumentParams params) {
    LOGGER.info("Document saved: " + params.getTextDocument().getUri());
    // Re-analyze the document after save
    final TextDocumentItem document =
        server.getOpenDocuments().get(params.getTextDocument().getUri());
    if (document != null) {
      // Trigger re-analysis by updating with same content
      server.updateOpenDocument(document.getUri(), document.getText(), document.getVersion());
    }
  }

  @Override
  public CompletableFuture<Hover> hover(final HoverParams params) {
    return CompletableFuture.supplyAsync(
        () -> {
          final String uri = params.getTextDocument().getUri();
          final Position position = params.getPosition();

          LOGGER.fine(
              "Hover request for "
                  + uri
                  + " at "
                  + position.getLine()
                  + ":"
                  + position.getCharacter());

          final TextDocumentItem document = server.getOpenDocuments().get(uri);
          if (document == null) {
            return null;
          }

          // Get hover information based on cursor position
          final String hoverText = getHoverInformation(document.getText(), position);
          if (hoverText == null) {
            return null;
          }

          final MarkupContent content = new MarkupContent();
          content.setKind(MarkupKind.MARKDOWN);
          content.setValue(hoverText);

          return new Hover(content);
        });
  }

  @Override
  public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
      final CompletionParams params) {
    return CompletableFuture.supplyAsync(
        () -> {
          final String uri = params.getTextDocument().getUri();
          final Position position = params.getPosition();

          LOGGER.fine(
              "Completion request for "
                  + uri
                  + " at "
                  + position.getLine()
                  + ":"
                  + position.getCharacter());

          final TextDocumentItem document = server.getOpenDocuments().get(uri);
          if (document == null) {
            return Either.forRight(new CompletionList(false, Collections.emptyList()));
          }

          final List<CompletionItem> items = generateCompletions(document.getText(), position, uri);
          return Either.forRight(new CompletionList(false, items));
        });
  }

  @Override
  public CompletableFuture<CompletionItem> resolveCompletionItem(final CompletionItem unresolved) {
    // For now, just return the item as-is
    return CompletableFuture.completedFuture(unresolved);
  }

  @Override
  public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>>
      definition(final DefinitionParams params) {
    return CompletableFuture.supplyAsync(
        () -> {
          final String uri = params.getTextDocument().getUri();
          final Position position = params.getPosition();

          LOGGER.fine(
              "Definition request for "
                  + uri
                  + " at "
                  + position.getLine()
                  + ":"
                  + position.getCharacter());

          // Find symbol definition
          final List<Location> definitions = findDefinitions(uri, position);
          return Either.forLeft(definitions);
        });
  }

  @Override
  public CompletableFuture<List<? extends Location>> references(final ReferenceParams params) {
    return CompletableFuture.supplyAsync(
        () -> {
          final String uri = params.getTextDocument().getUri();
          final Position position = params.getPosition();

          LOGGER.fine(
              "References request for "
                  + uri
                  + " at "
                  + position.getLine()
                  + ":"
                  + position.getCharacter());

          // Find symbol references
          return findReferences(uri, position);
        });
  }

  @Override
  public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(
      final DocumentSymbolParams params) {
    return CompletableFuture.supplyAsync(
        () -> {
          final String uri = params.getTextDocument().getUri();

          LOGGER.fine("Document symbols request for " + uri);

          final TextDocumentItem document = server.getOpenDocuments().get(uri);
          if (document == null) {
            return Collections.emptyList();
          }

          return extractDocumentSymbols(document);
        });
  }

  @Override
  public CompletableFuture<List<? extends TextEdit>> formatting(
      final DocumentFormattingParams params) {
    return CompletableFuture.supplyAsync(
        () -> {
          final String uri = params.getTextDocument().getUri();

          LOGGER.fine("Formatting request for " + uri);

          final TextDocumentItem document = server.getOpenDocuments().get(uri);
          if (document == null) {
            return Collections.emptyList();
          }

          return formatDocument(document, params.getOptions());
        });
  }

  @Override
  public CompletableFuture<List<? extends TextEdit>> rangeFormatting(
      final DocumentRangeFormattingParams params) {
    return CompletableFuture.supplyAsync(
        () -> {
          final String uri = params.getTextDocument().getUri();

          LOGGER.fine("Range formatting request for " + uri);

          final TextDocumentItem document = server.getOpenDocuments().get(uri);
          if (document == null) {
            return Collections.emptyList();
          }

          return formatRange(document, params.getRange(), params.getOptions());
        });
  }

  @Override
  public CompletableFuture<List<? extends CodeAction>> codeAction(final CodeActionParams params) {
    return CompletableFuture.supplyAsync(
        () -> {
          final String uri = params.getTextDocument().getUri();

          LOGGER.fine("Code action request for " + uri);

          return generateCodeActions(uri, params.getRange(), params.getContext());
        });
  }

  private String applyChange(final String text, final TextDocumentContentChangeEvent change) {
    if (change.getRange() == null) {
      // Full document change
      return change.getText();
    }

    // Incremental change
    final Range range = change.getRange();
    final String[] lines = text.split("\n", -1);

    final StringBuilder result = new StringBuilder();

    for (int i = 0; i < lines.length; i++) {
      if (i < range.getStart().getLine() || i > range.getEnd().getLine()) {
        // Line outside the change range
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

  private String getHoverInformation(final String text, final Position position) {
    final String[] lines = text.split("\n");
    if (position.getLine() >= lines.length) {
      return null;
    }

    final String line = lines[position.getLine()];
    if (position.getCharacter() >= line.length()) {
      return null;
    }

    // Extract word at position
    final String word = extractWordAtPosition(line, position.getCharacter());
    if (word == null) {
      return null;
    }

    // Get WebAssembly symbol information
    return getWasmSymbolDocumentation(word);
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

  private String getWasmSymbolDocumentation(final String symbol) {
    switch (symbol) {
      case "i32":
        return "**i32** - 32-bit integer type\n\nRange: -2^31 to 2^31-1";
      case "i64":
        return "**i64** - 64-bit integer type\n\nRange: -2^63 to 2^63-1";
      case "f32":
        return "**f32** - 32-bit floating point type\n\nIEEE 754 single precision";
      case "f64":
        return "**f64** - 64-bit floating point type\n\nIEEE 754 double precision";
      case "func":
        return "**func** - Function declaration\n\nDefines a WebAssembly function";
      case "module":
        return "**module** - WebAssembly module\n\nTop-level container for WebAssembly code";
      case "memory":
        return "**memory** - Linear memory declaration\n\nDefines a resizable array of bytes";
      case "table":
        return "**table** - Table declaration\n\nDefines a resizable array of references";
      case "global":
        return "**global** - Global variable declaration\n\nDefines a global variable";
      case "export":
        return "**export** - Export declaration\n\n"
            + "Makes an item available for import by other modules";
      case "import":
        return "**import** - Import declaration\n\nImports an item from another module";
      default:
        return null;
    }
  }

  private List<CompletionItem> generateCompletions(
      final String text, final Position position, final String uri) {
    final List<CompletionItem> items = new ArrayList<>();

    // WebAssembly types
    items.add(createCompletionItem("i32", CompletionItemKind.Keyword, "32-bit integer type"));
    items.add(createCompletionItem("i64", CompletionItemKind.Keyword, "64-bit integer type"));
    items.add(
        createCompletionItem("f32", CompletionItemKind.Keyword, "32-bit floating point type"));
    items.add(
        createCompletionItem("f64", CompletionItemKind.Keyword, "64-bit floating point type"));

    // WebAssembly constructs
    items.add(createCompletionItem("func", CompletionItemKind.Keyword, "Function declaration"));
    items.add(createCompletionItem("module", CompletionItemKind.Keyword, "WebAssembly module"));
    items.add(
        createCompletionItem("memory", CompletionItemKind.Keyword, "Linear memory declaration"));
    items.add(createCompletionItem("table", CompletionItemKind.Keyword, "Table declaration"));
    items.add(
        createCompletionItem("global", CompletionItemKind.Keyword, "Global variable declaration"));
    items.add(createCompletionItem("export", CompletionItemKind.Keyword, "Export declaration"));
    items.add(createCompletionItem("import", CompletionItemKind.Keyword, "Import declaration"));

    // WebAssembly instructions
    items.add(createCompletionItem("i32.add", CompletionItemKind.Function, "Add two i32 values"));
    items.add(
        createCompletionItem("i32.sub", CompletionItemKind.Function, "Subtract two i32 values"));
    items.add(
        createCompletionItem("i32.mul", CompletionItemKind.Function, "Multiply two i32 values"));
    items.add(
        createCompletionItem(
            "i32.div_s", CompletionItemKind.Function, "Signed division of two i32 values"));
    items.add(
        createCompletionItem(
            "i32.div_u", CompletionItemKind.Function, "Unsigned division of two i32 values"));
    items.add(
        createCompletionItem("i32.load", CompletionItemKind.Function, "Load i32 from memory"));
    items.add(
        createCompletionItem("i32.store", CompletionItemKind.Function, "Store i32 to memory"));

    // Add module-specific completions if available
    final ModuleInspector inspector = server.getModuleInspector(uri);
    if (inspector != null) {
      // Add function names from the module
      for (final ModuleInspector.Function function : inspector.getFunctions()) {
        if (function.getName() != null) {
          items.add(
              createCompletionItem(
                  function.getName(),
                  CompletionItemKind.Function,
                  "Function: " + function.getName()));
        }
      }

      // Add export names from the module
      for (final ModuleInspector.Export export : inspector.getExports()) {
        items.add(
            createCompletionItem(
                export.getName(),
                CompletionItemKind.Variable,
                "Export: " + export.getName() + " (" + export.getType() + ")"));
      }
    }

    return items;
  }

  private CompletionItem createCompletionItem(
      final String label, final CompletionItemKind kind, final String detail) {
    final CompletionItem item = new CompletionItem();
    item.setLabel(label);
    item.setKind(kind);
    item.setDetail(detail);
    item.setInsertText(label);
    return item;
  }

  private List<Location> findDefinitions(final String uri, final Position position) {
    // In a real implementation, this would analyze the document and find symbol definitions
    return Collections.emptyList();
  }

  private List<Location> findReferences(final String uri, final Position position) {
    // In a real implementation, this would find all references to a symbol
    return Collections.emptyList();
  }

  private List<Either<SymbolInformation, DocumentSymbol>> extractDocumentSymbols(
      final TextDocumentItem document) {
    final List<Either<SymbolInformation, DocumentSymbol>> symbols = new ArrayList<>();

    // Extract symbols from WebAssembly text
    final String[] lines = document.getText().split("\n");
    for (int i = 0; i < lines.length; i++) {
      final String line = lines[i].trim();

      // Look for function definitions
      if (line.startsWith("(func")) {
        final DocumentSymbol symbol = new DocumentSymbol();
        symbol.setName("function_" + i);
        symbol.setKind(SymbolKind.Function);
        symbol.setRange(new Range(new Position(i, 0), new Position(i, line.length())));
        symbol.setSelectionRange(new Range(new Position(i, 0), new Position(i, line.length())));
        symbols.add(Either.forRight(symbol));
      }

      // Look for global definitions
      if (line.startsWith("(global")) {
        final DocumentSymbol symbol = new DocumentSymbol();
        symbol.setName("global_" + i);
        symbol.setKind(SymbolKind.Variable);
        symbol.setRange(new Range(new Position(i, 0), new Position(i, line.length())));
        symbol.setSelectionRange(new Range(new Position(i, 0), new Position(i, line.length())));
        symbols.add(Either.forRight(symbol));
      }
    }

    return symbols;
  }

  private List<TextEdit> formatDocument(
      final TextDocumentItem document, final FormattingOptions options) {
    // Basic WAT formatting - in practice this would be much more sophisticated
    final String[] lines = document.getText().split("\n");
    final List<TextEdit> edits = new ArrayList<>();

    for (int i = 0; i < lines.length; i++) {
      final String line = lines[i];
      final String trimmed = line.trim();

      if (!trimmed.equals(line)) {
        // Fix indentation
        edits.add(
            new TextEdit(new Range(new Position(i, 0), new Position(i, line.length())), trimmed));
      }
    }

    return edits;
  }

  private List<TextEdit> formatRange(
      final TextDocumentItem document, final Range range, final FormattingOptions options) {
    // Format only the specified range
    return Collections.emptyList();
  }

  private List<CodeAction> generateCodeActions(
      final String uri, final Range range, final CodeActionContext context) {
    final List<CodeAction> actions = new ArrayList<>();

    // Generate quick fixes based on diagnostics
    for (final Diagnostic diagnostic : context.getDiagnostics()) {
      if (diagnostic.getMessage().contains("Unclosed parenthesis")) {
        final CodeAction action = new CodeAction();
        action.setTitle("Add closing parenthesis");
        action.setKind(CodeActionKind.QuickFix);

        final WorkspaceEdit edit = new WorkspaceEdit();
        final List<TextEdit> changes =
            Arrays.asList(
                new TextEdit(
                    new Range(diagnostic.getRange().getEnd(), diagnostic.getRange().getEnd()),
                    ")"));
        edit.setChanges(Collections.singletonMap(uri, changes));
        action.setEdit(edit);

        actions.add(action);
      }
    }

    return actions;
  }
}

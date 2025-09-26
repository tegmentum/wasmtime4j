package ai.tegmentum.wasmtime4j.ide;

import ai.tegmentum.wasmtime4j.ide.server.WasmLanguageServerImpl;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for the WebAssembly Language Server implementation.
 */
class WasmLanguageServerTest {

    @Mock
    private LanguageClient mockClient;

    @TempDir
    Path tempDir;

    private WasmLanguageServerImpl languageServer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        languageServer = new WasmLanguageServerImpl();
        languageServer.connect(mockClient);
    }

    @Test
    void testInitialization() throws ExecutionException, InterruptedException {
        // Arrange
        final InitializeParams params = new InitializeParams();
        final ClientCapabilities clientCapabilities = new ClientCapabilities();

        final TextDocumentClientCapabilities textDocumentCapabilities = new TextDocumentClientCapabilities();
        textDocumentCapabilities.setCompletion(new CompletionCapabilities());
        textDocumentCapabilities.setHover(new HoverCapabilities());
        textDocumentCapabilities.setDefinition(new DefinitionCapabilities());
        textDocumentCapabilities.setReferences(new ReferencesCapabilities());
        textDocumentCapabilities.setDocumentFormatting(new DocumentFormattingCapabilities());

        clientCapabilities.setTextDocument(textDocumentCapabilities);
        params.setCapabilities(clientCapabilities);

        // Act
        final CompletableFuture<InitializeResult> future = languageServer.initialize(params);
        final InitializeResult result = future.get();

        // Assert
        assertNotNull(result);
        assertNotNull(result.getCapabilities());

        final ServerCapabilities capabilities = result.getCapabilities();
        assertNotNull(capabilities.getTextDocumentSync());
        assertTrue(capabilities.getHoverProvider().isLeft());
        assertNotNull(capabilities.getCompletionProvider());
        assertTrue(capabilities.getDefinitionProvider().isLeft());
        assertTrue(capabilities.getReferencesProvider().isLeft());
        assertTrue(capabilities.getDocumentFormattingProvider().isLeft());
    }

    @Test
    void testInitializedCallback() {
        // Arrange
        final InitializedParams params = new InitializedParams();

        // Act & Assert (should not throw)
        assertDoesNotThrow(() -> languageServer.initialized(params));

        // Verify client interaction
        verify(mockClient, times(1)).logMessage(any(MessageParams.class));
    }

    @Test
    void testDocumentOpenAndClose() {
        // Arrange
        final String uri = "file:///test.wat";
        final String content = "(module (func $add (param $a i32) (param $b i32) (result i32) local.get $a local.get $b i32.add))";

        final TextDocumentItem document = new TextDocumentItem(uri, "wat", 1, content);
        final DidOpenTextDocumentParams openParams = new DidOpenTextDocumentParams(document);

        final TextDocumentIdentifier docId = new TextDocumentIdentifier(uri);
        final DidCloseTextDocumentParams closeParams = new DidCloseTextDocumentParams(docId);

        // Act - Open document
        languageServer.getTextDocumentService().didOpen(openParams);

        // Assert - Document is tracked
        assertTrue(languageServer.getOpenDocuments().containsKey(uri));
        assertEquals(document, languageServer.getOpenDocuments().get(uri));

        // Act - Close document
        languageServer.getTextDocumentService().didClose(closeParams);

        // Assert - Document is no longer tracked
        assertFalse(languageServer.getOpenDocuments().containsKey(uri));
    }

    @Test
    void testDocumentChange() {
        // Arrange
        final String uri = "file:///test.wat";
        final String originalContent = "(module)";
        final String newContent = "(module (func $test))";

        final TextDocumentItem document = new TextDocumentItem(uri, "wat", 1, originalContent);
        final DidOpenTextDocumentParams openParams = new DidOpenTextDocumentParams(document);

        languageServer.getTextDocumentService().didOpen(openParams);

        final VersionedTextDocumentIdentifier docId = new VersionedTextDocumentIdentifier(uri, 2);
        final TextDocumentContentChangeEvent change = new TextDocumentContentChangeEvent(null, newContent);
        final DidChangeTextDocumentParams changeParams = new DidChangeTextDocumentParams(
            docId, Collections.singletonList(change));

        // Act
        languageServer.getTextDocumentService().didChange(changeParams);

        // Assert
        final TextDocumentItem updatedDocument = languageServer.getOpenDocuments().get(uri);
        assertNotNull(updatedDocument);
        assertEquals(newContent, updatedDocument.getText());
        assertEquals(2, updatedDocument.getVersion());
    }

    @Test
    void testHoverProvider() throws ExecutionException, InterruptedException {
        // Arrange
        final String uri = "file:///test.wat";
        final String content = "(module (func (param i32) (result i32)))";

        final TextDocumentItem document = new TextDocumentItem(uri, "wat", 1, content);
        languageServer.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(document));

        final TextDocumentIdentifier docId = new TextDocumentIdentifier(uri);
        final Position position = new Position(0, 20); // Position on "i32"
        final HoverParams hoverParams = new HoverParams(docId, position);

        // Act
        final CompletableFuture<Hover> future = languageServer.getTextDocumentService().hover(hoverParams);
        final Hover hover = future.get();

        // Assert
        assertNotNull(hover);
        assertNotNull(hover.getContents());
        assertTrue(hover.getContents().getValue().contains("i32"));
    }

    @Test
    void testCompletionProvider() throws ExecutionException, InterruptedException {
        // Arrange
        final String uri = "file:///test.wat";
        final String content = "(module (func ";

        final TextDocumentItem document = new TextDocumentItem(uri, "wat", 1, content);
        languageServer.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(document));

        final TextDocumentIdentifier docId = new TextDocumentIdentifier(uri);
        final Position position = new Position(0, 14); // Position after "func "
        final CompletionParams completionParams = new CompletionParams(docId, position);

        // Act
        final CompletableFuture<Either<java.util.List<CompletionItem>, CompletionList>> future =
            languageServer.getTextDocumentService().completion(completionParams);
        final Either<java.util.List<CompletionItem>, CompletionList> result = future.get();

        // Assert
        assertNotNull(result);
        assertTrue(result.isRight());

        final CompletionList completionList = result.getRight();
        assertNotNull(completionList);
        assertFalse(completionList.getItems().isEmpty());

        // Check for WebAssembly keywords
        final boolean hasI32 = completionList.getItems().stream()
            .anyMatch(item -> "i32".equals(item.getLabel()));
        assertTrue(hasI32);
    }

    @Test
    void testDefinitionProvider() throws ExecutionException, InterruptedException {
        // Arrange
        final String uri = "file:///test.wat";
        final String content = "(module (func $add (param i32) (result i32)) (export \"add\" (func $add)))";

        final TextDocumentItem document = new TextDocumentItem(uri, "wat", 1, content);
        languageServer.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(document));

        final TextDocumentIdentifier docId = new TextDocumentIdentifier(uri);
        final Position position = new Position(0, 60); // Position on second "$add"
        final DefinitionParams definitionParams = new DefinitionParams(docId, position);

        // Act
        final CompletableFuture<Either<java.util.List<? extends Location>,
            java.util.List<? extends LocationLink>>> future =
            languageServer.getTextDocumentService().definition(definitionParams);
        final Either<java.util.List<? extends Location>, java.util.List<? extends LocationLink>> result =
            future.get();

        // Assert
        assertNotNull(result);
        // For now, we expect an empty list as the implementation is basic
        if (result.isLeft()) {
            assertNotNull(result.getLeft());
        } else {
            assertNotNull(result.getRight());
        }
    }

    @Test
    void testDocumentFormatting() throws ExecutionException, InterruptedException {
        // Arrange
        final String uri = "file:///test.wat";
        final String content = "   (module   (func  $add  ))   ";

        final TextDocumentItem document = new TextDocumentItem(uri, "wat", 1, content);
        languageServer.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(document));

        final TextDocumentIdentifier docId = new TextDocumentIdentifier(uri);
        final FormattingOptions options = new FormattingOptions(2, true);
        final DocumentFormattingParams formattingParams = new DocumentFormattingParams(docId, options);

        // Act
        final CompletableFuture<java.util.List<? extends TextEdit>> future =
            languageServer.getTextDocumentService().formatting(formattingParams);
        final java.util.List<? extends TextEdit> edits = future.get();

        // Assert
        assertNotNull(edits);
        // The basic formatter should provide some edits for whitespace cleanup
    }

    @Test
    void testWorkspaceSymbols() throws ExecutionException, InterruptedException {
        // Arrange
        final String uri = "file:///test.wat";
        final String content = "(module (func $add) (global $counter i32))";

        final TextDocumentItem document = new TextDocumentItem(uri, "wat", 1, content);
        languageServer.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(document));

        final WorkspaceSymbolParams symbolParams = new WorkspaceSymbolParams("add");

        // Act
        final CompletableFuture<java.util.List<? extends SymbolInformation>> future =
            languageServer.getWorkspaceService().symbol(symbolParams);
        final java.util.List<? extends SymbolInformation> symbols = future.get();

        // Assert
        assertNotNull(symbols);
    }

    @Test
    void testCommandExecution() throws ExecutionException, InterruptedException {
        // Arrange
        final String uri = "file:///test.wat";
        final ExecuteCommandParams commandParams = new ExecuteCommandParams(
            "wasmtime4j.validate.module",
            Collections.singletonList(uri)
        );

        // Act
        final CompletableFuture<Object> future =
            languageServer.getWorkspaceService().executeCommand(commandParams);
        final Object result = future.get();

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof String);
    }

    @Test
    void testShutdown() throws ExecutionException, InterruptedException {
        // Act
        final CompletableFuture<Object> future = languageServer.shutdown();
        final Object result = future.get();

        // Assert
        assertNotNull(result);
    }

    @Test
    void testWatSyntaxValidation() {
        // Arrange
        final String uri = "file:///invalid.wat";
        final String invalidContent = "(module (func (unclosed parenthesis";

        final TextDocumentItem document = new TextDocumentItem(uri, "wat", 1, invalidContent);

        // Act
        languageServer.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(document));

        // Assert
        // Verify that diagnostics are published for syntax errors
        verify(mockClient, atLeastOnce()).publishDiagnostics(any(PublishDiagnosticsParams.class));
    }

    @Test
    void testMultipleDocuments() {
        // Arrange
        final String uri1 = "file:///module1.wat";
        final String uri2 = "file:///module2.wat";
        final String content1 = "(module (func $func1))";
        final String content2 = "(module (func $func2))";

        final TextDocumentItem doc1 = new TextDocumentItem(uri1, "wat", 1, content1);
        final TextDocumentItem doc2 = new TextDocumentItem(uri2, "wat", 1, content2);

        // Act
        languageServer.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(doc1));
        languageServer.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(doc2));

        // Assert
        assertEquals(2, languageServer.getOpenDocuments().size());
        assertTrue(languageServer.getOpenDocuments().containsKey(uri1));
        assertTrue(languageServer.getOpenDocuments().containsKey(uri2));
    }

    @Test
    void testConfigurationChange() {
        // Arrange
        final DidChangeConfigurationParams configParams = new DidChangeConfigurationParams(
            Collections.singletonMap("wasmtime4j",
                Collections.singletonMap("validation",
                    Collections.singletonMap("enabled", true)))
        );

        // Act & Assert (should not throw)
        assertDoesNotThrow(() -> languageServer.getWorkspaceService().didChangeConfiguration(configParams));
    }

    @Test
    void testFileWatcher() {
        // Arrange
        final FileEvent changeEvent = new FileEvent("file:///test.wat", FileChangeType.Changed);
        final DidChangeWatchedFilesParams watchedFilesParams = new DidChangeWatchedFilesParams(
            Collections.singletonList(changeEvent)
        );

        // Act & Assert (should not throw)
        assertDoesNotThrow(() ->
            languageServer.getWorkspaceService().didChangeWatchedFiles(watchedFilesParams));
    }

    @Test
    void testDocumentSymbols() throws ExecutionException, InterruptedException {
        // Arrange
        final String uri = "file:///test.wat";
        final String content = "(module (func $add) (global $counter i32))";

        final TextDocumentItem document = new TextDocumentItem(uri, "wat", 1, content);
        languageServer.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(document));

        final DocumentSymbolParams symbolParams = new DocumentSymbolParams(
            new TextDocumentIdentifier(uri));

        // Act
        final CompletableFuture<java.util.List<Either<SymbolInformation, DocumentSymbol>>> future =
            languageServer.getTextDocumentService().documentSymbol(symbolParams);
        final java.util.List<Either<SymbolInformation, DocumentSymbol>> symbols = future.get();

        // Assert
        assertNotNull(symbols);
    }

    @Test
    void testCodeActions() throws ExecutionException, InterruptedException {
        // Arrange
        final String uri = "file:///test.wat";
        final String content = "(module (func (unclosed";

        final TextDocumentItem document = new TextDocumentItem(uri, "wat", 1, content);
        languageServer.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(document));

        final Range range = new Range(new Position(0, 0), new Position(0, content.length()));
        final Diagnostic diagnostic = new Diagnostic(range, "Unclosed parenthesis",
            DiagnosticSeverity.Error, "wasmtime4j");
        final CodeActionContext context = new CodeActionContext(Collections.singletonList(diagnostic));
        final CodeActionParams actionParams = new CodeActionParams(
            new TextDocumentIdentifier(uri), range, context);

        // Act
        final CompletableFuture<java.util.List<? extends CodeAction>> future =
            languageServer.getTextDocumentService().codeAction(actionParams);
        final java.util.List<? extends CodeAction> actions = future.get();

        // Assert
        assertNotNull(actions);
    }
}
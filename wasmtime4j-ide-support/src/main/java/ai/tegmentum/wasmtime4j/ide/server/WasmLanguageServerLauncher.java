package ai.tegmentum.wasmtime4j.ide.server;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main launcher class for the WebAssembly Language Server.
 * This class handles the initialization and lifecycle of the LSP server
 * for WebAssembly development tooling.
 */
public final class WasmLanguageServerLauncher {

    private static final Logger LOGGER = Logger.getLogger(WasmLanguageServerLauncher.class.getName());

    /**
     * Main entry point for the WebAssembly Language Server.
     *
     * @param args Command line arguments
     */
    public static void main(final String[] args) {
        LOGGER.info("Starting WebAssembly Language Server");

        try {
            // Parse command line arguments
            final ServerConfig config = parseArguments(args);

            // Create and start the language server
            startServer(config);

        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start WebAssembly Language Server", e);
            System.exit(1);
        }
    }

    /**
     * Starts the language server with the given configuration.
     *
     * @param config Server configuration
     * @throws Exception if server startup fails
     */
    private static void startServer(final ServerConfig config) throws Exception {
        // Determine input/output streams based on configuration
        final InputStream input = config.isStdio() ? System.in : null;
        final OutputStream output = config.isStdio() ? System.out : null;

        if (config.isStdio()) {
            startStdioServer(input, output);
        } else if (config.getPort() > 0) {
            startSocketServer(config.getPort());
        } else {
            throw new IllegalArgumentException("Either stdio or socket port must be specified");
        }
    }

    /**
     * Starts the language server using stdio communication.
     *
     * @param input Input stream for communication
     * @param output Output stream for communication
     * @throws Exception if server startup fails
     */
    private static void startStdioServer(final InputStream input, final OutputStream output) throws Exception {
        LOGGER.info("Starting language server with stdio communication");

        // Create the language server implementation
        final WasmLanguageServerImpl server = new WasmLanguageServerImpl();

        // Create the LSP4J launcher
        final Launcher<LanguageClient> launcher = Launcher.createLauncher(
            server,
            LanguageClient.class,
            input,
            output
        );

        // Connect the server to the client
        final LanguageClient client = launcher.getRemoteProxy();
        server.connect(client);

        // Start listening for messages
        final Future<Void> listening = launcher.startListening();

        LOGGER.info("WebAssembly Language Server started and listening");

        // Keep the server running
        listening.get();

        LOGGER.info("WebAssembly Language Server stopped");
    }

    /**
     * Starts the language server using socket communication.
     *
     * @param port Socket port to listen on
     * @throws Exception if server startup fails
     */
    private static void startSocketServer(final int port) throws Exception {
        LOGGER.info("Starting language server with socket communication on port " + port);

        // Socket-based server implementation would go here
        // This is more complex and typically used for debugging
        throw new UnsupportedOperationException("Socket server not yet implemented");
    }

    /**
     * Parses command line arguments into server configuration.
     *
     * @param args Command line arguments
     * @return Parsed server configuration
     */
    private static ServerConfig parseArguments(final String[] args) {
        boolean stdio = true;
        int port = -1;
        boolean debug = false;

        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];

            switch (arg) {
                case "--stdio":
                    stdio = true;
                    port = -1;
                    break;

                case "--socket":
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("--socket requires a port number");
                    }
                    stdio = false;
                    port = Integer.parseInt(args[++i]);
                    break;

                case "--debug":
                    debug = true;
                    break;

                case "--help":
                case "-h":
                    printUsage();
                    System.exit(0);
                    break;

                default:
                    if (arg.startsWith("--")) {
                        LOGGER.warning("Unknown argument: " + arg);
                    }
                    break;
            }
        }

        return new ServerConfig(stdio, port, debug);
    }

    /**
     * Prints usage information to stdout.
     */
    private static void printUsage() {
        System.out.println("WebAssembly Language Server");
        System.out.println();
        System.out.println("Usage: java -jar wasmtime4j-ide-support.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --stdio          Use stdio for communication (default)");
        System.out.println("  --socket <port>  Use socket for communication on specified port");
        System.out.println("  --debug          Enable debug logging");
        System.out.println("  --help, -h       Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar wasmtime4j-ide-support.jar --stdio");
        System.out.println("  java -jar wasmtime4j-ide-support.jar --socket 8080");
        System.out.println("  java -jar wasmtime4j-ide-support.jar --stdio --debug");
    }

    /**
     * Configuration for the language server.
     */
    private static final class ServerConfig {
        private final boolean stdio;
        private final int port;
        private final boolean debug;

        public ServerConfig(final boolean stdio, final int port, final boolean debug) {
            this.stdio = stdio;
            this.port = port;
            this.debug = debug;
        }

        public boolean isStdio() { return stdio; }
        public int getPort() { return port; }
        public boolean isDebug() { return debug; }
    }
}
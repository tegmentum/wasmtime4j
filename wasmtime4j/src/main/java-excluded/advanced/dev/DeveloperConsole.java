package ai.tegmentum.wasmtime4j.dev;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

/**
 * Interactive developer console for WebAssembly exploration and debugging. Provides REPL-style
 * interface for module inspection, function execution, and debugging.
 */
public final class DeveloperConsole implements AutoCloseable {

  private final Engine engine;
  private final Map<String, Module> loadedModules;
  private final Map<String, Instance> activeInstances;
  private final Map<String, Object> variables;
  private final List<ConsoleCommand> commands;
  private final ConsoleHistory history;
  private final ConsoleOutput output;
  private volatile boolean isRunning;
  private volatile Module currentModule;
  private volatile Instance currentInstance;

  /**
   * Creates a developer console with the given engine.
   *
   * @param engine The engine to use for WebAssembly operations
   * @throws IllegalArgumentException if engine is null
   */
  public DeveloperConsole(final Engine engine) {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    this.engine = engine;
    this.loadedModules = new HashMap<>();
    this.activeInstances = new HashMap<>();
    this.variables = new HashMap<>();
    this.commands = new ArrayList<>();
    this.history = new ConsoleHistory();
    this.output = new ConsoleOutput();
    this.isRunning = false;
    this.currentModule = null;
    this.currentInstance = null;

    initializeCommands();
  }

  /** Starts the interactive console session. */
  public void start() {
    if (isRunning) {
      return;
    }

    isRunning = true;
    output.println("wasmtime4j Developer Console");
    output.println("Type 'help' for available commands, 'exit' to quit");
    output.println();

    final Scanner scanner = new Scanner(System.in);

    while (isRunning) {
      output.print(getPrompt());
      final String input = scanner.nextLine().trim();

      if (!input.isEmpty()) {
        history.addCommand(input);
        processCommand(input);
      }
    }

    scanner.close();
  }

  /**
   * Executes a single command non-interactively.
   *
   * @param command The command to execute
   * @return The command execution result
   */
  public CommandResult executeCommand(final String command) {
    if (command == null || command.trim().isEmpty()) {
      return CommandResult.error("Empty command");
    }

    try {
      return processCommandInternal(command.trim());
    } catch (final Exception e) {
      return CommandResult.error("Command failed: " + e.getMessage());
    }
  }

  /**
   * Loads a WebAssembly module from bytes.
   *
   * @param name The name to assign to the module
   * @param moduleBytes The module bytes
   * @return Load result
   */
  public CompletableFuture<LoadResult> loadModule(final String name, final byte[] moduleBytes) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final Module module = Module.fromBinary(engine, moduleBytes);
            loadedModules.put(name, module);
            currentModule = module;

            output.println("Module '" + name + "' loaded successfully");
            return LoadResult.successful(name, module);
          } catch (final Exception e) {
            output.println("Failed to load module '" + name + "': " + e.getMessage());
            return LoadResult.failed(name, e.getMessage());
          }
        });
  }

  /**
   * Creates an instance of the specified module.
   *
   * @param moduleName The name of the module to instantiate
   * @param instanceName The name for the new instance
   * @return Instance creation result
   */
  public CompletableFuture<InstanceResult> createInstance(
      final String moduleName, final String instanceName) {
    return CompletableFuture.supplyAsync(
        () -> {
          final Module module = loadedModules.get(moduleName);
          if (module == null) {
            return InstanceResult.failed(instanceName, "Module not found: " + moduleName);
          }

          try {
            final Store store = Store.newStore(engine);
            final Instance instance = Instance.newInstance(store, module, new HashMap<>());
            activeInstances.put(instanceName, instance);
            currentInstance = instance;

            output.println("Instance '" + instanceName + "' created successfully");
            return InstanceResult.successful(instanceName, instance);
          } catch (final Exception e) {
            output.println("Failed to create instance '" + instanceName + "': " + e.getMessage());
            return InstanceResult.failed(instanceName, e.getMessage());
          }
        });
  }

  /**
   * Inspects the current module or instance.
   *
   * @return Inspection result
   */
  public InspectionResult inspect() {
    if (currentModule == null) {
      return InspectionResult.empty("No current module");
    }

    final ModuleInspector inspector = new ModuleInspector(currentModule);
    final ModuleInspector.ModuleAnalysis analysis = inspector.analyze();

    return new InspectionResult(analysis, currentInstance);
  }

  /**
   * Executes a function in the current instance.
   *
   * @param functionName The function name to execute
   * @param arguments The function arguments
   * @return Execution result
   */
  public CompletableFuture<ExecutionResult> executeFunction(
      final String functionName, final WasmValue... arguments) {
    return CompletableFuture.supplyAsync(
        () -> {
          if (currentInstance == null) {
            return ExecutionResult.failed("No current instance");
          }

          try {
            final WasmFunction function = currentInstance.getFunction(functionName);
            if (function == null) {
              return ExecutionResult.failed("Function not found: " + functionName);
            }

            final long startTime = System.nanoTime();
            final WasmValue[] results = function.call(arguments);
            final long executionTime = System.nanoTime() - startTime;

            return ExecutionResult.successful(functionName, results, executionTime);
          } catch (final Exception e) {
            return ExecutionResult.failed("Function execution failed: " + e.getMessage());
          }
        });
  }

  /**
   * Gets the console command history.
   *
   * @return Command history
   */
  public List<String> getHistory() {
    return history.getCommands();
  }

  /**
   * Gets all available commands.
   *
   * @return List of available commands
   */
  public List<ConsoleCommand> getCommands() {
    return Collections.unmodifiableList(commands);
  }

  @Override
  public void close() {
    isRunning = false;
    activeInstances.clear();
    loadedModules.clear();
    variables.clear();
  }

  private void initializeCommands() {
    commands.add(new ConsoleCommand("help", "Show available commands", args -> showHelp()));

    commands.add(
        new ConsoleCommand(
            "exit",
            "Exit the console",
            args -> {
              isRunning = false;
              return CommandResult.success("Goodbye!");
            }));

    commands.add(
        new ConsoleCommand(
            "load", "Load a WebAssembly module: load <name> <file>", this::loadModuleCommand));

    commands.add(
        new ConsoleCommand(
            "instantiate",
            "Create module instance: instantiate <module> <instance>",
            this::instantiateCommand));

    commands.add(
        new ConsoleCommand("list", "List loaded modules and instances", args -> listResources()));

    commands.add(
        new ConsoleCommand("current", "Show current module/instance", args -> showCurrent()));

    commands.add(
        new ConsoleCommand("use", "Switch to module/instance: use <name>", this::useCommand));

    commands.add(new ConsoleCommand("inspect", "Inspect current module", args -> inspectCommand()));

    commands.add(
        new ConsoleCommand("functions", "List exported functions", args -> listFunctions()));

    commands.add(
        new ConsoleCommand("call", "Call function: call <name> [args...]", this::callFunction));

    commands.add(
        new ConsoleCommand(
            "memory", "Inspect memory: memory [address] [length]", this::inspectMemory));

    commands.add(new ConsoleCommand("globals", "List global variables", args -> listGlobals()));

    commands.add(new ConsoleCommand("set", "Set variable: set <name> <value>", this::setVariable));

    commands.add(new ConsoleCommand("get", "Get variable: get <name>", this::getVariable));

    commands.add(
        new ConsoleCommand(
            "clear",
            "Clear console output",
            args -> {
              output.clear();
              return CommandResult.success("Console cleared");
            }));

    commands.add(new ConsoleCommand("history", "Show command history", args -> showHistory()));

    commands.add(
        new ConsoleCommand(
            "benchmark",
            "Benchmark function: benchmark <name> [iterations]",
            this::benchmarkFunction));

    commands.add(
        new ConsoleCommand(
            "profile",
            "Profile function execution: profile <name> [args...]",
            this::profileFunction));
  }

  private String getPrompt() {
    final StringBuilder prompt = new StringBuilder("wasmtime4j");

    if (currentModule != null) {
      prompt.append("(module)");
    }
    if (currentInstance != null) {
      prompt.append("(instance)");
    }

    prompt.append("> ");
    return prompt.toString();
  }

  private void processCommand(final String input) {
    final CommandResult result = processCommandInternal(input);

    if (!result.isSuccessful()) {
      output.println("Error: " + result.getError());
    } else if (result.getOutput() != null && !result.getOutput().isEmpty()) {
      output.println(result.getOutput());
    }
  }

  private CommandResult processCommandInternal(final String input) {
    final String[] parts = input.split("\\s+");
    final String commandName = parts[0].toLowerCase();
    final String[] args = java.util.Arrays.copyOfRange(parts, 1, parts.length);

    for (final ConsoleCommand command : commands) {
      if (command.getName().equals(commandName)) {
        return command.getHandler().apply(args);
      }
    }

    return CommandResult.error(
        "Unknown command: " + commandName + ". Type 'help' for available commands.");
  }

  private CommandResult showHelp() {
    final StringBuilder help = new StringBuilder();
    help.append("Available commands:\n");

    for (final ConsoleCommand command : commands) {
      help.append(String.format("  %-12s %s\n", command.getName(), command.getDescription()));
    }

    return CommandResult.success(help.toString());
  }

  private CommandResult loadModuleCommand(final String[] args) {
    if (args.length != 2) {
      return CommandResult.error("Usage: load <name> <file>");
    }

    final String name = args[0];
    final String filename = args[1];

    try {
      final byte[] moduleBytes =
          java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filename));

      loadModule(name, moduleBytes).join();
      return CommandResult.success("Module loaded: " + name);
    } catch (final Exception e) {
      return CommandResult.error("Failed to load module: " + e.getMessage());
    }
  }

  private CommandResult instantiateCommand(final String[] args) {
    if (args.length != 2) {
      return CommandResult.error("Usage: instantiate <module> <instance>");
    }

    final String moduleName = args[0];
    final String instanceName = args[1];

    try {
      createInstance(moduleName, instanceName).join();
      return CommandResult.success("Instance created: " + instanceName);
    } catch (final Exception e) {
      return CommandResult.error("Failed to create instance: " + e.getMessage());
    }
  }

  private CommandResult listResources() {
    final StringBuilder list = new StringBuilder();

    list.append("Loaded modules:\n");
    for (final String name : loadedModules.keySet()) {
      list.append("  ").append(name);
      if (loadedModules.get(name) == currentModule) {
        list.append(" (current)");
      }
      list.append("\n");
    }

    list.append("\nActive instances:\n");
    for (final String name : activeInstances.keySet()) {
      list.append("  ").append(name);
      if (activeInstances.get(name) == currentInstance) {
        list.append(" (current)");
      }
      list.append("\n");
    }

    return CommandResult.success(list.toString());
  }

  private CommandResult showCurrent() {
    final StringBuilder current = new StringBuilder();

    if (currentModule != null) {
      current.append("Current module: loaded\n");
    } else {
      current.append("Current module: none\n");
    }

    if (currentInstance != null) {
      current.append("Current instance: active\n");
    } else {
      current.append("Current instance: none\n");
    }

    return CommandResult.success(current.toString());
  }

  private CommandResult useCommand(final String[] args) {
    if (args.length != 1) {
      return CommandResult.error("Usage: use <name>");
    }

    final String name = args[0];

    if (loadedModules.containsKey(name)) {
      currentModule = loadedModules.get(name);
      return CommandResult.success("Switched to module: " + name);
    }

    if (activeInstances.containsKey(name)) {
      currentInstance = activeInstances.get(name);
      return CommandResult.success("Switched to instance: " + name);
    }

    return CommandResult.error("Module or instance not found: " + name);
  }

  private CommandResult inspectCommand() {
    final InspectionResult result = inspect();
    if (result.isEmpty()) {
      return CommandResult.error(result.getError());
    }

    final StringBuilder inspection = new StringBuilder();
    final ModuleInspector.ModuleAnalysis analysis = result.getAnalysis();

    inspection.append("Module Analysis:\n");
    inspection.append("  Functions: ").append(analysis.getFunctions().size()).append("\n");
    inspection.append("  Imports: ").append(analysis.getImports().size()).append("\n");
    inspection.append("  Exports: ").append(analysis.getExports().size()).append("\n");
    inspection
        .append("  Memory: ")
        .append(analysis.getMemory().getInitialSize())
        .append(" bytes\n");
    inspection.append("  Tables: ").append(analysis.getTables().size()).append("\n");
    inspection.append("  Globals: ").append(analysis.getGlobals().size()).append("\n");

    return CommandResult.success(inspection.toString());
  }

  private CommandResult listFunctions() {
    if (currentInstance == null) {
      return CommandResult.error("No current instance");
    }

    final StringBuilder functions = new StringBuilder();
    functions.append("Exported functions:\n");

    // TODO: Get actual exported functions from instance
    functions.append("  (function listing not yet implemented)\n");

    return CommandResult.success(functions.toString());
  }

  private CommandResult callFunction(final String[] args) {
    if (args.length < 1) {
      return CommandResult.error("Usage: call <name> [args...]");
    }

    final String functionName = args[0];
    final WasmValue[] arguments = new WasmValue[args.length - 1];

    // Parse arguments (simplified)
    for (int i = 1; i < args.length; i++) {
      try {
        final int value = Integer.parseInt(args[i]);
        arguments[i - 1] = WasmValue.fromI32(value);
      } catch (final NumberFormatException e) {
        return CommandResult.error("Invalid argument: " + args[i]);
      }
    }

    try {
      final ExecutionResult result = executeFunction(functionName, arguments).join();
      if (result.isSuccessful()) {
        final StringBuilder output = new StringBuilder();
        output.append("Function executed successfully\n");
        output.append("Results: ");
        for (final WasmValue value : result.getResults()) {
          output.append(value.toString()).append(" ");
        }
        output
            .append("\nExecution time: ")
            .append(result.getExecutionTimeNs() / 1_000_000.0)
            .append(" ms");
        return CommandResult.success(output.toString());
      } else {
        return CommandResult.error(result.getError());
      }
    } catch (final Exception e) {
      return CommandResult.error("Function call failed: " + e.getMessage());
    }
  }

  private CommandResult inspectMemory(final String[] args) {
    if (currentInstance == null) {
      return CommandResult.error("No current instance");
    }

    long address = 0;
    int length = 16;

    if (args.length >= 1) {
      try {
        address = Long.parseLong(args[0]);
      } catch (final NumberFormatException e) {
        return CommandResult.error("Invalid address: " + args[0]);
      }
    }

    if (args.length >= 2) {
      try {
        length = Integer.parseInt(args[1]);
      } catch (final NumberFormatException e) {
        return CommandResult.error("Invalid length: " + args[1]);
      }
    }

    // TODO: Implement actual memory inspection
    return CommandResult.success("Memory inspection at address " + address + ", length " + length);
  }

  private CommandResult listGlobals() {
    if (currentInstance == null) {
      return CommandResult.error("No current instance");
    }

    // TODO: Implement global variable listing
    return CommandResult.success("Global variables listing not yet implemented");
  }

  private CommandResult setVariable(final String[] args) {
    if (args.length != 2) {
      return CommandResult.error("Usage: set <name> <value>");
    }

    final String name = args[0];
    final String value = args[1];

    variables.put(name, value);
    return CommandResult.success("Variable set: " + name + " = " + value);
  }

  private CommandResult getVariable(final String[] args) {
    if (args.length != 1) {
      return CommandResult.error("Usage: get <name>");
    }

    final String name = args[0];
    final Object value = variables.get(name);

    if (value == null) {
      return CommandResult.error("Variable not found: " + name);
    }

    return CommandResult.success(name + " = " + value);
  }

  private CommandResult showHistory() {
    final StringBuilder hist = new StringBuilder();
    hist.append("Command history:\n");

    final List<String> commands = history.getCommands();
    for (int i = 0; i < commands.size(); i++) {
      hist.append(String.format("%3d: %s\n", i + 1, commands.get(i)));
    }

    return CommandResult.success(hist.toString());
  }

  private CommandResult benchmarkFunction(final String[] args) {
    if (args.length < 1) {
      return CommandResult.error("Usage: benchmark <name> [iterations]");
    }

    final String functionName = args[0];
    int iterations = 1000;

    if (args.length >= 2) {
      try {
        iterations = Integer.parseInt(args[1]);
      } catch (final NumberFormatException e) {
        return CommandResult.error("Invalid iterations: " + args[1]);
      }
    }

    // TODO: Implement function benchmarking
    return CommandResult.success(
        "Benchmarking " + functionName + " for " + iterations + " iterations");
  }

  private CommandResult profileFunction(final String[] args) {
    if (args.length < 1) {
      return CommandResult.error("Usage: profile <name> [args...]");
    }

    final String functionName = args[0];

    // TODO: Implement function profiling
    return CommandResult.success("Profiling function: " + functionName);
  }

  /** Console command definition. */
  public static final class ConsoleCommand {
    private final String name;
    private final String description;
    private final java.util.function.Function<String[], CommandResult> handler;

    public ConsoleCommand(
        final String name,
        final String description,
        final java.util.function.Function<String[], CommandResult> handler) {
      this.name = name;
      this.description = description;
      this.handler = handler;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    public java.util.function.Function<String[], CommandResult> getHandler() {
      return handler;
    }
  }

  /** Command execution result. */
  public static final class CommandResult {
    private final boolean successful;
    private final String output;
    private final String error;

    private CommandResult(final boolean successful, final String output, final String error) {
      this.successful = successful;
      this.output = output;
      this.error = error;
    }

    public static CommandResult success(final String output) {
      return new CommandResult(true, output, null);
    }

    public static CommandResult error(final String error) {
      return new CommandResult(false, null, error);
    }

    public boolean isSuccessful() {
      return successful;
    }

    public String getOutput() {
      return output;
    }

    public String getError() {
      return error;
    }
  }

  /** Module load result. */
  public static final class LoadResult {
    private final String name;
    private final Module module;
    private final boolean successful;
    private final String error;

    private LoadResult(
        final String name, final Module module, final boolean successful, final String error) {
      this.name = name;
      this.module = module;
      this.successful = successful;
      this.error = error;
    }

    public static LoadResult successful(final String name, final Module module) {
      return new LoadResult(name, module, true, null);
    }

    public static LoadResult failed(final String name, final String error) {
      return new LoadResult(name, null, false, error);
    }

    public String getName() {
      return name;
    }

    public Module getModule() {
      return module;
    }

    public boolean isSuccessful() {
      return successful;
    }

    public String getError() {
      return error;
    }
  }

  /** Instance creation result. */
  public static final class InstanceResult {
    private final String name;
    private final Instance instance;
    private final boolean successful;
    private final String error;

    private InstanceResult(
        final String name, final Instance instance, final boolean successful, final String error) {
      this.name = name;
      this.instance = instance;
      this.successful = successful;
      this.error = error;
    }

    public static InstanceResult successful(final String name, final Instance instance) {
      return new InstanceResult(name, instance, true, null);
    }

    public static InstanceResult failed(final String name, final String error) {
      return new InstanceResult(name, null, false, error);
    }

    public String getName() {
      return name;
    }

    public Instance getInstance() {
      return instance;
    }

    public boolean isSuccessful() {
      return successful;
    }

    public String getError() {
      return error;
    }
  }

  /** Inspection result. */
  public static final class InspectionResult {
    private final ModuleInspector.ModuleAnalysis analysis;
    private final Instance instance;
    private final boolean empty;
    private final String error;

    public InspectionResult(
        final ModuleInspector.ModuleAnalysis analysis, final Instance instance) {
      this.analysis = analysis;
      this.instance = instance;
      this.empty = false;
      this.error = null;
    }

    public static InspectionResult empty(final String error) {
      final InspectionResult result = new InspectionResult(null, null);
      return new InspectionResult(null, null) {
        @Override
        public boolean isEmpty() {
          return true;
        }

        @Override
        public String getError() {
          return error;
        }
      };
    }

    public ModuleInspector.ModuleAnalysis getAnalysis() {
      return analysis;
    }

    public Instance getInstance() {
      return instance;
    }

    public boolean isEmpty() {
      return empty;
    }

    public String getError() {
      return error;
    }
  }

  /** Function execution result. */
  public static final class ExecutionResult {
    private final String functionName;
    private final WasmValue[] results;
    private final long executionTimeNs;
    private final boolean successful;
    private final String error;

    private ExecutionResult(
        final String functionName,
        final WasmValue[] results,
        final long executionTimeNs,
        final boolean successful,
        final String error) {
      this.functionName = functionName;
      this.results = results;
      this.executionTimeNs = executionTimeNs;
      this.successful = successful;
      this.error = error;
    }

    public static ExecutionResult successful(
        final String functionName, final WasmValue[] results, final long executionTimeNs) {
      return new ExecutionResult(functionName, results, executionTimeNs, true, null);
    }

    public static ExecutionResult failed(final String error) {
      return new ExecutionResult(null, null, 0, false, error);
    }

    public String getFunctionName() {
      return functionName;
    }

    public WasmValue[] getResults() {
      return results;
    }

    public long getExecutionTimeNs() {
      return executionTimeNs;
    }

    public boolean isSuccessful() {
      return successful;
    }

    public String getError() {
      return error;
    }
  }

  /** Console command history. */
  private static final class ConsoleHistory {
    private final List<String> commands = new ArrayList<>();
    private static final int MAX_HISTORY = 100;

    public void addCommand(final String command) {
      commands.add(command);
      if (commands.size() > MAX_HISTORY) {
        commands.remove(0);
      }
    }

    public List<String> getCommands() {
      return Collections.unmodifiableList(commands);
    }
  }

  /** Console output handler. */
  private static final class ConsoleOutput {
    public void print(final String text) {
      System.out.print(text);
    }

    public void println(final String text) {
      System.out.println(text);
    }

    public void println() {
      System.out.println();
    }

    public void clear() {
      // ANSI escape sequence to clear screen
      System.out.print("\033[2J\033[H");
    }
  }
}

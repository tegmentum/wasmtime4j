package ai.tegmentum.wasmtime4j.dev;

import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.WasmValueType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Advanced module analysis tool for comprehensive WebAssembly module introspection. Provides
 * detailed analysis of module structure, dependencies, and optimization opportunities.
 */
public final class ModuleInspector {

  private final Module module;
  private final long moduleHandle;

  /**
   * Creates a module inspector for the given WebAssembly module.
   *
   * @param module The module to inspect
   * @throws IllegalArgumentException if module is null
   */
  public ModuleInspector(final Module module) {
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    this.module = module;
    this.moduleHandle = getModuleHandle(module);
  }

  /**
   * Analyzes the module structure and returns comprehensive information.
   *
   * @return Module analysis result containing all inspection data
   */
  public ModuleAnalysis analyze() {
    final List<FunctionInfo> functions = analyzeFunctions();
    final List<ImportInfo> imports = analyzeImports();
    final List<ExportInfo> exports = analyzeExports();
    final MemoryInfo memory = analyzeMemory();
    final List<TableInfo> tables = analyzeTables();
    final List<GlobalInfo> globals = analyzeGlobals();
    final SecurityAnalysis security = performSecurityAnalysis();
    final PerformanceAnalysis performance = analyzePerformance();
    final SizeAnalysis size = analyzeSizeMetrics();

    return new ModuleAnalysis(
        functions, imports, exports, memory, tables, globals, security, performance, size);
  }

  /**
   * Generates function dependency graph showing call relationships.
   *
   * @return Function dependency graph
   */
  public FunctionDependencyGraph generateDependencyGraph() {
    final List<FunctionInfo> functions = analyzeFunctions();
    final Map<Integer, List<Integer>> dependencies = new HashMap<>();

    for (final FunctionInfo function : functions) {
      final List<Integer> calls = analyzeFunction(function.getIndex());
      dependencies.put(function.getIndex(), calls);
    }

    return new FunctionDependencyGraph(functions, dependencies);
  }

  /**
   * Provides optimization recommendations based on module analysis.
   *
   * @return List of optimization recommendations
   */
  public List<OptimizationRecommendation> getOptimizationRecommendations() {
    final List<OptimizationRecommendation> recommendations = new ArrayList<>();
    final ModuleAnalysis analysis = analyze();

    // Check for unused functions
    final List<FunctionInfo> unusedFunctions = findUnusedFunctions(analysis);
    if (!unusedFunctions.isEmpty()) {
      recommendations.add(
          new OptimizationRecommendation(
              OptimizationType.DEAD_CODE_ELIMINATION,
              "Remove " + unusedFunctions.size() + " unused functions",
              "These functions are never called and can be safely removed to reduce module size",
              EstimatedImpact.MEDIUM));
    }

    // Check for oversized memory
    if (analysis.getMemory().getInitialSize() > 1024 * 1024) { // 1MB
      recommendations.add(
          new OptimizationRecommendation(
              OptimizationType.MEMORY_OPTIMIZATION,
              "Consider reducing initial memory size",
              "Large initial memory allocation may impact startup performance",
              EstimatedImpact.LOW));
    }

    // Check for complex function signatures
    final long complexFunctions =
        analysis.getFunctions().stream().filter(f -> f.getParameterCount() > 8).count();

    if (complexFunctions > 0) {
      recommendations.add(
          new OptimizationRecommendation(
              OptimizationType.INTERFACE_SIMPLIFICATION,
              "Simplify complex function signatures",
              complexFunctions + " functions have more than 8 parameters",
              EstimatedImpact.LOW));
    }

    return recommendations;
  }

  /**
   * Validates the module for common issues and security concerns.
   *
   * @return Module validation result
   */
  public ModuleValidationReport validate() {
    final List<ValidationIssue> issues = new ArrayList<>();
    final ModuleAnalysis analysis = analyze();

    // Check for security issues
    final SecurityAnalysis security = analysis.getSecurity();
    if (security.hasUnboundedLoops()) {
      issues.add(
          new ValidationIssue(
              IssueType.SECURITY,
              IssueSeverity.HIGH,
              "Module contains potentially unbounded loops",
              "This may lead to denial-of-service attacks"));
    }

    if (security.hasLargeMemoryAccess()) {
      issues.add(
          new ValidationIssue(
              IssueType.SECURITY,
              IssueSeverity.MEDIUM,
              "Module performs large memory operations",
              "Consider adding bounds checking for memory operations"));
    }

    // Check for performance issues
    final PerformanceAnalysis performance = analysis.getPerformance();
    if (performance.getComplexityScore() > 0.8) {
      issues.add(
          new ValidationIssue(
              IssueType.PERFORMANCE,
              IssueSeverity.MEDIUM,
              "High computational complexity detected",
              "Module may have performance impact on execution"));
    }

    return new ModuleValidationReport(issues, analysis);
  }

  private native long getModuleHandle(Module module);

  private native List<FunctionInfo> analyzeFunctions();

  private native List<ImportInfo> analyzeImports();

  private native List<ExportInfo> analyzeExports();

  private native MemoryInfo analyzeMemory();

  private native List<TableInfo> analyzeTables();

  private native List<GlobalInfo> analyzeGlobals();

  private native SecurityAnalysis performSecurityAnalysis();

  private native PerformanceAnalysis analyzePerformance();

  private native SizeAnalysis analyzeSizeMetrics();

  private native List<Integer> analyzeFunction(int functionIndex);

  private native List<FunctionInfo> findUnusedFunctions(ModuleAnalysis analysis);

  /** Comprehensive module analysis result. */
  public static final class ModuleAnalysis {
    private final List<FunctionInfo> functions;
    private final List<ImportInfo> imports;
    private final List<ExportInfo> exports;
    private final MemoryInfo memory;
    private final List<TableInfo> tables;
    private final List<GlobalInfo> globals;
    private final SecurityAnalysis security;
    private final PerformanceAnalysis performance;
    private final SizeAnalysis size;

    public ModuleAnalysis(
        final List<FunctionInfo> functions,
        final List<ImportInfo> imports,
        final List<ExportInfo> exports,
        final MemoryInfo memory,
        final List<TableInfo> tables,
        final List<GlobalInfo> globals,
        final SecurityAnalysis security,
        final PerformanceAnalysis performance,
        final SizeAnalysis size) {
      this.functions = Collections.unmodifiableList(new ArrayList<>(functions));
      this.imports = Collections.unmodifiableList(new ArrayList<>(imports));
      this.exports = Collections.unmodifiableList(new ArrayList<>(exports));
      this.memory = memory;
      this.tables = Collections.unmodifiableList(new ArrayList<>(tables));
      this.globals = Collections.unmodifiableList(new ArrayList<>(globals));
      this.security = security;
      this.performance = performance;
      this.size = size;
    }

    public List<FunctionInfo> getFunctions() {
      return functions;
    }

    public List<ImportInfo> getImports() {
      return imports;
    }

    public List<ExportInfo> getExports() {
      return exports;
    }

    public MemoryInfo getMemory() {
      return memory;
    }

    public List<TableInfo> getTables() {
      return tables;
    }

    public List<GlobalInfo> getGlobals() {
      return globals;
    }

    public SecurityAnalysis getSecurity() {
      return security;
    }

    public PerformanceAnalysis getPerformance() {
      return performance;
    }

    public SizeAnalysis getSize() {
      return size;
    }
  }

  /** Function dependency graph representation. */
  public static final class FunctionDependencyGraph {
    private final List<FunctionInfo> functions;
    private final Map<Integer, List<Integer>> dependencies;

    public FunctionDependencyGraph(
        final List<FunctionInfo> functions, final Map<Integer, List<Integer>> dependencies) {
      this.functions = Collections.unmodifiableList(new ArrayList<>(functions));
      this.dependencies = Collections.unmodifiableMap(new HashMap<>(dependencies));
    }

    public List<FunctionInfo> getFunctions() {
      return functions;
    }

    public Map<Integer, List<Integer>> getDependencies() {
      return dependencies;
    }

    /**
     * Gets functions that call the specified function.
     *
     * @param functionIndex The function index to check
     * @return List of function indices that call this function
     */
    public List<Integer> getCallers(final int functionIndex) {
      final List<Integer> callers = new ArrayList<>();
      for (final Map.Entry<Integer, List<Integer>> entry : dependencies.entrySet()) {
        if (entry.getValue().contains(functionIndex)) {
          callers.add(entry.getKey());
        }
      }
      return callers;
    }

    /**
     * Gets functions called by the specified function.
     *
     * @param functionIndex The function index to check
     * @return List of function indices called by this function
     */
    public List<Integer> getCalls(final int functionIndex) {
      return dependencies.getOrDefault(functionIndex, Collections.emptyList());
    }
  }

  /** Function information. */
  public static final class FunctionInfo {
    private final int index;
    private final String name;
    private final List<WasmValueType> parameters;
    private final List<WasmValueType> returns;
    private final int instructionCount;
    private final boolean isExported;
    private final boolean isImported;

    public FunctionInfo(
        final int index,
        final String name,
        final List<WasmValueType> parameters,
        final List<WasmValueType> returns,
        final int instructionCount,
        final boolean isExported,
        final boolean isImported) {
      this.index = index;
      this.name = name;
      this.parameters = Collections.unmodifiableList(new ArrayList<>(parameters));
      this.returns = Collections.unmodifiableList(new ArrayList<>(returns));
      this.instructionCount = instructionCount;
      this.isExported = isExported;
      this.isImported = isImported;
    }

    public int getIndex() {
      return index;
    }

    public String getName() {
      return name;
    }

    public List<WasmValueType> getParameters() {
      return parameters;
    }

    public List<WasmValueType> getReturns() {
      return returns;
    }

    public int getInstructionCount() {
      return instructionCount;
    }

    public boolean isExported() {
      return isExported;
    }

    public boolean isImported() {
      return isImported;
    }

    public int getParameterCount() {
      return parameters.size();
    }

    public int getReturnCount() {
      return returns.size();
    }
  }

  /** Import information. */
  public static final class ImportInfo {
    private final String module;
    private final String name;
    private final String type;
    private final boolean isOptional;

    public ImportInfo(
        final String module, final String name, final String type, final boolean isOptional) {
      this.module = module;
      this.name = name;
      this.type = type;
      this.isOptional = isOptional;
    }

    public String getModule() {
      return module;
    }

    public String getName() {
      return name;
    }

    public String getType() {
      return type;
    }

    public boolean isOptional() {
      return isOptional;
    }
  }

  /** Export information. */
  public static final class ExportInfo {
    private final String name;
    private final String type;
    private final int index;

    public ExportInfo(final String name, final String type, final int index) {
      this.name = name;
      this.type = type;
      this.index = index;
    }

    public String getName() {
      return name;
    }

    public String getType() {
      return type;
    }

    public int getIndex() {
      return index;
    }
  }

  /** Memory information. */
  public static final class MemoryInfo {
    private final long initialSize;
    private final long maximumSize;
    private final boolean isShared;
    private final boolean is64Bit;

    public MemoryInfo(
        final long initialSize,
        final long maximumSize,
        final boolean isShared,
        final boolean is64Bit) {
      this.initialSize = initialSize;
      this.maximumSize = maximumSize;
      this.isShared = isShared;
      this.is64Bit = is64Bit;
    }

    public long getInitialSize() {
      return initialSize;
    }

    public long getMaximumSize() {
      return maximumSize;
    }

    public boolean isShared() {
      return isShared;
    }

    public boolean is64Bit() {
      return is64Bit;
    }
  }

  /** Table information. */
  public static final class TableInfo {
    private final int index;
    private final WasmValueType elementType;
    private final long initialSize;
    private final long maximumSize;

    public TableInfo(
        final int index,
        final WasmValueType elementType,
        final long initialSize,
        final long maximumSize) {
      this.index = index;
      this.elementType = elementType;
      this.initialSize = initialSize;
      this.maximumSize = maximumSize;
    }

    public int getIndex() {
      return index;
    }

    public WasmValueType getElementType() {
      return elementType;
    }

    public long getInitialSize() {
      return initialSize;
    }

    public long getMaximumSize() {
      return maximumSize;
    }
  }

  /** Global information. */
  public static final class GlobalInfo {
    private final int index;
    private final WasmValueType type;
    private final boolean isMutable;
    private final boolean isExported;

    public GlobalInfo(
        final int index,
        final WasmValueType type,
        final boolean isMutable,
        final boolean isExported) {
      this.index = index;
      this.type = type;
      this.isMutable = isMutable;
      this.isExported = isExported;
    }

    public int getIndex() {
      return index;
    }

    public WasmValueType getType() {
      return type;
    }

    public boolean isMutable() {
      return isMutable;
    }

    public boolean isExported() {
      return isExported;
    }
  }

  /** Security analysis results. */
  public static final class SecurityAnalysis {
    private final boolean hasUnboundedLoops;
    private final boolean hasLargeMemoryAccess;
    private final boolean hasIndirectCalls;
    private final int riskScore;

    public SecurityAnalysis(
        final boolean hasUnboundedLoops,
        final boolean hasLargeMemoryAccess,
        final boolean hasIndirectCalls,
        final int riskScore) {
      this.hasUnboundedLoops = hasUnboundedLoops;
      this.hasLargeMemoryAccess = hasLargeMemoryAccess;
      this.hasIndirectCalls = hasIndirectCalls;
      this.riskScore = riskScore;
    }

    public boolean hasUnboundedLoops() {
      return hasUnboundedLoops;
    }

    public boolean hasLargeMemoryAccess() {
      return hasLargeMemoryAccess;
    }

    public boolean hasIndirectCalls() {
      return hasIndirectCalls;
    }

    public int getRiskScore() {
      return riskScore;
    }
  }

  /** Performance analysis results. */
  public static final class PerformanceAnalysis {
    private final double complexityScore;
    private final int hotspotCount;
    private final long estimatedExecutionTime;
    private final List<String> bottlenecks;

    public PerformanceAnalysis(
        final double complexityScore,
        final int hotspotCount,
        final long estimatedExecutionTime,
        final List<String> bottlenecks) {
      this.complexityScore = complexityScore;
      this.hotspotCount = hotspotCount;
      this.estimatedExecutionTime = estimatedExecutionTime;
      this.bottlenecks = Collections.unmodifiableList(new ArrayList<>(bottlenecks));
    }

    public double getComplexityScore() {
      return complexityScore;
    }

    public int getHotspotCount() {
      return hotspotCount;
    }

    public long getEstimatedExecutionTime() {
      return estimatedExecutionTime;
    }

    public List<String> getBottlenecks() {
      return bottlenecks;
    }
  }

  /** Size analysis results. */
  public static final class SizeAnalysis {
    private final long totalSize;
    private final long codeSize;
    private final long dataSize;
    private final Map<String, Long> sectionSizes;

    public SizeAnalysis(
        final long totalSize,
        final long codeSize,
        final long dataSize,
        final Map<String, Long> sectionSizes) {
      this.totalSize = totalSize;
      this.codeSize = codeSize;
      this.dataSize = dataSize;
      this.sectionSizes = Collections.unmodifiableMap(new HashMap<>(sectionSizes));
    }

    public long getTotalSize() {
      return totalSize;
    }

    public long getCodeSize() {
      return codeSize;
    }

    public long getDataSize() {
      return dataSize;
    }

    public Map<String, Long> getSectionSizes() {
      return sectionSizes;
    }
  }

  /** Optimization recommendation. */
  public static final class OptimizationRecommendation {
    private final OptimizationType type;
    private final String title;
    private final String description;
    private final EstimatedImpact impact;

    public OptimizationRecommendation(
        final OptimizationType type,
        final String title,
        final String description,
        final EstimatedImpact impact) {
      this.type = type;
      this.title = title;
      this.description = description;
      this.impact = impact;
    }

    public OptimizationType getType() {
      return type;
    }

    public String getTitle() {
      return title;
    }

    public String getDescription() {
      return description;
    }

    public EstimatedImpact getImpact() {
      return impact;
    }
  }

  /** Module validation report. */
  public static final class ModuleValidationReport {
    private final List<ValidationIssue> issues;
    private final ModuleAnalysis analysis;

    public ModuleValidationReport(
        final List<ValidationIssue> issues, final ModuleAnalysis analysis) {
      this.issues = Collections.unmodifiableList(new ArrayList<>(issues));
      this.analysis = analysis;
    }

    public List<ValidationIssue> getIssues() {
      return issues;
    }

    public ModuleAnalysis getAnalysis() {
      return analysis;
    }

    public boolean hasErrors() {
      return issues.stream().anyMatch(issue -> issue.getSeverity() == IssueSeverity.HIGH);
    }
  }

  /** Validation issue. */
  public static final class ValidationIssue {
    private final IssueType type;
    private final IssueSeverity severity;
    private final String message;
    private final String description;

    public ValidationIssue(
        final IssueType type,
        final IssueSeverity severity,
        final String message,
        final String description) {
      this.type = type;
      this.severity = severity;
      this.message = message;
      this.description = description;
    }

    public IssueType getType() {
      return type;
    }

    public IssueSeverity getSeverity() {
      return severity;
    }

    public String getMessage() {
      return message;
    }

    public String getDescription() {
      return description;
    }
  }

  public enum OptimizationType {
    DEAD_CODE_ELIMINATION,
    MEMORY_OPTIMIZATION,
    INTERFACE_SIMPLIFICATION,
    LOOP_OPTIMIZATION,
    INLINING
  }

  public enum EstimatedImpact {
    LOW,
    MEDIUM,
    HIGH
  }

  public enum IssueType {
    SECURITY,
    PERFORMANCE,
    COMPATIBILITY,
    STYLE
  }

  public enum IssueSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
  }
}

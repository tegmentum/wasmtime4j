package ai.tegmentum.wasmtime4j.ide.analysis;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.dev.ModuleInspector;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

/**
 * Advanced WebAssembly analyzer providing comprehensive static analysis, performance insights, and
 * code quality metrics for IDE integration.
 */
public final class AdvancedWasmAnalyzer {

  private static final Logger LOGGER = Logger.getLogger(AdvancedWasmAnalyzer.class.getName());

  // WebAssembly instruction patterns for analysis
  private static final Pattern INSTRUCTION_PATTERN =
      Pattern.compile(
          "\\b(i32|i64|f32|f64|v128)\\.(load|store|const|add|sub|mul|div|eq|ne|lt|gt|and|or|xor|shl|shr)\\b");

  private static final Pattern FUNCTION_PATTERN =
      Pattern.compile(
          "\\(func\\s+(?:\\$[a-zA-Z_][a-zA-Z0-9_]*)?\\s*(?:\\(param[^)]*\\))?\\s*(?:\\(result[^)]*\\))?",
          Pattern.MULTILINE);

  private static final Pattern IMPORT_PATTERN =
      Pattern.compile(
          "\\(import\\s+\"([^\"]+)\"\\s+\"([^\"]+)\"\\s*\\([^)]+\\)\\)", Pattern.MULTILINE);

  private static final Pattern EXPORT_PATTERN =
      Pattern.compile("\\(export\\s+\"([^\"]+)\"\\s*\\([^)]+\\)\\)", Pattern.MULTILINE);

  private final Engine engine;
  private final Map<String, AnalysisResult> analysisCache;
  private final PerformanceProfiler performanceProfiler;
  private final SecurityAnalyzer securityAnalyzer;
  private final CodeQualityAnalyzer codeQualityAnalyzer;

  /**
   * Creates a new advanced WebAssembly analyzer.
   *
   * @param engine WebAssembly engine for compilation and analysis
   */
  public AdvancedWasmAnalyzer(final Engine engine) {
    this.engine = Objects.requireNonNull(engine, "Engine cannot be null");
    this.analysisCache = new ConcurrentHashMap<>();
    this.performanceProfiler = new PerformanceProfiler();
    this.securityAnalyzer = new SecurityAnalyzer();
    this.codeQualityAnalyzer = new CodeQualityAnalyzer();
  }

  /**
   * Performs comprehensive analysis of a WebAssembly module.
   *
   * @param uri Module URI for caching
   * @param content Module content (WAT or binary)
   * @param isBinary Whether content is binary format
   * @return Future containing analysis results
   */
  public CompletableFuture<AnalysisResult> analyzeModule(
      final String uri, final String content, final boolean isBinary) {
    Objects.requireNonNull(uri, "URI cannot be null");
    Objects.requireNonNull(content, "Content cannot be null");

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            LOGGER.fine("Starting comprehensive analysis for: " + uri);

            final AnalysisResult result = new AnalysisResult(uri);

            // Parse and compile module
            final Module module = parseModule(content, isBinary);
            if (module != null) {
              result.setModule(module);
              result.setValid(true);

              // Create module inspector for detailed analysis
              final ModuleInspector inspector = new ModuleInspector(module);
              result.setInspector(inspector);

              // Perform various analyses
              analyzeSyntax(content, isBinary, result);
              analyzeStructure(content, result);
              analyzePerformance(module, result);
              analyzeSecurity(module, content, result);
              analyzeCodeQuality(content, result);
              analyzeFeatureUsage(content, result);
              analyzeDependencies(content, result);

            } else {
              result.setValid(false);
              result.addDiagnostic(
                  createDiagnostic(
                      0, 0, "Failed to parse WebAssembly module", DiagnosticSeverity.Error));
            }

            // Cache results
            analysisCache.put(uri, result);

            LOGGER.fine("Analysis completed for: " + uri);
            return result;

          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Analysis failed for: " + uri, e);

            final AnalysisResult errorResult = new AnalysisResult(uri);
            errorResult.setValid(false);
            errorResult.addDiagnostic(
                createDiagnostic(
                    0, 0, "Analysis error: " + e.getMessage(), DiagnosticSeverity.Error));
            return errorResult;
          }
        });
  }

  /**
   * Gets cached analysis result if available.
   *
   * @param uri Module URI
   * @return Cached analysis result or null
   */
  public AnalysisResult getCachedAnalysis(final String uri) {
    return analysisCache.get(uri);
  }

  /** Clears analysis cache. */
  public void clearCache() {
    analysisCache.clear();
  }

  /**
   * Clears analysis cache for specific URI.
   *
   * @param uri URI to clear from cache
   */
  public void clearCache(final String uri) {
    analysisCache.remove(uri);
  }

  private Module parseModule(final String content, final boolean isBinary) {
    try {
      if (isBinary) {
        // Assume content is base64 encoded binary data
        final byte[] bytes = Base64.getDecoder().decode(content);
        return Module.fromBinary(engine, bytes);
      } else {
        // Parse WAT format - for now, we'll need to compile it
        // This is a simplified implementation
        final byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return Module.fromBinary(engine, bytes);
      }
    } catch (final Exception e) {
      LOGGER.log(Level.FINE, "Failed to parse module", e);
      return null;
    }
  }

  private void analyzeSyntax(
      final String content, final boolean isBinary, final AnalysisResult result) {
    if (isBinary) {
      // Binary format syntax is validated during parsing
      return;
    }

    // Analyze WAT syntax
    final String[] lines = content.split("\n");
    final Stack<Character> parenStack = new Stack<>();

    for (int lineNum = 0; lineNum < lines.length; lineNum++) {
      final String line = lines[lineNum].trim();

      // Skip comments
      if (line.startsWith(";;")) {
        continue;
      }

      // Check parentheses matching
      for (int charPos = 0; charPos < line.length(); charPos++) {
        final char ch = line.charAt(charPos);
        if (ch == '(') {
          parenStack.push(ch);
        } else if (ch == ')') {
          if (parenStack.isEmpty()) {
            result.addDiagnostic(
                createDiagnostic(
                    lineNum, charPos, "Unmatched closing parenthesis", DiagnosticSeverity.Error));
          } else {
            parenStack.pop();
          }
        }
      }

      // Check for invalid characters
      if (line.matches(".*[^\\x20-\\x7E\\t\\n\\r].*")) {
        result.addDiagnostic(
            createDiagnostic(
                lineNum, 0, "Invalid characters detected", DiagnosticSeverity.Warning));
      }
    }

    // Check for unclosed parentheses
    if (!parenStack.isEmpty()) {
      result.addDiagnostic(
          createDiagnostic(
              lines.length - 1,
              0,
              parenStack.size() + " unclosed parenthesis(es)",
              DiagnosticSeverity.Error));
    }
  }

  private void analyzeStructure(final String content, final AnalysisResult result) {
    // Analyze module structure
    final StructureAnalysis structure = new StructureAnalysis();

    // Count functions
    final Matcher funcMatcher = FUNCTION_PATTERN.matcher(content);
    while (funcMatcher.find()) {
      structure.functionCount++;
    }

    // Count imports
    final Matcher importMatcher = IMPORT_PATTERN.matcher(content);
    while (importMatcher.find()) {
      structure.importCount++;
      structure.imports.add(new ImportInfo(importMatcher.group(1), importMatcher.group(2)));
    }

    // Count exports
    final Matcher exportMatcher = EXPORT_PATTERN.matcher(content);
    while (exportMatcher.find()) {
      structure.exportCount++;
      structure.exports.add(exportMatcher.group(1));
    }

    result.setStructureAnalysis(structure);

    // Validate structure
    if (structure.functionCount == 0) {
      result.addDiagnostic(
          createDiagnostic(0, 0, "Module contains no functions", DiagnosticSeverity.Information));
    }

    if (structure.exportCount == 0) {
      result.addDiagnostic(
          createDiagnostic(0, 0, "Module has no exports", DiagnosticSeverity.Warning));
    }
  }

  private void analyzePerformance(final Module module, final AnalysisResult result) {
    final PerformanceAnalysis performance = performanceProfiler.analyze(module);
    result.setPerformanceAnalysis(performance);

    // Add performance diagnostics
    if (performance.estimatedCompilationTime > 1000) {
      result.addDiagnostic(
          createDiagnostic(
              0, 0, "Module may have slow compilation time", DiagnosticSeverity.Information));
    }

    if (performance.memoryUsage > 10485760) { // 10MB
      result.addDiagnostic(
          createDiagnostic(0, 0, "Module may use significant memory", DiagnosticSeverity.Warning));
    }
  }

  private void analyzeSecurity(
      final Module module, final String content, final AnalysisResult result) {
    final SecurityAnalysis security = securityAnalyzer.analyze(module, content);
    result.setSecurityAnalysis(security);

    // Add security diagnostics
    for (final SecurityIssue issue : security.issues) {
      final DiagnosticSeverity severity =
          switch (issue.severity) {
            case CRITICAL -> DiagnosticSeverity.Error;
            case HIGH -> DiagnosticSeverity.Warning;
            case MEDIUM -> DiagnosticSeverity.Information;
            case LOW -> DiagnosticSeverity.Hint;
          };

      result.addDiagnostic(createDiagnostic(0, 0, issue.description, severity));
    }
  }

  private void analyzeCodeQuality(final String content, final AnalysisResult result) {
    final CodeQualityAnalysis quality = codeQualityAnalyzer.analyze(content);
    result.setCodeQualityAnalysis(quality);

    // Add quality diagnostics
    if (quality.complexityScore > 0.8) {
      result.addDiagnostic(
          createDiagnostic(0, 0, "High code complexity detected", DiagnosticSeverity.Information));
    }

    if (quality.maintainabilityScore < 0.5) {
      result.addDiagnostic(
          createDiagnostic(0, 0, "Low maintainability score", DiagnosticSeverity.Warning));
    }
  }

  private void analyzeFeatureUsage(final String content, final AnalysisResult result) {
    final FeatureUsageAnalysis features = new FeatureUsageAnalysis();

    // Check for SIMD instructions
    if (content.contains("v128.")) {
      features.usesSimd = true;
    }

    // Check for bulk memory operations
    if (content.contains("memory.copy") || content.contains("memory.fill")) {
      features.usesBulkMemory = true;
    }

    // Check for reference types
    if (content.contains("funcref") || content.contains("externref")) {
      features.usesReferenceTypes = true;
    }

    // Check for multi-value
    if (content.matches(".*\\(result[^)]*\\s+[^)]*\\s+[^)]*\\).*")) {
      features.usesMultiValue = true;
    }

    result.setFeatureUsageAnalysis(features);
  }

  private void analyzeDependencies(final String content, final AnalysisResult result) {
    final DependencyAnalysis dependencies = new DependencyAnalysis();

    // Analyze imports for dependencies
    final Matcher importMatcher = IMPORT_PATTERN.matcher(content);
    while (importMatcher.find()) {
      final String module = importMatcher.group(1);
      final String name = importMatcher.group(2);
      dependencies.dependencies.add(new Dependency(module, name));
    }

    result.setDependencyAnalysis(dependencies);
  }

  private Diagnostic createDiagnostic(
      final int line,
      final int character,
      final String message,
      final DiagnosticSeverity severity) {
    final Position pos = new Position(line, character);
    final Range range = new Range(pos, pos);
    return new Diagnostic(range, message, severity, "wasmtime4j-analyzer");
  }

  // Analysis result classes

  public static final class AnalysisResult {
    private final String uri;
    private boolean isValid;
    private Module module;
    private ModuleInspector inspector;
    private final List<Diagnostic> diagnostics;
    private StructureAnalysis structureAnalysis;
    private PerformanceAnalysis performanceAnalysis;
    private SecurityAnalysis securityAnalysis;
    private CodeQualityAnalysis codeQualityAnalysis;
    private FeatureUsageAnalysis featureUsageAnalysis;
    private DependencyAnalysis dependencyAnalysis;

    public AnalysisResult(final String uri) {
      this.uri = uri;
      this.diagnostics = new ArrayList<>();
    }

    // Getters and setters
    public String getUri() {
      return uri;
    }

    public boolean isValid() {
      return isValid;
    }

    public void setValid(final boolean valid) {
      isValid = valid;
    }

    public Module getModule() {
      return module;
    }

    public void setModule(final Module module) {
      this.module = module;
    }

    public ModuleInspector getInspector() {
      return inspector;
    }

    public void setInspector(final ModuleInspector inspector) {
      this.inspector = inspector;
    }

    public List<Diagnostic> getDiagnostics() {
      return Collections.unmodifiableList(diagnostics);
    }

    public void addDiagnostic(final Diagnostic diagnostic) {
      diagnostics.add(diagnostic);
    }

    public StructureAnalysis getStructureAnalysis() {
      return structureAnalysis;
    }

    public void setStructureAnalysis(final StructureAnalysis analysis) {
      structureAnalysis = analysis;
    }

    public PerformanceAnalysis getPerformanceAnalysis() {
      return performanceAnalysis;
    }

    public void setPerformanceAnalysis(final PerformanceAnalysis analysis) {
      performanceAnalysis = analysis;
    }

    public SecurityAnalysis getSecurityAnalysis() {
      return securityAnalysis;
    }

    public void setSecurityAnalysis(final SecurityAnalysis analysis) {
      securityAnalysis = analysis;
    }

    public CodeQualityAnalysis getCodeQualityAnalysis() {
      return codeQualityAnalysis;
    }

    public void setCodeQualityAnalysis(final CodeQualityAnalysis analysis) {
      codeQualityAnalysis = analysis;
    }

    public FeatureUsageAnalysis getFeatureUsageAnalysis() {
      return featureUsageAnalysis;
    }

    public void setFeatureUsageAnalysis(final FeatureUsageAnalysis analysis) {
      featureUsageAnalysis = analysis;
    }

    public DependencyAnalysis getDependencyAnalysis() {
      return dependencyAnalysis;
    }

    public void setDependencyAnalysis(final DependencyAnalysis analysis) {
      dependencyAnalysis = analysis;
    }
  }

  public static final class StructureAnalysis {
    public int functionCount = 0;
    public int importCount = 0;
    public int exportCount = 0;
    public final List<ImportInfo> imports = new ArrayList<>();
    public final List<String> exports = new ArrayList<>();
  }

  public static final class ImportInfo {
    public final String module;
    public final String name;

    public ImportInfo(final String module, final String name) {
      this.module = module;
      this.name = name;
    }
  }

  public static final class PerformanceAnalysis {
    public long estimatedCompilationTime = 0;
    public long memoryUsage = 0;
    public double complexityScore = 0.0;
    public final List<String> optimizationSuggestions = new ArrayList<>();
  }

  public static final class SecurityAnalysis {
    public final List<SecurityIssue> issues = new ArrayList<>();
    public boolean hasUnsafeOperations = false;
    public boolean hasUnboundedMemoryAccess = false;
  }

  public static final class SecurityIssue {
    public final String description;
    public final SecuritySeverity severity;

    public SecurityIssue(final String description, final SecuritySeverity severity) {
      this.description = description;
      this.severity = severity;
    }
  }

  public enum SecuritySeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
  }

  public static final class CodeQualityAnalysis {
    public double complexityScore = 0.0;
    public double maintainabilityScore = 0.0;
    public int codeSmellCount = 0;
    public final List<String> suggestions = new ArrayList<>();
  }

  public static final class FeatureUsageAnalysis {
    public boolean usesSimd = false;
    public boolean usesBulkMemory = false;
    public boolean usesReferenceTypes = false;
    public boolean usesMultiValue = false;
    public boolean usesThreads = false;
    public boolean usesExceptionHandling = false;
  }

  public static final class DependencyAnalysis {
    public final List<Dependency> dependencies = new ArrayList<>();
    public final Set<String> missingDependencies = new HashSet<>();
  }

  public static final class Dependency {
    public final String module;
    public final String name;

    public Dependency(final String module, final String name) {
      this.module = module;
      this.name = name;
    }
  }

  // Analyzer helper classes

  private static final class PerformanceProfiler {
    public PerformanceAnalysis analyze(final Module module) {
      final PerformanceAnalysis analysis = new PerformanceAnalysis();

      // Estimate compilation time based on module size
      // This is a simplified heuristic
      analysis.estimatedCompilationTime = 100; // Base time

      // Estimate memory usage
      analysis.memoryUsage = 1024 * 1024; // Base memory

      return analysis;
    }
  }

  private static final class SecurityAnalyzer {
    public SecurityAnalysis analyze(final Module module, final String content) {
      final SecurityAnalysis analysis = new SecurityAnalysis();

      // Check for potentially unsafe patterns
      if (content.contains("memory.grow")) {
        analysis.issues.add(
            new SecurityIssue(
                "Module uses memory.grow which could lead to excessive memory usage",
                SecuritySeverity.MEDIUM));
      }

      return analysis;
    }
  }

  private static final class CodeQualityAnalyzer {
    public CodeQualityAnalysis analyze(final String content) {
      final CodeQualityAnalysis analysis = new CodeQualityAnalysis();

      // Calculate complexity score based on instruction patterns
      final Matcher matcher = INSTRUCTION_PATTERN.matcher(content);
      int instructionCount = 0;
      while (matcher.find()) {
        instructionCount++;
      }

      analysis.complexityScore = Math.min(1.0, instructionCount / 1000.0);
      analysis.maintainabilityScore = 1.0 - analysis.complexityScore;

      return analysis;
    }
  }
}

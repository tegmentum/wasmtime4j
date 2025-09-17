package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.List;
import java.util.Set;

/**
 * Validator for template structure and content.
 *
 * @since 1.0.0
 */
public final class TemplateValidator {
  /**
   * Validates the given template.
   *
   * @param template the template to validate
   * @return validation result
   */
  public TemplateValidationResult validate(final ReportTemplate template) {
    final List<String> errors = new java.util.ArrayList<>();
    final List<String> warnings = new java.util.ArrayList<>();

    // Validate template structure
    if (template.getComponents().isEmpty()) {
      warnings.add("Template has no components");
    }

    // Validate component IDs are unique
    final Set<String> componentIds = new java.util.HashSet<>();
    for (final TemplateComponent component : template.getComponents()) {
      if (!componentIds.add(component.getComponentId())) {
        errors.add("Duplicate component ID: " + component.getComponentId());
      }
    }

    // Validate required components
    final boolean hasSummary =
        template.getComponents().stream()
            .anyMatch(component -> component.getComponentType() == ComponentType.SUMMARY);
    if (!hasSummary) {
      warnings.add("Template should include a summary component");
    }

    // Validate template content syntax (basic check)
    for (final TemplateComponent component : template.getComponents()) {
      final String content = component.getTemplateContent();
      if (content.contains("${") && !content.contains("}")) {
        errors.add("Malformed template syntax in component: " + component.getComponentId());
      }
    }

    final boolean isValid = errors.isEmpty();
    return new TemplateValidationResult(isValid, errors, warnings);
  }
}

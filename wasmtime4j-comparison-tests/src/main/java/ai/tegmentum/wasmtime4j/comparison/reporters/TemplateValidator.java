package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Validator for report templates.
 *
 * @since 1.0.0
 */
public final class TemplateValidator {

  public TemplateValidationResult validate(final ReportTemplate template) {
    Objects.requireNonNull(template, "template cannot be null");

    final List<String> errors = new ArrayList<>();
    final List<String> warnings = new ArrayList<>();

    // Validate template name
    if (template.getTemplateName() == null || template.getTemplateName().trim().isEmpty()) {
      errors.add("Template name cannot be null or empty");
    }

    // Validate components
    if (template.getComponents() == null || template.getComponents().isEmpty()) {
      errors.add("Template must have at least one component");
    } else {
      for (int i = 0; i < template.getComponents().size(); i++) {
        final TemplateComponent component = template.getComponents().get(i);
        if (component == null) {
          errors.add("Component at index " + i + " cannot be null");
        } else {
          validateComponent(component, i, errors, warnings);
        }
      }
    }

    return new TemplateValidationResult(errors.isEmpty(), errors, warnings);
  }

  private void validateComponent(
      final TemplateComponent component,
      final int index,
      final List<String> errors,
      final List<String> warnings) {

    // Validate component ID
    if (component.getComponentId() == null || component.getComponentId().trim().isEmpty()) {
      errors.add("Component at index " + index + " must have a valid ID");
    }

    // Validate template content
    if (component.getTemplateContent() == null || component.getTemplateContent().trim().isEmpty()) {
      errors.add("Component " + component.getComponentId() + " must have template content");
    }

    // Check for potential issues
    final String content = component.getTemplateContent();
    if (content != null) {
      if (content.contains("${") && !content.contains("}")) {
        warnings.add("Component " + component.getComponentId() + " may have unclosed variable references");
      }

      if (content.length() > 10000) {
        warnings.add("Component " + component.getComponentId() + " has very large template content");
      }
    }
  }
}
package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Message resolver for internationalization support in templates.
 *
 * @since 1.0.0
 */
final class MessageResolver {
  private static final Logger LOGGER = Logger.getLogger(MessageResolver.class.getName());

  private final String resourceBundleName;
  private final Locale locale;
  private final ResourceBundle resourceBundle;

  public MessageResolver(final String resourceBundleName, final Locale locale) {
    this.resourceBundleName =
        Objects.requireNonNull(resourceBundleName, "resourceBundleName cannot be null");
    this.locale = Objects.requireNonNull(locale, "locale cannot be null");
    this.resourceBundle = loadResourceBundle();
  }

  /**
   * Gets a localized message for the given key.
   *
   * @param key the message key
   * @return the localized message or the key if not found
   */
  public String getMessage(final String key) {
    Objects.requireNonNull(key, "key cannot be null");

    if (resourceBundle == null) {
      return key;
    }

    try {
      return resourceBundle.getString(key);
    } catch (final MissingResourceException e) {
      LOGGER.log(Level.FINE, "Missing resource key: {0}", key);
      return key;
    }
  }

  /**
   * Gets a localized message with parameter substitution.
   *
   * @param key the message key
   * @param params the parameters for substitution
   * @return the formatted localized message
   */
  public String getMessage(final String key, final Object... params) {
    final String message = getMessage(key);
    if (params.length == 0 || message.equals(key)) {
      return message;
    }

    try {
      return MessageFormat.format(message, params);
    } catch (final IllegalArgumentException e) {
      LOGGER.log(Level.WARNING, "Failed to format message for key: {0}", key);
      return message;
    }
  }

  /**
   * Checks if a message key exists in the resource bundle.
   *
   * @param key the message key
   * @return true if the key exists
   */
  public boolean containsKey(final String key) {
    Objects.requireNonNull(key, "key cannot be null");

    if (resourceBundle == null) {
      return false;
    }

    return resourceBundle.containsKey(key);
  }

  public Locale getLocale() {
    return locale;
  }

  public String getResourceBundleName() {
    return resourceBundleName;
  }

  private ResourceBundle loadResourceBundle() {
    try {
      return ResourceBundle.getBundle(resourceBundleName, locale);
    } catch (final MissingResourceException e) {
      LOGGER.log(
          Level.WARNING,
          "Could not load resource bundle: {0} for locale: {1}",
          new Object[] {resourceBundleName, locale});
      return null;
    }
  }
}

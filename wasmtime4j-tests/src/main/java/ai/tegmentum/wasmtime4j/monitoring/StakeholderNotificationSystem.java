package ai.tegmentum.wasmtime4j.monitoring;

import ai.tegmentum.wasmtime4j.monitoring.CoverageRegressionDetector.RegressionEvent;
import ai.tegmentum.wasmtime4j.monitoring.ExecutiveDashboard.StakeholderNotification;
import ai.tegmentum.wasmtime4j.monitoring.PredictiveCoverageAnalytics.PredictiveAlert;
import ai.tegmentum.wasmtime4j.monitoring.RealTimeCoverageMonitor.CoverageHealthAssessment;
import ai.tegmentum.wasmtime4j.monitoring.RealTimeCoverageMonitor.HealthStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Comprehensive stakeholder alerting and notification system that provides multi-channel
 * communication, escalation procedures, and intelligent alert management for coverage monitoring
 * and quality assurance stakeholders.
 *
 * <p>Features:
 *
 * <ul>
 *   <li>Multi-channel notification delivery (email, Slack, webhooks, dashboard)
 *   <li>Intelligent escalation procedures with stakeholder hierarchies
 *   <li>Alert deduplication and rate limiting to prevent notification fatigue
 *   <li>Custom notification templates and content personalization
 *   <li>Delivery tracking and acknowledgment management
 * </ul>
 *
 * @since 1.0.0
 */
public final class StakeholderNotificationSystem {
  private static final Logger LOGGER = Logger.getLogger(StakeholderNotificationSystem.class.getName());

  // Notification configuration
  private static final Duration ESCALATION_TIMEOUT = Duration.ofMinutes(30);
  private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(5);
  private static final int MAX_NOTIFICATIONS_PER_WINDOW = 3;
  private static final Path NOTIFICATION_LOG_DIR = Paths.get("target", "notifications");

  // Stakeholder configuration
  private static final Map<String, StakeholderProfile> STAKEHOLDER_PROFILES;

  static {
    final Map<String, StakeholderProfile> profiles = new HashMap<>();

    // Executive stakeholders
    profiles.put("CTO", new StakeholderProfile(
        "CTO", "Chief Technology Officer",
        List.of(NotificationChannel.EMAIL, NotificationChannel.SLACK),
        NotificationPriority.CRITICAL,
        Duration.ofHours(2), // Response time expectation
        List.of("VP Engineering", "Director of Engineering")
    ));

    profiles.put("VP Engineering", new StakeholderProfile(
        "VP Engineering", "Vice President of Engineering",
        List.of(NotificationChannel.EMAIL, NotificationChannel.SLACK, NotificationChannel.DASHBOARD),
        NotificationPriority.HIGH,
        Duration.ofMinutes(30),
        List.of("Director of Engineering", "QA Director")
    ));

    profiles.put("QA Director", new StakeholderProfile(
        "QA Director", "Director of Quality Assurance",
        List.of(NotificationChannel.EMAIL, NotificationChannel.SLACK, NotificationChannel.DASHBOARD),
        NotificationPriority.MEDIUM,
        Duration.ofMinutes(15),
        List.of("QA Lead", "Test Automation Lead")
    ));

    profiles.put("Development Lead", new StakeholderProfile(
        "Development Lead", "Development Team Lead",
        List.of(NotificationChannel.SLACK, NotificationChannel.DASHBOARD),
        NotificationPriority.MEDIUM,
        Duration.ofMinutes(15),
        List.of("Senior Developer", "DevOps Engineer")
    ));

    profiles.put("QA Team", new StakeholderProfile(
        "QA Team", "Quality Assurance Team",
        List.of(NotificationChannel.SLACK, NotificationChannel.DASHBOARD),
        NotificationPriority.LOW,
        Duration.ofMinutes(30),
        List.of()
    ));

    STAKEHOLDER_PROFILES = Map.copyOf(profiles);
  }

  // System state
  private final ScheduledExecutorService scheduler;
  private final Map<String, NotificationChannel> channels;
  private final List<NotificationDelivery> deliveryHistory;
  private final Map<String, RateLimiter> rateLimiters;
  private final List<EscalationRule> escalationRules;
  private final NotificationTemplateEngine templateEngine;

  /**
   * Creates a new stakeholder notification system.
   */
  public StakeholderNotificationSystem() {
    this.scheduler = Executors.newScheduledThreadPool(2);
    this.channels = new ConcurrentHashMap<>();
    this.deliveryHistory = new ArrayList<>();
    this.rateLimiters = new ConcurrentHashMap<>();
    this.escalationRules = new ArrayList<>();
    this.templateEngine = new NotificationTemplateEngine();

    initializeNotificationChannels();
    initializeEscalationRules();
    initializeRateLimiters();
    startPeriodicMaintenance();

    LOGGER.info("Stakeholder notification system initialized with " + channels.size() + " channels");
  }

  /**
   * Sends a coverage-related notification to stakeholders.
   *
   * @param notification the stakeholder notification to send
   * @return notification delivery result
   */
  public NotificationDeliveryResult sendNotification(final StakeholderNotification notification) {
    final Instant deliveryTime = Instant.now();
    final List<DeliveryAttempt> deliveryAttempts = new ArrayList<>();

    LOGGER.info(String.format("Sending notification: %s to %d recipients",
        notification.getTitle(), notification.getRecipients().size()));

    for (final String recipient : notification.getRecipients()) {
      final StakeholderProfile profile = STAKEHOLDER_PROFILES.get(recipient);
      if (profile == null) {
        LOGGER.warning("Unknown stakeholder: " + recipient);
        continue;
      }

      // Check rate limiting
      if (isRateLimited(recipient)) {
        LOGGER.warning("Rate limit exceeded for stakeholder: " + recipient);
        deliveryAttempts.add(new DeliveryAttempt(
            recipient, NotificationChannel.NONE, DeliveryStatus.RATE_LIMITED, "Rate limit exceeded"));
        continue;
      }

      // Attempt delivery through each channel
      for (final NotificationChannel channelType : profile.getChannels()) {
        final NotificationChannel channel = channels.get(channelType.name());
        if (channel != null) {
          final DeliveryAttempt attempt = attemptDelivery(notification, recipient, channel, profile);
          deliveryAttempts.add(attempt);

          if (attempt.getStatus() == DeliveryStatus.SUCCESS) {
            updateRateLimit(recipient);
            break; // Success, no need to try other channels
          }
        }
      }
    }

    final NotificationDelivery delivery = new NotificationDelivery(
        notification.getId(),
        notification,
        deliveryTime,
        deliveryAttempts
    );
    deliveryHistory.add(delivery);

    // Schedule escalation if needed
    scheduleEscalation(notification, delivery);

    // Log notification
    logNotification(delivery);

    final long successfulDeliveries = deliveryAttempts.stream()
        .mapToLong(attempt -> attempt.getStatus() == DeliveryStatus.SUCCESS ? 1 : 0)
        .sum();

    final NotificationDeliveryResult result = new NotificationDeliveryResult(
        delivery.getDeliveryId(),
        deliveryTime,
        successfulDeliveries,
        deliveryAttempts.size(),
        deliveryAttempts
    );

    LOGGER.info(String.format("Notification delivery completed: %d/%d successful",
        successfulDeliveries, deliveryAttempts.size()));

    return result;
  }

  /**
   * Sends a regression alert to relevant stakeholders.
   *
   * @param regressionEvent the regression event to alert about
   * @return notification delivery result
   */
  public NotificationDeliveryResult sendRegressionAlert(final RegressionEvent regressionEvent) {
    final String title = String.format("Coverage Regression Alert - %s", regressionEvent.getType());
    final String message = String.format(
        "Regression detected: %s\n\nSeverity: %s\nDescription: %s\nTimestamp: %s",
        regressionEvent.getType(),
        regressionEvent.getSeverity(),
        regressionEvent.getDescription(),
        regressionEvent.getTimestamp()
    );

    final List<String> recipients = determineRegressionRecipients(regressionEvent);

    final StakeholderNotification notification = new StakeholderNotification(
        "REGRESSION_" + System.currentTimeMillis(),
        mapSeverityToNotificationType(regressionEvent.getSeverity()),
        title,
        message,
        recipients,
        regressionEvent.getTimestamp()
    );

    return sendNotification(notification);
  }

  /**
   * Sends a predictive alert to relevant stakeholders.
   *
   * @param predictiveAlert the predictive alert to send
   * @return notification delivery result
   */
  public NotificationDeliveryResult sendPredictiveAlert(final PredictiveAlert predictiveAlert) {
    final String title = String.format("Predictive Alert - %s", predictiveAlert.getTitle());
    final String message = String.format(
        "Predictive analysis alert: %s\n\nSeverity: %s\nMessage: %s\nTimestamp: %s",
        predictiveAlert.getTitle(),
        predictiveAlert.getSeverity(),
        predictiveAlert.getMessage(),
        predictiveAlert.getTimestamp()
    );

    final List<String> recipients = determinePredictiveRecipients(predictiveAlert);

    final StakeholderNotification notification = new StakeholderNotification(
        "PREDICTIVE_" + System.currentTimeMillis(),
        mapAlertSeverityToNotificationType(predictiveAlert.getSeverity()),
        title,
        message,
        recipients,
        predictiveAlert.getTimestamp()
    );

    return sendNotification(notification);
  }

  /**
   * Sends a health status change notification.
   *
   * @param healthAssessment the health assessment to notify about
   * @return notification delivery result
   */
  public NotificationDeliveryResult sendHealthStatusNotification(final CoverageHealthAssessment healthAssessment) {
    final String title = String.format("Coverage Health Status: %s", healthAssessment.getStatus());
    final String message = formatHealthStatusMessage(healthAssessment);

    final List<String> recipients = determineHealthStatusRecipients(healthAssessment.getStatus());

    final StakeholderNotification notification = new StakeholderNotification(
        "HEALTH_" + System.currentTimeMillis(),
        mapHealthStatusToNotificationType(healthAssessment.getStatus()),
        title,
        message,
        recipients,
        Instant.now()
    );

    return sendNotification(notification);
  }

  /**
   * Gets notification delivery statistics.
   *
   * @return notification statistics
   */
  public NotificationStatistics getNotificationStatistics() {
    final int totalNotifications = deliveryHistory.size();
    final long successfulDeliveries = deliveryHistory.stream()
        .flatMap(delivery -> delivery.getAttempts().stream())
        .mapToLong(attempt -> attempt.getStatus() == DeliveryStatus.SUCCESS ? 1 : 0)
        .sum();

    final long totalAttempts = deliveryHistory.stream()
        .flatMap(delivery -> delivery.getAttempts().stream())
        .count();

    final double deliveryRate = totalAttempts > 0 ? (double) successfulDeliveries / totalAttempts : 0.0;

    final Map<NotificationChannel, Integer> channelUsage = new HashMap<>();
    for (final NotificationChannel channelType : NotificationChannel.values()) {
      channelUsage.put(channelType, 0);
    }

    deliveryHistory.stream()
        .flatMap(delivery -> delivery.getAttempts().stream())
        .forEach(attempt -> channelUsage.merge(attempt.getChannel(), 1, Integer::sum));

    return new NotificationStatistics(
        totalNotifications,
        (int) successfulDeliveries,
        (int) totalAttempts,
        deliveryRate,
        channelUsage
    );
  }

  /**
   * Acknowledges a notification delivery.
   *
   * @param deliveryId the delivery ID to acknowledge
   * @param stakeholder the stakeholder acknowledging
   * @return acknowledgment result
   */
  public AcknowledgmentResult acknowledgeNotification(final String deliveryId, final String stakeholder) {
    final NotificationDelivery delivery = deliveryHistory.stream()
        .filter(d -> d.getDeliveryId().equals(deliveryId))
        .findFirst()
        .orElse(null);

    if (delivery == null) {
      return new AcknowledgmentResult(false, "Delivery not found");
    }

    // Mark as acknowledged
    delivery.acknowledge(stakeholder, Instant.now());

    LOGGER.info(String.format("Notification %s acknowledged by %s", deliveryId, stakeholder));

    return new AcknowledgmentResult(true, "Acknowledged successfully");
  }

  /**
   * Shuts down the notification system gracefully.
   */
  public void shutdown() {
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
    LOGGER.info("Stakeholder notification system shut down");
  }

  private void initializeNotificationChannels() {
    try {
      Files.createDirectories(NOTIFICATION_LOG_DIR);
    } catch (IOException e) {
      LOGGER.warning("Failed to create notification log directory: " + e.getMessage());
    }

    // Initialize channels
    channels.put("EMAIL", new EmailNotificationChannel());
    channels.put("SLACK", new SlackNotificationChannel());
    channels.put("DASHBOARD", new DashboardNotificationChannel());
    channels.put("WEBHOOK", new WebhookNotificationChannel());
  }

  private void initializeEscalationRules() {
    // Critical alerts escalate to CTO if not acknowledged within 30 minutes
    escalationRules.add(new EscalationRule(
        ExecutiveDashboard.NotificationType.CRITICAL_ALERT,
        Duration.ofMinutes(30),
        List.of("CTO"),
        "Critical alert escalation"
    ));

    // Warning alerts escalate to VP Engineering if not acknowledged within 1 hour
    escalationRules.add(new EscalationRule(
        ExecutiveDashboard.NotificationType.WARNING,
        Duration.ofHours(1),
        List.of("VP Engineering"),
        "Warning alert escalation"
    ));
  }

  private void initializeRateLimiters() {
    for (final String stakeholder : STAKEHOLDER_PROFILES.keySet()) {
      rateLimiters.put(stakeholder, new RateLimiter(MAX_NOTIFICATIONS_PER_WINDOW, RATE_LIMIT_WINDOW));
    }
  }

  private void startPeriodicMaintenance() {
    // Clean up old notifications
    scheduler.scheduleAtFixedRate(this::performMaintenance, 1, 1, TimeUnit.HOURS);

    // Process escalations
    scheduler.scheduleAtFixedRate(this::processEscalations, 1, 5, TimeUnit.MINUTES);
  }

  private DeliveryAttempt attemptDelivery(
      final StakeholderNotification notification,
      final String recipient,
      final NotificationChannel channel,
      final StakeholderProfile profile) {

    try {
      final String personalizedContent = templateEngine.generateContent(notification, recipient, profile);
      final boolean success = channel.send(recipient, notification.getTitle(), personalizedContent);

      final DeliveryStatus status = success ? DeliveryStatus.SUCCESS : DeliveryStatus.FAILED;
      final String statusMessage = success ? "Delivered successfully" : "Delivery failed";

      return new DeliveryAttempt(recipient, channel.getChannelType(), status, statusMessage);

    } catch (Exception e) {
      LOGGER.warning(String.format("Delivery attempt failed for %s via %s: %s",
          recipient, channel.getChannelType(), e.getMessage()));

      return new DeliveryAttempt(recipient, channel.getChannelType(), DeliveryStatus.ERROR, e.getMessage());
    }
  }

  private boolean isRateLimited(final String stakeholder) {
    final RateLimiter rateLimiter = rateLimiters.get(stakeholder);
    return rateLimiter != null && !rateLimiter.allowRequest();
  }

  private void updateRateLimit(final String stakeholder) {
    final RateLimiter rateLimiter = rateLimiters.get(stakeholder);
    if (rateLimiter != null) {
      rateLimiter.recordRequest();
    }
  }

  private void scheduleEscalation(final StakeholderNotification notification, final NotificationDelivery delivery) {
    for (final EscalationRule rule : escalationRules) {
      if (rule.getNotificationType() == notification.getType()) {
        scheduler.schedule(
            () -> processEscalation(rule, notification, delivery),
            rule.getEscalationDelay().toMinutes(),
            TimeUnit.MINUTES
        );
      }
    }
  }

  private void processEscalation(
      final EscalationRule rule,
      final StakeholderNotification notification,
      final NotificationDelivery delivery) {

    if (delivery.isAcknowledged()) {
      return; // Already acknowledged, no escalation needed
    }

    LOGGER.info(String.format("Escalating notification %s: %s",
        notification.getId(), rule.getDescription()));

    final StakeholderNotification escalatedNotification = new StakeholderNotification(
        "ESCALATED_" + notification.getId(),
        ExecutiveDashboard.NotificationType.CRITICAL_ALERT,
        "ESCALATED: " + notification.getTitle(),
        "This notification has been escalated due to lack of acknowledgment.\n\n" + notification.getMessage(),
        rule.getEscalationTargets(),
        Instant.now()
    );

    sendNotification(escalatedNotification);
  }

  private void processEscalations() {
    // Process any pending escalations (simplified implementation)
    LOGGER.fine("Processing escalations...");
  }

  private void performMaintenance() {
    // Clean up old delivery history
    final Instant cutoff = Instant.now().minus(Duration.ofDays(30));
    deliveryHistory.removeIf(delivery -> delivery.getDeliveryTime().isBefore(cutoff));

    // Reset rate limiters
    rateLimiters.values().forEach(RateLimiter::reset);

    LOGGER.fine("Notification system maintenance completed");
  }

  private void logNotification(final NotificationDelivery delivery) {
    try {
      final String timestamp = delivery.getDeliveryTime().toString().replace(":", "-");
      final Path logFile = NOTIFICATION_LOG_DIR.resolve("notification-" + timestamp + ".log");

      final StringBuilder logContent = new StringBuilder();
      logContent.append("Notification Delivery Log\n");
      logContent.append("Delivery ID: ").append(delivery.getDeliveryId()).append("\n");
      logContent.append("Title: ").append(delivery.getNotification().getTitle()).append("\n");
      logContent.append("Type: ").append(delivery.getNotification().getType()).append("\n");
      logContent.append("Recipients: ").append(delivery.getNotification().getRecipients()).append("\n");
      logContent.append("Delivery Time: ").append(delivery.getDeliveryTime()).append("\n");
      logContent.append("Attempts: ").append(delivery.getAttempts().size()).append("\n");

      for (final DeliveryAttempt attempt : delivery.getAttempts()) {
        logContent.append("  - ").append(attempt.getRecipient())
            .append(" via ").append(attempt.getChannel())
            .append(": ").append(attempt.getStatus())
            .append(" (").append(attempt.getStatusMessage()).append(")\n");
      }

      Files.writeString(logFile, logContent.toString());

    } catch (IOException e) {
      LOGGER.warning("Failed to log notification: " + e.getMessage());
    }
  }

  private List<String> determineRegressionRecipients(final RegressionEvent regressionEvent) {
    return switch (regressionEvent.getSeverity()) {
      case CRITICAL -> List.of("CTO", "VP Engineering", "QA Director", "Development Lead");
      case MAJOR -> List.of("VP Engineering", "QA Director", "Development Lead");
      case MINOR -> List.of("QA Director", "QA Team");
    };
  }

  private List<String> determinePredictiveRecipients(final PredictiveAlert predictiveAlert) {
    return switch (predictiveAlert.getSeverity()) {
      case CRITICAL -> List.of("CTO", "VP Engineering", "QA Director");
      case HIGH -> List.of("VP Engineering", "QA Director", "Development Lead");
      case MEDIUM -> List.of("QA Director", "Development Lead");
      case LOW -> List.of("QA Team");
    };
  }

  private List<String> determineHealthStatusRecipients(final HealthStatus status) {
    return switch (status) {
      case CRITICAL -> List.of("CTO", "VP Engineering", "QA Director", "Development Lead");
      case WARNING -> List.of("VP Engineering", "QA Director", "Development Lead");
      case HEALTHY -> List.of(); // No notifications for healthy status
      case UNKNOWN -> List.of("QA Director");
    };
  }

  private ExecutiveDashboard.NotificationType mapSeverityToNotificationType(
      final CoverageRegressionDetector.RegressionSeverity severity) {
    return switch (severity) {
      case CRITICAL -> ExecutiveDashboard.NotificationType.CRITICAL_ALERT;
      case MAJOR -> ExecutiveDashboard.NotificationType.WARNING;
      case MINOR -> ExecutiveDashboard.NotificationType.INFORMATION;
    };
  }

  private ExecutiveDashboard.NotificationType mapAlertSeverityToNotificationType(
      final PredictiveCoverageAnalytics.AlertSeverity severity) {
    return switch (severity) {
      case CRITICAL -> ExecutiveDashboard.NotificationType.CRITICAL_ALERT;
      case HIGH, MEDIUM -> ExecutiveDashboard.NotificationType.WARNING;
      case LOW -> ExecutiveDashboard.NotificationType.INFORMATION;
    };
  }

  private ExecutiveDashboard.NotificationType mapHealthStatusToNotificationType(final HealthStatus status) {
    return switch (status) {
      case CRITICAL -> ExecutiveDashboard.NotificationType.CRITICAL_ALERT;
      case WARNING -> ExecutiveDashboard.NotificationType.WARNING;
      case HEALTHY, UNKNOWN -> ExecutiveDashboard.NotificationType.INFORMATION;
    };
  }

  private String formatHealthStatusMessage(final CoverageHealthAssessment healthAssessment) {
    final StringBuilder message = new StringBuilder();
    message.append("Coverage Health Status Update\n\n");
    message.append("Status: ").append(healthAssessment.getStatus()).append("\n");
    message.append("Summary: ").append(healthAssessment.getSummary()).append("\n\n");

    if (!healthAssessment.getIssues().isEmpty()) {
      message.append("Issues Identified:\n");
      for (final String issue : healthAssessment.getIssues()) {
        message.append("- ").append(issue).append("\n");
      }
      message.append("\n");
    }

    if (!healthAssessment.getRecommendations().isEmpty()) {
      message.append("Recommendations:\n");
      for (final String recommendation : healthAssessment.getRecommendations()) {
        message.append("- ").append(recommendation).append("\n");
      }
    }

    return message.toString();
  }

  // Enumerations and data classes
  public enum NotificationChannel { EMAIL, SLACK, DASHBOARD, WEBHOOK, NONE }
  public enum NotificationPriority { LOW, MEDIUM, HIGH, CRITICAL }
  public enum DeliveryStatus { SUCCESS, FAILED, ERROR, RATE_LIMITED }

  // Stakeholder configuration
  public static final class StakeholderProfile {
    private final String id;
    private final String title;
    private final List<NotificationChannel> channels;
    private final NotificationPriority priority;
    private final Duration responseTime;
    private final List<String> escalationTargets;

    public StakeholderProfile(String id, String title, List<NotificationChannel> channels,
                            NotificationPriority priority, Duration responseTime, List<String> escalationTargets) {
      this.id = id;
      this.title = title;
      this.channels = List.copyOf(channels);
      this.priority = priority;
      this.responseTime = responseTime;
      this.escalationTargets = List.copyOf(escalationTargets);
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public List<NotificationChannel> getChannels() { return channels; }
    public NotificationPriority getPriority() { return priority; }
    public Duration getResponseTime() { return responseTime; }
    public List<String> getEscalationTargets() { return escalationTargets; }
  }

  // Notification delivery tracking
  public static final class NotificationDelivery {
    private final String deliveryId;
    private final StakeholderNotification notification;
    private final Instant deliveryTime;
    private final List<DeliveryAttempt> attempts;
    private boolean acknowledged;
    private String acknowledgedBy;
    private Instant acknowledgedAt;

    public NotificationDelivery(String deliveryId, StakeholderNotification notification,
                              Instant deliveryTime, List<DeliveryAttempt> attempts) {
      this.deliveryId = deliveryId;
      this.notification = notification;
      this.deliveryTime = deliveryTime;
      this.attempts = List.copyOf(attempts);
      this.acknowledged = false;
    }

    public void acknowledge(String stakeholder, Instant timestamp) {
      this.acknowledged = true;
      this.acknowledgedBy = stakeholder;
      this.acknowledgedAt = timestamp;
    }

    public String getDeliveryId() { return deliveryId; }
    public StakeholderNotification getNotification() { return notification; }
    public Instant getDeliveryTime() { return deliveryTime; }
    public List<DeliveryAttempt> getAttempts() { return attempts; }
    public boolean isAcknowledged() { return acknowledged; }
    public String getAcknowledgedBy() { return acknowledgedBy; }
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
  }

  public static final class DeliveryAttempt {
    private final String recipient;
    private final NotificationChannel channel;
    private final DeliveryStatus status;
    private final String statusMessage;

    public DeliveryAttempt(String recipient, NotificationChannel channel, DeliveryStatus status, String statusMessage) {
      this.recipient = recipient;
      this.channel = channel;
      this.status = status;
      this.statusMessage = statusMessage;
    }

    public String getRecipient() { return recipient; }
    public NotificationChannel getChannel() { return channel; }
    public DeliveryStatus getStatus() { return status; }
    public String getStatusMessage() { return statusMessage; }
  }

  // Rate limiting
  private static final class RateLimiter {
    private final int maxRequests;
    private final Duration window;
    private final List<Instant> requests;

    public RateLimiter(int maxRequests, Duration window) {
      this.maxRequests = maxRequests;
      this.window = window;
      this.requests = new ArrayList<>();
    }

    public boolean allowRequest() {
      cleanupOldRequests();
      return requests.size() < maxRequests;
    }

    public void recordRequest() {
      requests.add(Instant.now());
    }

    public void reset() {
      requests.clear();
    }

    private void cleanupOldRequests() {
      final Instant cutoff = Instant.now().minus(window);
      requests.removeIf(request -> request.isBefore(cutoff));
    }
  }

  // Escalation management
  private static final class EscalationRule {
    private final ExecutiveDashboard.NotificationType notificationType;
    private final Duration escalationDelay;
    private final List<String> escalationTargets;
    private final String description;

    public EscalationRule(ExecutiveDashboard.NotificationType notificationType, Duration escalationDelay,
                         List<String> escalationTargets, String description) {
      this.notificationType = notificationType;
      this.escalationDelay = escalationDelay;
      this.escalationTargets = List.copyOf(escalationTargets);
      this.description = description;
    }

    public ExecutiveDashboard.NotificationType getNotificationType() { return notificationType; }
    public Duration getEscalationDelay() { return escalationDelay; }
    public List<String> getEscalationTargets() { return escalationTargets; }
    public String getDescription() { return description; }
  }

  // Template engine for personalized content
  private static final class NotificationTemplateEngine {
    public String generateContent(StakeholderNotification notification, String recipient, StakeholderProfile profile) {
      final StringBuilder content = new StringBuilder();

      content.append("Dear ").append(profile.getTitle()).append(",\n\n");
      content.append(notification.getMessage()).append("\n\n");

      content.append("This notification was sent to you based on your role as ").append(profile.getTitle()).append(".\n");
      content.append("Expected response time: ").append(formatDuration(profile.getResponseTime())).append("\n\n");

      content.append("For more details, please check the coverage dashboard.\n\n");
      content.append("Best regards,\nWasmtime4j Coverage Monitoring System");

      return content.toString();
    }

    private String formatDuration(Duration duration) {
      if (duration.toDays() > 0) {
        return duration.toDays() + " days";
      } else if (duration.toHours() > 0) {
        return duration.toHours() + " hours";
      } else {
        return duration.toMinutes() + " minutes";
      }
    }
  }

  // Notification channels (simplified implementations)
  private interface NotificationChannel {
    boolean send(String recipient, String title, String content);
    NotificationChannel getChannelType();
  }

  private static final class EmailNotificationChannel implements NotificationChannel {
    @Override
    public boolean send(String recipient, String title, String content) {
      // Simplified email sending (would integrate with actual email service)
      LOGGER.info(String.format("Sending email to %s: %s", recipient, title));
      return true; // Assume success for demo
    }

    @Override
    public NotificationChannel getChannelType() {
      return NotificationChannel.EMAIL;
    }
  }

  private static final class SlackNotificationChannel implements NotificationChannel {
    @Override
    public boolean send(String recipient, String title, String content) {
      // Simplified Slack sending (would integrate with Slack API)
      LOGGER.info(String.format("Sending Slack message to %s: %s", recipient, title));
      return true; // Assume success for demo
    }

    @Override
    public NotificationChannel getChannelType() {
      return NotificationChannel.SLACK;
    }
  }

  private static final class DashboardNotificationChannel implements NotificationChannel {
    @Override
    public boolean send(String recipient, String title, String content) {
      // Simplified dashboard notification (would update dashboard UI)
      LOGGER.info(String.format("Posting dashboard notification for %s: %s", recipient, title));
      return true; // Assume success for demo
    }

    @Override
    public NotificationChannel getChannelType() {
      return NotificationChannel.DASHBOARD;
    }
  }

  private static final class WebhookNotificationChannel implements NotificationChannel {
    @Override
    public boolean send(String recipient, String title, String content) {
      // Simplified webhook sending (would make HTTP requests)
      LOGGER.info(String.format("Sending webhook for %s: %s", recipient, title));
      return true; // Assume success for demo
    }

    @Override
    public NotificationChannel getChannelType() {
      return NotificationChannel.WEBHOOK;
    }
  }

  // Result classes
  public static final class NotificationDeliveryResult {
    private final String deliveryId;
    private final Instant deliveryTime;
    private final long successfulDeliveries;
    private final long totalAttempts;
    private final List<DeliveryAttempt> attempts;

    public NotificationDeliveryResult(String deliveryId, Instant deliveryTime, long successfulDeliveries,
                                    long totalAttempts, List<DeliveryAttempt> attempts) {
      this.deliveryId = deliveryId;
      this.deliveryTime = deliveryTime;
      this.successfulDeliveries = successfulDeliveries;
      this.totalAttempts = totalAttempts;
      this.attempts = List.copyOf(attempts);
    }

    public String getDeliveryId() { return deliveryId; }
    public Instant getDeliveryTime() { return deliveryTime; }
    public long getSuccessfulDeliveries() { return successfulDeliveries; }
    public long getTotalAttempts() { return totalAttempts; }
    public List<DeliveryAttempt> getAttempts() { return attempts; }
  }

  public static final class NotificationStatistics {
    private final int totalNotifications;
    private final int successfulDeliveries;
    private final int totalAttempts;
    private final double deliveryRate;
    private final Map<NotificationChannel, Integer> channelUsage;

    public NotificationStatistics(int totalNotifications, int successfulDeliveries, int totalAttempts,
                                double deliveryRate, Map<NotificationChannel, Integer> channelUsage) {
      this.totalNotifications = totalNotifications;
      this.successfulDeliveries = successfulDeliveries;
      this.totalAttempts = totalAttempts;
      this.deliveryRate = deliveryRate;
      this.channelUsage = Map.copyOf(channelUsage);
    }

    public int getTotalNotifications() { return totalNotifications; }
    public int getSuccessfulDeliveries() { return successfulDeliveries; }
    public int getTotalAttempts() { return totalAttempts; }
    public double getDeliveryRate() { return deliveryRate; }
    public Map<NotificationChannel, Integer> getChannelUsage() { return channelUsage; }
  }

  public static final class AcknowledgmentResult {
    private final boolean success;
    private final String message;

    public AcknowledgmentResult(boolean success, String message) {
      this.success = success;
      this.message = message;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
  }
}
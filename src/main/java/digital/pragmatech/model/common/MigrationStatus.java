package digital.pragmatech.model.common;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MigrationStatus {
  private String id;
  private MigrationPhase phase;
  private MigrationState state;
  private LocalDateTime startedAt;
  private LocalDateTime completedAt;
  private Progress progress;
  private Statistics statistics;
  @Builder.Default private List<MigrationError> errors = new ArrayList<>();

  public enum MigrationPhase {
    INITIALIZATION,
    TAG_GROUP_MIGRATION,
    ECOMMERCE_SETUP,
    SUBSCRIBER_MIGRATION,
    CAMPAIGN_MIGRATION,
    COMPLETION
  }

  public enum MigrationState {
    NOT_STARTED,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    FAILED
  }

  @Data
  @Builder
  public static class Progress {
    private int totalItems;
    private int processedItems;
    private int successfulItems;
    private int failedItems;
    private double percentComplete;

    public void updatePercentComplete() {
      if (totalItems > 0) {
        this.percentComplete = (double) processedItems / totalItems * 100;
      }
    }
  }

  @Data
  @Builder
  public static class Statistics {
    private int totalSubscribers;
    private int migratedSubscribers;
    private int totalTags;
    private int migratedGroups;
    private int totalProducts;
    private int migratedProducts;
    private int totalCategories;
    private int migratedCategories;
    private long estimatedTimeRemaining;
  }

  @Data
  @Builder
  public static class MigrationError {
    private LocalDateTime timestamp;
    private String phase;
    private String entity;
    private String entityId;
    private String errorMessage;
    private String errorCode;
    private boolean retryable;
  }
}

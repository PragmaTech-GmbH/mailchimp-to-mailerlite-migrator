package digital.pragmatech.controller;

import digital.pragmatech.dto.response.ApiResponse;
import digital.pragmatech.model.common.MigrationStatus;
import digital.pragmatech.service.migration.MigrationOrchestrator;
import digital.pragmatech.service.migration.MigrationProgressTracker;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/migration")
@RequiredArgsConstructor
public class MigrationController {

  private final MigrationOrchestrator migrationOrchestrator;
  private final MigrationProgressTracker progressTracker;

  @PostMapping("/start")
  public ApiResponse<String> startMigration() {
    if (progressTracker.isMigrationInProgress()) {
      return ApiResponse.error("Migration is already in progress", "MIGRATION_IN_PROGRESS");
    }

    try {
      String migrationId = migrationOrchestrator.startMigration();
      return ApiResponse.success("Migration started successfully", migrationId);
    } catch (Exception e) {
      log.error("Failed to start migration", e);
      return ApiResponse.error(
          "Failed to start migration: " + e.getMessage(), "MIGRATION_START_FAILED");
    }
  }

  @GetMapping("/status")
  public ResponseEntity<ApiResponse<MigrationStatus>> getMigrationStatus() {
    MigrationStatus status = progressTracker.getCurrentStatus();

    if (status == null) {
      return ResponseEntity.ok(ApiResponse.error("No migration in progress", "NO_MIGRATION"));
    }

    return ResponseEntity.ok(ApiResponse.success(status));
  }

  @PostMapping("/pause")
  public ResponseEntity<ApiResponse<String>> pauseMigration() {
    MigrationStatus current = progressTracker.getCurrentStatus();

    if (current == null) {
      return ResponseEntity.ok(ApiResponse.error("No migration in progress", "NO_MIGRATION"));
    }

    if (current.getState() != MigrationStatus.MigrationState.IN_PROGRESS) {
      return ResponseEntity.ok(
          ApiResponse.error("Migration is not in progress", "MIGRATION_NOT_IN_PROGRESS"));
    }

    progressTracker.pauseMigration();
    return ResponseEntity.ok(ApiResponse.success("Migration paused successfully"));
  }

  @PostMapping("/resume")
  public ResponseEntity<ApiResponse<String>> resumeMigration() {
    MigrationStatus current = progressTracker.getCurrentStatus();

    if (current == null) {
      return ResponseEntity.ok(ApiResponse.error("No migration found", "NO_MIGRATION"));
    }

    if (current.getState() != MigrationStatus.MigrationState.PAUSED) {
      return ResponseEntity.ok(
          ApiResponse.error("Migration is not paused", "MIGRATION_NOT_PAUSED"));
    }

    progressTracker.resumeMigration();
    return ResponseEntity.ok(ApiResponse.success("Migration resumed successfully"));
  }

  @PostMapping("/cancel")
  public ResponseEntity<ApiResponse<String>> cancelMigration() {
    MigrationStatus current = progressTracker.getCurrentStatus();

    if (current == null) {
      return ResponseEntity.ok(ApiResponse.error("No migration in progress", "NO_MIGRATION"));
    }

    if (current.getState() == MigrationStatus.MigrationState.COMPLETED) {
      return ResponseEntity.ok(
          ApiResponse.error("Migration already completed", "MIGRATION_COMPLETED"));
    }

    progressTracker.failMigration("Migration cancelled by user");
    return ResponseEntity.ok(ApiResponse.success("Migration cancelled successfully"));
  }

  @GetMapping("/history")
  public ResponseEntity<ApiResponse<Object>> getMigrationHistory() {
    // For simplicity, we'll just return the current migration status
    // In a real application, you might want to store migration history in a database
    MigrationStatus current = progressTracker.getCurrentStatus();

    if (current == null) {
      return ResponseEntity.ok(ApiResponse.success("No migration history found", null));
    }

    return ResponseEntity.ok(ApiResponse.success("Migration history retrieved", current));
  }

  // Step-based migration endpoints
  @PostMapping("/migrate-tags")
  public ResponseEntity<ApiResponse<String>> migrateTags(@RequestBody Map<String, List<String>> request) {
    try {
      List<String> tags = request.get("tags");
      if (tags == null || tags.isEmpty()) {
        return ResponseEntity.ok(ApiResponse.error("No tags provided", "NO_TAGS"));
      }

      migrationOrchestrator.migrateTags(tags);
      return ResponseEntity.ok(ApiResponse.success("Tags migrated successfully"));
    } catch (Exception e) {
      log.error("Failed to migrate tags", e);
      return ResponseEntity.ok(ApiResponse.error("Failed to migrate tags: " + e.getMessage(), "TAG_MIGRATION_FAILED"));
    }
  }

  @PostMapping("/migrate-stores")
  public ResponseEntity<ApiResponse<String>> migrateStores(@RequestBody Map<String, List<String>> request) {
    try {
      List<String> stores = request.get("stores");
      if (stores == null || stores.isEmpty()) {
        return ResponseEntity.ok(ApiResponse.success("No stores to migrate"));
      }

      migrationOrchestrator.migrateStores(stores);
      return ResponseEntity.ok(ApiResponse.success("Stores migrated successfully"));
    } catch (Exception e) {
      log.error("Failed to migrate stores", e);
      return ResponseEntity.ok(ApiResponse.error("Failed to migrate stores: " + e.getMessage(), "STORE_MIGRATION_FAILED"));
    }
  }

  @PostMapping("/migrate-subscribers")
  public ResponseEntity<ApiResponse<String>> migrateSubscribers(@RequestBody Map<String, Object> request) {
    try {
      @SuppressWarnings("unchecked")
      List<String> selectedTags = (List<String>) request.get("selectedTags");
      @SuppressWarnings("unchecked")
      List<String> selectedStores = (List<String>) request.get("selectedStores");

      migrationOrchestrator.migrateSubscribers(selectedTags, selectedStores);
      return ResponseEntity.ok(ApiResponse.success("Subscribers migrated successfully"));
    } catch (Exception e) {
      log.error("Failed to migrate subscribers", e);
      return ResponseEntity.ok(ApiResponse.error("Failed to migrate subscribers: " + e.getMessage(), "SUBSCRIBER_MIGRATION_FAILED"));
    }
  }

  @PostMapping("/sync-orders")
  public ResponseEntity<ApiResponse<String>> syncOrders(@RequestBody Map<String, List<String>> request) {
    try {
      List<String> selectedStores = request.get("selectedStores");
      if (selectedStores == null || selectedStores.isEmpty()) {
        return ResponseEntity.ok(ApiResponse.success("No orders to sync - no stores selected"));
      }

      migrationOrchestrator.syncOrders(selectedStores);
      return ResponseEntity.ok(ApiResponse.success("Orders synced successfully"));
    } catch (Exception e) {
      log.error("Failed to sync orders", e);
      return ResponseEntity.ok(ApiResponse.error("Failed to sync orders: " + e.getMessage(), "ORDER_SYNC_FAILED"));
    }
  }
}

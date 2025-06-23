package digital.pragmatech.controller;

import digital.pragmatech.dto.response.ApiResponse;
import digital.pragmatech.model.common.MigrationStatus;
import digital.pragmatech.service.migration.MigrationOrchestrator;
import digital.pragmatech.service.migration.MigrationProgressTracker;
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
}

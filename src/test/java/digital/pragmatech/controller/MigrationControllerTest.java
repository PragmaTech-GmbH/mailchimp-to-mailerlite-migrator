package digital.pragmatech.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import digital.pragmatech.model.common.MigrationStatus;
import digital.pragmatech.service.migration.MigrationOrchestrator;
import digital.pragmatech.service.migration.MigrationProgressTracker;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MigrationController.class)
class MigrationControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private MigrationOrchestrator migrationOrchestrator;

  @MockBean private MigrationProgressTracker progressTracker;

  @Test
  void shouldStartMigrationSuccessfully() throws Exception {
    // Given
    when(progressTracker.isMigrationInProgress()).thenReturn(false);
    when(migrationOrchestrator.startMigration()).thenReturn("migration-123");

    // When & Then
    mockMvc
        .perform(post("/api/migration/start"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Migration started successfully"))
        .andExpect(jsonPath("$.data").value("migration-123"));
  }

  @Test
  void shouldReturnErrorWhenMigrationAlreadyInProgress() throws Exception {
    // Given
    when(progressTracker.isMigrationInProgress()).thenReturn(true);

    // When & Then
    mockMvc
        .perform(post("/api/migration/start"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("Migration is already in progress"))
        .andExpect(jsonPath("$.errorCode").value("MIGRATION_IN_PROGRESS"));
  }

  @Test
  void shouldHandleMigrationStartFailure() throws Exception {
    // Given
    when(progressTracker.isMigrationInProgress()).thenReturn(false);
    when(migrationOrchestrator.startMigration())
        .thenThrow(new RuntimeException("API connection failed"));

    // When & Then
    mockMvc
        .perform(post("/api/migration/start"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("Failed to start migration: API connection failed"))
        .andExpect(jsonPath("$.errorCode").value("MIGRATION_START_FAILED"));
  }

  @Test
  void shouldGetMigrationStatusSuccessfully() throws Exception {
    // Given
    MigrationStatus status = createMockMigrationStatus();
    when(progressTracker.getCurrentStatus()).thenReturn(status);

    // When & Then
    mockMvc
        .perform(get("/api/migration/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value("migration-123"))
        .andExpect(jsonPath("$.data.state").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.data.phase").value("SUBSCRIBER_MIGRATION"))
        .andExpect(jsonPath("$.data.progress.percentComplete").value(45.5))
        .andExpect(jsonPath("$.data.progress.totalItems").value(1000))
        .andExpect(jsonPath("$.data.progress.processedItems").value(455));
  }

  @Test
  void shouldReturnErrorWhenNoMigrationInProgress() throws Exception {
    // Given
    when(progressTracker.getCurrentStatus()).thenReturn(null);

    // When & Then
    mockMvc
        .perform(get("/api/migration/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("No migration in progress"))
        .andExpect(jsonPath("$.errorCode").value("NO_MIGRATION"));
  }

  @Test
  void shouldPauseMigrationSuccessfully() throws Exception {
    // Given
    MigrationStatus status = createMockMigrationStatus();
    status.setState(MigrationStatus.MigrationState.IN_PROGRESS);
    when(progressTracker.getCurrentStatus()).thenReturn(status);

    // When & Then
    mockMvc
        .perform(post("/api/migration/pause"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Migration paused successfully"));
  }

  @Test
  void shouldReturnErrorWhenPausingWithNoMigration() throws Exception {
    // Given
    when(progressTracker.getCurrentStatus()).thenReturn(null);

    // When & Then
    mockMvc
        .perform(post("/api/migration/pause"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("No migration in progress"))
        .andExpect(jsonPath("$.errorCode").value("NO_MIGRATION"));
  }

  @Test
  void shouldReturnErrorWhenPausingMigrationNotInProgress() throws Exception {
    // Given
    MigrationStatus status = createMockMigrationStatus();
    status.setState(MigrationStatus.MigrationState.PAUSED);
    when(progressTracker.getCurrentStatus()).thenReturn(status);

    // When & Then
    mockMvc
        .perform(post("/api/migration/pause"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("Migration is not in progress"))
        .andExpect(jsonPath("$.errorCode").value("MIGRATION_NOT_IN_PROGRESS"));
  }

  @Test
  void shouldResumeMigrationSuccessfully() throws Exception {
    // Given
    MigrationStatus status = createMockMigrationStatus();
    status.setState(MigrationStatus.MigrationState.PAUSED);
    when(progressTracker.getCurrentStatus()).thenReturn(status);

    // When & Then
    mockMvc
        .perform(post("/api/migration/resume"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Migration resumed successfully"));
  }

  @Test
  void shouldReturnErrorWhenResumingWithNoMigration() throws Exception {
    // Given
    when(progressTracker.getCurrentStatus()).thenReturn(null);

    // When & Then
    mockMvc
        .perform(post("/api/migration/resume"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("No migration found"))
        .andExpect(jsonPath("$.errorCode").value("NO_MIGRATION"));
  }

  @Test
  void shouldReturnErrorWhenResumingMigrationNotPaused() throws Exception {
    // Given
    MigrationStatus status = createMockMigrationStatus();
    status.setState(MigrationStatus.MigrationState.IN_PROGRESS);
    when(progressTracker.getCurrentStatus()).thenReturn(status);

    // When & Then
    mockMvc
        .perform(post("/api/migration/resume"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("Migration is not paused"))
        .andExpect(jsonPath("$.errorCode").value("MIGRATION_NOT_PAUSED"));
  }

  @Test
  void shouldCancelMigrationSuccessfully() throws Exception {
    // Given
    MigrationStatus status = createMockMigrationStatus();
    status.setState(MigrationStatus.MigrationState.IN_PROGRESS);
    when(progressTracker.getCurrentStatus()).thenReturn(status);

    // When & Then
    mockMvc
        .perform(post("/api/migration/cancel"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Migration cancelled successfully"));
  }

  @Test
  void shouldReturnErrorWhenCancellingWithNoMigration() throws Exception {
    // Given
    when(progressTracker.getCurrentStatus()).thenReturn(null);

    // When & Then
    mockMvc
        .perform(post("/api/migration/cancel"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("No migration in progress"))
        .andExpect(jsonPath("$.errorCode").value("NO_MIGRATION"));
  }

  @Test
  void shouldReturnErrorWhenCancellingCompletedMigration() throws Exception {
    // Given
    MigrationStatus status = createMockMigrationStatus();
    status.setState(MigrationStatus.MigrationState.COMPLETED);
    when(progressTracker.getCurrentStatus()).thenReturn(status);

    // When & Then
    mockMvc
        .perform(post("/api/migration/cancel"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("Migration already completed"))
        .andExpect(jsonPath("$.errorCode").value("MIGRATION_COMPLETED"));
  }

  @Test
  void shouldGetMigrationHistoryWithCurrentMigration() throws Exception {
    // Given
    MigrationStatus status = createMockMigrationStatus();
    when(progressTracker.getCurrentStatus()).thenReturn(status);

    // When & Then
    mockMvc
        .perform(get("/api/migration/history"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Migration history retrieved"))
        .andExpect(jsonPath("$.data.id").value("migration-123"));
  }

  @Test
  void shouldGetMigrationHistoryWithNoMigration() throws Exception {
    // Given
    when(progressTracker.getCurrentStatus()).thenReturn(null);

    // When & Then
    mockMvc
        .perform(get("/api/migration/history"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("No migration history found"))
        .andExpect(jsonPath("$.data").isEmpty());
  }

  private MigrationStatus createMockMigrationStatus() {
    MigrationStatus.Progress progress =
        MigrationStatus.Progress.builder()
            .totalItems(1000)
            .processedItems(455)
            .successfulItems(450)
            .failedItems(5)
            .percentComplete(45.5)
            .build();

    MigrationStatus.Statistics statistics =
        MigrationStatus.Statistics.builder()
            .totalSubscribers(800)
            .migratedSubscribers(360)
            .totalTags(15)
            .migratedGroups(12)
            .totalProducts(25)
            .migratedProducts(20)
            .estimatedTimeRemaining(8)
            .build();

    return MigrationStatus.builder()
        .id("migration-123")
        .state(MigrationStatus.MigrationState.IN_PROGRESS)
        .phase(MigrationStatus.MigrationPhase.SUBSCRIBER_MIGRATION)
        .startedAt(LocalDateTime.now().minusMinutes(10))
        .progress(progress)
        .statistics(statistics)
        .build();
  }
}

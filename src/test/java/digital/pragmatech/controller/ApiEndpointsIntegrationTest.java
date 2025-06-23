package digital.pragmatech.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import digital.pragmatech.dto.request.ApiKeysRequest;
import digital.pragmatech.model.common.MigrationStatus;
import digital.pragmatech.model.mailchimp.MailchimpList;
import digital.pragmatech.service.mailchimp.MailchimpService;
import digital.pragmatech.service.mailerlite.MailerLiteService;
import digital.pragmatech.service.migration.MigrationOrchestrator;
import digital.pragmatech.service.migration.MigrationProgressTracker;
import digital.pragmatech.service.migration.MigrationValidator;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiEndpointsIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private MailchimpService mailchimpService;

  @MockBean private MailerLiteService mailerLiteService;

  @MockBean private MigrationValidator migrationValidator;

  @MockBean private MigrationOrchestrator migrationOrchestrator;

  @MockBean private MigrationProgressTracker progressTracker;

  @Test
  void shouldHandleCompleteApiWorkflow() throws Exception {
    // Step 1: Configure API keys
    ApiKeysRequest request = new ApiKeysRequest();
    request.setMailchimpApiKey("test-key-us1");
    request.setMailerLiteApiToken("test-token");

    mockMvc
        .perform(
            post("/api/configure")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    // Step 2: Validate connections
    MigrationValidator.ValidationResult validationResult =
        MigrationValidator.ValidationResult.builder()
            .valid(true)
            .errors(List.of())
            .validatedAt(LocalDateTime.now())
            .build();

    when(migrationValidator.validateApiConnections()).thenReturn(validationResult);

    mockMvc
        .perform(post("/api/validate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.valid").value(true));

    // Step 3: Analyze for migration
    MigrationValidator.PreMigrationAnalysis analysis =
        MigrationValidator.PreMigrationAnalysis.builder()
            .totalSubscribers(500)
            .totalTags(25)
            .totalLists(3)
            .estimatedMigrationTimeMinutes(12)
            .analyzedAt(LocalDateTime.now())
            .build();

    when(migrationValidator.analyzeForMigration()).thenReturn(analysis);

    mockMvc
        .perform(post("/api/analyze"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.totalSubscribers").value(500))
        .andExpect(jsonPath("$.data.totalTags").value(25))
        .andExpect(jsonPath("$.data.estimatedMigrationTimeMinutes").value(12));

    // Step 4: Fetch tags
    MailchimpList list1 = new MailchimpList();
    list1.setId("list1");
    list1.setName("Newsletter");

    MailchimpList list2 = new MailchimpList();
    list2.setId("list2");
    list2.setName("Marketing");

    when(mailchimpService.getAllLists()).thenReturn(Arrays.asList(list1, list2));
    when(mailchimpService.getAllTags("list1")).thenReturn(Arrays.asList("VIP", "Customer"));
    when(mailchimpService.getAllTags("list2")).thenReturn(Arrays.asList("Lead", "Prospect"));

    mockMvc
        .perform(get("/api/tags"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").isMap());

    // Step 5: Start migration
    when(progressTracker.isMigrationInProgress()).thenReturn(false);
    when(migrationOrchestrator.startMigration()).thenReturn("migration-123");

    mockMvc
        .perform(post("/api/migration/start"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").value("migration-123"));
  }

  @Test
  void shouldHandleValidationFailures() throws Exception {
    // Given
    MigrationValidator.ValidationResult validationResult =
        MigrationValidator.ValidationResult.builder()
            .valid(false)
            .errors(Arrays.asList("Invalid Mailchimp API key", "Network timeout"))
            .validatedAt(LocalDateTime.now())
            .build();

    when(migrationValidator.validateApiConnections()).thenReturn(validationResult);

    // When & Then
    mockMvc
        .perform(post("/api/validate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("API validation failed"));
  }

  @Test
  void shouldHandleAnalysisWithErrors() throws Exception {
    // Given
    MigrationValidator.PreMigrationAnalysis analysis =
        MigrationValidator.PreMigrationAnalysis.builder()
            .error("Failed to connect to Mailchimp API")
            .analyzedAt(LocalDateTime.now())
            .build();

    when(migrationValidator.analyzeForMigration()).thenReturn(analysis);

    // When & Then
    mockMvc
        .perform(post("/api/analyze"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(
            jsonPath("$.message").value("Analysis failed: Failed to connect to Mailchimp API"));
  }

  @Test
  void shouldHandleTagsFetchFailure() throws Exception {
    // Given
    when(mailchimpService.getAllLists()).thenThrow(new RuntimeException("API rate limit exceeded"));

    // When & Then
    mockMvc
        .perform(get("/api/tags"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("Failed to fetch tags: API rate limit exceeded"));
  }

  @Test
  void shouldHandleConcurrentMigrationRequests() throws Exception {
    // Given - First migration is in progress
    when(progressTracker.isMigrationInProgress()).thenReturn(true);

    // When & Then
    mockMvc
        .perform(post("/api/migration/start"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorCode").value("MIGRATION_IN_PROGRESS"));
  }

  @Test
  void shouldProvideDetailedMigrationStatus() throws Exception {
    // Given
    MigrationStatus status = createDetailedMigrationStatus();
    when(progressTracker.getCurrentStatus()).thenReturn(status);

    // When & Then
    mockMvc
        .perform(get("/api/migration/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value("migration-456"))
        .andExpect(jsonPath("$.data.state").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.data.phase").value("TAG_GROUP_MIGRATION"))
        .andExpect(jsonPath("$.data.progress.totalItems").value(2000))
        .andExpect(jsonPath("$.data.progress.processedItems").value(750))
        .andExpect(jsonPath("$.data.progress.percentComplete").value(37.5))
        .andExpect(jsonPath("$.data.statistics.totalSubscribers").value(1500))
        .andExpect(jsonPath("$.data.statistics.migratedSubscribers").value(500))
        .andExpect(jsonPath("$.data.statistics.estimatedTimeRemaining").value(15));
  }

  @Test
  void shouldHandleMigrationStateTransitions() throws Exception {
    // Test pause transition
    MigrationStatus inProgressStatus = createDetailedMigrationStatus();
    inProgressStatus.setState(MigrationStatus.MigrationState.IN_PROGRESS);
    when(progressTracker.getCurrentStatus()).thenReturn(inProgressStatus);

    mockMvc
        .perform(post("/api/migration/pause"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    // Test resume transition
    MigrationStatus pausedStatus = createDetailedMigrationStatus();
    pausedStatus.setState(MigrationStatus.MigrationState.PAUSED);
    when(progressTracker.getCurrentStatus()).thenReturn(pausedStatus);

    mockMvc
        .perform(post("/api/migration/resume"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    // Test cancel transition
    when(progressTracker.getCurrentStatus()).thenReturn(inProgressStatus);

    mockMvc
        .perform(post("/api/migration/cancel"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  void shouldHandleApiValidationWithTimeout() throws Exception {
    // Given
    when(migrationValidator.validateApiConnections())
        .thenThrow(new RuntimeException("Connection timeout after 30 seconds"));

    // When & Then
    mockMvc
        .perform(post("/api/validate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(
            jsonPath("$.message").value("Validation failed: Connection timeout after 30 seconds"));
  }

  @Test
  void shouldHandleEmptyTagsResponse() throws Exception {
    // Given
    when(mailchimpService.getAllLists()).thenReturn(Arrays.asList());

    // When & Then
    mockMvc
        .perform(get("/api/tags"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").isMap())
        .andExpect(jsonPath("$.data").isEmpty());
  }

  private MigrationStatus createDetailedMigrationStatus() {
    MigrationStatus.Progress progress =
        MigrationStatus.Progress.builder()
            .totalItems(2000)
            .processedItems(750)
            .successfulItems(745)
            .failedItems(5)
            .percentComplete(37.5)
            .build();

    MigrationStatus.Statistics statistics =
        MigrationStatus.Statistics.builder()
            .totalSubscribers(1500)
            .migratedSubscribers(500)
            .totalTags(30)
            .migratedGroups(20)
            .totalProducts(50)
            .migratedProducts(35)
            .estimatedTimeRemaining(15)
            .build();

    return MigrationStatus.builder()
        .id("migration-456")
        .state(MigrationStatus.MigrationState.IN_PROGRESS)
        .phase(MigrationStatus.MigrationPhase.TAG_GROUP_MIGRATION)
        .startedAt(LocalDateTime.now().minusMinutes(25))
        .progress(progress)
        .statistics(statistics)
        .errors(Arrays.asList())
        .build();
  }
}

package digital.pragmatech.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import digital.pragmatech.config.ApiConfiguration;
import digital.pragmatech.dto.request.ApiKeysRequest;
import digital.pragmatech.service.mailchimp.MailchimpService;
import digital.pragmatech.service.migration.MigrationValidator;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.htmlunit.WebClient;
import org.htmlunit.html.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.htmlunit.MockMvcWebClientBuilder;

@WebMvcTest(HomeController.class)
class HomeControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private ApiConfiguration apiConfiguration;

  @MockBean private MigrationValidator migrationValidator;

  @MockBean private MailchimpService mailchimpService;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void shouldDisplayIndexPageWithEmptyApiKeys() throws Exception {
    // Given
    ApiConfiguration.MailchimpConfig mailchimpConfig = new ApiConfiguration.MailchimpConfig();
    ApiConfiguration.MailerLiteConfig mailerLiteConfig = new ApiConfiguration.MailerLiteConfig();

    when(apiConfiguration.getMailchimp()).thenReturn(mailchimpConfig);
    when(apiConfiguration.getMailerlite()).thenReturn(mailerLiteConfig);

    // When & Then
    mockMvc
        .perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(view().name("index"))
        .andExpect(model().attributeExists("apiKeysRequest"))
        .andExpect(model().attribute("hasMailchimpKey", false))
        .andExpect(model().attribute("hasMailerLiteToken", false))
        .andExpect(model().attribute("canProceed", false))
        .andExpect(model().attribute("preConfigured", false));
  }

  @Test
  void shouldDisplayIndexPageWithPreConfiguredApiKeys() throws Exception {
    // Given
    ApiConfiguration.MailchimpConfig mailchimpConfig = new ApiConfiguration.MailchimpConfig();
    mailchimpConfig.setDefaultApiKey("test-key-us1");
    mailchimpConfig.setApiKey("test-key-us1");

    ApiConfiguration.MailerLiteConfig mailerLiteConfig = new ApiConfiguration.MailerLiteConfig();
    mailerLiteConfig.setDefaultApiToken("test-token");
    mailerLiteConfig.setApiToken("test-token");

    when(apiConfiguration.getMailchimp()).thenReturn(mailchimpConfig);
    when(apiConfiguration.getMailerlite()).thenReturn(mailerLiteConfig);

    // When & Then
    mockMvc
        .perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(view().name("index"))
        .andExpect(model().attribute("hasMailchimpKey", true))
        .andExpect(model().attribute("hasMailerLiteToken", true))
        .andExpect(model().attribute("canProceed", true))
        .andExpect(model().attribute("preConfigured", true));
  }

  @Test
  void shouldRedirectToDashboardWhenApiKeysConfigured() throws Exception {
    // Given
    ApiConfiguration.MailchimpConfig mailchimpConfig = new ApiConfiguration.MailchimpConfig();
    mailchimpConfig.setApiKey("test-key-us1");

    ApiConfiguration.MailerLiteConfig mailerLiteConfig = new ApiConfiguration.MailerLiteConfig();
    mailerLiteConfig.setApiToken("test-token");

    when(apiConfiguration.getMailchimp()).thenReturn(mailchimpConfig);
    when(apiConfiguration.getMailerlite()).thenReturn(mailerLiteConfig);

    // When & Then
    mockMvc
        .perform(get("/dashboard"))
        .andExpect(status().isOk())
        .andExpect(view().name("dashboard"));
  }

  @Test
  void shouldRedirectToIndexWhenApiKeysNotConfiguredForDashboard() throws Exception {
    // Given
    ApiConfiguration.MailchimpConfig mailchimpConfig = new ApiConfiguration.MailchimpConfig();
    ApiConfiguration.MailerLiteConfig mailerLiteConfig = new ApiConfiguration.MailerLiteConfig();

    when(apiConfiguration.getMailchimp()).thenReturn(mailchimpConfig);
    when(apiConfiguration.getMailerlite()).thenReturn(mailerLiteConfig);

    // When & Then
    mockMvc
        .perform(get("/dashboard"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/?error=api-keys-required"));
  }

  @Test
  void shouldConfigureApiKeysViaForm() throws Exception {
    // Given
    ApiConfiguration.MailchimpConfig mailchimpConfig = new ApiConfiguration.MailchimpConfig();
    ApiConfiguration.MailerLiteConfig mailerLiteConfig = new ApiConfiguration.MailerLiteConfig();

    when(apiConfiguration.getMailchimp()).thenReturn(mailchimpConfig);
    when(apiConfiguration.getMailerlite()).thenReturn(mailerLiteConfig);

    // When & Then
    mockMvc
        .perform(
            post("/configure")
                .param("mailchimpApiKey", "test-key-us1")
                .param("mailerLiteApiToken", "test-token"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/dashboard"));
  }

  @Test
  void shouldReturnValidationErrorsForInvalidApiKeys() throws Exception {
    // Given
    ApiConfiguration.MailchimpConfig mailchimpConfig = new ApiConfiguration.MailchimpConfig();
    ApiConfiguration.MailerLiteConfig mailerLiteConfig = new ApiConfiguration.MailerLiteConfig();

    when(apiConfiguration.getMailchimp()).thenReturn(mailchimpConfig);
    when(apiConfiguration.getMailerlite()).thenReturn(mailerLiteConfig);

    // When & Then
    mockMvc
        .perform(post("/configure").param("mailchimpApiKey", "").param("mailerLiteApiToken", ""))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/?error=validation"));
  }

  @Test
  void shouldConfigureApiKeysViaJsonApi() throws Exception {
    // Given
    ApiConfiguration.MailchimpConfig mailchimpConfig = new ApiConfiguration.MailchimpConfig();
    ApiConfiguration.MailerLiteConfig mailerLiteConfig = new ApiConfiguration.MailerLiteConfig();

    when(apiConfiguration.getMailchimp()).thenReturn(mailchimpConfig);
    when(apiConfiguration.getMailerlite()).thenReturn(mailerLiteConfig);

    ApiKeysRequest request = new ApiKeysRequest();
    request.setMailchimpApiKey("test-key-us1");
    request.setMailerLiteApiToken("test-token");

    // When & Then
    mockMvc
        .perform(
            post("/api/configure")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("API keys configured successfully"));
  }

  @Test
  void shouldValidateApiConnections() throws Exception {
    // Given
    MigrationValidator.ValidationResult validationResult =
        MigrationValidator.ValidationResult.builder()
            .valid(true)
            .errors(List.of())
            .validatedAt(LocalDateTime.now())
            .build();

    when(migrationValidator.validateApiConnections()).thenReturn(validationResult);

    // When & Then
    mockMvc
        .perform(post("/api/validate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("API connections validated successfully"));
  }

  @Test
  void shouldAnalyzeForMigration() throws Exception {
    // Given
    MigrationValidator.PreMigrationAnalysis analysis =
        MigrationValidator.PreMigrationAnalysis.builder()
            .totalSubscribers(100)
            .totalTags(10)
            .totalLists(2)
            .estimatedMigrationTimeMinutes(15)
            .analyzedAt(LocalDateTime.now())
            .build();

    when(migrationValidator.analyzeForMigration()).thenReturn(analysis);

    // When & Then
    mockMvc
        .perform(post("/api/analyze"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Pre-migration analysis completed"))
        .andExpect(jsonPath("$.data.totalSubscribers").value(100))
        .andExpect(jsonPath("$.data.totalTags").value(10));
  }

  @Test
  void shouldFetchMailchimpTags() throws Exception {
    // Given
    Map<String, List<String>> tagsByList = new HashMap<>();
    tagsByList.put("Newsletter List", List.of("VIP", "Customer", "Prospect"));
    tagsByList.put("Marketing List", List.of("Lead", "Trial"));

    when(mailchimpService.getAllLists()).thenReturn(List.of()); // Simplified for test
    when(mailchimpService.getAllTags(any())).thenReturn(List.of("VIP", "Customer"));

    // When & Then
    mockMvc
        .perform(get("/api/tags"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Tags fetched successfully"));
  }

  @Test
  void shouldDisplayIndexPageWithHtmlUnit() throws Exception {
    // Given
    ApiConfiguration.MailchimpConfig mailchimpConfig = new ApiConfiguration.MailchimpConfig();
    ApiConfiguration.MailerLiteConfig mailerLiteConfig = new ApiConfiguration.MailerLiteConfig();

    when(apiConfiguration.getMailchimp()).thenReturn(mailchimpConfig);
    when(apiConfiguration.getMailerlite()).thenReturn(mailerLiteConfig);

    WebClient webClient = MockMvcWebClientBuilder.mockMvcSetup(mockMvc).build();
    webClient.getOptions().setJavaScriptEnabled(false);

    // When
    HtmlPage page = webClient.getPage("http://localhost/");

    // Then
    assertThat(page.getTitleText()).contains("Setup - Mailchimp to MailerLite Migrator");

    // Check for main heading
    HtmlElement mainHeading =
        (HtmlElement) page.getFirstByXPath("//h1[contains(@class, 'text-3xl')]");
    assertThat(mainHeading.getTextContent()).contains("Mailchimp to MailerLite Migrator");

    // Check for API configuration form
    HtmlForm form = page.getForms().get(0);
    assertThat(form).isNotNull();

    // Check for required input fields
    HtmlInput mailchimpInput = form.getInputByName("mailchimpApiKey");
    HtmlInput mailerLiteInput = form.getInputByName("mailerLiteApiToken");

    assertThat(mailchimpInput.getAttribute("type")).isEqualTo("password");
    assertThat(mailchimpInput.getAttribute("required")).isEqualTo("required");
    assertThat(mailerLiteInput.getAttribute("type")).isEqualTo("password");
    assertThat(mailerLiteInput.getAttribute("required")).isEqualTo("required");

    webClient.close();
  }

  @Test
  void shouldSubmitFormWithHtmlUnit() throws Exception {
    // Given
    ApiConfiguration.MailchimpConfig mailchimpConfig = new ApiConfiguration.MailchimpConfig();
    ApiConfiguration.MailerLiteConfig mailerLiteConfig = new ApiConfiguration.MailerLiteConfig();

    when(apiConfiguration.getMailchimp()).thenReturn(mailchimpConfig);
    when(apiConfiguration.getMailerlite()).thenReturn(mailerLiteConfig);

    WebClient webClient = MockMvcWebClientBuilder.mockMvcSetup(mockMvc).build();
    webClient.getOptions().setJavaScriptEnabled(false);

    // When
    HtmlPage page = webClient.getPage("http://localhost/");
    HtmlForm form = page.getForms().get(0);

    HtmlInput mailchimpInput = form.getInputByName("mailchimpApiKey");
    HtmlInput mailerLiteInput = form.getInputByName("mailerLiteApiToken");

    mailchimpInput.setValue("test-key-us1");
    mailerLiteInput.setValue("test-token");

    HtmlButton submitButton = form.getButtonByName("");
    HtmlPage resultPage = submitButton.click();

    // Then
    // Should redirect to dashboard
    assertThat(resultPage.getUrl().getPath()).isEqualTo("/dashboard");

    webClient.close();
  }

  @Test
  void shouldDisplayPreConfiguredMessageWithHtmlUnit() throws Exception {
    // Given
    ApiConfiguration.MailchimpConfig mailchimpConfig = new ApiConfiguration.MailchimpConfig();
    mailchimpConfig.setDefaultApiKey("test-key-us1");
    mailchimpConfig.setApiKey("test-key-us1");

    ApiConfiguration.MailerLiteConfig mailerLiteConfig = new ApiConfiguration.MailerLiteConfig();
    mailerLiteConfig.setDefaultApiToken("test-token");
    mailerLiteConfig.setApiToken("test-token");

    when(apiConfiguration.getMailchimp()).thenReturn(mailchimpConfig);
    when(apiConfiguration.getMailerlite()).thenReturn(mailerLiteConfig);

    WebClient webClient = MockMvcWebClientBuilder.mockMvcSetup(mockMvc).build();
    webClient.getOptions().setJavaScriptEnabled(false);

    // When
    HtmlPage page = webClient.getPage("http://localhost/");

    // Then
    // Should display pre-configured message
    assertThat(page.asXml()).contains("API keys are pre-configured from environment variables");

    // Should display validation section
    DomElement validationSection = page.getElementById("validation-status");
    assertThat(validationSection).isNotNull();

    // Should display tags panel
    DomElement tagsPanel = page.getElementById("tags-panel");
    assertThat(tagsPanel).isNotNull();

    webClient.close();
  }

  @Test
  void shouldDisplayResultsPage() throws Exception {
    // When & Then
    mockMvc
        .perform(get("/results"))
        .andExpect(status().isOk())
        .andExpect(view().name("results"))
        .andExpect(model().attributeExists("migrationId"));
  }

  @Test
  void shouldDisplayResultsPageWithMigrationId() throws Exception {
    // When & Then
    mockMvc
        .perform(get("/results").param("migrationId", "test-123"))
        .andExpect(status().isOk())
        .andExpect(view().name("results"))
        .andExpect(model().attribute("migrationId", "test-123"));
  }
}

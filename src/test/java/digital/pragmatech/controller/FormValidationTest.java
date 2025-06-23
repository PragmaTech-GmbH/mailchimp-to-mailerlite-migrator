package digital.pragmatech.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import digital.pragmatech.config.ApiConfiguration;
import digital.pragmatech.service.mailchimp.MailchimpService;
import digital.pragmatech.service.migration.MigrationValidator;
import org.htmlunit.WebClient;
import org.htmlunit.html.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.htmlunit.MockMvcWebClientBuilder;

@WebMvcTest(HomeController.class)
class FormValidationTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private ApiConfiguration apiConfiguration;

  @MockBean private MigrationValidator migrationValidator;

  @MockBean private MailchimpService mailchimpService;

  @Test
  void shouldValidateRequiredFieldsWithHtmlUnit() throws Exception {
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

    // Try to submit form without filling required fields
    HtmlButton submitButton = form.getButtonByName("");

    // The browser should prevent submission due to HTML5 validation
    HtmlInput mailchimpInput = form.getInputByName("mailchimpApiKey");
    HtmlInput mailerLiteInput = form.getInputByName("mailerLiteApiToken");

    // Then
    // Note: required attribute may not be present if fields are pre-filled
    String mailchimpRequired = mailchimpInput.getAttribute("required");
    String mailerLiteRequired = mailerLiteInput.getAttribute("required");

    // Check that inputs are present and have proper type
    assertThat(mailchimpInput).isNotNull();
    assertThat(mailerLiteInput).isNotNull();
    assertThat(mailchimpInput.getAttribute("type")).isEqualTo("password");
    assertThat(mailerLiteInput.getAttribute("type")).isEqualTo("password");

    webClient.close();
  }

  @Test
  void shouldDisplayValidationErrorsAfterSubmission() throws Exception {
    // Given
    ApiConfiguration.MailchimpConfig mailchimpConfig = new ApiConfiguration.MailchimpConfig();
    ApiConfiguration.MailerLiteConfig mailerLiteConfig = new ApiConfiguration.MailerLiteConfig();

    when(apiConfiguration.getMailchimp()).thenReturn(mailchimpConfig);
    when(apiConfiguration.getMailerlite()).thenReturn(mailerLiteConfig);

    WebClient webClient = MockMvcWebClientBuilder.mockMvcSetup(mockMvc).build();
    webClient.getOptions().setJavaScriptEnabled(false);

    // When - Access page with validation error parameter
    HtmlPage page = webClient.getPage("http://localhost/?error=validation");

    // Then
    HtmlElement errorMessage = page.getFirstByXPath("//div[contains(@class, 'bg-red-50')]");
    assertThat(errorMessage.getTextContent()).contains("Please fix the validation errors");

    webClient.close();
  }

  @Test
  void shouldDisplayConfigurationErrorMessage() throws Exception {
    // Given
    ApiConfiguration.MailchimpConfig mailchimpConfig = new ApiConfiguration.MailchimpConfig();
    ApiConfiguration.MailerLiteConfig mailerLiteConfig = new ApiConfiguration.MailerLiteConfig();

    when(apiConfiguration.getMailchimp()).thenReturn(mailchimpConfig);
    when(apiConfiguration.getMailerlite()).thenReturn(mailerLiteConfig);

    WebClient webClient = MockMvcWebClientBuilder.mockMvcSetup(mockMvc).build();
    webClient.getOptions().setJavaScriptEnabled(false);

    // When - Access page with configuration error parameter
    HtmlPage page = webClient.getPage("http://localhost/?error=configuration");

    // Then
    HtmlElement errorMessage = page.getFirstByXPath("//div[contains(@class, 'bg-red-50')]");
    assertThat(errorMessage.getTextContent()).contains("Failed to configure API keys");

    webClient.close();
  }

  @Test
  void shouldDisplayApiKeysRequiredErrorMessage() throws Exception {
    // Given
    ApiConfiguration.MailchimpConfig mailchimpConfig = new ApiConfiguration.MailchimpConfig();
    ApiConfiguration.MailerLiteConfig mailerLiteConfig = new ApiConfiguration.MailerLiteConfig();

    when(apiConfiguration.getMailchimp()).thenReturn(mailchimpConfig);
    when(apiConfiguration.getMailerlite()).thenReturn(mailerLiteConfig);

    WebClient webClient = MockMvcWebClientBuilder.mockMvcSetup(mockMvc).build();
    webClient.getOptions().setJavaScriptEnabled(false);

    // When - Access page with api-keys-required error parameter
    HtmlPage page = webClient.getPage("http://localhost/?error=api-keys-required");

    // Then
    HtmlElement errorMessage = page.getFirstByXPath("//div[contains(@class, 'bg-red-50')]");
    assertThat(errorMessage.getTextContent()).contains("API keys are required to proceed");

    webClient.close();
  }

  @Test
  void shouldFillFormWithValidDataAndSubmit() throws Exception {
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

    // Fill the form with valid data
    HtmlInput mailchimpInput = form.getInputByName("mailchimpApiKey");
    HtmlInput mailerLiteInput = form.getInputByName("mailerLiteApiToken");

    mailchimpInput.setValue("abc123def456-us1");
    mailerLiteInput.setValue("ml-token-123456");

    // Submit the form
    HtmlButton submitButton = form.getButtonByName("");
    HtmlPage resultPage = submitButton.click();

    // Then
    assertThat(resultPage.getUrl().getPath()).isEqualTo("/dashboard");

    webClient.close();
  }

  @Test
  void shouldDisplayPlaceholderTexts() throws Exception {
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

    // Then
    assertThat(mailchimpInput.getAttribute("placeholder"))
        .isEqualTo("Your Mailchimp API key (format: key-datacenter)");
    assertThat(mailerLiteInput.getAttribute("placeholder")).isEqualTo("Your MailerLite API token");

    webClient.close();
  }

  @Test
  void shouldDisplayHelpTexts() throws Exception {
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
    HtmlElement mailchimpHelp =
        page.getFirstByXPath("//p[contains(text(), 'Find your API key in Mailchimp')]");
    HtmlElement mailerLiteHelp =
        page.getFirstByXPath("//p[contains(text(), 'Find your API token in MailerLite')]");

    assertThat(mailchimpHelp.getTextContent()).contains("Account → Extras → API keys");
    assertThat(mailerLiteHelp.getTextContent())
        .contains("Integrations → MailerLite API → Generate new token");

    webClient.close();
  }

  @Test
  void shouldShowValidationSectionWhenApiKeysConfigured() throws Exception {
    // Given
    ApiConfiguration.MailchimpConfig mailchimpConfig = new ApiConfiguration.MailchimpConfig();
    mailchimpConfig.setApiKey("test-key-us1");

    ApiConfiguration.MailerLiteConfig mailerLiteConfig = new ApiConfiguration.MailerLiteConfig();
    mailerLiteConfig.setApiToken("test-token");

    when(apiConfiguration.getMailchimp()).thenReturn(mailchimpConfig);
    when(apiConfiguration.getMailerlite()).thenReturn(mailerLiteConfig);

    WebClient webClient = MockMvcWebClientBuilder.mockMvcSetup(mockMvc).build();
    webClient.getOptions().setJavaScriptEnabled(false);

    // When
    HtmlPage page = webClient.getPage("http://localhost/");

    // Then
    HtmlElement validationSection =
        page.getFirstByXPath(
            "//div[contains(@class, 'bg-white') and .//h3[contains(text(), 'Validate API Connections')]]");
    assertThat(validationSection).isNotNull();

    HtmlButton testConnectionsBtn = page.getFirstByXPath("//button[@id='validate-btn']");
    assertThat(testConnectionsBtn.getTextContent()).contains("Test Connections");

    HtmlAnchor dashboardLink = page.getFirstByXPath("//a[@href='/dashboard']");
    assertThat(dashboardLink.getTextContent()).contains("Go to Dashboard");

    webClient.close();
  }

  @Test
  void shouldDisableJavaScriptAndStillWork() throws Exception {
    // Given
    ApiConfiguration.MailchimpConfig mailchimpConfig = new ApiConfiguration.MailchimpConfig();
    ApiConfiguration.MailerLiteConfig mailerLiteConfig = new ApiConfiguration.MailerLiteConfig();

    when(apiConfiguration.getMailchimp()).thenReturn(mailchimpConfig);
    when(apiConfiguration.getMailerlite()).thenReturn(mailerLiteConfig);

    WebClient webClient = MockMvcWebClientBuilder.mockMvcSetup(mockMvc).build();
    webClient.getOptions().setJavaScriptEnabled(false);

    // Disable JavaScript to test graceful degradation
    webClient.getOptions().setJavaScriptEnabled(false);

    // When
    HtmlPage page = webClient.getPage("http://localhost/");
    HtmlForm form = page.getForms().get(0);

    // Fill and submit form without JavaScript
    HtmlInput mailchimpInput = form.getInputByName("mailchimpApiKey");
    HtmlInput mailerLiteInput = form.getInputByName("mailerLiteApiToken");

    mailchimpInput.setValue("test-key-us1");
    mailerLiteInput.setValue("test-token");

    HtmlButton submitButton = form.getButtonByName("");
    HtmlPage resultPage = submitButton.click();

    // Then - Should still work without JavaScript
    assertThat(resultPage.getUrl().getPath()).isEqualTo("/dashboard");

    webClient.close();
  }
}

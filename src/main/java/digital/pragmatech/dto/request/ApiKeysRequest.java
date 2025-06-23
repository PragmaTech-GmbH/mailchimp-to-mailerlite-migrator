package digital.pragmatech.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApiKeysRequest {

  @NotBlank(message = "Mailchimp API key is required")
  private String mailchimpApiKey;

  @NotBlank(message = "MailerLite API token is required")
  private String mailerLiteApiToken;
}

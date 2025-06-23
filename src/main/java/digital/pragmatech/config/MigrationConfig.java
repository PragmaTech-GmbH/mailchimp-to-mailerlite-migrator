package digital.pragmatech.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "migration")
public class MigrationConfig {

  private int batchSize = 500;
  private RetryConfig retry = new RetryConfig();
  private RateLimitConfig rateLimit = new RateLimitConfig();

  @Data
  public static class RetryConfig {
    private int maxAttempts = 3;
    private long backoffDelay = 1000;
  }

  @Data
  public static class RateLimitConfig {
    private MailchimpLimits mailchimp = new MailchimpLimits();
    private MailerLiteLimits mailerlite = new MailerLiteLimits();

    @Data
    public static class MailchimpLimits {
      private int connections = 10;
      private long timeout = 120000;
    }

    @Data
    public static class MailerLiteLimits {
      private int requestsPerMinute = 120;
    }
  }
}

package digital.pragmatech.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "api")
public class ApiConfiguration {
    
    private MailchimpConfig mailchimp = new MailchimpConfig();
    private MailerLiteConfig mailerlite = new MailerLiteConfig();
    
    @Data
    public static class MailchimpConfig {
        private String baseUrl;
        private String apiKey;
        private String datacenter;
        
        public String getFullBaseUrl() {
            if (datacenter != null && baseUrl != null) {
                return baseUrl.replace("{datacenter}", datacenter);
            }
            return baseUrl;
        }
    }
    
    @Data
    public static class MailerLiteConfig {
        private String baseUrl;
        private String apiToken;
    }
}
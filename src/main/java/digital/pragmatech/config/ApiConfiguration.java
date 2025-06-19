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
        private String defaultApiKey;
        private String datacenter;
        
        public String getApiKey() {
            // Use configured API key if available, otherwise fall back to default from env
            return apiKey != null ? apiKey : defaultApiKey;
        }
        
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
            // Extract datacenter from API key (format: key-datacenter)
            if (apiKey != null && apiKey.contains("-")) {
                this.datacenter = apiKey.substring(apiKey.lastIndexOf("-") + 1);
            }
        }
        
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
        private String defaultApiToken;
        
        public String getApiToken() {
            // Use configured API token if available, otherwise fall back to default from env
            return apiToken != null ? apiToken : defaultApiToken;
        }
    }
}
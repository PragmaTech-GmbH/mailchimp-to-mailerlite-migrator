package digital.pragmatech.config;

import digital.pragmatech.service.mailchimp.MailchimpApiClient;
import digital.pragmatech.service.mailerlite.MailerLiteApiClient;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class MockBeansTestConfiguration {

    @Bean
    @Primary
    public MailchimpApiClient mockMailchimpApiClient() {
        return Mockito.mock(MailchimpApiClient.class);
    }

    @Bean
    @Primary
    public MailerLiteApiClient mockMailerLiteApiClient() {
        return Mockito.mock(MailerLiteApiClient.class);
    }
}
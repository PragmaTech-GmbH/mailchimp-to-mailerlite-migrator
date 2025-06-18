package digital.pragmatech.config;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableAsync
@EnableScheduling
public class WebConfig {
    
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder()
                .requestFactory(clientHttpRequestFactory());
    }
    
    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(120));
        return factory;
    }
    
    @Bean
    public RestClientCustomizer restClientCustomizer() {
        return restClientBuilder -> restClientBuilder
                .defaultHeader("User-Agent", "MailchimpToMailerLiteMigrator/1.0")
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Content-Type", "application/json");
    }
}
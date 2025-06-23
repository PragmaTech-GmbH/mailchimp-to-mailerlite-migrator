package digital.pragmatech.service.mailchimp;

import digital.pragmatech.config.ApiConfiguration;
import digital.pragmatech.exception.ApiException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailchimpApiClient {

  private final RestClient.Builder restClientBuilder;
  private final ApiConfiguration apiConfiguration;

  private RestClient restClient;

  private RestClient getRestClient() {
    if (restClient == null) {
      String apiKey = apiConfiguration.getMailchimp().getApiKey();
      if (apiKey == null || apiKey.isEmpty()) {
        throw new ApiException("Mailchimp", null, "API key not configured");
      }

      // Extract datacenter from API key
      String[] parts = apiKey.split("-");
      if (parts.length != 2) {
        throw new ApiException("Mailchimp", null, "Invalid API key format");
      }

      apiConfiguration.getMailchimp().setDatacenter(parts[1]);
      String baseUrl = apiConfiguration.getMailchimp().getFullBaseUrl();

      // Create Basic Auth header
      String auth = "anystring:" + apiKey;
      String encodedAuth =
          Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

      this.restClient =
          restClientBuilder
              .baseUrl(baseUrl)
              .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth)
              .build();
    }
    return restClient;
  }

  public <T> T get(String endpoint, Class<T> responseType, Object... uriVariables) {
    try {
      log.debug("Mailchimp GET request to: {}", endpoint);
      return getRestClient().get().uri(endpoint, uriVariables).retrieve().body(responseType);
    } catch (RestClientResponseException e) {
      log.error("Mailchimp API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
      throw new ApiException("Mailchimp", e.getStatusCode(), e.getMessage(), e);
    }
  }

  public <T> T get(
      String endpoint, ParameterizedTypeReference<T> responseType, Object... uriVariables) {
    try {
      log.debug("Mailchimp GET request to: {}", endpoint);
      return getRestClient().get().uri(endpoint, uriVariables).retrieve().body(responseType);
    } catch (RestClientResponseException e) {
      log.error("Mailchimp API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
      throw new ApiException("Mailchimp", e.getStatusCode(), e.getMessage(), e);
    }
  }

  public <T> T post(String endpoint, Object body, Class<T> responseType, Object... uriVariables) {
    try {
      log.debug("Mailchimp POST request to: {}", endpoint);
      return getRestClient()
          .post()
          .uri(endpoint, uriVariables)
          .body(body)
          .retrieve()
          .body(responseType);
    } catch (RestClientResponseException e) {
      log.error("Mailchimp API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
      throw new ApiException("Mailchimp", e.getStatusCode(), e.getMessage(), e);
    }
  }

  public <T> T put(String endpoint, Object body, Class<T> responseType, Object... uriVariables) {
    try {
      log.debug("Mailchimp PUT request to: {}", endpoint);
      return getRestClient()
          .put()
          .uri(endpoint, uriVariables)
          .body(body)
          .retrieve()
          .body(responseType);
    } catch (RestClientResponseException e) {
      log.error("Mailchimp API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
      throw new ApiException("Mailchimp", e.getStatusCode(), e.getMessage(), e);
    }
  }

  public void delete(String endpoint, Object... uriVariables) {
    try {
      log.debug("Mailchimp DELETE request to: {}", endpoint);
      getRestClient().delete().uri(endpoint, uriVariables).retrieve().toBodilessEntity();
    } catch (RestClientResponseException e) {
      log.error("Mailchimp API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
      throw new ApiException("Mailchimp", e.getStatusCode(), e.getMessage(), e);
    }
  }

  public boolean testConnection() {
    try {
      Map<String, Object> response =
          get("/", new ParameterizedTypeReference<Map<String, Object>>() {});
      return response != null && response.containsKey("account_id");
    } catch (Exception e) {
      log.error("Failed to test Mailchimp connection", e);
      return false;
    }
  }
}

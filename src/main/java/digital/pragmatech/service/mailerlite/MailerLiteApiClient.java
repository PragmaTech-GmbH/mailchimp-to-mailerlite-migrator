package digital.pragmatech.service.mailerlite;

import digital.pragmatech.config.ApiConfiguration;
import digital.pragmatech.exception.ApiException;
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
public class MailerLiteApiClient {

  private final RestClient.Builder restClientBuilder;
  private final ApiConfiguration apiConfiguration;

  private RestClient restClient;

  private RestClient getRestClient() {
    if (restClient == null) {
      String apiToken = apiConfiguration.getMailerlite().getApiToken();
      if (apiToken == null || apiToken.isEmpty()) {
        throw new ApiException("MailerLite", null, "API token not configured");
      }

      String baseUrl = apiConfiguration.getMailerlite().getBaseUrl();

      this.restClient =
          restClientBuilder
              .baseUrl(baseUrl)
              .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
              .build();
    }
    return restClient;
  }

  public <T> T get(String endpoint, Class<T> responseType, Object... uriVariables) {
    try {
      log.debug("MailerLite GET request to: {}", endpoint);
      return getRestClient().get().uri(endpoint, uriVariables).retrieve().body(responseType);
    } catch (RestClientResponseException e) {
      log.error("MailerLite API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
      throw new ApiException("MailerLite", e.getStatusCode(), e.getMessage(), e);
    }
  }

  public <T> T get(
      String endpoint, ParameterizedTypeReference<T> responseType, Object... uriVariables) {
    try {
      log.debug("MailerLite GET request to: {}", endpoint);
      return getRestClient().get().uri(endpoint, uriVariables).retrieve().body(responseType);
    } catch (RestClientResponseException e) {
      log.error("MailerLite API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
      throw new ApiException("MailerLite", e.getStatusCode(), e.getMessage(), e);
    }
  }

  public <T> T post(String endpoint, Object body, Class<T> responseType, Object... uriVariables) {
    try {
      log.debug("MailerLite POST request to: {} with body: {}", endpoint, body);
      return getRestClient()
          .post()
          .uri(endpoint, uriVariables)
          .body(body)
          .retrieve()
          .body(responseType);
    } catch (RestClientResponseException e) {
      log.error("MailerLite API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
      throw new ApiException("MailerLite", e.getStatusCode(), e.getMessage(), e);
    }
  }

  public <T> T post(
      String endpoint,
      Object body,
      ParameterizedTypeReference<T> responseType,
      Object... uriVariables) {
    try {
      log.debug("MailerLite POST request to: {} with body: {}", endpoint, body);
      return getRestClient()
          .post()
          .uri(endpoint, uriVariables)
          .body(body)
          .retrieve()
          .body(responseType);
    } catch (RestClientResponseException e) {
      log.error("MailerLite API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
      throw new ApiException("MailerLite", e.getStatusCode(), e.getMessage(), e);
    }
  }

  public <T> T put(String endpoint, Object body, Class<T> responseType, Object... uriVariables) {
    try {
      log.debug("MailerLite PUT request to: {} with body: {}", endpoint, body);
      return getRestClient()
          .put()
          .uri(endpoint, uriVariables)
          .body(body)
          .retrieve()
          .body(responseType);
    } catch (RestClientResponseException e) {
      log.error("MailerLite API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
      throw new ApiException("MailerLite", e.getStatusCode(), e.getMessage(), e);
    }
  }

  public void delete(String endpoint, Object... uriVariables) {
    try {
      log.debug("MailerLite DELETE request to: {}", endpoint);
      getRestClient().delete().uri(endpoint, uriVariables).retrieve().toBodilessEntity();
    } catch (RestClientResponseException e) {
      log.error("MailerLite API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
      throw new ApiException("MailerLite", e.getStatusCode(), e.getMessage(), e);
    }
  }

  public boolean testConnection() {
    try {
      // Test connection by trying to fetch groups (simpler endpoint that requires minimal
      // permissions)
      Map<String, Object> response =
          get("/groups", new ParameterizedTypeReference<Map<String, Object>>() {});
      // MailerLite API returns data in a "data" field for successful responses
      return response != null && response.containsKey("data");
    } catch (Exception e) {
      log.debug("Failed to test MailerLite connection: {}", e.getMessage());
      return false;
    }
  }
}

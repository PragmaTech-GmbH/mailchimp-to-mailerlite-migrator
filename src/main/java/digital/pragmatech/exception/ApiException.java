package digital.pragmatech.exception;

import org.springframework.http.HttpStatusCode;

public class ApiException extends RuntimeException {

  private final HttpStatusCode statusCode;
  private final String provider;

  public ApiException(String provider, HttpStatusCode statusCode, String message) {
    super(String.format("%s API error (HTTP %s): %s", provider, statusCode, message));
    this.provider = provider;
    this.statusCode = statusCode;
  }

  public ApiException(String provider, HttpStatusCode statusCode, String message, Throwable cause) {
    super(String.format("%s API error (HTTP %s): %s", provider, statusCode, message), cause);
    this.provider = provider;
    this.statusCode = statusCode;
  }

  public HttpStatusCode getStatusCode() {
    return statusCode;
  }

  public String getProvider() {
    return provider;
  }

  public boolean isRateLimitError() {
    return statusCode.value() == 429;
  }

  public boolean isAuthenticationError() {
    return statusCode.value() == 401;
  }
}

package digital.pragmatech.exception;

public class MigrationException extends RuntimeException {

  private final String errorCode;

  public MigrationException(String message) {
    super(message);
    this.errorCode = "MIGRATION_ERROR";
  }

  public MigrationException(String message, Throwable cause) {
    super(message, cause);
    this.errorCode = "MIGRATION_ERROR";
  }

  public MigrationException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public MigrationException(String errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return errorCode;
  }
}

package digital.pragmatech.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

  private boolean success;
  private String message;
  private T data;
  private String errorCode;
  @Builder.Default private LocalDateTime timestamp = LocalDateTime.now();

  public static <T> ApiResponse<T> success(T data) {
    return ApiResponse.<T>builder().success(true).data(data).build();
  }

  public static <T> ApiResponse<T> success(String message) {
    return ApiResponse.<T>builder().success(true).message(message).build();
  }

  public static <T> ApiResponse<T> success(String message, T data) {
    return ApiResponse.<T>builder().success(true).message(message).data(data).build();
  }

  public static <T> ApiResponse<T> error(String message) {
    return ApiResponse.<T>builder().success(false).message(message).build();
  }

  public static <T> ApiResponse<T> error(String message, String errorCode) {
    return ApiResponse.<T>builder().success(false).message(message).errorCode(errorCode).build();
  }
}

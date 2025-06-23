package digital.pragmatech.model.common;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Subscriber {
  private String id;
  private String email;
  private String firstName;
  private String lastName;
  private SubscriberStatus status;
  private Set<String> tags;
  private Map<String, Object> customFields;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String source;
  private Double totalSpent;
  private Integer orderCount;

  public enum SubscriberStatus {
    SUBSCRIBED,
    UNSUBSCRIBED,
    PENDING,
    CLEANED,
    BOUNCED
  }

  public String getFullName() {
    if (firstName != null && lastName != null) {
      return firstName + " " + lastName;
    } else if (firstName != null) {
      return firstName;
    } else if (lastName != null) {
      return lastName;
    }
    return "";
  }
}

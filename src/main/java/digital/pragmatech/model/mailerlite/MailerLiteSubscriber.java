package digital.pragmatech.model.mailerlite;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class MailerLiteSubscriber {
  private String id;
  private String email;
  private String status;
  private String source;

  @JsonProperty("date_subscribe")
  private LocalDateTime dateSubscribe;

  @JsonProperty("date_unsubscribe")
  private LocalDateTime dateUnsubscribe;

  @JsonProperty("date_created")
  private LocalDateTime dateCreated;

  @JsonProperty("date_updated")
  private LocalDateTime dateUpdated;

  private Map<String, Object> fields;

  private List<MailerLiteGroup> groups;

  @JsonProperty("opted_in_at")
  private LocalDateTime optedInAt;

  @JsonProperty("optin_ip")
  private String optinIp;

  @JsonProperty("unsubscribed_at")
  private LocalDateTime unsubscribedAt;

  @JsonProperty("unsubscribe_ip")
  private String unsubscribeIp;

  @JsonProperty("consent_timestamp")
  private LocalDateTime consentTimestamp;

  @JsonProperty("confirmation_timestamp")
  private LocalDateTime confirmationTimestamp;

  @JsonProperty("confirmation_ip")
  private String confirmationIp;

  private boolean bounced;

  @JsonProperty("bounce_reason")
  private String bounceReason;

  @Data
  public static class MailerLiteGroup {
    private String id;
    private String name;

    @JsonProperty("active_count")
    private Integer activeCount;

    @JsonProperty("sent_count")
    private Integer sentCount;

    @JsonProperty("opens_count")
    private Integer opensCount;

    @JsonProperty("clicks_count")
    private Integer clicksCount;

    @JsonProperty("date_created")
    private LocalDateTime dateCreated;

    @JsonProperty("date_updated")
    private LocalDateTime dateUpdated;
  }
}

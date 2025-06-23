package digital.pragmatech.model.mailerlite;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;

@Data
public class MailerLiteGroup {
  private String id;
  private String name;

  @JsonProperty("active_count")
  private Integer activeCount;

  @JsonProperty("sent_count")
  private Integer sentCount;

  @JsonProperty("opens_count")
  private Integer opensCount;

  @JsonProperty("opens_rate")
  private Map<String, Double> opensRate;

  @JsonProperty("clicks_count")
  private Integer clicksCount;

  @JsonProperty("clicks_rate")
  private Map<String, Double> clicksRate;

  @JsonProperty("unsubscribed_count")
  private Integer unsubscribedCount;

  @JsonProperty("unconfirmed_count")
  private Integer unconfirmedCount;

  @JsonProperty("bounced_count")
  private Integer bouncedCount;

  @JsonProperty("junk_count")
  private Integer junkCount;

  @JsonProperty("created_at")
  private LocalDateTime createdAt;

  @JsonProperty("updated_at")
  private LocalDateTime updatedAt;
}

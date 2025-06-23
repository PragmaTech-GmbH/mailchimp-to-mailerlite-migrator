package digital.pragmatech.model.mailchimp;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;

@Data
public class MailchimpList {
  private String id;
  private String name;

  @JsonProperty("web_id")
  private Integer webId;

  @JsonProperty("permission_reminder")
  private String permissionReminder;

  @JsonProperty("use_archive_bar")
  private boolean useArchiveBar;

  @JsonProperty("campaign_defaults")
  private CampaignDefaults campaignDefaults;

  @JsonProperty("notify_on_subscribe")
  private String notifyOnSubscribe;

  @JsonProperty("notify_on_unsubscribe")
  private String notifyOnUnsubscribe;

  @JsonProperty("date_created")
  private LocalDateTime dateCreated;

  @JsonProperty("list_rating")
  private Integer listRating;

  @JsonProperty("email_type_option")
  private boolean emailTypeOption;

  @JsonProperty("subscribe_url_short")
  private String subscribeUrlShort;

  @JsonProperty("subscribe_url_long")
  private String subscribeUrlLong;

  @JsonProperty("beamer_address")
  private String beamerAddress;

  private String visibility;

  @JsonProperty("double_optin")
  private boolean doubleOptin;

  @JsonProperty("has_welcome")
  private boolean hasWelcome;

  @JsonProperty("marketing_permissions")
  private boolean marketingPermissions;

  private Map<String, Integer> stats;

  @Data
  public static class CampaignDefaults {
    @JsonProperty("from_name")
    private String fromName;

    @JsonProperty("from_email")
    private String fromEmail;

    private String subject;
    private String language;
  }
}

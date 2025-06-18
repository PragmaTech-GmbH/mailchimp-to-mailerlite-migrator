package digital.pragmatech.model.mailchimp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class MailchimpMember {
    private String id;
    
    @JsonProperty("email_address")
    private String emailAddress;
    
    @JsonProperty("unique_email_id")
    private String uniqueEmailId;
    
    @JsonProperty("web_id")
    private Integer webId;
    
    @JsonProperty("email_type")
    private String emailType;
    
    private String status;
    
    @JsonProperty("unsubscribe_reason")
    private String unsubscribeReason;
    
    @JsonProperty("merge_fields")
    private Map<String, Object> mergeFields;
    
    private Map<String, Boolean> interests;
    
    private Map<String, Integer> stats;
    
    @JsonProperty("ip_signup")
    private String ipSignup;
    
    @JsonProperty("timestamp_signup")
    private LocalDateTime timestampSignup;
    
    @JsonProperty("ip_opt")
    private String ipOpt;
    
    @JsonProperty("timestamp_opt")
    private LocalDateTime timestampOpt;
    
    @JsonProperty("member_rating")
    private Integer memberRating;
    
    @JsonProperty("last_changed")
    private LocalDateTime lastChanged;
    
    private String language;
    
    private boolean vip;
    
    @JsonProperty("email_client")
    private String emailClient;
    
    private Location location;
    
    @JsonProperty("marketing_permissions")
    private List<MarketingPermission> marketingPermissions;
    
    @JsonProperty("last_note")
    private Note lastNote;
    
    private String source;
    
    @JsonProperty("tags_count")
    private Integer tagsCount;
    
    private List<Tag> tags;
    
    @JsonProperty("list_id")
    private String listId;
    
    @Data
    public static class Location {
        private Double latitude;
        private Double longitude;
        private Integer gmtoff;
        private Integer dstoff;
        @JsonProperty("country_code")
        private String countryCode;
        private String timezone;
        private String region;
    }
    
    @Data
    public static class MarketingPermission {
        @JsonProperty("marketing_permission_id")
        private String marketingPermissionId;
        private String text;
        private boolean enabled;
    }
    
    @Data
    public static class Note {
        @JsonProperty("note_id")
        private Integer noteId;
        @JsonProperty("created_at")
        private LocalDateTime createdAt;
        @JsonProperty("created_by")
        private String createdBy;
        private String note;
    }
    
    @Data
    public static class Tag {
        private Integer id;
        private String name;
    }
}
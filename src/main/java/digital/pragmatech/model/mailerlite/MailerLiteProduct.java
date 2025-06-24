package digital.pragmatech.model.mailerlite;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MailerLiteProduct {
  private String id;
  private String name;
  private String description;
  private String url;

  @JsonProperty("image_url")
  private String imageUrl;

  private String price;
  private String sku;
  private List<String> categories;

  @JsonProperty("shop_id")
  private String shopId;

  @JsonProperty("created_at")
  private String createdAt;

  @JsonProperty("updated_at")
  private String updatedAt;
}

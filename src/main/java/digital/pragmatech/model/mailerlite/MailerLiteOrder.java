package digital.pragmatech.model.mailerlite;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MailerLiteOrder {
  private String id;

  @JsonProperty("shop_id")
  private String shopId;

  @JsonProperty("email")
  private String email;

  @JsonProperty("order_url")
  private String orderUrl;

  @JsonProperty("external_id")
  private String externalId;

  @JsonProperty("status")
  private String status;

  @JsonProperty("total")
  private String total;

  @JsonProperty("currency")
  private String currency;

  @JsonProperty("items")
  private List<MailerLiteOrderItem> items;

  @JsonProperty("billing_info")
  private Map<String, Object> billingInfo;

  @JsonProperty("ordered_at")
  private String orderedAt;

  @JsonProperty("created_at")
  private String createdAt;

  @JsonProperty("updated_at")
  private String updatedAt;

  @Data
  @Builder
  public static class MailerLiteOrderItem {
    @JsonProperty("product_id")
    private String productId;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("product_url")
    private String productUrl;

    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("price")
    private String price;

    @JsonProperty("discount")
    private String discount;
  }
}

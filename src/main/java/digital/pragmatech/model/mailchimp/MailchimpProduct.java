package digital.pragmatech.model.mailchimp;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class MailchimpProduct {
  private String id;
  private String title;
  private String handle;
  private String url;
  private String description;
  private String type;
  private String vendor;

  @JsonProperty("image_url")
  private String imageUrl;

  private List<MailchimpProductVariant> variants;
  private List<MailchimpProductImage> images;

  @JsonProperty("published_at_foreign")
  private String publishedAtForeign;

  @JsonProperty("_links")
  private List<Map<String, Object>> links;

  @Data
  public static class MailchimpProductVariant {
    private String id;
    private String title;
    private String url;
    private String sku;
    private String price;

    @JsonProperty("inventory_quantity")
    private Integer inventoryQuantity;

    @JsonProperty("image_url")
    private String imageUrl;

    private List<String> backorders;
    private String visibility;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("_links")
    private List<Map<String, Object>> links;
  }

  @Data
  public static class MailchimpProductImage {
    private String id;
    private String url;

    @JsonProperty("variant_ids")
    private List<String> variantIds;

    @JsonProperty("_links")
    private List<Map<String, Object>> links;
  }

  @Data
  public static class MailchimpProductsResponse {
    @JsonProperty("store_id")
    private String storeId;

    private List<MailchimpProduct> products;

    @JsonProperty("total_items")
    private Integer totalItems;

    @JsonProperty("_links")
    private List<Map<String, Object>> links;
  }
}

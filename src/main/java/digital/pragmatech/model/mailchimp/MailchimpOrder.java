package digital.pragmatech.model.mailchimp;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class MailchimpOrder {
  private String id;

  private MailchimpCustomer customer;

  @JsonProperty("store_id")
  private String storeId;

  @JsonProperty("campaign_id")
  private String campaignId;

  @JsonProperty("financial_status")
  private String financialStatus;

  @JsonProperty("fulfillment_status")
  private String fulfillmentStatus;

  @JsonProperty("currency_code")
  private String currencyCode;

  @JsonProperty("order_total")
  private String orderTotal;

  @JsonProperty("order_url")
  private String orderUrl;

  @JsonProperty("discount_total")
  private String discountTotal;

  @JsonProperty("tax_total")
  private String taxTotal;

  @JsonProperty("shipping_total")
  private String shippingTotal;

  @JsonProperty("tracking_code")
  private String trackingCode;

  @JsonProperty("processed_at_foreign")
  private String processedAtForeign;

  @JsonProperty("cancelled_at_foreign")
  private String cancelledAtForeign;

  @JsonProperty("updated_at_foreign")
  private String updatedAtForeign;

  @JsonProperty("shipping_address")
  private MailchimpAddress shippingAddress;

  @JsonProperty("billing_address")
  private MailchimpAddress billingAddress;

  private List<MailchimpOrderLine> lines;

  @JsonProperty("_links")
  private List<Map<String, Object>> links;

  @Data
  public static class MailchimpCustomer {
    private String id;

    @JsonProperty("email_address")
    private String emailAddress;

    @JsonProperty("opt_in_status")
    private Boolean optInStatus;

    private String company;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("orders_count")
    private Integer ordersCount;

    @JsonProperty("total_spent")
    private String totalSpent;

    private MailchimpAddress address;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("_links")
    private List<Map<String, Object>> links;
  }

  @Data
  public static class MailchimpOrderLine {
    private String id;

    @JsonProperty("product_id")
    private String productId;

    @JsonProperty("product_title")
    private String productTitle;

    @JsonProperty("product_variant_id")
    private String productVariantId;

    @JsonProperty("product_variant_title")
    private String productVariantTitle;

    private String sku;
    private Integer quantity;
    private String price;
    private String discount;

    @JsonProperty("_links")
    private List<Map<String, Object>> links;
  }

  @Data
  public static class MailchimpAddress {
    private String name;
    private String company;
    private String address1;
    private String address2;
    private String city;
    private String province;

    @JsonProperty("province_code")
    private String provinceCode;

    @JsonProperty("postal_code")
    private String postalCode;

    private String country;

    @JsonProperty("country_code")
    private String countryCode;

    private String longitude;
    private String latitude;
    private String phone;
  }

  @Data
  public static class MailchimpOrdersResponse {
    @JsonProperty("store_id")
    private String storeId;

    private List<MailchimpOrder> orders;

    @JsonProperty("total_items")
    private Integer totalItems;

    @JsonProperty("_links")
    private List<Map<String, Object>> links;
  }
}

package digital.pragmatech.model.common;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Order {
  private String id;
  private String orderNumber;
  private String customerId;
  private String customerEmail;
  private String storeId;
  private OrderStatus financialStatus;
  private FulfillmentStatus fulfillmentStatus;
  private String currencyCode;
  private BigDecimal orderTotal;
  private BigDecimal taxTotal;
  private BigDecimal shippingTotal;
  private BigDecimal discountTotal;
  private List<OrderLine> lines;
  private Customer customer;
  private Address shippingAddress;
  private Address billingAddress;
  private List<String> tags;
  private Map<String, Object> metadata;
  private LocalDateTime processedAt;
  private LocalDateTime cancelledAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public enum OrderStatus {
    PENDING,
    PAID,
    PARTIALLY_PAID,
    REFUNDED,
    PARTIALLY_REFUNDED,
    VOIDED,
    CANCELLED
  }

  public enum FulfillmentStatus {
    PENDING,
    PARTIALLY_SHIPPED,
    SHIPPED,
    DELIVERED,
    RETURNED,
    CANCELLED
  }

  @Data
  @Builder
  public static class OrderLine {
    private String id;
    private String productId;
    private String productVariantId;
    private String productTitle;
    private String variantTitle;
    private String sku;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal discount;
    private BigDecimal total;
    private Map<String, Object> metadata;
  }

  @Data
  @Builder
  public static class Address {
    private String name;
    private String company;
    private String address1;
    private String address2;
    private String city;
    private String province;
    private String provinceCode;
    private String country;
    private String countryCode;
    private String postalCode;
    private String phone;
  }
}

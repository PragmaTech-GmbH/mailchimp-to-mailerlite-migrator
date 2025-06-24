package digital.pragmatech.service.mailerlite;

import digital.pragmatech.model.common.*;
import digital.pragmatech.model.mailerlite.MailerLiteGroup;
import digital.pragmatech.model.mailerlite.MailerLiteOrder;
import digital.pragmatech.model.mailerlite.MailerLiteProduct;
import digital.pragmatech.model.mailerlite.MailerLiteSubscriber;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailerLiteService {

  private final MailerLiteApiClient apiClient;

  public boolean testConnection() {
    return apiClient.testConnection();
  }

  public MailerLiteGroup createGroup(String name) {
    Map<String, Object> request = Map.of("name", name);

    Map<String, Object> response =
        apiClient.post(
            "/groups", request, new ParameterizedTypeReference<Map<String, Object>>() {});

    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) response.get("data");

    return mapToMailerLiteGroup(data);
  }

  public List<MailerLiteGroup> getAllGroups() {
    Map<String, Object> response =
        apiClient.get("/groups", new ParameterizedTypeReference<Map<String, Object>>() {});

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");

    if (data == null) {
      return Collections.emptyList();
    }

    return data.stream().map(this::mapToMailerLiteGroup).collect(Collectors.toList());
  }

  public MailerLiteSubscriber createOrUpdateSubscriber(Subscriber subscriber) {
    Map<String, Object> request = new HashMap<>();
    request.put("email", subscriber.getEmail());
    request.put("status", mapStatus(subscriber.getStatus()));

    Map<String, Object> fields = new HashMap<>();
    if (subscriber.getFirstName() != null) {
      fields.put("name", subscriber.getFirstName());
    }
    if (subscriber.getLastName() != null) {
      fields.put("last_name", subscriber.getLastName());
    }

    // Add custom fields
    if (subscriber.getCustomFields() != null) {
      fields.putAll(subscriber.getCustomFields());
    }

    if (!fields.isEmpty()) {
      request.put("fields", fields);
    }

    Map<String, Object> response =
        apiClient.post(
            "/subscribers", request, new ParameterizedTypeReference<Map<String, Object>>() {});

    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) response.get("data");

    return mapToMailerLiteSubscriber(data);
  }

  public void assignSubscriberToGroup(String subscriberId, String groupId) {
    // MailerLite API expects a POST to assign subscriber to group
    apiClient.post(
        "/subscribers/{subscriberId}/groups/{groupId}",
        null,
        new ParameterizedTypeReference<Map<String, Object>>() {},
        subscriberId,
        groupId);

    log.debug("Assigned subscriber {} to group {}", subscriberId, groupId);
  }

  public void bulkImportSubscribers(List<Subscriber> subscribers, String groupId) {
    if (subscribers.isEmpty()) {
      return;
    }

    List<Map<String, Object>> subscriberRequests =
        subscribers.stream().map(this::mapSubscriberForImport).collect(Collectors.toList());

    if (groupId != null) {
      // Use the correct bulk import endpoint for groups
      Map<String, Object> request = Map.of("subscribers", subscriberRequests);

      Map<String, Object> response =
          apiClient.post(
              "/groups/{groupId}/import-subscribers",
              request,
              new ParameterizedTypeReference<Map<String, Object>>() {},
              groupId);

      log.info("Bulk import initiated for group {}: {}", groupId, response);
    } else {
      // For bulk import without specific group, create subscribers individually
      // MailerLite doesn't have a global bulk import endpoint
      log.info("Creating {} subscribers individually (no group specified)", subscribers.size());

      int successCount = 0;
      int failCount = 0;

      for (Subscriber subscriber : subscribers) {
        try {
          createOrUpdateSubscriber(subscriber);
          successCount++;

          // Small delay to respect rate limits (120 requests per minute = ~500ms per request)
          Thread.sleep(500);
        } catch (Exception e) {
          failCount++;
          log.warn("Failed to create subscriber {}: {}", subscriber.getEmail(), e.getMessage());
        }
      }

      log.info(
          "Individual subscriber creation completed. Success: {}, Failed: {}",
          successCount,
          failCount);
    }
  }

  public Map<String, Object> createEcommerceShop(EcommerceShop shop) {
    Map<String, Object> request = new HashMap<>();
    request.put("name", shop.getName());
    request.put("url", shop.getDomain());
    request.put("currency", shop.getCurrency() != null ? shop.getCurrency() : "USD");

    return apiClient.post(
        "/ecommerce/shops", request, new ParameterizedTypeReference<Map<String, Object>>() {});
  }

  public Map<String, Object> createCategory(String shopId, Category category) {
    Map<String, Object> request = new HashMap<>();
    request.put("name", category.getName());

    if (category.getParentId() != null) {
      request.put("parent_id", category.getParentId());
    }

    return apiClient.post(
        "/ecommerce/shops/{shopId}/categories",
        request,
        new ParameterizedTypeReference<Map<String, Object>>() {},
        shopId);
  }

  public MailerLiteProduct createProduct(String shopId, Product product) {
    Map<String, Object> request = new HashMap<>();
    request.put("name", product.getName());
    request.put("description", product.getDescription());

    // Generate a URL based on the product name if not available
    String productUrl = "https://shop.example.com/products/" + product.getId();
    request.put("url", productUrl);

    // Get the first image URL if available
    if (product.getImages() != null && !product.getImages().isEmpty()) {
      request.put("image_url", product.getImages().get(0).getUrl());
    }

    // Get price and SKU from first variant
    if (product.getVariants() != null && !product.getVariants().isEmpty()) {
      Product.ProductVariant firstVariant = product.getVariants().get(0);
      if (firstVariant.getPrice() != null) {
        request.put("price", firstVariant.getPrice().toString());
      }
      if (firstVariant.getSku() != null) {
        request.put("sku", firstVariant.getSku());
      }
    }

    // Map tags to categories if needed
    if (product.getTags() != null && !product.getTags().isEmpty()) {
      request.put("categories", new ArrayList<>(product.getTags()));
    }

    Map<String, Object> response =
        apiClient.post(
            "/ecommerce/shops/{shopId}/products",
            request,
            new ParameterizedTypeReference<Map<String, Object>>() {},
            shopId);

    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) response.get("data");

    return mapToMailerLiteProduct(data);
  }

  public MailerLiteOrder syncOrder(String shopId, Order order) {
    Map<String, Object> request = new HashMap<>();
    request.put("external_id", order.getId());
    request.put("email", order.getCustomerEmail());
    request.put("status", mapOrderStatus(order.getFinancialStatus()));
    request.put("total", order.getOrderTotal().toString());
    request.put("currency", order.getCurrencyCode() != null ? order.getCurrencyCode() : "USD");

    // Generate order URL
    String orderUrl = "https://shop.example.com/orders/" + order.getId();
    request.put("order_url", orderUrl);

    // Map order lines to items
    List<Map<String, Object>> items = new ArrayList<>();
    if (order.getLines() != null) {
      for (Order.OrderLine line : order.getLines()) {
        Map<String, Object> item = new HashMap<>();
        item.put("product_id", line.getProductId());
        item.put("product_name", line.getProductTitle());
        item.put("product_url", "https://shop.example.com/products/" + line.getProductId());
        item.put("quantity", line.getQuantity());
        item.put("price", line.getPrice().toString());
        item.put("discount", line.getDiscount().toString());
        items.add(item);
      }
    }
    request.put("items", items);

    // Add billing info if available
    if (order.getBillingAddress() != null) {
      Map<String, Object> billingInfo = new HashMap<>();
      billingInfo.put("name", order.getBillingAddress().getName());
      billingInfo.put("company", order.getBillingAddress().getCompany());
      billingInfo.put("address1", order.getBillingAddress().getAddress1());
      billingInfo.put("address2", order.getBillingAddress().getAddress2());
      billingInfo.put("city", order.getBillingAddress().getCity());
      billingInfo.put("state", order.getBillingAddress().getProvince());
      billingInfo.put("country", order.getBillingAddress().getCountry());
      billingInfo.put("zip", order.getBillingAddress().getPostalCode());
      request.put("billing_info", billingInfo);
    }

    // Format order date
    if (order.getProcessedAt() != null) {
      DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
      request.put("ordered_at", order.getProcessedAt().format(formatter));
    }

    Map<String, Object> response =
        apiClient.post(
            "/ecommerce/shops/{shopId}/orders",
            request,
            new ParameterizedTypeReference<Map<String, Object>>() {},
            shopId);

    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) response.get("data");

    return mapToMailerLiteOrder(data);
  }

  private MailerLiteProduct mapToMailerLiteProduct(Map<String, Object> data) {
    return MailerLiteProduct.builder()
        .id((String) data.get("id"))
        .name((String) data.get("name"))
        .description((String) data.get("description"))
        .url((String) data.get("url"))
        .imageUrl((String) data.get("image_url"))
        .price((String) data.get("price"))
        .sku((String) data.get("sku"))
        .shopId((String) data.get("shop_id"))
        .createdAt((String) data.get("created_at"))
        .updatedAt((String) data.get("updated_at"))
        .build();
  }

  private MailerLiteOrder mapToMailerLiteOrder(Map<String, Object> data) {
    return MailerLiteOrder.builder()
        .id((String) data.get("id"))
        .shopId((String) data.get("shop_id"))
        .email((String) data.get("email"))
        .orderUrl((String) data.get("order_url"))
        .externalId((String) data.get("external_id"))
        .status((String) data.get("status"))
        .total((String) data.get("total"))
        .currency((String) data.get("currency"))
        .orderedAt((String) data.get("ordered_at"))
        .createdAt((String) data.get("created_at"))
        .updatedAt((String) data.get("updated_at"))
        .build();
  }

  private String mapOrderStatus(Order.OrderStatus status) {
    if (status == null) return "pending";

    return switch (status) {
      case PAID -> "completed";
      case PENDING -> "pending";
      case REFUNDED, PARTIALLY_REFUNDED -> "refunded";
      case CANCELLED, VOIDED -> "cancelled";
      default -> "pending";
    };
  }

  private MailerLiteGroup mapToMailerLiteGroup(Map<String, Object> data) {
    MailerLiteGroup group = new MailerLiteGroup();
    group.setId((String) data.get("id"));
    group.setName((String) data.get("name"));
    group.setActiveCount((Integer) data.get("active_count"));
    group.setSentCount((Integer) data.get("sent_count"));
    group.setOpensCount((Integer) data.get("opens_count"));
    group.setClicksCount((Integer) data.get("clicks_count"));
    group.setUnsubscribedCount((Integer) data.get("unsubscribed_count"));
    group.setUnconfirmedCount((Integer) data.get("unconfirmed_count"));
    group.setBouncedCount((Integer) data.get("bounced_count"));
    group.setJunkCount((Integer) data.get("junk_count"));

    return group;
  }

  private MailerLiteSubscriber mapToMailerLiteSubscriber(Map<String, Object> data) {
    MailerLiteSubscriber subscriber = new MailerLiteSubscriber();
    subscriber.setId((String) data.get("id"));
    subscriber.setEmail((String) data.get("email"));
    subscriber.setStatus((String) data.get("status"));
    subscriber.setSource((String) data.get("source"));

    @SuppressWarnings("unchecked")
    Map<String, Object> fields = (Map<String, Object>) data.get("fields");
    subscriber.setFields(fields);

    return subscriber;
  }

  private Map<String, Object> mapSubscriberForImport(Subscriber subscriber) {
    Map<String, Object> request = new HashMap<>();
    request.put("email", subscriber.getEmail());
    request.put("status", mapStatus(subscriber.getStatus()));

    Map<String, Object> fields = new HashMap<>();
    if (subscriber.getFirstName() != null) {
      fields.put("name", subscriber.getFirstName());
    }
    if (subscriber.getLastName() != null) {
      fields.put("last_name", subscriber.getLastName());
    }

    if (subscriber.getCustomFields() != null) {
      fields.putAll(subscriber.getCustomFields());
    }

    if (!fields.isEmpty()) {
      request.put("fields", fields);
    }

    return request;
  }

  private String mapStatus(Subscriber.SubscriberStatus status) {
    if (status == null) {
      return "active";
    }

    return switch (status) {
      case SUBSCRIBED -> "active";
      case UNSUBSCRIBED -> "unsubscribed";
      case PENDING -> "unconfirmed";
      case CLEANED, BOUNCED -> "bounced";
    };
  }
}

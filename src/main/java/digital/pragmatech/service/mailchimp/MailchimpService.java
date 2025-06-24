package digital.pragmatech.service.mailchimp;

import digital.pragmatech.model.common.*;
import digital.pragmatech.model.mailchimp.MailchimpList;
import digital.pragmatech.model.mailchimp.MailchimpMember;
import digital.pragmatech.model.mailchimp.MailchimpOrder;
import digital.pragmatech.model.mailchimp.MailchimpProduct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailchimpService {

  private final MailchimpApiClient apiClient;

  public boolean testConnection() {
    return apiClient.testConnection();
  }

  public List<MailchimpList> getAllLists() {
    Map<String, Object> response =
        apiClient.get(
            "/lists?count=1000", new ParameterizedTypeReference<Map<String, Object>>() {});

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> lists = (List<Map<String, Object>>) response.get("lists");

    return lists.stream().map(this::mapToMailchimpList).collect(Collectors.toList());
  }

  public List<MailchimpMember> getAllMembers(String listId) {
    List<MailchimpMember> allMembers = new ArrayList<>();
    int offset = 0;
    int count = 1000;
    boolean hasMore = true;

    while (hasMore) {
      Map<String, Object> response =
          apiClient.get(
              "/lists/{listId}/members?count={count}&offset={offset}&status=subscribed,unsubscribed,cleaned,pending",
              new ParameterizedTypeReference<Map<String, Object>>() {},
              listId,
              count,
              offset);

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> members = (List<Map<String, Object>>) response.get("members");

      if (members == null || members.isEmpty()) {
        hasMore = false;
      } else {
        List<MailchimpMember> batchMembers =
            members.stream().map(this::mapToMailchimpMember).collect(Collectors.toList());
        allMembers.addAll(batchMembers);

        offset += count;
        hasMore = members.size() == count;
      }

      log.debug(
          "Fetched {} members for list {}, offset: {}",
          members != null ? members.size() : 0,
          listId,
          offset);
    }

    return allMembers;
  }

  public int getMemberCount(String listId) {
    // Get only the stats from the list endpoint, which includes member count without fetching all
    // members
    Map<String, Object> response =
        apiClient.get(
            "/lists/{listId}?fields=stats.member_count",
            new ParameterizedTypeReference<Map<String, Object>>() {},
            listId);

    @SuppressWarnings("unchecked")
    Map<String, Object> stats = (Map<String, Object>) response.get("stats");

    if (stats != null && stats.containsKey("member_count")) {
      return (Integer) stats.get("member_count");
    }

    log.warn("Could not get member count for list {}, falling back to 0", listId);
    return 0;
  }

  public List<String> getAllTags(String listId) {
    Set<String> allTags = new HashSet<>();

    // Get tags from segments
    Map<String, Object> segmentsResponse =
        apiClient.get(
            "/lists/{listId}/segments?count=1000",
            new ParameterizedTypeReference<Map<String, Object>>() {},
            listId);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> segments =
        (List<Map<String, Object>>) segmentsResponse.get("segments");

    if (segments != null) {
      for (Map<String, Object> segment : segments) {
        String name = (String) segment.get("name");
        if (name != null) {
          allTags.add(name);
        }
      }
    }

    // Get tags from interest categories
    Map<String, Object> interestsResponse =
        apiClient.get(
            "/lists/{listId}/interest-categories?count=60",
            new ParameterizedTypeReference<Map<String, Object>>() {},
            listId);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> categories =
        (List<Map<String, Object>>) interestsResponse.get("categories");

    if (categories != null) {
      for (Map<String, Object> category : categories) {
        String categoryId = (String) category.get("id");
        String categoryName = (String) category.get("title");

        if (categoryName != null) {
          allTags.add(categoryName);
        }

        // Get interests within this category
        Map<String, Object> categoryInterests =
            apiClient.get(
                "/lists/{listId}/interest-categories/{categoryId}/interests?count=1000",
                new ParameterizedTypeReference<Map<String, Object>>() {},
                listId,
                categoryId);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> interests =
            (List<Map<String, Object>>) categoryInterests.get("interests");

        if (interests != null) {
          for (Map<String, Object> interest : interests) {
            String interestName = (String) interest.get("name");
            if (interestName != null) {
              allTags.add(interestName);
            }
          }
        }
      }
    }

    return new ArrayList<>(allTags);
  }

  public EcommerceShop getEcommerceShop(String storeId) {
    try {
      Map<String, Object> response =
          apiClient.get(
              "/ecommerce/stores/{storeId}",
              new ParameterizedTypeReference<Map<String, Object>>() {},
              storeId);
      return mapToEcommerceShop(response);
    } catch (Exception e) {
      log.warn("No e-commerce store found with ID: {}", storeId);
      return null;
    }
  }

  public List<EcommerceShop> getAllEcommerceShops() {
    Map<String, Object> response =
        apiClient.get(
            "/ecommerce/stores?count=1000",
            new ParameterizedTypeReference<Map<String, Object>>() {});

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> stores = (List<Map<String, Object>>) response.get("stores");

    if (stores == null) {
      return Collections.emptyList();
    }

    return stores.stream().map(this::mapToEcommerceShop).collect(Collectors.toList());
  }

  private MailchimpList mapToMailchimpList(Map<String, Object> data) {
    MailchimpList list = new MailchimpList();
    list.setId((String) data.get("id"));
    list.setName((String) data.get("name"));
    list.setWebId((Integer) data.get("web_id"));
    list.setPermissionReminder((String) data.get("permission_reminder"));
    list.setUseArchiveBar(Boolean.TRUE.equals(data.get("use_archive_bar")));
    list.setDoubleOptin(Boolean.TRUE.equals(data.get("double_optin")));
    list.setHasWelcome(Boolean.TRUE.equals(data.get("has_welcome")));
    list.setMarketingPermissions(Boolean.TRUE.equals(data.get("marketing_permissions")));

    @SuppressWarnings("unchecked")
    Map<String, Integer> stats = (Map<String, Integer>) data.get("stats");
    list.setStats(stats);

    return list;
  }

  private MailchimpMember mapToMailchimpMember(Map<String, Object> data) {
    MailchimpMember member = new MailchimpMember();
    member.setId((String) data.get("id"));
    member.setEmailAddress((String) data.get("email_address"));
    member.setUniqueEmailId((String) data.get("unique_email_id"));
    member.setWebId((Integer) data.get("web_id"));
    member.setEmailType((String) data.get("email_type"));
    member.setStatus((String) data.get("status"));
    member.setUnsubscribeReason((String) data.get("unsubscribe_reason"));
    member.setLanguage((String) data.get("language"));
    member.setVip(Boolean.TRUE.equals(data.get("vip")));
    member.setEmailClient((String) data.get("email_client"));
    member.setSource((String) data.get("source"));
    member.setTagsCount((Integer) data.get("tags_count"));
    member.setListId((String) data.get("list_id"));

    @SuppressWarnings("unchecked")
    Map<String, Object> mergeFields = (Map<String, Object>) data.get("merge_fields");
    member.setMergeFields(mergeFields);

    @SuppressWarnings("unchecked")
    Map<String, Boolean> interests = (Map<String, Boolean>) data.get("interests");
    member.setInterests(interests);

    @SuppressWarnings("unchecked")
    Map<String, Integer> stats = (Map<String, Integer>) data.get("stats");
    member.setStats(stats);

    return member;
  }

  private EcommerceShop mapToEcommerceShop(Map<String, Object> data) {
    return EcommerceShop.builder()
        .id((String) data.get("id"))
        .name((String) data.get("name"))
        .platform((String) data.get("platform"))
        .domain((String) data.get("domain"))
        .currency((String) data.get("currency_code"))
        .timezone((String) data.get("timezone"))
        .email((String) data.get("email_address"))
        .phone((String) data.get("phone"))
        .createdAt(parseDateTime((String) data.get("created_at")))
        .updatedAt(parseDateTime((String) data.get("updated_at")))
        .build();
  }

  private LocalDateTime parseDateTime(String dateTime) {
    if (dateTime == null) return null;
    try {
      return LocalDateTime.parse(dateTime.replace("Z", ""));
    } catch (Exception e) {
      log.warn("Failed to parse datetime: {}", dateTime);
      return null;
    }
  }

  public List<Product> getStoreProducts(String storeId) {
    List<Product> allProducts = new ArrayList<>();
    int offset = 0;
    int count = 100;
    boolean hasMore = true;

    while (hasMore) {
      MailchimpProduct.MailchimpProductsResponse response =
          apiClient.get(
              "/ecommerce/stores/{storeId}/products?count={count}&offset={offset}",
              MailchimpProduct.MailchimpProductsResponse.class,
              storeId,
              count,
              offset);

      if (response.getProducts() == null || response.getProducts().isEmpty()) {
        hasMore = false;
      } else {
        List<Product> batchProducts =
            response.getProducts().stream().map(this::mapToProduct).collect(Collectors.toList());
        allProducts.addAll(batchProducts);

        offset += count;
        hasMore = response.getProducts().size() == count;
      }

      log.debug(
          "Fetched {} products for store {}, offset: {}",
          response.getProducts() != null ? response.getProducts().size() : 0,
          storeId,
          offset);
    }

    return allProducts;
  }

  public List<Order> getStoreOrders(String storeId) {
    List<Order> allOrders = new ArrayList<>();
    int offset = 0;
    int count = 100;
    boolean hasMore = true;

    while (hasMore) {
      MailchimpOrder.MailchimpOrdersResponse response =
          apiClient.get(
              "/ecommerce/stores/{storeId}/orders?count={count}&offset={offset}",
              MailchimpOrder.MailchimpOrdersResponse.class,
              storeId,
              count,
              offset);

      if (response.getOrders() == null || response.getOrders().isEmpty()) {
        hasMore = false;
      } else {
        List<Order> batchOrders =
            response.getOrders().stream().map(this::mapToOrder).collect(Collectors.toList());
        allOrders.addAll(batchOrders);

        offset += count;
        hasMore = response.getOrders().size() == count;
      }

      log.debug(
          "Fetched {} orders for store {}, offset: {}",
          response.getOrders() != null ? response.getOrders().size() : 0,
          storeId,
          offset);
    }

    return allOrders;
  }

  private Product mapToProduct(MailchimpProduct mailchimpProduct) {
    List<Product.ProductVariant> variants = new ArrayList<>();
    if (mailchimpProduct.getVariants() != null) {
      variants =
          mailchimpProduct.getVariants().stream()
              .map(this::mapToProductVariant)
              .collect(Collectors.toList());
    }

    List<Product.ProductImage> images = new ArrayList<>();
    if (mailchimpProduct.getImages() != null) {
      images =
          mailchimpProduct.getImages().stream()
              .map(this::mapToProductImage)
              .collect(Collectors.toList());
    }

    return Product.builder()
        .id(mailchimpProduct.getId())
        .name(mailchimpProduct.getTitle())
        .description(mailchimpProduct.getDescription())
        .handle(mailchimpProduct.getHandle())
        .productType(mailchimpProduct.getType())
        .vendor(mailchimpProduct.getVendor())
        .status(Product.ProductStatus.ACTIVE)
        .variants(variants)
        .images(images)
        .createdAt(parseDateTime(mailchimpProduct.getPublishedAtForeign()))
        .updatedAt(LocalDateTime.now())
        .build();
  }

  private Product.ProductVariant mapToProductVariant(
      MailchimpProduct.MailchimpProductVariant variant) {
    return Product.ProductVariant.builder()
        .id(variant.getId())
        .sku(variant.getSku())
        .title(variant.getTitle())
        .price(new BigDecimal(variant.getPrice() != null ? variant.getPrice() : "0"))
        .inventoryQuantity(variant.getInventoryQuantity())
        .trackInventory(true)
        .status(Product.ProductVariant.VariantStatus.ACTIVE)
        .createdAt(parseDateTime(variant.getCreatedAt()))
        .updatedAt(parseDateTime(variant.getUpdatedAt()))
        .build();
  }

  private Product.ProductImage mapToProductImage(MailchimpProduct.MailchimpProductImage image) {
    return Product.ProductImage.builder()
        .id(image.getId())
        .url(image.getUrl())
        .variantIds(image.getVariantIds())
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  private Order mapToOrder(MailchimpOrder mailchimpOrder) {
    Customer customer = null;
    if (mailchimpOrder.getCustomer() != null) {
      customer = mapToCustomer(mailchimpOrder.getCustomer());
    }

    List<Order.OrderLine> lines = new ArrayList<>();
    if (mailchimpOrder.getLines() != null) {
      lines =
          mailchimpOrder.getLines().stream().map(this::mapToOrderLine).collect(Collectors.toList());
    }

    return Order.builder()
        .id(mailchimpOrder.getId())
        .storeId(mailchimpOrder.getStoreId())
        .customerId(customer != null ? customer.getId() : null)
        .customerEmail(customer != null ? customer.getEmail() : null)
        .customer(customer)
        .financialStatus(mapFinancialStatus(mailchimpOrder.getFinancialStatus()))
        .fulfillmentStatus(mapFulfillmentStatus(mailchimpOrder.getFulfillmentStatus()))
        .currencyCode(mailchimpOrder.getCurrencyCode())
        .orderTotal(
            new BigDecimal(
                mailchimpOrder.getOrderTotal() != null ? mailchimpOrder.getOrderTotal() : "0"))
        .taxTotal(
            new BigDecimal(
                mailchimpOrder.getTaxTotal() != null ? mailchimpOrder.getTaxTotal() : "0"))
        .shippingTotal(
            new BigDecimal(
                mailchimpOrder.getShippingTotal() != null
                    ? mailchimpOrder.getShippingTotal()
                    : "0"))
        .discountTotal(
            new BigDecimal(
                mailchimpOrder.getDiscountTotal() != null
                    ? mailchimpOrder.getDiscountTotal()
                    : "0"))
        .lines(lines)
        .shippingAddress(mapToOrderAddress(mailchimpOrder.getShippingAddress()))
        .billingAddress(mapToOrderAddress(mailchimpOrder.getBillingAddress()))
        .processedAt(parseDateTime(mailchimpOrder.getProcessedAtForeign()))
        .cancelledAt(parseDateTime(mailchimpOrder.getCancelledAtForeign()))
        .createdAt(LocalDateTime.now())
        .updatedAt(parseDateTime(mailchimpOrder.getUpdatedAtForeign()))
        .build();
  }

  private Customer mapToCustomer(MailchimpOrder.MailchimpCustomer mailchimpCustomer) {
    return Customer.builder()
        .id(mailchimpCustomer.getId())
        .email(mailchimpCustomer.getEmailAddress())
        .firstName(mailchimpCustomer.getFirstName())
        .lastName(mailchimpCustomer.getLastName())
        .company(mailchimpCustomer.getCompany())
        .emailMarketingConsent(mailchimpCustomer.getOptInStatus())
        .totalSpent(
            new BigDecimal(
                mailchimpCustomer.getTotalSpent() != null
                    ? mailchimpCustomer.getTotalSpent()
                    : "0"))
        .ordersCount(mailchimpCustomer.getOrdersCount())
        .status(Customer.CustomerStatus.ACTIVE)
        .defaultAddress(mapToCustomerAddress(mailchimpCustomer.getAddress()))
        .createdAt(parseDateTime(mailchimpCustomer.getCreatedAt()))
        .updatedAt(parseDateTime(mailchimpCustomer.getUpdatedAt()))
        .build();
  }

  private Order.OrderLine mapToOrderLine(MailchimpOrder.MailchimpOrderLine line) {
    return Order.OrderLine.builder()
        .id(line.getId())
        .productId(line.getProductId())
        .productVariantId(line.getProductVariantId())
        .productTitle(line.getProductTitle())
        .variantTitle(line.getProductVariantTitle())
        .sku(line.getSku())
        .quantity(line.getQuantity())
        .price(new BigDecimal(line.getPrice() != null ? line.getPrice() : "0"))
        .discount(new BigDecimal(line.getDiscount() != null ? line.getDiscount() : "0"))
        .total(
            new BigDecimal(line.getPrice() != null ? line.getPrice() : "0")
                .multiply(new BigDecimal(line.getQuantity())))
        .build();
  }

  private Order.Address mapToOrderAddress(MailchimpOrder.MailchimpAddress address) {
    if (address == null) return null;

    return Order.Address.builder()
        .name(address.getName())
        .company(address.getCompany())
        .address1(address.getAddress1())
        .address2(address.getAddress2())
        .city(address.getCity())
        .province(address.getProvince())
        .provinceCode(address.getProvinceCode())
        .country(address.getCountry())
        .countryCode(address.getCountryCode())
        .postalCode(address.getPostalCode())
        .phone(address.getPhone())
        .build();
  }

  private Customer.Address mapToCustomerAddress(MailchimpOrder.MailchimpAddress address) {
    if (address == null) return null;

    return Customer.Address.builder()
        .address1(address.getAddress1())
        .address2(address.getAddress2())
        .city(address.getCity())
        .province(address.getProvince())
        .provinceCode(address.getProvinceCode())
        .country(address.getCountry())
        .countryCode(address.getCountryCode())
        .postalCode(address.getPostalCode())
        .phone(address.getPhone())
        .build();
  }

  private Order.OrderStatus mapFinancialStatus(String status) {
    if (status == null) return Order.OrderStatus.PENDING;

    return switch (status.toLowerCase()) {
      case "paid" -> Order.OrderStatus.PAID;
      case "pending" -> Order.OrderStatus.PENDING;
      case "refunded" -> Order.OrderStatus.REFUNDED;
      case "cancelled" -> Order.OrderStatus.CANCELLED;
      default -> Order.OrderStatus.PENDING;
    };
  }

  private Order.FulfillmentStatus mapFulfillmentStatus(String status) {
    if (status == null) return Order.FulfillmentStatus.PENDING;

    return switch (status.toLowerCase()) {
      case "shipped" -> Order.FulfillmentStatus.SHIPPED;
      case "partially_shipped" -> Order.FulfillmentStatus.PARTIALLY_SHIPPED;
      case "delivered" -> Order.FulfillmentStatus.DELIVERED;
      case "cancelled" -> Order.FulfillmentStatus.CANCELLED;
      default -> Order.FulfillmentStatus.PENDING;
    };
  }
}

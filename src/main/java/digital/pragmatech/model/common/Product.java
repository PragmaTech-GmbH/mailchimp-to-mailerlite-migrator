package digital.pragmatech.model.common;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Product {
  private String id;
  private String name;
  private String description;
  private String handle;
  private String productType;
  private String vendor;
  private ProductStatus status;
  private List<ProductVariant> variants;
  private List<String> tags;
  private List<ProductImage> images;
  private Map<String, Object> metadata;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public enum ProductStatus {
    ACTIVE,
    ARCHIVED,
    DRAFT
  }

  @Data
  @Builder
  public static class ProductVariant {
    private String id;
    private String sku;
    private String title;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private Integer inventoryQuantity;
    private BigDecimal weight;
    private String weightUnit;
    private Boolean trackInventory;
    private VariantStatus status;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum VariantStatus {
      ACTIVE,
      INACTIVE,
      OUT_OF_STOCK
    }
  }

  @Data
  @Builder
  public static class ProductImage {
    private String id;
    private String url;
    private String alt;
    private Integer position;
    private List<String> variantIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
  }
}

package digital.pragmatech.model.common;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Product {
  private String id;
  private String title;
  private String handle;
  private String url;
  private String description;
  private String type;
  private String vendor;
  private String imageUrl;
  private List<ProductVariant> variants;
  private Set<String> categories;
  private LocalDateTime publishedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  @Data
  @Builder
  public static class ProductVariant {
    private String id;
    private String title;
    private String url;
    private String sku;
    private BigDecimal price;
    private Integer inventoryQuantity;
    private String imageUrl;
    private String barcode;
    private BigDecimal weight;
    private String weightUnit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
  }
}

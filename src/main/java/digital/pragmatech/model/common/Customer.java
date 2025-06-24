package digital.pragmatech.model.common;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Customer {
  private String id;
  private String email;
  private String firstName;
  private String lastName;
  private String phone;
  private String company;
  private CustomerStatus status;
  private Boolean emailMarketingConsent;
  private Boolean smsMarketingConsent;
  private BigDecimal totalSpent;
  private Integer ordersCount;
  private BigDecimal avgOrderValue;
  private List<String> tags;
  private Address defaultAddress;
  private Map<String, Object> metadata;
  private LocalDateTime lastOrderDate;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public enum CustomerStatus {
    ACTIVE,
    INACTIVE,
    UNSUBSCRIBED,
    BLOCKED
  }

  @Data
  @Builder
  public static class Address {
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

  public String getFullName() {
    if (firstName != null && lastName != null) {
      return firstName + " " + lastName;
    } else if (firstName != null) {
      return firstName;
    } else if (lastName != null) {
      return lastName;
    }
    return email;
  }
}

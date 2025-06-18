package digital.pragmatech.model.common;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EcommerceShop {
    private String id;
    private String name;
    private String platform;
    private String domain;
    private String currency;
    private String timezone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String email;
    private String phone;
    private Address address;
    
    @Data
    @Builder
    public static class Address {
        private String address1;
        private String address2;
        private String city;
        private String province;
        private String provinceCode;
        private String postalCode;
        private String country;
        private String countryCode;
    }
}
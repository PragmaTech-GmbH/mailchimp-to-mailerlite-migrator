package digital.pragmatech.service.mailerlite;

import digital.pragmatech.model.common.*;
import digital.pragmatech.model.mailerlite.MailerLiteGroup;
import digital.pragmatech.model.mailerlite.MailerLiteSubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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
        
        Map<String, Object> response = apiClient.post("/groups", request,
                new ParameterizedTypeReference<Map<String, Object>>() {});
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        
        return mapToMailerLiteGroup(data);
    }
    
    public List<MailerLiteGroup> getAllGroups() {
        Map<String, Object> response = apiClient.get("/groups",
                new ParameterizedTypeReference<Map<String, Object>>() {});
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        
        if (data == null) {
            return Collections.emptyList();
        }
        
        return data.stream()
                .map(this::mapToMailerLiteGroup)
                .collect(Collectors.toList());
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
        
        Map<String, Object> response = apiClient.post("/subscribers", request,
                new ParameterizedTypeReference<Map<String, Object>>() {});
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        
        return mapToMailerLiteSubscriber(data);
    }
    
    public void assignSubscriberToGroup(String subscriberId, String groupId) {
        apiClient.post("/subscribers/{subscriberId}/groups/{groupId}", null, Void.class,
                subscriberId, groupId);
    }
    
    public void bulkImportSubscribers(List<Subscriber> subscribers, String groupId) {
        if (subscribers.isEmpty()) {
            return;
        }
        
        List<Map<String, Object>> subscriberRequests = subscribers.stream()
                .map(this::mapSubscriberForImport)
                .collect(Collectors.toList());
        
        Map<String, Object> request = Map.of("subscribers", subscriberRequests);
        
        if (groupId != null) {
            Map<String, Object> response = apiClient.post("/groups/{groupId}/import-subscribers", request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}, groupId);
            
            log.info("Bulk import initiated for group {}: {}", groupId, response);
        } else {
            Map<String, Object> response = apiClient.post("/import-subscribers", request,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            
            log.info("Bulk import initiated: {}", response);
        }
    }
    
    public Map<String, Object> createEcommerceShop(EcommerceShop shop) {
        Map<String, Object> request = new HashMap<>();
        request.put("name", shop.getName());
        request.put("url", shop.getDomain());
        request.put("currency", shop.getCurrency() != null ? shop.getCurrency() : "USD");
        
        return apiClient.post("/ecommerce/shops", request,
                new ParameterizedTypeReference<Map<String, Object>>() {});
    }
    
    public Map<String, Object> createCategory(String shopId, Category category) {
        Map<String, Object> request = new HashMap<>();
        request.put("name", category.getName());
        
        if (category.getParentId() != null) {
            request.put("parent_id", category.getParentId());
        }
        
        return apiClient.post("/ecommerce/shops/{shopId}/categories", request,
                new ParameterizedTypeReference<Map<String, Object>>() {}, shopId);
    }
    
    public Map<String, Object> createProduct(String shopId, Product product) {
        Map<String, Object> request = new HashMap<>();
        request.put("name", product.getTitle());
        request.put("url", product.getUrl());
        request.put("description", product.getDescription());
        
        if (product.getImageUrl() != null) {
            request.put("image_url", product.getImageUrl());
        }
        
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            Product.ProductVariant firstVariant = product.getVariants().get(0);
            if (firstVariant.getPrice() != null) {
                request.put("price", firstVariant.getPrice().doubleValue());
            }
            if (firstVariant.getSku() != null) {
                request.put("sku", firstVariant.getSku());
            }
        }
        
        if (product.getCategories() != null && !product.getCategories().isEmpty()) {
            request.put("categories", new ArrayList<>(product.getCategories()));
        }
        
        return apiClient.post("/ecommerce/shops/{shopId}/products", request,
                new ParameterizedTypeReference<Map<String, Object>>() {}, shopId);
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
package digital.pragmatech.service.mailchimp;

import digital.pragmatech.model.common.*;
import digital.pragmatech.model.mailchimp.MailchimpList;
import digital.pragmatech.model.mailchimp.MailchimpMember;
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
}

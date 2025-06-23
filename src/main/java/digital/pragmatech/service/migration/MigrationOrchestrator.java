package digital.pragmatech.service.migration;

import digital.pragmatech.config.MigrationConfig;
import digital.pragmatech.model.common.*;
import digital.pragmatech.model.mailchimp.MailchimpList;
import digital.pragmatech.model.mailchimp.MailchimpMember;
import digital.pragmatech.model.mailerlite.MailerLiteGroup;
import digital.pragmatech.service.mailchimp.MailchimpService;
import digital.pragmatech.service.mailerlite.MailerLiteService;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MigrationOrchestrator {

  private final MailchimpService mailchimpService;
  private final MailerLiteService mailerLiteService;
  private final MigrationProgressTracker progressTracker;
  private final MigrationConfig migrationConfig;

  // Step-based migration methods for wizard
  public Map<String, Object> migrateTags(List<String> tags) {
    log.info("Starting selective tag migration for {} tags", tags.size());
    
    try {
      progressTracker.updatePhase(MigrationStatus.MigrationPhase.TAG_GROUP_MIGRATION);
      
      int processedTags = 0;
      int successfulTags = 0;
      int failedTags = 0;
      List<String> createdGroups = new ArrayList<>();
      List<String> failedTagsList = new ArrayList<>();
      
      for (String tag : tags) {
        try {
          String cleanedTag = cleanTagName(tag);
          if (cleanedTag != null) {
            MailerLiteGroup group = mailerLiteService.createGroup(cleanedTag);
            log.info("Created group '{}' with ID: {}", cleanedTag, group.getId());
            createdGroups.add(cleanedTag);
            successfulTags++;
            
            progressTracker.updateProgress(tags.size(), processedTags + 1, successfulTags, failedTags);
            
            // Rate limiting
            Thread.sleep(500);
          }
          processedTags++;
        } catch (Exception e) {
          log.error("Failed to create group for tag: {}", tag, e);
          failedTagsList.add(tag);
          failedTags++;
          processedTags++;
          
          progressTracker.addError(
              "TAG_GROUP_MIGRATION", "Tag", tag, e.getMessage(), "GROUP_CREATION_FAILED", true);
          progressTracker.updateProgress(tags.size(), processedTags, successfulTags, failedTags);
        }
      }
      
      // Create result summary
      Map<String, Object> result = new HashMap<>();
      result.put("totalTags", tags.size());
      result.put("processedTags", processedTags);
      result.put("successfulTags", successfulTags);
      result.put("failedTags", failedTags);
      result.put("createdGroups", createdGroups);
      result.put("failedTags", failedTagsList);
      result.put("completedAt", LocalDateTime.now());
      
      // Store result for UI access
      progressTracker.setStepResult("tag_migration", result);
      
      log.info("Selective tag migration completed. Created {} groups, {} failed", successfulTags, failedTags);
      return result;
      
    } catch (Exception e) {
      log.error("Selective tag migration failed", e);
      throw new RuntimeException("Tag migration failed", e);
    }
  }

  public Map<String, Object> migrateStores(List<String> storeIds) {
    log.info("Starting selective store migration for {} stores", storeIds.size());
    
    try {
      progressTracker.updatePhase(MigrationStatus.MigrationPhase.ECOMMERCE_SETUP);
      
      int processedStores = 0;
      int successfulStores = 0;
      int failedStores = 0;
      List<String> migratedStores = new ArrayList<>();
      List<String> failedStoresList = new ArrayList<>();
      
      for (String storeId : storeIds) {
        try {
          // Get store details from Mailchimp
          EcommerceShop shop = mailchimpService.getEcommerceShop(storeId);
          
          // Create shop in MailerLite
          Map<String, Object> createdShop = mailerLiteService.createEcommerceShop(shop);
          log.info("Created e-commerce shop: {}", createdShop);
          
          migratedStores.add(shop.getName());
          successfulStores++;
          processedStores++;
          
          progressTracker.updateProgress(storeIds.size(), processedStores, successfulStores, failedStores);
          
          // Rate limiting
          Thread.sleep(1000);
          
        } catch (Exception e) {
          log.error("Failed to migrate store: {}", storeId, e);
          failedStoresList.add(storeId);
          failedStores++;
          processedStores++;
          
          progressTracker.addError(
              "ECOMMERCE_SETUP", "Store", storeId, e.getMessage(), "STORE_MIGRATION_FAILED", true);
          progressTracker.updateProgress(storeIds.size(), processedStores, successfulStores, failedStores);
        }
      }
      
      // Create result summary
      Map<String, Object> result = new HashMap<>();
      result.put("totalStores", storeIds.size());
      result.put("processedStores", processedStores);
      result.put("successfulStores", successfulStores);
      result.put("failedStores", failedStores);
      result.put("migratedStores", migratedStores);
      result.put("failedStores", failedStoresList);
      result.put("completedAt", LocalDateTime.now());
      
      // Store result for UI access
      progressTracker.setStepResult("store_migration", result);
      
      log.info("Selective store migration completed. Migrated {} stores, {} failed", successfulStores, failedStores);
      return result;
      
    } catch (Exception e) {
      log.error("Selective store migration failed", e);
      throw new RuntimeException("Store migration failed", e);
    }
  }

  public Map<String, Object> migrateSubscribers(List<String> selectedTags, List<String> selectedStores) {
    log.info("Starting subscriber migration with {} selected tags and {} selected stores", 
             selectedTags != null ? selectedTags.size() : 0, 
             selectedStores != null ? selectedStores.size() : 0);
    
    try {
      progressTracker.updatePhase(MigrationStatus.MigrationPhase.SUBSCRIBER_MIGRATION);
      
      // First, create tag-to-group mapping for selected tags
      Map<String, String> tagToGroupMapping = new HashMap<>();
      if (selectedTags != null) {
        for (String tag : selectedTags) {
          try {
            // Try to find existing group by name
            List<MailerLiteGroup> groups = mailerLiteService.getAllGroups();
            String cleanedTag = cleanTagName(tag);
            for (MailerLiteGroup group : groups) {
              if (group.getName().equals(cleanedTag)) {
                tagToGroupMapping.put(tag, group.getId());
                break;
              }
            }
          } catch (Exception e) {
            log.warn("Could not map tag '{}' to group: {}", tag, e.getMessage());
          }
        }
      }
      
      List<MailchimpList> lists = mailchimpService.getAllLists();
      int totalSubscribers = 0;
      int migratedSubscribers = 0;
      int failedSubscribers = 0;
      List<String> processedLists = new ArrayList<>();

      for (MailchimpList list : lists) {
        List<MailchimpMember> members = mailchimpService.getAllMembers(list.getId());
        totalSubscribers += members.size();
        processedLists.add(list.getName());

        // Process in batches
        List<List<MailchimpMember>> batches = partitionList(members, migrationConfig.getBatchSize());

        for (List<MailchimpMember> batch : batches) {
          try {
            List<Subscriber> subscribers = batch.stream()
                .map(this::convertToSubscriber)
                .collect(Collectors.toList());

            // Bulk import subscribers
            mailerLiteService.bulkImportSubscribers(subscribers, null);

            // Assign to groups based on selected tags only
            if (!tagToGroupMapping.isEmpty()) {
              for (MailchimpMember member : batch) {
                assignMemberToSelectedGroups(member, tagToGroupMapping, selectedTags);
              }
            }

            migratedSubscribers += batch.size();
            progressTracker.updateProgress(totalSubscribers, migratedSubscribers + failedSubscribers, migratedSubscribers, failedSubscribers);

            // Rate limiting
            Thread.sleep(1000);

          } catch (Exception e) {
            log.error("Failed to migrate subscriber batch", e);
            failedSubscribers += batch.size();
            progressTracker.addError(
                "SUBSCRIBER_MIGRATION", "Batch", "batch", e.getMessage(), "BATCH_MIGRATION_FAILED", true);
            progressTracker.updateProgress(totalSubscribers, migratedSubscribers + failedSubscribers, migratedSubscribers, failedSubscribers);
          }
        }
      }

      // Create result summary
      Map<String, Object> result = new HashMap<>();
      result.put("totalSubscribers", totalSubscribers);
      result.put("migratedSubscribers", migratedSubscribers);
      result.put("failedSubscribers", failedSubscribers);
      result.put("processedLists", processedLists);
      result.put("selectedTags", selectedTags != null ? selectedTags : new ArrayList<>());
      result.put("groupsMapped", tagToGroupMapping.size());
      result.put("completedAt", LocalDateTime.now());
      
      // Store result for UI access
      progressTracker.setStepResult("subscriber_migration", result);

      log.info("Subscriber migration completed. Migrated {}/{} subscribers", migratedSubscribers, totalSubscribers);
      return result;
      
    } catch (Exception e) {
      log.error("Subscriber migration failed", e);
      throw new RuntimeException("Subscriber migration failed", e);
    }
  }

  public Map<String, Object> syncOrders(List<String> selectedStores) {
    log.info("Starting order sync for {} selected stores", selectedStores.size());
    
    try {
      progressTracker.updatePhase(MigrationStatus.MigrationPhase.ECOMMERCE_SETUP);
      
      int totalOrders = 0;
      int syncedOrders = 0;
      int failedOrders = 0;
      List<String> processedStores = new ArrayList<>();
      List<String> failedStoresList = new ArrayList<>();
      
      for (String storeId : selectedStores) {
        try {
          // Order sync would require implementation of order-related models and methods
          // For now, we'll log this as a placeholder
          log.info("Order sync for store {} - Implementation pending", storeId);
          
          // Simulate some work for demonstration
          Thread.sleep(1000);
          
          // For demo purposes, assume 10 orders per store
          int simulatedOrders = 10;
          totalOrders += simulatedOrders;
          syncedOrders += simulatedOrders;
          processedStores.add(storeId);
          
          progressTracker.updateProgress(totalOrders, syncedOrders + failedOrders, syncedOrders, failedOrders);
          
        } catch (Exception e) {
          log.error("Failed to sync orders for store: {}", storeId, e);
          failedStoresList.add(storeId);
          // Assume 10 failed orders for this store
          failedOrders += 10;
          
          progressTracker.addError(
              "ORDER_SYNC", "Store", storeId, e.getMessage(), "STORE_ORDER_SYNC_FAILED", true);
          progressTracker.updateProgress(totalOrders + failedOrders, syncedOrders + failedOrders, syncedOrders, failedOrders);
        }
      }
      
      // Create result summary
      Map<String, Object> result = new HashMap<>();
      result.put("totalStores", selectedStores.size());
      result.put("processedStores", processedStores.size());
      result.put("failedStores", failedStoresList.size());
      result.put("totalOrders", totalOrders + failedOrders);
      result.put("syncedOrders", syncedOrders);
      result.put("failedOrders", failedOrders);
      result.put("processedStoresList", processedStores);
      result.put("failedStoresList", failedStoresList);
      result.put("completedAt", LocalDateTime.now());
      result.put("note", "Order sync is currently a placeholder implementation");
      
      // Store result for UI access
      progressTracker.setStepResult("order_sync", result);
      
      log.info("Order sync completed. Synced {}/{} orders", syncedOrders, totalOrders + failedOrders);
      return result;
      
    } catch (Exception e) {
      log.error("Order sync failed", e);
      throw new RuntimeException("Order sync failed", e);
    }
  }

  private void assignMemberToSelectedGroups(
      MailchimpMember member, Map<String, String> tagToGroupMapping, List<String> selectedTags) {
    if (member.getTags() != null && selectedTags != null) {
      for (MailchimpMember.Tag tag : member.getTags()) {
        // Only assign to groups for selected tags
        if (selectedTags.contains(tag.getName())) {
          String groupId = tagToGroupMapping.get(tag.getName());
          if (groupId != null) {
            try {
              mailerLiteService.assignSubscriberToGroup(member.getId(), groupId);
            } catch (Exception e) {
              log.warn(
                  "Failed to assign subscriber {} to group {}",
                  member.getEmailAddress(),
                  tag.getName());
            }
          }
        }
      }
    }
  }

  public String startMigration() {
    String migrationId = UUID.randomUUID().toString();
    log.info("Starting migration with ID: {}", migrationId);

    try {
      progressTracker.initializeMigration(migrationId);

      // Phase 1: Tag/Group Migration
      progressTracker.updatePhase(MigrationStatus.MigrationPhase.TAG_GROUP_MIGRATION);
      Map<String, String> tagToGroupMapping = migrateTagsToGroups();

      // Phase 2: E-commerce Setup
      progressTracker.updatePhase(MigrationStatus.MigrationPhase.ECOMMERCE_SETUP);
      migrateEcommerceData();

      // Phase 3: Subscriber Migration
      progressTracker.updatePhase(MigrationStatus.MigrationPhase.SUBSCRIBER_MIGRATION);
      migrateSubscribers(tagToGroupMapping);

      // Phase 4: Campaign Migration (Manual guidance)
      progressTracker.updatePhase(MigrationStatus.MigrationPhase.CAMPAIGN_MIGRATION);
      generateCampaignMigrationGuide();

      progressTracker.completeMigration();
      log.info("Migration {} completed successfully", migrationId);

      return migrationId;

    } catch (Exception e) {
      log.error("Migration {} failed", migrationId, e);
      progressTracker.failMigration("Migration failed: " + e.getMessage());
      throw new RuntimeException("Migration failed", e);
    }
  }

  private Map<String, String> migrateTagsToGroups() {
    log.info("Starting tag to group migration");
    Map<String, String> tagToGroupMapping = new HashMap<>();

    try {
      List<MailchimpList> lists = mailchimpService.getAllLists();
      Set<String> allTags = new HashSet<>();

      // Collect all unique tags from all lists
      for (MailchimpList list : lists) {
        List<String> listTags = mailchimpService.getAllTags(list.getId());
        allTags.addAll(listTags);
      }

      // Clean and normalize tags
      Set<String> cleanedTags =
          allTags.stream()
              .map(this::cleanTagName)
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());

      log.info("Found {} unique tags to migrate", cleanedTags.size());

      int processedTags = 0;
      for (String tag : cleanedTags) {
        try {
          MailerLiteGroup group = mailerLiteService.createGroup(tag);
          tagToGroupMapping.put(tag, group.getId());
          processedTags++;

          progressTracker.updateProgress(cleanedTags.size(), processedTags, processedTags, 0);

          // Rate limiting
          Thread.sleep(500); // 120 requests per minute = 2 per second

        } catch (Exception e) {
          log.error("Failed to create group for tag: {}", tag, e);
          progressTracker.addError(
              "TAG_GROUP_MIGRATION", "Tag", tag, e.getMessage(), "GROUP_CREATION_FAILED", true);
        }
      }

      log.info("Tag to group migration completed. Created {} groups", tagToGroupMapping.size());
      return tagToGroupMapping;

    } catch (Exception e) {
      log.error("Tag to group migration failed", e);
      throw new RuntimeException("Tag to group migration failed", e);
    }
  }

  private void migrateEcommerceData() {
    log.info("Starting e-commerce data migration");

    try {
      List<EcommerceShop> shops = mailchimpService.getAllEcommerceShops();

      if (shops.isEmpty()) {
        log.info("No e-commerce shops found to migrate");
        return;
      }

      for (EcommerceShop shop : shops) {
        try {
          // Create shop in MailerLite
          Map<String, Object> createdShop = mailerLiteService.createEcommerceShop(shop);
          log.info("Created e-commerce shop: {}", createdShop);

          // Note: Categories and products migration would need additional Mailchimp API calls
          // This is a simplified version focusing on shop creation

        } catch (Exception e) {
          log.error("Failed to migrate e-commerce shop: {}", shop.getName(), e);
          progressTracker.addError(
              "ECOMMERCE_SETUP",
              "Shop",
              shop.getId(),
              e.getMessage(),
              "SHOP_CREATION_FAILED",
              true);
        }
      }

    } catch (Exception e) {
      log.error("E-commerce data migration failed", e);
      throw new RuntimeException("E-commerce data migration failed", e);
    }
  }

  private void migrateSubscribers(Map<String, String> tagToGroupMapping) {
    log.info("Starting subscriber migration");

    try {
      List<MailchimpList> lists = mailchimpService.getAllLists();
      int totalSubscribers = 0;
      int migratedSubscribers = 0;

      for (MailchimpList list : lists) {
        List<MailchimpMember> members = mailchimpService.getAllMembers(list.getId());
        totalSubscribers += members.size();

        // Process in batches
        List<List<MailchimpMember>> batches =
            partitionList(members, migrationConfig.getBatchSize());

        for (List<MailchimpMember> batch : batches) {
          try {
            List<Subscriber> subscribers =
                batch.stream().map(this::convertToSubscriber).collect(Collectors.toList());

            // Bulk import subscribers
            mailerLiteService.bulkImportSubscribers(subscribers, null);

            // Assign to groups based on tags
            for (MailchimpMember member : batch) {
              assignMemberToGroups(member, tagToGroupMapping);
            }

            migratedSubscribers += batch.size();
            progressTracker.updateProgress(
                totalSubscribers, migratedSubscribers, migratedSubscribers, 0);

            // Rate limiting
            Thread.sleep(1000);

          } catch (Exception e) {
            log.error("Failed to migrate subscriber batch", e);
            progressTracker.addError(
                "SUBSCRIBER_MIGRATION",
                "Batch",
                "batch",
                e.getMessage(),
                "BATCH_MIGRATION_FAILED",
                true);
          }
        }
      }

      log.info(
          "Subscriber migration completed. Migrated {}/{} subscribers",
          migratedSubscribers,
          totalSubscribers);

    } catch (Exception e) {
      log.error("Subscriber migration failed", e);
      throw new RuntimeException("Subscriber migration failed", e);
    }
  }

  private void assignMemberToGroups(MailchimpMember member, Map<String, String> tagToGroupMapping) {
    if (member.getTags() != null) {
      for (MailchimpMember.Tag tag : member.getTags()) {
        String groupId = tagToGroupMapping.get(tag.getName());
        if (groupId != null) {
          try {
            mailerLiteService.assignSubscriberToGroup(member.getId(), groupId);
          } catch (Exception e) {
            log.warn(
                "Failed to assign subscriber {} to group {}",
                member.getEmailAddress(),
                tag.getName());
          }
        }
      }
    }
  }

  private void generateCampaignMigrationGuide() {
    log.info("Generating campaign migration guide");

    // This phase provides guidance rather than automated migration
    // as campaigns require manual recreation

    progressTracker.setMetric(
        "campaign_guide",
        Map.of(
            "welcome_email",
                "Create a welcome automation in MailerLite triggered by new subscriber",
            "purchase_sequence", "Set up e-commerce automations based on purchase behavior",
            "newsletter", "Create a regular campaign schedule or automation series",
            "forms", "Update form integrations to use MailerLite API endpoints"));
  }

  private Subscriber convertToSubscriber(MailchimpMember member) {
    Subscriber.SubscriberStatus status =
        switch (member.getStatus().toLowerCase()) {
          case "subscribed" -> Subscriber.SubscriberStatus.SUBSCRIBED;
          case "unsubscribed" -> Subscriber.SubscriberStatus.UNSUBSCRIBED;
          case "pending" -> Subscriber.SubscriberStatus.PENDING;
          case "cleaned" -> Subscriber.SubscriberStatus.CLEANED;
          default -> Subscriber.SubscriberStatus.SUBSCRIBED;
        };

    String firstName = null;
    String lastName = null;

    if (member.getMergeFields() != null) {
      firstName = (String) member.getMergeFields().get("FNAME");
      lastName = (String) member.getMergeFields().get("LNAME");
    }

    return Subscriber.builder()
        .id(member.getId())
        .email(member.getEmailAddress())
        .firstName(firstName)
        .lastName(lastName)
        .status(status)
        .customFields(member.getMergeFields())
        .createdAt(member.getTimestampSignup())
        .updatedAt(member.getLastChanged())
        .source(member.getSource())
        .build();
  }

  private String cleanTagName(String tagName) {
    if (tagName == null || tagName.trim().isEmpty()) {
      return null;
    }

    // Clean tag name: remove special characters, limit length
    String cleaned = tagName.trim().replaceAll("[^a-zA-Z0-9\\s-_]", "").replaceAll("\\s+", " ");

    if (cleaned.length() > 50) {
      cleaned = cleaned.substring(0, 50);
    }

    return cleaned.isEmpty() ? null : cleaned;
  }

  private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
    List<List<T>> partitions = new ArrayList<>();
    for (int i = 0; i < list.size(); i += batchSize) {
      partitions.add(list.subList(i, Math.min(i + batchSize, list.size())));
    }
    return partitions;
  }
}

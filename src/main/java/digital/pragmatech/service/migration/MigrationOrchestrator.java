package digital.pragmatech.service.migration;

import digital.pragmatech.config.MigrationConfig;
import digital.pragmatech.model.common.*;
import digital.pragmatech.model.mailchimp.MailchimpList;
import digital.pragmatech.model.mailchimp.MailchimpMember;
import digital.pragmatech.model.mailerlite.MailerLiteGroup;
import digital.pragmatech.model.mailerlite.MailerLiteOrder;
import digital.pragmatech.model.mailerlite.MailerLiteProduct;
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

            progressTracker.updateProgress(
                tags.size(), processedTags + 1, successfulTags, failedTags);

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

      log.info(
          "Selective tag migration completed. Created {} groups, {} failed",
          successfulTags,
          failedTags);
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

          progressTracker.updateProgress(
              storeIds.size(), processedStores, successfulStores, failedStores);

          // Rate limiting
          Thread.sleep(1000);

        } catch (Exception e) {
          log.error("Failed to migrate store: {}", storeId, e);
          failedStoresList.add(storeId);
          failedStores++;
          processedStores++;

          progressTracker.addError(
              "ECOMMERCE_SETUP", "Store", storeId, e.getMessage(), "STORE_MIGRATION_FAILED", true);
          progressTracker.updateProgress(
              storeIds.size(), processedStores, successfulStores, failedStores);
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

      log.info(
          "Selective store migration completed. Migrated {} stores, {} failed",
          successfulStores,
          failedStores);
      return result;

    } catch (Exception e) {
      log.error("Selective store migration failed", e);
      throw new RuntimeException("Store migration failed", e);
    }
  }

  public Map<String, Object> migrateSubscribers(
      List<String> selectedTags, List<String> selectedStores) {
    log.info(
        "Starting subscriber migration with {} selected tags and {} selected stores",
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
        List<List<MailchimpMember>> batches =
            partitionList(members, migrationConfig.getBatchSize());

        for (List<MailchimpMember> batch : batches) {
          try {
            List<Subscriber> subscribers =
                batch.stream().map(this::convertToSubscriber).collect(Collectors.toList());

            // Create subscribers individually (MailerLite doesn't have global bulk import)
            int batchSuccessCount = 0;
            for (Subscriber subscriber : subscribers) {
              try {
                mailerLiteService.createOrUpdateSubscriber(subscriber);
                batchSuccessCount++;

                // Rate limiting - MailerLite allows 120 requests per minute
                Thread.sleep(500);
              } catch (Exception e) {
                log.warn("Failed to create subscriber: {}", subscriber.getEmail(), e);
              }
            }

            migratedSubscribers += batchSuccessCount;
            progressTracker.updateProgress(
                totalSubscribers,
                migratedSubscribers + failedSubscribers,
                migratedSubscribers,
                failedSubscribers);

          } catch (Exception e) {
            log.error("Failed to migrate subscriber batch", e);
            failedSubscribers += batch.size();
            progressTracker.addError(
                "SUBSCRIBER_MIGRATION",
                "Batch",
                "batch",
                e.getMessage(),
                "BATCH_MIGRATION_FAILED",
                true);
            progressTracker.updateProgress(
                totalSubscribers,
                migratedSubscribers + failedSubscribers,
                migratedSubscribers,
                failedSubscribers);
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

      log.info(
          "Subscriber migration completed. Migrated {}/{} subscribers",
          migratedSubscribers,
          totalSubscribers);
      return result;

    } catch (Exception e) {
      log.error("Subscriber migration failed", e);
      throw new RuntimeException("Subscriber migration failed", e);
    }
  }

  public Map<String, Object> testMigrateSubscribers(
      List<String> selectedTags, List<String> selectedStores, int sampleSize) {
    log.info(
        "Starting test subscriber migration with {} selected tags, {} selected stores, sample size: {}",
        selectedTags != null ? selectedTags.size() : 0,
        selectedStores != null ? selectedStores.size() : 0,
        sampleSize);

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

      // Calculate total available subscribers first (but don't fetch them all)
      int availableSubscribers = 0;
      for (MailchimpList list : lists) {
        availableSubscribers += mailchimpService.getMemberCount(list.getId());
      }

      // Adjust sample size if it's larger than available subscribers
      int actualSampleSize = Math.min(sampleSize, availableSubscribers);
      log.info(
          "Test migration will process {} subscribers out of {} available",
          actualSampleSize,
          availableSubscribers);

      int remaining = actualSampleSize;

      for (MailchimpList list : lists) {
        if (remaining <= 0) {
          break; // We've reached our sample limit
        }

        // Only fetch the number of subscribers we actually need from this list
        int subscribersFromThisList = Math.min(remaining, 100); // Max 100 per list for test
        List<MailchimpMember> members =
            mailchimpService.getLimitedMembers(list.getId(), subscribersFromThisList);

        if (members.isEmpty()) {
          log.debug("No members found in list: {}", list.getName());
          continue;
        }

        processedLists.add(list.getName());
        totalSubscribers += members.size();
        remaining -= members.size();

        log.info(
            "Test migration: fetched {} members from list '{}', remaining to fetch: {}",
            members.size(),
            list.getName(),
            remaining);

        // Process in smaller batches for the test
        List<List<MailchimpMember>> batches =
            partitionList(members, 25); // Smaller batches for test

        for (List<MailchimpMember> batch : batches) {
          try {
            List<Subscriber> subscribers =
                batch.stream().map(this::convertToSubscriber).collect(Collectors.toList());

            // Create subscribers individually (MailerLite doesn't have global bulk import)
            int batchSuccessCount = 0;
            for (Subscriber subscriber : subscribers) {
              try {
                mailerLiteService.createOrUpdateSubscriber(subscriber);
                batchSuccessCount++;

                // Small delay between individual creates to respect rate limits
                Thread.sleep(100);
              } catch (Exception e) {
                log.warn("Failed to create subscriber during test: {}", subscriber.getEmail(), e);
              }
            }

            migratedSubscribers += batchSuccessCount;
            progressTracker.updateProgress(
                actualSampleSize,
                migratedSubscribers + failedSubscribers,
                migratedSubscribers,
                failedSubscribers);

            log.debug(
                "Test migration: migrated batch of {}/{} subscribers, total migrated: {}",
                batchSuccessCount,
                batch.size(),
                migratedSubscribers);

          } catch (Exception e) {
            log.error("Failed to migrate test subscriber batch", e);
            failedSubscribers += batch.size();
            progressTracker.addError(
                "TEST_SUBSCRIBER_MIGRATION",
                "Batch",
                "test_batch",
                e.getMessage(),
                "TEST_BATCH_MIGRATION_FAILED",
                true);
            progressTracker.updateProgress(
                actualSampleSize,
                migratedSubscribers + failedSubscribers,
                migratedSubscribers,
                failedSubscribers);
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
      result.put("sampleSize", actualSampleSize);
      result.put("availableSubscribers", availableSubscribers);
      result.put("isTestMigration", true);
      result.put("fetchedSubscribers", totalSubscribers);
      result.put("optimizedFetch", true);
      result.put("completedAt", LocalDateTime.now());

      // Store result for UI access
      progressTracker.setStepResult("test_subscriber_migration", result);

      log.info(
          "Test subscriber migration completed. Migrated {}/{} subscribers (sample size: {})",
          migratedSubscribers,
          totalSubscribers,
          actualSampleSize);
      return result;

    } catch (Exception e) {
      log.error("Test subscriber migration failed", e);
      throw new RuntimeException("Test subscriber migration failed", e);
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
      Map<String, Integer> storeOrderCounts = new HashMap<>();

      // First, get all MailerLite shops that were created
      List<EcommerceShop> mailchimpStores = mailchimpService.getAllEcommerceShops();
      Map<String, String> mailchimpToMailerLiteShopMapping = new HashMap<>();

      // Create mapping of Mailchimp store IDs to MailerLite shop IDs
      // For now, we'll use the store name to match since we don't persist the mapping
      // In production, you'd want to store this mapping during store migration
      for (String storeId : selectedStores) {
        EcommerceShop mailchimpStore =
            mailchimpStores.stream()
                .filter(s -> s.getId().equals(storeId))
                .findFirst()
                .orElse(null);

        if (mailchimpStore != null) {
          // Since we don't have the actual MailerLite shop ID stored,
          // we'll use a placeholder approach. In production, you'd retrieve this from DB
          mailchimpToMailerLiteShopMapping.put(storeId, "ml_" + storeId);
        }
      }

      for (String storeId : selectedStores) {
        try {
          String mailerLiteShopId = mailchimpToMailerLiteShopMapping.get(storeId);
          if (mailerLiteShopId == null) {
            log.warn("No MailerLite shop found for Mailchimp store {}", storeId);
            failedStoresList.add(storeId);
            continue;
          }

          // Get orders from Mailchimp
          List<Order> orders = mailchimpService.getStoreOrders(storeId);
          int storeOrderCount = orders.size();
          totalOrders += storeOrderCount;
          storeOrderCounts.put(storeId, storeOrderCount);

          log.info("Found {} orders to sync for store {}", storeOrderCount, storeId);

          // Sync each order to MailerLite
          int storeSyncedCount = 0;
          int storeFailedCount = 0;

          for (Order order : orders) {
            try {
              mailerLiteService.syncOrder(mailerLiteShopId, order);
              syncedOrders++;
              storeSyncedCount++;

              progressTracker.updateProgress(
                  totalOrders, syncedOrders + failedOrders, syncedOrders, failedOrders);

            } catch (Exception e) {
              log.error("Failed to sync order {} for store {}", order.getId(), storeId, e);
              failedOrders++;
              storeFailedCount++;

              progressTracker.addError(
                  "ORDER_SYNC", "Order", order.getId(), e.getMessage(), "ORDER_SYNC_FAILED", false);
            }
          }

          processedStores.add(storeId);
          log.info(
              "Store {} order sync completed. Synced: {}, Failed: {}",
              storeId,
              storeSyncedCount,
              storeFailedCount);

        } catch (Exception e) {
          log.error("Failed to sync orders for store: {}", storeId, e);
          failedStoresList.add(storeId);

          progressTracker.addError(
              "ORDER_SYNC", "Store", storeId, e.getMessage(), "STORE_ORDER_SYNC_FAILED", true);
        }
      }

      // Create result summary
      Map<String, Object> result = new HashMap<>();
      result.put("totalStores", selectedStores.size());
      result.put("processedStores", processedStores.size());
      result.put("failedStores", failedStoresList.size());
      result.put("totalOrders", totalOrders);
      result.put("syncedOrders", syncedOrders);
      result.put("failedOrders", failedOrders);
      result.put("processedStoresList", processedStores);
      result.put("failedStoresList", failedStoresList);
      result.put("storeOrderCounts", storeOrderCounts);
      result.put("completedAt", LocalDateTime.now());

      // Store result for UI access
      progressTracker.setStepResult("order_sync", result);

      log.info("Order sync completed. Synced {}/{} orders", syncedOrders, totalOrders);
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

          @SuppressWarnings("unchecked")
          Map<String, Object> shopData = (Map<String, Object>) createdShop.get("data");
          String mailerLiteShopId = (String) shopData.get("id");

          // Migrate products for this shop
          migrateProducts(shop.getId(), mailerLiteShopId);

          // Migrate orders for this shop
          migrateOrders(shop.getId(), mailerLiteShopId);

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

  private void migrateProducts(String mailchimpStoreId, String mailerLiteShopId) {
    log.info("Starting product migration for store {} -> {}", mailchimpStoreId, mailerLiteShopId);

    try {
      List<Product> products = mailchimpService.getStoreProducts(mailchimpStoreId);

      if (products.isEmpty()) {
        log.info("No products found in Mailchimp store {}", mailchimpStoreId);
        return;
      }

      log.info("Found {} products to migrate", products.size());
      progressTracker.updatePhase(MigrationStatus.MigrationPhase.ECOMMERCE_SETUP);

      int migratedCount = 0;
      int failedCount = 0;

      for (Product product : products) {
        try {
          MailerLiteProduct createdProduct =
              mailerLiteService.createProduct(mailerLiteShopId, product);
          log.debug(
              "Successfully migrated product: {} -> {}", product.getName(), createdProduct.getId());
          migratedCount++;

          progressTracker.updateProgress(
              products.size(), migratedCount, migratedCount, failedCount);

        } catch (Exception e) {
          log.error("Failed to migrate product: {}", product.getName(), e);
          failedCount++;
          progressTracker.addError(
              "PRODUCT_MIGRATION",
              "Product",
              product.getId(),
              e.getMessage(),
              "PRODUCT_CREATION_FAILED",
              false);
        }
      }

      log.info("Product migration completed. Migrated: {}, Failed: {}", migratedCount, failedCount);

    } catch (Exception e) {
      log.error("Product migration failed for store {}", mailchimpStoreId, e);
      progressTracker.addError(
          "PRODUCT_MIGRATION",
          "Store",
          mailchimpStoreId,
          e.getMessage(),
          "PRODUCT_FETCH_FAILED",
          true);
    }
  }

  private void migrateOrders(String mailchimpStoreId, String mailerLiteShopId) {
    log.info("Starting order migration for store {} -> {}", mailchimpStoreId, mailerLiteShopId);

    try {
      List<Order> orders = mailchimpService.getStoreOrders(mailchimpStoreId);

      if (orders.isEmpty()) {
        log.info("No orders found in Mailchimp store {}", mailchimpStoreId);
        return;
      }

      log.info("Found {} orders to migrate", orders.size());
      progressTracker.updatePhase(MigrationStatus.MigrationPhase.ECOMMERCE_SETUP);

      int syncedCount = 0;
      int failedCount = 0;

      // Process orders in batches to avoid overwhelming the API
      List<List<Order>> batches = partitionList(orders, 50);

      for (List<Order> batch : batches) {
        for (Order order : batch) {
          try {
            MailerLiteOrder syncedOrder = mailerLiteService.syncOrder(mailerLiteShopId, order);
            log.debug("Successfully synced order: {} -> {}", order.getId(), syncedOrder.getId());
            syncedCount++;

            progressTracker.updateProgress(orders.size(), syncedCount, syncedCount, failedCount);

          } catch (Exception e) {
            log.error("Failed to sync order: {}", order.getId(), e);
            failedCount++;
            progressTracker.addError(
                "ORDER_MIGRATION",
                "Order",
                order.getId(),
                e.getMessage(),
                "ORDER_SYNC_FAILED",
                false);
          }
        }

        // Rate limiting between batches
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }

      log.info("Order migration completed. Synced: {}, Failed: {}", syncedCount, failedCount);

    } catch (Exception e) {
      log.error("Order migration failed for store {}", mailchimpStoreId, e);
      progressTracker.addError(
          "ORDER_MIGRATION", "Store", mailchimpStoreId, e.getMessage(), "ORDER_FETCH_FAILED", true);
    }
  }

  private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
    List<List<T>> partitions = new ArrayList<>();
    for (int i = 0; i < list.size(); i += batchSize) {
      partitions.add(list.subList(i, Math.min(i + batchSize, list.size())));
    }
    return partitions;
  }
}

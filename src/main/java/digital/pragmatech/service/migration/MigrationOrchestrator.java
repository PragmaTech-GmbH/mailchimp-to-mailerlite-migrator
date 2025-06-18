package digital.pragmatech.service.migration;

import digital.pragmatech.config.MigrationConfig;
import digital.pragmatech.model.common.*;
import digital.pragmatech.model.mailchimp.MailchimpList;
import digital.pragmatech.model.mailchimp.MailchimpMember;
import digital.pragmatech.model.mailerlite.MailerLiteGroup;
import digital.pragmatech.service.mailchimp.MailchimpService;
import digital.pragmatech.service.mailerlite.MailerLiteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MigrationOrchestrator {
    
    private final MailchimpService mailchimpService;
    private final MailerLiteService mailerLiteService;
    private final MigrationProgressTracker progressTracker;
    private final MigrationConfig migrationConfig;
    
    @Async
    public CompletableFuture<String> startMigration() {
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
            
            return CompletableFuture.completedFuture(migrationId);
            
        } catch (Exception e) {
            log.error("Migration {} failed", migrationId, e);
            progressTracker.failMigration("Migration failed: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
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
            Set<String> cleanedTags = allTags.stream()
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
                    progressTracker.addError("TAG_GROUP_MIGRATION", "Tag", tag, 
                            e.getMessage(), "GROUP_CREATION_FAILED", true);
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
                    progressTracker.addError("ECOMMERCE_SETUP", "Shop", shop.getId(),
                            e.getMessage(), "SHOP_CREATION_FAILED", true);
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
                List<List<MailchimpMember>> batches = partitionList(members, migrationConfig.getBatchSize());
                
                for (List<MailchimpMember> batch : batches) {
                    try {
                        List<Subscriber> subscribers = batch.stream()
                                .map(this::convertToSubscriber)
                                .collect(Collectors.toList());
                        
                        // Bulk import subscribers
                        mailerLiteService.bulkImportSubscribers(subscribers, null);
                        
                        // Assign to groups based on tags
                        for (MailchimpMember member : batch) {
                            assignMemberToGroups(member, tagToGroupMapping);
                        }
                        
                        migratedSubscribers += batch.size();
                        progressTracker.updateProgress(totalSubscribers, migratedSubscribers, migratedSubscribers, 0);
                        
                        // Rate limiting
                        Thread.sleep(1000);
                        
                    } catch (Exception e) {
                        log.error("Failed to migrate subscriber batch", e);
                        progressTracker.addError("SUBSCRIBER_MIGRATION", "Batch", "batch", 
                                e.getMessage(), "BATCH_MIGRATION_FAILED", true);
                    }
                }
            }
            
            log.info("Subscriber migration completed. Migrated {}/{} subscribers", 
                    migratedSubscribers, totalSubscribers);
            
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
                        log.warn("Failed to assign subscriber {} to group {}", member.getEmailAddress(), tag.getName());
                    }
                }
            }
        }
    }
    
    private void generateCampaignMigrationGuide() {
        log.info("Generating campaign migration guide");
        
        // This phase provides guidance rather than automated migration
        // as campaigns require manual recreation
        
        progressTracker.setMetric("campaign_guide", Map.of(
                "welcome_email", "Create a welcome automation in MailerLite triggered by new subscriber",
                "purchase_sequence", "Set up e-commerce automations based on purchase behavior",
                "newsletter", "Create a regular campaign schedule or automation series",
                "forms", "Update form integrations to use MailerLite API endpoints"
        ));
    }
    
    private Subscriber convertToSubscriber(MailchimpMember member) {
        Subscriber.SubscriberStatus status = switch (member.getStatus().toLowerCase()) {
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
        String cleaned = tagName.trim()
                .replaceAll("[^a-zA-Z0-9\\s-_]", "")
                .replaceAll("\\s+", " ");
        
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